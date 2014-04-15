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
package edu.uw.cs.lil.tiny.mr.lambda.printers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Term;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;

/**
 * Creates a string representation for a {@link LogicalExpression}.
 * 
 * @author Yoav Artzi
 */
public class LogicalExpressionToString implements ILogicalExpressionVisitor {
	private final Set<Variable>		definedVariables	= new HashSet<Variable>();
	
	private final List<Variable>	variablesNamingList	= new LinkedList<Variable>();
	
	protected final StringBuilder	outputString		= new StringBuilder();
	
	protected LogicalExpressionToString() {
		// Usage only through static 'of' method.
	}
	
	public static String of(LogicalExpression expression) {
		final LogicalExpressionToString visitor = new LogicalExpressionToString();
		visitor.visit(expression);
		return visitor.outputString.toString();
	}
	
	@Override
	final public void visit(Lambda lambda) {
		outputString.append("(lambda ");
		lambda.getArgument().accept(this);
		outputString.append(' ');
		lambda.getBody().accept(this);
		outputString.append(')');
	}
	
	@Override
	public void visit(Literal literal) {
		outputString.append("(");
		literal.getPredicate().accept(this);
		// Visit the arguments to print them. Print a space before each
		// argument.
		for (final LogicalExpression argument : literal.getArguments()) {
			outputString.append(' ');
			argument.accept(this);
		}
		outputString.append(')');
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		outputString.append(logicalConstant.getName());
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		
		outputString.append(getVariableName(variable));
		if (!definedVariables.contains(variable)) {
			outputString.append(Term.TYPE_SEPARATOR);
			outputString.append(variable.getType().getName());
			definedVariables.add(variable);
		}
	}
	
	private String getVariableName(Variable variable) {
		int num = 0;
		for (final Variable namedVariable : variablesNamingList) {
			if (namedVariable == variable) {
				return "$" + String.valueOf(num);
			}
			++num;
		}
		variablesNamingList.add(variable);
		return "$" + String.valueOf(num);
	}
	
	public static class Printer implements ILogicalExpressionPrinter {
		
		@Override
		public String toString(LogicalExpression exp) {
			return LogicalExpressionToString.of(exp);
		}
		
	}
	
}
