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

import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Given a logical expression, will extract a list of variables that are
 * equivalent to the parameters the expression takes. For example: (lambda
 * $0:<e,t> (lambda $1:e ($0 $1))) will return the list [$0 $1].
 *
 * @author Yoav Artzi
 */
public class GetParameterVariablesList implements ILogicalExpressionVisitor {
	private final List<Variable>	paramList	= new LinkedList<Variable>();

	private GetParameterVariablesList() {
		// Usage only through static 'of' method.
	}

	public static List<Variable> of(LogicalExpression exp) {
		final GetParameterVariablesList visitor = new GetParameterVariablesList();
		visitor.visit(exp);
		return visitor.getParamList();
	}

	public List<Variable> getParamList() {
		return paramList;
	}

	@Override
	public void visit(Lambda lambda) {
		paramList.add(lambda.getArgument());
		lambda.getBody().accept(this);
	}

	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final int len = literal.numArgs();
		for (int i = 0; i < len; ++i) {
			literal.getArg(i).accept(this);
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Nothing to do
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		// Nothing to do
	}

}
