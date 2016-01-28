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

import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Check if there exists a variable declared in a {@link Lambda} term and never
 * used elsewhere.
 *
 * @author Yoav Artzi
 */
public class HasUnusedVariable implements ILogicalExpressionVisitor {

	private boolean				unusedVariableDetected	= false;
	private final Set<Variable>	variables				= new HashSet<Variable>();

	public static boolean of(LogicalExpression exp) {
		final HasUnusedVariable visitor = new HasUnusedVariable();
		visitor.visit(exp);
		return visitor.unusedVariableDetected;
	}

	@Override
	public void visit(Lambda lambda) {
		final boolean variableOverwrite;
		if (variables.add(lambda.getArgument())) {
			variableOverwrite = true;
		} else {
			variableOverwrite = false;
		}

		lambda.getBody().accept(this);

		if (variables.contains(lambda.getArgument())) {
			unusedVariableDetected = true;
		}

		if (variableOverwrite) {
			variables.add(lambda.getArgument());
		}
	}

	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		if (unusedVariableDetected) {
			return;
		}
		final int len = literal.numArgs();
		for (int i = 0; i < len; ++i) {
			literal.getArg(i).accept(this);
			if (unusedVariableDetected) {
				return;
			}
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Nothing to do.
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		// Nothing to do.
	}

}
