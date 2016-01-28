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

import java.util.HashMap;
import java.util.Map;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.utils.counter.Counter;

/**
 * Create map of constants to counts.
 *
 * @author Yoav Artzi
 */
public class GetConstCounts implements ILogicalExpressionVisitor {
	private final Map<LogicalConstant, Counter>	constants	= new HashMap<LogicalConstant, Counter>();

	private GetConstCounts() {
		// Usage only through static 'of' method.
	}

	public static Map<LogicalConstant, Counter> of(LogicalExpression exp) {
		final GetConstCounts visitor = new GetConstCounts();
		visitor.visit(exp);
		return visitor.getConstants();
	}

	public Map<LogicalConstant, Counter> getConstants() {
		return constants;
	}

	@Override
	public void visit(Lambda lambda) {
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
		if (constants.containsKey(logicalConstant)) {
			constants.get(logicalConstant).inc();
		} else {
			constants.put(logicalConstant, new Counter(1));
		}
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
