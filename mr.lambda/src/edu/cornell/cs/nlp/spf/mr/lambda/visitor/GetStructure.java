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
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Replace the base name of constants with an anonymized name. Keep types.
 *
 * @author Yoav Artzi
 */
public class GetStructure implements ILogicalExpressionVisitor {

	private static final String	DEFAULT_ANONNYMOUS_TAG	= "anon";
	private final String		anonnymousTag;
	private LogicalExpression	tempReturn;

	public GetStructure(String anonnymousName) {
		this.anonnymousTag = anonnymousName;
	}

	public static LogicalExpression of(LogicalExpression exp) {
		return of(exp, DEFAULT_ANONNYMOUS_TAG);
	}

	public static LogicalExpression of(LogicalExpression exp,
			String anonnymousName) {
		final GetStructure visitor = new GetStructure(anonnymousName);
		visitor.visit(exp);
		return visitor.tempReturn;
	}

	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
		if (lambda.getBody() == tempReturn) {
			tempReturn = lambda;
		} else {
			tempReturn = new Lambda(lambda.getArgument(), tempReturn);
		}

	}

	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final LogicalExpression newPredicate = tempReturn;

		boolean argsChanged = false;
		final int len = literal.numArgs();
		final LogicalExpression[] newArgs = new LogicalExpression[len];
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);
			arg.accept(this);
			newArgs[i] = tempReturn;
			if (arg != tempReturn) {
				argsChanged = true;
			}
		}

		if (argsChanged || newPredicate != literal.getPredicate()) {
			if (argsChanged) {
				tempReturn = new Literal(newPredicate, newArgs);
			} else {
				tempReturn = new Literal(newPredicate, literal);
			}
		} else {
			tempReturn = literal;
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		tempReturn = LogicalConstant.create(anonnymousTag,
				logicalConstant.getType(), true);
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (variable instanceof SkolemId) {
			tempReturn = LogicalConstant.create(anonnymousTag,
					variable.getType(), true);
		} else {
			tempReturn = variable;
		}
	}

}
