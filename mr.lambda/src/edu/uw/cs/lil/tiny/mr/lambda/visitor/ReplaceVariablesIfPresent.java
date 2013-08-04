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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Given a set of variables, replace all their instances in the given
 * expression. Variables are compared based on instance. The returned expression
 * is logically identical and is promised not to have any of the variables in
 * the given set.
 * 
 * @author Yoav Artzi
 */
public class ReplaceVariablesIfPresent implements ILogicalExpressionVisitor {
	private final Map<Variable, Variable>	oldVariablesToNew	= new HashMap<Variable, Variable>();
	private LogicalExpression				tempReturn			= null;
	private final Set<Variable>				variables;
	
	/**
	 * Usage only through 'of' static method.
	 * 
	 * @param variables
	 */
	private ReplaceVariablesIfPresent(Set<Variable> variables) {
		this.variables = variables;
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			Set<Variable> variables) {
		final ReplaceVariablesIfPresent visitor = new ReplaceVariablesIfPresent(
				variables);
		visitor.visit(exp);
		return visitor.getResult();
	}
	
	public LogicalExpression getResult() {
		return tempReturn;
	}
	
	@Override
	public void visit(Lambda lambda) {
		boolean lambdaChanged = false;
		
		// Visit argument
		lambda.getArgument().accept(this);
		final Variable newArgument = (Variable) tempReturn;
		if (newArgument != lambda.getArgument()) {
			lambdaChanged = true;
		}
		
		// Visit body
		lambda.getBody().accept(this);
		final LogicalExpression newBody = tempReturn;
		if (newBody != lambda.getBody()) {
			lambdaChanged = true;
		}
		
		// Recreate if changed, otherwise return the original
		if (lambdaChanged) {
			tempReturn = new Lambda(newArgument, newBody);
		} else {
			tempReturn = lambda;
		}
	}
	
	@Override
	public void visit(Literal literal) {
		boolean literalChanged = false;
		
		// Visit predicate
		literal.getPredicate().accept(this);
		final LogicalExpression newPredicate = tempReturn;
		if (newPredicate != literal.getPredicate()) {
			literalChanged = true;
		}
		
		// Visit arguments
		boolean argsChanged = false;
		final List<LogicalExpression> newArguments = new ArrayList<LogicalExpression>(
				literal.getArguments().size());
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
			final LogicalExpression newArg = tempReturn;
			newArguments.add(newArg);
			if (newArg != arg) {
				argsChanged = true;
			}
		}
		final List<LogicalExpression> argListToUse;
		if (argsChanged) {
			literalChanged = true;
			argListToUse = newArguments;
		} else {
			argListToUse = literal.getArguments();
		}
		
		// Return the updated literal, if it changed, otherwise return the same
		// old object
		if (literalChanged) {
			tempReturn = new Literal(newPredicate, argListToUse);
		} else {
			tempReturn = literal;
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		tempReturn = logicalConstant;
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		if (variables.contains(variable)) {
			if (!oldVariablesToNew.containsKey(variable)) {
				oldVariablesToNew.put(variable,
						new Variable(variable.getType()));
			}
			tempReturn = oldVariablesToNew.get(variable);
		} else {
			tempReturn = variable;
		}
	}
}
