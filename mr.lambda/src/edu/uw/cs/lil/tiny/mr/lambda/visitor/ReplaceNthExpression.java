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

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.utils.compare.IBooleanComparator;

/**
 * Replaces all the N-th occurrence of the given sub-expression with the
 * replacement expression. The ordering is undefined, but consistent across
 * multiple calls with the same input expression. The indexing starts at 0 (the
 * first instance is position 0).
 * 
 * @author Luke Zettlemoyer
 */
public class ReplaceNthExpression implements ILogicalExpressionVisitor {
	
	private final IBooleanComparator<LogicalExpression>	comparator;
	private final int									N;
	private int											numTimesSeen	= 0;
	private final LogicalExpression						replacement;
	private final LogicalExpression						subExp;
	private LogicalExpression							tempReturn		= null;
	
	/**
	 * Usage only through 'of' static method.
	 * 
	 * @param subExp
	 * @param relacement
	 * @param N
	 * @param comparator2
	 */
	private ReplaceNthExpression(LogicalExpression subExp,
			LogicalExpression relacement, int N,
			IBooleanComparator<LogicalExpression> comparator) {
		this.comparator = comparator;
		this.numTimesSeen = 0;
		this.subExp = subExp;
		this.N = N + 1; // switch from 0 first index to 1 first index
		this.replacement = relacement;
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression subExp, LogicalExpression replacement, int N) {
		return of(exp, subExp, replacement, N,
				ReplaceExpression.EQUALS_COMPARATOR);
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression subExp, LogicalExpression replacement, int N,
			IBooleanComparator<LogicalExpression> comparator) {
		final ReplaceNthExpression visitor = new ReplaceNthExpression(subExp,
				replacement, N, comparator);
		visitor.visit(exp);
		return visitor.getResult();
	}
	
	public LogicalExpression getResult() {
		return tempReturn;
	}
	
	@Override
	public void visit(Lambda lambda) {
		if (comparator.compare(lambda, subExp)) {
			numTimesSeen++;
			if (numTimesSeen == N) {
				tempReturn = replacement;
			} else {
				tempReturn = lambda;
			}
		} else {
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
	}
	
	@Override
	public void visit(Literal literal) {
		if (comparator.compare(literal, subExp)) {
			numTimesSeen++;
			if (numTimesSeen == N) {
				tempReturn = replacement;
			} else {
				tempReturn = literal;
			}
		} else {
			boolean literalChanged = false;
			// Visit the predicate
			literal.getPredicate().accept(this);
			final LogicalExpression newPredicate;
			if (tempReturn == literal.getPredicate()) {
				newPredicate = literal.getPredicate();
			} else {
				if (tempReturn == null) {
					return;
				}
				newPredicate = tempReturn;
				literalChanged = true;
			}
			// Go over the arguments
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					literal.getArguments().size());
			for (final LogicalExpression arg : literal.getArguments()) {
				arg.accept(this);
				if (tempReturn == null) {
					return;
				}
				final LogicalExpression newArg = tempReturn;
				if (newArg == arg) {
					args.add(arg);
				} else {
					args.add(newArg);
					literalChanged = true;
				}
			}
			if (literalChanged) {
				tempReturn = new Literal(newPredicate, args);
			} else {
				tempReturn = literal;
			}
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		if (comparator.compare(logicalConstant, subExp)) {
			numTimesSeen++;
			if (numTimesSeen == N) {
				tempReturn = replacement;
				return;
			}
		}
		tempReturn = logicalConstant;
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		if (comparator.compare(variable, subExp)) {
			numTimesSeen++;
			if (numTimesSeen == N) {
				tempReturn = replacement;
				return;
			}
		}
		tempReturn = variable;
	}
	
}
