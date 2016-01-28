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

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Replaces all the occurrences of the given sub-expression with the replacement
 * expression.
 *
 * @author Yoav Artzi
 */
public class ReplaceExpression implements ILogicalExpressionVisitor {

	private final LogicalExpression	replacement;
	private LogicalExpression		result	= null;
	private final LogicalExpression	subExp;

	/**
	 * Usage only through 'of' static method.
	 *
	 * @param subExp
	 * @param relacement
	 * @param useInstanceComparison
	 */
	private ReplaceExpression(LogicalExpression subExp,
			LogicalExpression relacement) {
		this.subExp = subExp;
		this.replacement = relacement;
	}

	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression subExp, LogicalExpression replacement) {
		final ReplaceExpression visitor = new ReplaceExpression(subExp,
				replacement);
		visitor.visit(exp);
		return visitor.getResult();
	}

	public LogicalExpression getResult() {
		return result;
	}

	@Override
	public void visit(Lambda lambda) {
		if (subExp instanceof Variable
				&& !lambda.containsFreeVariable((Variable) subExp)) {
			// Never replace bound variables.
			result = lambda;
			return;
		}

		if (subExp.equals(lambda)) {
			result = replacement;
		} else {
			lambda.getArgument().accept(this);
			if (result == null) {
				return;
			}
			final LogicalExpression newArg = result;
			lambda.getBody().accept(this);
			if (result == null) {
				return;
			}
			final LogicalExpression newBody = result;
			if (newBody == lambda.getBody() && newArg == lambda.getArgument()) {
				result = lambda;
			} else {
				// Need to check that the new argument is actually a variable,
				// to avoid a runtime exception
				if (newArg instanceof Variable) {
					result = new Lambda((Variable) newArg, newBody);
				} else {
					// Case we don't have a legal expression, just return null
					result = null;
				}
			}

		}
	}

	@Override
	public void visit(Literal literal) {
		if (subExp instanceof Variable
				&& !literal.containsFreeVariable((Variable) subExp)) {
			// Never replace bound variables.
			result = literal;
			return;
		}

		if (subExp.equals(literal)) {
			result = replacement;
		} else {
			// Visit the predicate
			literal.getPredicate().accept(this);
			final LogicalExpression newPredicate;
			if (result == literal.getPredicate()) {
				newPredicate = literal.getPredicate();
			} else {
				if (result == null) {
					return;
				}
				newPredicate = result;
			}
			// Go over the arguments
			final int len = literal.numArgs();
			final LogicalExpression[] newArgs = new LogicalExpression[len];
			boolean argChanged = false;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(i);
				arg.accept(this);
				if (result == null) {
					return;
				}
				newArgs[i] = result;
				if (result != arg) {
					argChanged = true;
				}
			}

			if (argChanged) {
				result = new Literal(newPredicate, newArgs);
			} else if (newPredicate != literal.getPredicate()) {
				result = new Literal(newPredicate, literal);
			} else {
				result = literal;
			}
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		if (subExp.equals(logicalConstant)) {
			result = replacement;
		} else {
			result = logicalConstant;
		}
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (subExp.equals(variable)) {
			result = replacement;
		} else {
			result = variable;
		}
	}
}
