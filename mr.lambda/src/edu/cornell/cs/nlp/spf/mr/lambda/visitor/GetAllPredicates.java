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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Create a set of predicates (function types only) present in the given
 * expression.
 *
 * @author Yoav Artzi
 */
public class GetAllPredicates implements ILogicalExpressionVisitor {
	final private Set<LogicalConstant>	predicates	= new HashSet<LogicalConstant>();

	private GetAllPredicates() {
		// Usage only through static 'of' method.
	}

	public static Set<LogicalConstant> of(LogicalExpression exp) {
		final GetAllPredicates visitor = new GetAllPredicates();
		visitor.visit(exp);
		return visitor.getResult();
	}

	public Set<LogicalConstant> getResult() {
		return predicates;
	}

	@Override
	public void visit(Lambda lambda) {
		lambda.getArgument().accept(this);
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
		if (logicalConstant.getType().isComplex()
				&& !LogicLanguageServices
						.isCoordinationPredicate(logicalConstant)
				&& !LogicLanguageServices
						.isArrayIndexPredicate(logicalConstant)
				&& !LogicLanguageServices.isArraySubPredicate(logicalConstant)) {
			// Case found a predicate, add it to the return set
			predicates.add(logicalConstant);
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
