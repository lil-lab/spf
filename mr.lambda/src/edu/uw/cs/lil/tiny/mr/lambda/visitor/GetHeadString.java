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

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Returns the head string of the expression. This is a single string value.
 * 
 * @author Yoav Artzi
 */
public class GetHeadString implements ILogicalExpressionVisitor {
	private static final String	VARIABLE_HEAD_STRING	= "var";
	private String				headString				= null;
	
	private GetHeadString() {
		// Usage only through static 'of' method.
	}
	
	public static String of(LogicalExpression exp) {
		final GetHeadString visitor = new GetHeadString();
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
		headString = logicalConstant.getName();
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
