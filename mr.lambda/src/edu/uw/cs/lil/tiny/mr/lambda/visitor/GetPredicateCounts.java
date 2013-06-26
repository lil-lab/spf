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

import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.utils.counter.Counter;

/**
 * Create map of predicate types to counts.
 * 
 * @author Yoav Artzi
 */
public class GetPredicateCounts implements ILogicalExpressionVisitor {
	private final Map<LogicalConstant, Counter>	predicates	= new HashMap<LogicalConstant, Counter>();
	
	private GetPredicateCounts() {
		// Usage only through static 'of' method.
	}
	
	public static Map<LogicalConstant, Counter> of(LogicalExpression exp) {
		final GetPredicateCounts visitor = new GetPredicateCounts();
		visitor.visit(exp);
		return visitor.getPredicates();
	}
	
	public Map<LogicalConstant, Counter> getPredicates() {
		return predicates;
	}
	
	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
	}
	
	@Override
	public void visit(Literal literal) {
		if (literal.getPredicate() instanceof LogicalConstant) {
			if (!predicates.containsKey(literal.getPredicate())) {
				predicates.put((LogicalConstant) literal.getPredicate(),
						new Counter());
			}
			predicates.get(literal.getPredicate()).inc();
		}
		
		literal.getPredicate().accept(this);
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
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
