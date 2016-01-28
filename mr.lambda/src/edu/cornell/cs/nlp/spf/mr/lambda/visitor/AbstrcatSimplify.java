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

import java.util.Arrays;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.IPredicateSimplifier;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType;

/**
 * This class holds simplification code shared by {@link ApplyAndSimplify} and
 * {@link Simplify}.
 *
 * @author Yoav Artzi
 */
public abstract class AbstrcatSimplify implements ILogicalExpressionVisitor {

	private final boolean		stripLambda;
	/**
	 * Variable to temporary store the return value of visits as we traverse the
	 * expression.
	 */
	protected LogicalExpression	result	= null;

	protected AbstrcatSimplify(boolean stripLambdas) {
		this.stripLambda = stripLambdas;
	}

	private static boolean shouldConsumeArgs(LogicalExpression newPred) {
		return newPred.getType().isComplex()
				&& !(newPred instanceof LogicalConstant)
				&& !(newPred instanceof Variable);
	}

	/**
	 * Try to fold the lambda operator. Handles the case where the lambda
	 * operator is redundant. For example: (lambda $0:e (foo:<e,<e,t>> dada:e
	 * $0)) --> (foo:<e,<e,t>> dada:e)
	 *
	 * @param lambdaVariable
	 * @param lambdaBody
	 * @return 'null' if can't remove the Lambda operator, else return the
	 *         modified body, which replaces the entire Lambda expression.
	 */
	private static LogicalExpression stripRedundantLambda(Variable lambdaArg,
			LogicalExpression lambdaBody) {
		if (!(lambdaBody instanceof Literal)) {
			// Only remove Lambda operators when the body is a single literal
			return null;
		}
		final Literal literal = (Literal) lambdaBody;

		// Check if we can fold the lambda operators.
		final int len = literal.numArgs();
		if (!(literal.getPredicateType() instanceof RecursiveComplexType)
				&& literal.getArg(len - 1) == lambdaArg) {
			// Verify that the variable is not used in any other place in
			// the expression (except as the last argument in the literal)
			boolean usedElsewehre = IsContainingVariable.of(
					literal.getPredicate(), lambdaArg);
			if (!usedElsewehre) {
				for (int i = 0; i < len - 1 && !usedElsewehre; ++i) {
					usedElsewehre |= IsContainingVariable.of(literal.getArg(i),
							lambdaArg);
				}
			}

			if (usedElsewehre) {
				return null;
			} else if (len == 1) {
				return literal.getPredicate();
			} else {
				return new Literal(literal.getPredicate(),
						literal.argumentCopy(0, len - 1));
			}
		} else {
			return null;
		}
	}

	@Override
	public void visit(Lambda lambda) {

		// No need to visit the argument.

		lambda.getBody().accept(this);
		if (result == null) {
			return;
		}
		final LogicalExpression newBody = result;

		// Try to fold the Lambda operator
		if (stripLambda) {
			final LogicalExpression lambdaStripped = stripRedundantLambda(
					lambda.getArgument(), newBody);
			if (lambdaStripped != null) {
				// Case we successfully removed the lambda operator, return the
				// modified content
				result = lambdaStripped;
				return;
			}
		}

		if (newBody == lambda.getBody()) {
			result = lambda;
		} else {
			result = new Lambda(lambda.getArgument(), newBody);
		}
	}

	@Override
	public void visit(Literal literal) {
		// Visit the predicate. We have to do this, to make sure that
		// the predicate has a lambda form for later applications.
		literal.getPredicate().accept(this);
		final LogicalExpression simplifiedPred = result;

		// Visit the arguments. This block tries to re-use objects as much
		// as possible, including the actual immutable list that holds the
		// arguments.
		final int len = literal.numArgs();
		final LogicalExpression[] simplifiedArgs = new LogicalExpression[len];
		boolean argsChanged = false;
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);
			arg.accept(this);
			argsChanged |= result != arg;
			simplifiedArgs[i] = result;
		}

		// At this point the predicate and arguments are all simplified, so we
		// can try to simplify further by consuming arguments

		// The final list of arguments after simplification and/or consumption
		// (see below)
		final LogicalExpression[] finalArguments;
		LogicalExpression newPred = simplifiedPred;

		// If the predicate is a lambda expression, consume as many arguments as
		// possible. Basically simplifying in the case of an functional
		// application.
		if (shouldConsumeArgs(newPred)) {
			boolean changeDueToLambdaApplication = false;
			int i = 0;
			while (shouldConsumeArgs(newPred) && i < simplifiedArgs.length) {
				final LogicalExpression applyResult = ApplyAndSimplify
						.ofUnsafe(newPred, simplifiedArgs[i]);
				if (applyResult == null) {
					// Application failed, so stop consuming.
					break;
				} else {
					++i;
					newPred = applyResult;
					changeDueToLambdaApplication = true;
				}
			}

			// Need to update the arguments and simplify the new predicate, if
			// it actually consumed arguments
			if (changeDueToLambdaApplication) {
				// The updated arguments list includes the remaining arguments.
				finalArguments = Arrays.copyOfRange(simplifiedArgs, i,
						simplifiedArgs.length);
				argsChanged = true;
			} else {
				finalArguments = simplifiedArgs;
			}
		} else {
			finalArguments = simplifiedArgs;
		}

		final LogicalExpression newExp;

		if (newPred != literal.getPredicate() || argsChanged) {
			// Something changed, so we need to create a new literal
			if (finalArguments.length == 0) {
				// No arguments left, the predicate is the new expression
				newExp = newPred;
			} else if (argsChanged) {
				// Create a new literal. The arguments are re-used, if possible,
				// by the previous code, so no need to do any comparison here.
				newExp = new Literal(newPred, finalArguments);
			} else {
				// Predicate changed, but arguments are the same, so copy them
				// from the original literal.
				newExp = new Literal(newPred, literal);
			}
		} else {
			// Case neither the predicate nor the arguments changed, so just
			// continue with the same literal
			newExp = literal;
		}

		// Load the updated expression into the temporary return member and
		// continue to try to do predicate specific simplification
		result = newExp;

		// Predicate specific simplification. This part is only relevant if we
		// have arguments.
		if (finalArguments.length != 0) {
			final IPredicateSimplifier simplifier = LogicLanguageServices
					.getSimplifier(newPred);
			if (simplifier != null) {
				final LogicalExpression simplifiedExp = simplifier
						.simplify(newExp);
				if (simplifiedExp != newExp) {
					// Update the return expression if changed
					result = simplifiedExp;
					return;
				}
			}
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		result = logicalConstant;
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

}
