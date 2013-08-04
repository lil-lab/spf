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

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Replaces all bound variables with fresh identical variables (fresh == new
 * objects, same type).
 * 
 * @author Yoav Artzi
 */
public class ReplaceBoundVariables implements ILogicalExpressionVisitor {
	
	private LogicalExpression				result			= null;
	private final Map<Variable, Variable>	variableMapping	= new HashMap<Variable, Variable>();
	
	public static LogicalExpression of(LogicalExpression exp) {
		final ReplaceBoundVariables visitor = new ReplaceBoundVariables();
		visitor.visit(exp);
		return visitor.result;
	}
	
	@Override
	public void visit(Lambda lambda) {
		// Create new mapping for this variable
		final Variable newVar = new Variable(lambda.getArgument().getType());
		variableMapping.put(lambda.getArgument(), newVar);
		lambda.getBody().accept(this);
		variableMapping.remove(lambda.getArgument());
		result = new Lambda(newVar, result);
	}
	
	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final LogicalExpression newPred = result;
		final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
				literal.getArguments().size());
		boolean argChanged = false;
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
			newArgs.add(result);
			if (arg != result) {
				argChanged = true;
			}
		}
		
		if (argChanged) {
			result = new Literal(newPred, newArgs);
		} else {
			if (newPred == literal.getPredicate()) {
				result = literal;
			} else {
				result = new Literal(newPred, literal.getArguments());
			}
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		result = logicalConstant;
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		if (variableMapping.containsKey(variable)) {
			result = variableMapping.get(variable);
		} else {
			result = variable;
		}
	}
	
}
