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
 * Replaces all the N-th occurrence of the given sub-expression with the
 * replacement expression. The ordering is undefined, but consistent across
 * multiple calls with the same input expression. The indexing starts at 0 (the
 * first instance is position 0).
 *
 * @author Luke Zettlemoyer
 */
public class ReplaceNthExpression implements ILogicalExpressionVisitor {

	private final int				n;
	private int						numTimesSeen	= 0;
	private final LogicalExpression	replacement;
	private LogicalExpression		result			= null;
	private final LogicalExpression	subExp;

	/**
	 * Usage only through 'of' static method.
	 */
	private ReplaceNthExpression(LogicalExpression subExp,
			LogicalExpression relacement, int n) {
		this.numTimesSeen = 0;
		this.subExp = subExp;
		this.n = n + 1; // switch from 0 first index to 1 first index
		this.replacement = relacement;
	}

	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression subExp, LogicalExpression replacement, int n) {
		final ReplaceNthExpression visitor = new ReplaceNthExpression(subExp,
				replacement, n);
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
			// Never replace (or count) bound variables.
			result = lambda;
			return;
		}

		if (subExp.equals(lambda)) {
			numTimesSeen++;
			if (numTimesSeen == n) {
				result = replacement;
			} else {
				result = lambda;
			}
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
			// Never replace (or count) bound variables.
			result = literal;
			return;
		}

		if (subExp.equals(literal)) {
			numTimesSeen++;
			if (numTimesSeen == n) {
				result = replacement;
			} else {
				result = literal;
			}
		} else {
			literal.getPredicate().accept(this);
			if (result == null) {
				return;
			}
			final LogicalExpression newPredicate = result;

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
				if (arg != result) {
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
			numTimesSeen++;
			if (numTimesSeen == n) {
				result = replacement;
				return;
			}
		}
		result = logicalConstant;
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (subExp.equals(variable)) {
			numTimesSeen++;
			if (numTimesSeen == n) {
				result = replacement;
				return;
			}
		}
		result = variable;
	}

}
