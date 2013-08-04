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
 * Replaces all the occurrences of the given sub-expression with the replacement
 * expression.
 * 
 * @author Yoav Artzi
 */
public class ReplaceExpression implements ILogicalExpressionVisitor {
	
	public static final IBooleanComparator<LogicalExpression>	EQUALS_COMPARATOR		= new IBooleanComparator<LogicalExpression>() {
																							@Override
																							public boolean compare(
																									LogicalExpression o1,
																									LogicalExpression o2) {
																								return o1
																										.equals(o2);
																							}
																						};
	
	public static final IBooleanComparator<LogicalExpression>	INSTANCE_COMPARATOR		= new IBooleanComparator<LogicalExpression>() {
																							@Override
																							public boolean compare(
																									LogicalExpression o1,
																									LogicalExpression o2) {
																								return o1 == o2;
																							}
																						};
	
	public static final IBooleanComparator<LogicalExpression>	VAR_INSTANCE_COMPARATOR	= new IBooleanComparator<LogicalExpression>() {
																							@Override
																							public boolean compare(
																									LogicalExpression o1,
																									LogicalExpression o2) {
																								if (o1 instanceof Variable
																										|| o2 instanceof Variable) {
																									return o1 == o2;
																								} else {
																									return o1
																											.equals(o2);
																								}
																							}
																						};
	
	private final IBooleanComparator<LogicalExpression>			comparator;
	private final LogicalExpression								replacement;
	private final LogicalExpression								subExp;
	private LogicalExpression									tempReturn				= null;
	
	/**
	 * Usage only through 'of' static method.
	 * 
	 * @param subExp
	 * @param relacement
	 * @param useInstanceComparison
	 */
	private ReplaceExpression(LogicalExpression subExp,
			LogicalExpression relacement,
			IBooleanComparator<LogicalExpression> comparator) {
		this.subExp = subExp;
		this.replacement = relacement;
		this.comparator = comparator;;
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression subExp, LogicalExpression replacement) {
		return of(exp, subExp, replacement, EQUALS_COMPARATOR);
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression subExp, LogicalExpression replacement,
			IBooleanComparator<LogicalExpression> comparator) {
		final ReplaceExpression visitor = new ReplaceExpression(subExp,
				replacement, comparator);
		visitor.visit(exp);
		return visitor.getResult();
	}
	
	public LogicalExpression getResult() {
		return tempReturn;
	}
	
	@Override
	public void visit(Lambda lambda) {
		if (comparator.compare(subExp, lambda)) {
			tempReturn = replacement;
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
		if (comparator.compare(subExp, literal)) {
			tempReturn = replacement;
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
		if (comparator.compare(subExp, logicalConstant)) {
			tempReturn = replacement;
		} else {
			tempReturn = logicalConstant;
		}
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		if (comparator.compare(subExp, variable)) {
			tempReturn = replacement;
		} else {
			tempReturn = variable;
		}
	}
}
