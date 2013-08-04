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
package edu.uw.cs.lil.tiny.genlex.ccg.unification.split;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.AllSubExpressions;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;

public class AllConstrainedSubExpressions implements ILogicalExpressionVisitor {
	private final Set<LogicalExpression>	subExpressions	= new HashSet<LogicalExpression>();
	
	private AllConstrainedSubExpressions() {
		// Usage only through static 'of' method
	}
	
	public static Set<LogicalExpression> of(LogicalExpression exp) {
		final AllConstrainedSubExpressions visitor = new AllConstrainedSubExpressions();
		visitor.visit(exp);
		return visitor.subExpressions;
	}
	
	@Override
	public void visit(Lambda lambda) {
		addSubExpression(lambda);
		lambda.getBody().accept(this);
	}
	
	@Override
	public void visit(Literal literal) {
		addSubExpression(literal);
		
		// Add the literal
		addSubExpression(literal.getPredicate());
		
		if (literal.getArguments().size() == 1) {
			// Case single argument, continue to visit arguments
			for (final LogicalExpression arg : literal.getArguments()) {
				arg.accept(this);
			}
		} else {
			// Case multiple arguments, don't traverse deeper into the logical
			// expression. Add the arguments as sub-expressions.
			
			final Iterator<? extends LogicalExpression> iterator = literal
					.getArguments().iterator();
			
			// Add the first arg to the set of subexpressions and get its
			// sub-expressions, to look for shared sub-expressions
			final LogicalExpression firstArg = iterator.next();
			addSubExpression(firstArg);
			final Set<LogicalExpression> sharedSubExpressions = new HashSet<LogicalExpression>(
					AllSubExpressions.of(firstArg));
			
			// Iterate over the rest of the expressions
			while (iterator.hasNext()) {
				final LogicalExpression arg = iterator.next();
				addSubExpression(arg);
				sharedSubExpressions.retainAll(AllSubExpressions.of(arg));
			}
			
			// Add shared sub-expressions
			for (final LogicalExpression sharedSub : sharedSubExpressions) {
				addSubExpression(sharedSub);
			}
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		addSubExpression(logicalConstant);
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		// Nothing to do here
	}
	
	private boolean addSubExpression(LogicalExpression sub) {
		if (sub instanceof Variable) {
			return false;
		} else {
			return subExpressions.add(sub);
		}
	}
	
}
