/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
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
 ******************************************************************************/
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.IPredicateSimplifier;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType;
import edu.uw.cs.utils.collections.CollectionUtils;

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
	protected LogicalExpression	tempReturn	= null;
	
	protected AbstrcatSimplify(boolean stripLambdas) {
		this.stripLambda = stripLambdas;
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
		final List<LogicalExpression> args = literal.getArguments();
		if (!(literal.getPredicateType() instanceof RecursiveComplexType)
				&& args.get(args.size() - 1) == lambdaArg) {
			// Verify that the variable is not used in any other place in
			// the expression (except as the last argument in the literal)
			boolean usedElsewehre = IsContainingVariable.of(
					literal.getPredicate(), lambdaArg);
			if (!usedElsewehre) {
				for (int i = 0; i < args.size() - 1 && !usedElsewehre; ++i) {
					usedElsewehre |= IsContainingVariable.of(args.get(i),
							lambdaArg);
				}
			}
			
			if (usedElsewehre) {
				return null;
			} else if (args.size() == 1) {
				return literal.getPredicate();
			} else {
				return new Literal(literal.getPredicate(),
						CollectionUtils.subList(args, 0, args.size() - 1));
			}
		} else {
			return null;
		}
	}
	
	@Override
	public void visit(Lambda lambda) {
		
		lambda.getArgument().accept(this);
		if (tempReturn == null) {
			return;
		}
		final LogicalExpression newArg = tempReturn;
		lambda.getBody().accept(this);
		if (tempReturn == null) {
			return;
		}
		final LogicalExpression newBody = tempReturn;
		
		// Try to fold the Lambda operator
		if (stripLambda && newArg instanceof Variable) {
			final LogicalExpression lambdaStripped = stripRedundantLambda(
					(Variable) newArg, newBody);
			if (lambdaStripped != null) {
				// Case we successfully removed the lambda operator, return the
				// modified content
				tempReturn = lambdaStripped;
				return;
			}
		}
		
		if (newBody == lambda.getBody() && newArg == lambda.getArgument()) {
			tempReturn = lambda;
		} else {
			// Need to check that the new argument is actually a variable,
			// to avoid a runtime exception
			if (newArg instanceof Variable) {
				tempReturn = new Lambda((Variable) newArg, newBody);
			} else {
				// Case we don't have a legal expression, just return null
				tempReturn = null;
			}
		}
	}
	
	@Override
	public final void visit(Literal literal) {
		// Visit the predicate. We have to do this, to make sure that
		// the predicate has a lambda form for later applications.
		literal.getPredicate().accept(this);
		final LogicalExpression simplifiedPred = tempReturn;
		
		// Visit the arguments. This block tries to re-use objects as much
		// as possible, including the actual immutable list that holds the
		// arguments.
		final List<LogicalExpression> simplifiedArgs;
		final List<LogicalExpression> newArgs = new LinkedList<LogicalExpression>();
		final ListIterator<? extends LogicalExpression> iteratorForVisiting = literal
				.getArguments().listIterator();
		boolean argsChanged = false;
		while (iteratorForVisiting.hasNext()) {
			final LogicalExpression arg = iteratorForVisiting.next();
			arg.accept(this);
			argsChanged |= tempReturn != arg;
			newArgs.add(tempReturn);
		}
		if (argsChanged) {
			simplifiedArgs = newArgs;
		} else {
			simplifiedArgs = literal.getArguments();
		}
		
		// At this point the predicate and arguments are all simplified, so we
		// can try to simplify further by consuming arguments
		
		// The final list of arguments after simplification and/or consumption
		// (see below)
		final List<LogicalExpression> finalArguments;
		LogicalExpression newPred = simplifiedPred;
		
		// If the predicate is a lambda expression, consume as many arguments as
		// possible. Basically simplifying in the case of an functional
		// application.
		if (shouldConsumeArgs(newPred)) {
			boolean changeDueToLambdaApplication = false;
			final ListIterator<? extends LogicalExpression> argsIterator = simplifiedArgs
					.listIterator();
			while (shouldConsumeArgs(newPred) && argsIterator.hasNext()) {
				final LogicalExpression applyResult = ApplyAndSimplify.of(
						newPred, argsIterator.next());
				if (applyResult == null) {
					// Application failed, so stop consuming. Reverse one
					// arguments, so we can add it later to the new list of
					// arguments.
					argsIterator.previous();
					break;
				} else {
					newPred = applyResult;
					changeDueToLambdaApplication = true;
				}
			}
			
			// Need to update the arguments and simplify the new predicate, if
			// it actually consumed arguments
			if (changeDueToLambdaApplication) {
				// The updated arguments list includes the remaining arguments
				finalArguments = CollectionUtils.subList(simplifiedArgs,
						argsIterator.nextIndex(), simplifiedArgs.size());
			} else {
				finalArguments = simplifiedArgs;
			}
		} else {
			finalArguments = simplifiedArgs;
		}
		
		final LogicalExpression newExp;
		
		if (newPred != literal.getPredicate()
				|| finalArguments != literal.getArguments()) {
			// Something changed, so we need to create a new literal
			if (finalArguments.isEmpty()) {
				// No arguments left, the predicate is the new expression
				newExp = newPred;
			} else {
				// Create a new literal. The arguments are re-used, if possible,
				// by the previous code, so need to do any comparison here.
				newExp = new Literal(newPred, finalArguments);
			}
		} else {
			// Case neither the predicate nor the arguments changed, so just
			// continue with the same literal
			newExp = literal;
		}
		
		// Load the updated expression into the temporary return member and
		// continue to try to do predicate specific simplification
		tempReturn = newExp;
		
		// Predicate specific simplification. This part is only relevant if we
		// have arguments.
		if (!finalArguments.isEmpty()) {
			final IPredicateSimplifier simplifier = LogicLanguageServices
					.getSimplifier(newPred);
			if (simplifier != null) {
				final LogicalExpression simplifiedExp = simplifier
						.simplify(newExp);
				if (simplifiedExp != newExp) {
					// Update the return expression if changed
					tempReturn = simplifiedExp;
					return;
				}
			}
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		tempReturn = logicalConstant;
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	private boolean shouldConsumeArgs(LogicalExpression newPred) {
		return newPred.getType().isComplex()
				&& !(newPred instanceof LogicalConstant)
				&& !(newPred instanceof Variable);
	}
	
}
