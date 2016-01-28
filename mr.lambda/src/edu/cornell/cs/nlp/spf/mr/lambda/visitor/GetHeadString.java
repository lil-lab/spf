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
 * Returns the head string of the expression. This is a single string value.
 *
 * @author Yoav Artzi
 */
public class GetHeadString implements ILogicalExpressionVisitor {
	private static final String	VARIABLE_HEAD_STRING	= "var";
	private String				headString				= null;
	private final boolean		stripType;

	private GetHeadString(boolean stripType) {
		// Usage only through static 'of' method.
		this.stripType = stripType;
	}

	public static String of(LogicalExpression exp) {
		return of(exp, false);
	}

	public static String of(LogicalExpression exp, boolean stripType) {
		final GetHeadString visitor = new GetHeadString(stripType);
		visitor.visit(exp);
		return visitor.getHeadString();
	}

	public String getHeadString() {
		return headString;
	}

	public void setHeadString(String headString) {
		this.headString = headString;
	}

	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
	}

	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		headString = stripType ? logicalConstant.getBaseName()
				: logicalConstant.getName();
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		headString = VARIABLE_HEAD_STRING;
	}
}
