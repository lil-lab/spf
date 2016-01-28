/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMappingOverlay;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType;
import edu.cornell.cs.nlp.utils.collections.ArrayUtils;
import edu.cornell.cs.nlp.utils.collections.stackmap.IdentityFastStackMap;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Given a function and the application result, will try to guess the
 * application argument. This is a greedy process and easily fails. See the
 * related unit test for common fail cases.
 *
 * @author Yoav Artzi
 */
public class GetApplicationArgument implements ILogicalExpressionVisitor {

	public static final ILogger						LOG			= LoggerFactory
			.create(GetApplicationArgument.class);
	private final Variable							applicationArgument;
	private LogicalExpression						applicationResult;
	private LogicalExpression						argument	= null;

	private boolean									isValid		= true;

	private final ScopeMapping<Variable, Variable>	scope		= new ScopeMapping<Variable, Variable>(
			new IdentityFastStackMap<Variable, Variable>(),
			new IdentityFastStackMap<Variable, Variable>());

	public GetApplicationArgument(LogicalExpression result,
			Variable applicationArgument) {
		this.applicationResult = result;
		this.applicationArgument = applicationArgument;

	}

	public static LogicalExpression of(LogicalExpression function,
			LogicalExpression result) {
		// This process relies heavily on how the logical forms look like, so we
		// normalize them in advance by wrapping with lambda terms.
		final LogicalExpression wrappedFunction = LambdaWrapped.of(function);
		final LogicalExpression wrappedResult = LambdaWrapped.of(result);

		if (!(wrappedFunction instanceof Lambda)) {
			return null;
		}

		final Lambda functionLambda = (Lambda) wrappedFunction;
		final Variable applicationArgument = functionLambda.getArgument();

		// Traverse the function while tracking the result, every time a
		// difference is encountered try to extract the argument from there.
		// There should be a single argument and order insensitive and recursive
		// literals get special treatment.
		final GetApplicationArgument visitor = new GetApplicationArgument(
				wrappedResult, applicationArgument);
		visitor.visit(functionLambda.getBody());

		assert !visitor.isValid || visitor.argument == null
				|| wrappedResult.equals(ApplyAndSimplify.of(wrappedFunction,
						visitor.argument)) : String.format(
								"Application with generated result failed: function=%s result=%s argument=%s",
								function, result, visitor.argument);
		return visitor.isValid ? visitor.argument : null;
	}

	/**
	 * Given a candidate sub expression, the application variable and a the
	 * potentially equivalent sub-expression before the application, try to
	 * return the application argument.
	 *
	 * @author Yoav Artzi
	 */
	static LogicalExpression createArgument(LogicalExpression resultSubExp,
			LogicalExpression functionSubExpSource,
			Variable applicationArgument,
			ScopeMapping<Variable, Variable> scope) {
		// If the function subexp is a literal, the application argument must be
		// the predicate.
		if (functionSubExpSource instanceof Literal
				&& ((Literal) functionSubExpSource).getPredicate()
						.equals(applicationArgument)) {
			final Literal sourceLiteral = (Literal) functionSubExpSource;
			final int len = sourceLiteral.numArgs();
			// Iterate over the arguments of the source literal, for each of
			// them, replace the corresponding items in the result with a new
			// variable.
			LogicalExpression currentArgument = resultSubExp;
			final Stack<Variable> newVariables = new Stack<Variable>();
			// Verify that each source argument is seen only once. Repeating
			// source arguments cause ambiguity and we prefer to fail in such
			// cases.
			final Set<LogicalExpression> sourceArgs = new HashSet<LogicalExpression>();
			for (int i = 0; i < len; ++i) {
				final LogicalExpression sourceArg = sourceLiteral.getArg(i);
				if (!sourceArgs.add(sourceArg)) {
					return null;
				}
				final Variable newVariable = new Variable(
						LogicLanguageServices.getTypeRepository()
								.generalizeType(sourceArg.getType()));
				newVariables.push(newVariable);
				final LogicalExpression preReplace = currentArgument;

				// Replace the sub-expression that is coming from the function
				// with the newly created variable.
				if (sourceArg instanceof Variable
						&& scope.containsKey((Variable) sourceArg)) {
					// Case the argument is simply a variable. This just
					// replaces one variable for another. The new variable will
					// later be bind.
					currentArgument = ReplaceExpression.of(currentArgument,
							scope.peek((Variable) sourceArg), newVariable);
				} else {
					currentArgument = ReplaceExpression.of(currentArgument,
							sourceArg, newVariable);
				}

				// Try to strip sourceArg from lambda terms and do the
				// replacement. This is necessary for cases where in the source
				// it's used as an argument, but in the target appears as a
				// predicate. The reason for that is that logical expressions in
				// the predicate positions are normalized differently (i.e.,
				// stripped and wrapped with lambda terms differently than in
				// other positions).
				final LogicalExpression strippedSourceArg = Simplify
						.of(sourceArg, true);
				if (strippedSourceArg != sourceArg) {
					if (strippedSourceArg instanceof Variable && scope
							.containsKey((Variable) strippedSourceArg)) {
						currentArgument = ReplaceExpression.of(currentArgument,
								scope.peek((Variable) strippedSourceArg),
								newVariable);
					} else {
						currentArgument = ReplaceExpression.of(currentArgument,
								strippedSourceArg, newVariable);
					}
				}
				// Verify the logical form changed due to replacement. This
				// means the replaced sub expression was present. If it wasn't
				// present, this extraction should fail. We can efficiently
				// check this using instance comparison.
				if (preReplace == currentArgument) {
					return null;
				}
			}
			while (!newVariables.isEmpty()) {
				currentArgument = new Lambda(newVariables.pop(),
						currentArgument);
			}

			// Verify the argument doesn't contain any variables that are mapped
			// by the scoping table. This indicates that the argument carries
			// variables that are bound by the function and therefore is not a
			// valid argument.
			for (final Variable freeVariable : currentArgument
					.getFreeVariables()) {
				if (scope.containsValue(freeVariable)) {
					return null;
				}
			}

			if (LogicLanguageServices.getTypeComparator().verifyArgType(
					applicationArgument.getType(), currentArgument.getType())) {
				return LambdaWrapped.of(currentArgument);
			}
		} else if (functionSubExpSource.equals(applicationArgument)) {
			// Case the source is a variable and it's equal to the application
			// argument.
			if (LogicLanguageServices.getTypeComparator().verifyArgType(
					applicationArgument.getType(), resultSubExp.getType())) {
				return LambdaWrapped.of(resultSubExp);
			}
		}

		return null;
	}

	@Override
	public void visit(Lambda lambda) {
		if (!(applicationResult instanceof Lambda) || !lambda.getType()
				.isExtendingOrExtendedBy(applicationResult.getType())) {
			isValid = false;
			return;
		}

		if (!lambda.containsFreeVariable(applicationArgument)) {
			isValid = lambda.equals(applicationResult, scope);
			return;
		}

		if (!lambda.getArgument().getType()
				.equals(((Lambda) applicationResult).getArgument().getType())) {
			isValid = false;
			return;
		}

		scope.push(lambda.getArgument(),
				((Lambda) applicationResult).getArgument());
		applicationResult = ((Lambda) applicationResult).getBody();
		lambda.getBody().accept(this);
		scope.pop(lambda.getArgument());
	}

	@Override
	public void visit(Literal literal) {
		if (!(applicationResult instanceof Literal) || !literal.getType()
				.isExtendingOrExtendedBy(applicationResult.getType())) {
			isValid = false;
			return;
		}

		if (!literal.containsFreeVariable(applicationArgument)) {
			isValid = literal.equals(applicationResult, scope);
			return;
		}

		final Literal resultLiteral = (Literal) applicationResult;
		final int len = literal.numArgs();

		if (literal.getPredicateType() instanceof RecursiveComplexType
				&& resultLiteral
						.getPredicateType() instanceof RecursiveComplexType
				&& literal.getPredicateType()
						.isOrderSensitive() == resultLiteral.getPredicateType()
								.isOrderSensitive()) {
			if (literal.getPredicateType().isOrderSensitive()) {
				LOG.error(
						"Recrusive order-sensitive predicate support is not implemented");
				isValid = false;
				// This placeholder assertion is meant to throw an exception if
				// running with assertion. In practice, when the logical form
				// includes a order sensitive predicate, getting the application
				// argument will simply fail. We will implement this in the
				// future as needed.
				assert false;
				return;
			} else {
				// Case recursive order insensitive literal.
				applicationResult = resultLiteral.getPredicate();
				literal.getPredicate().accept(this);

				// Track what result arguments were processed successfully, so
				// they can be ignored later.
				final int resultLen = resultLiteral.numArgs();
				final int[] resultArgIndices = ArrayUtils.range(resultLen);

				// Track what function arguments were processed successfully, so
				// they can be ignored later.
				final int[] argIndices = ArrayUtils.range(len);

				// Try to match each of the arguments using equals(). This is a
				// greedy process and therefore might not be exact (leading to
				// false negatives).
				for (int i = 0; i < resultLen; ++i) {
					final LogicalExpression resultArg = resultLiteral.getArg(i);
					for (final int sourceArgIndex : argIndices) {
						if (sourceArgIndex != -1) {
							final LogicalExpression arg = literal
									.getArg(sourceArgIndex);
							if (!arg.containsFreeVariable(
									applicationArgument)) {
								// Create the scope overlap. This object enables
								// the comparison by aligning the variables from
								// the logical expressions.
								final ScopeMappingOverlay<Variable, Variable> scopeOverlay = new ScopeMappingOverlay<Variable, Variable>(
										scope,
										new IdentityFastStackMap<Variable, Variable>(),
										new IdentityFastStackMap<Variable, Variable>());
								if (arg.equals(resultArg, scopeOverlay)) {
									resultArgIndices[i] = -1;
									argIndices[sourceArgIndex] = -1;
									scopeOverlay.applyToBase();
								}
							}
						}
					}
				}

				// Get all leftovers from the result.
				final List<LogicalExpression> resultLeftovers = new ArrayList<LogicalExpression>(
						resultLen);
				for (final int resultArgIndex : resultArgIndices) {
					if (resultArgIndex != -1) {
						resultLeftovers
								.add(resultLiteral.getArg(resultArgIndex));
					}
				}

				// Get all leftovers from the function literal.
				final List<LogicalExpression> leftovers = new ArrayList<LogicalExpression>(
						len);
				for (final int argIndex : argIndices) {
					if (argIndex != -1) {
						leftovers.add(literal.getArg(argIndex));
					}
				}

				// Case no leftovers.
				if (resultLeftovers.isEmpty() || leftovers.isEmpty()) {
					isValid = resultLeftovers.isEmpty() && leftovers.isEmpty();
					return;
				}

				// TODO There's a bug here with free variables. This can be
				// checked in the above loops.

				if (leftovers.size() == 1) {
					// Case we have a single leftover argument from the
					// function. Create the result literal from what remains,
					// and process it with what remains from the function
					// literal.
					final LogicalExpression leftover = leftovers.get(0);
					// The remaining argument must contain the free variable to
					// allow it to match.
					if (!leftover.containsFreeVariable(applicationArgument)) {
						isValid = false;
						return;
					} else {
						applicationResult = resultLeftovers.size() == 1
								? resultLeftovers.get(0)
								: new Literal(resultLiteral.getPredicate(),
										resultLeftovers.toArray(
												new LogicalExpression[resultLeftovers
														.size()]));
						leftover.accept(this);
					}
				} else if (resultLeftovers.size() > 1) {
					// Create a new literal from the leftovers. Create a literal
					// from the result leftovers (if we have only 1, fail).
					// Process them both as conventional literals. This might
					// fail due to ordering, but we try to do our best.
					visitConventionalLiteral(
							new Literal(literal.getPredicate(),
									leftovers.toArray(
											new LogicalExpression[leftovers
													.size()])),
							new Literal(resultLiteral.getPredicate(),
									resultLeftovers.toArray(
											new LogicalExpression[resultLeftovers
													.size()])));
				} else {
					// Fail.
					isValid = false;
					return;
				}
			}
		} else {
			visitConventionalLiteral(literal, resultLiteral);
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		isValid = logicalConstant.equals(applicationResult);
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (variable instanceof SkolemId) {
			variable.equals(variable, scope);
		} else if (variable.equals(applicationArgument)) {
			final LogicalExpression newArgument = createArgument(
					applicationResult, variable, variable, scope);
			if (argument == null) {
				argument = newArgument;
				isValid = true;
			} else if (!argument.equals(newArgument)) {
				isValid = false;
			}
		} else {
			isValid = variable.equals(applicationResult, scope);
		}
	}

	private void visitConventionalLiteral(Literal sourceLiteral,
			final Literal resultLiteral) {
		final int len = sourceLiteral.numArgs();
		final boolean noPriorArgument = argument == null;
		if (len == resultLiteral.numArgs()) {
			applicationResult = resultLiteral.getPredicate();
			sourceLiteral.getPredicate().accept(this);
			if (isValid) {
				for (int i = 0; i < len; ++i) {
					applicationResult = resultLiteral.getArg(i);
					sourceLiteral.getArg(i).accept(this);
					if (!isValid) {
						break;
					}

				}
			}
		} else {
			isValid = false;
		}

		if (!isValid) {
			final LogicalExpression newArgument = createArgument(resultLiteral,
					sourceLiteral, applicationArgument, scope);
			if (newArgument != null) {
				if (noPriorArgument) {
					argument = newArgument;
					isValid = true;
				} else if (!argument.equals(newArgument)) {
					isValid = false;
				}
			}
		}
	}

}
