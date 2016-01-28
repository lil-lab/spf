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
import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Visitor to return 'true' iff and a {@link LogicalExpression} includes at
 * least one {@link LogicalConstant} with the given {@link Type}.
 * 
 * @author Yoav Artzi
 */
public class ContainsConstantType implements ILogicalExpressionVisitor {

	private boolean		result	= false;
	private final Type	type;

	private ContainsConstantType(Type type) {
		this.type = type;
	}

	public static boolean of(LogicalExpression exp, Type type) {
		final ContainsConstantType visitor = new ContainsConstantType(type);
		visitor.visit(exp);
		return visitor.result;
	}

	@Override
	public void visit(Lambda lambda) {
		lambda.getArgument().accept(this);
		if (result) {
			return;
		}
		lambda.getBody().accept(this);
	}

	@Override
	public void visit(Literal literal) {
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
		result = logicalConstant.getType().equals(type);
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
