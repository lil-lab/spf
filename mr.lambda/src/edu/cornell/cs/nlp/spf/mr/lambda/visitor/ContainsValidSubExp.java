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

import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Given a predicate, tests to see if there's at least one sub-expression that
 * tests positive.
 *
 * @author Yoav Artzi
 */
public class ContainsValidSubExp implements ILogicalExpressionVisitor {

	private final Predicate<LogicalExpression>	predicate;
	private boolean								result	= false;

	private ContainsValidSubExp(Predicate<LogicalExpression> predicate) {
		this.predicate = predicate;
	}

	public static boolean of(LogicalExpression exp,
			Predicate<LogicalExpression> predicate) {
		final ContainsValidSubExp visitor = new ContainsValidSubExp(predicate);
		visitor.visit(exp);
		return visitor.result;
	}

	@Override
	public void visit(Lambda lambda) {
		result = predicate.test(lambda);
		if (result) {
			return;
		}

		lambda.getArgument().accept(this);
		if (result == true) {
			return;
		}
		lambda.getBody().accept(this);
	}

	@Override
	public void visit(Literal literal) {
		result = predicate.test(literal);
		if (result) {
			return;
		}
		literal.getPredicate().accept(this);
		if (result) {
			return;
		}
		final int len = literal.numArgs();
		for (int i = 0; i < len; ++i) {
			literal.getArg(i).accept(this);
			if (result) {
				return;
			}
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		result = predicate.test(logicalConstant);
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		result = predicate.test(variable);
	}

}
