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
package edu.cornell.cs.nlp.spf.mr.lambda.printers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Term;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.utils.collections.stackmap.HashStackMap;
import edu.cornell.cs.nlp.utils.collections.stackmap.IdentityFastStackMap;

/**
 * Creates a string representation for a {@link LogicalExpression}.
 *
 * @author Yoav Artzi
 */
public class LogicalExpressionToString implements ILogicalExpressionVisitor {
	private final Set<Variable>						definedVariables	= new HashSet<Variable>();
	private int										skolemIdCounter		= 1;
	private final Map<SkolemId, String>				skolemIds			= new HashMap<SkolemId, String>();
	private int										variableIdCounter	= 0;
	private final ScopeMapping<Variable, String>	variableIds			= new ScopeMapping<Variable, String>(
																				new IdentityFastStackMap<Variable, String>(),
																				new HashStackMap<String, Variable>());
	protected final StringBuilder					outputString		= new StringBuilder();

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
		processVariable(lambda.getArgument(), true);
		outputString.append(' ');
		lambda.getBody().accept(this);
		outputString.append(')');
		// Pop the variable name.
		variableIds.pop(lambda.getArgument());
	}

	@Override
	public void visit(Literal literal) {
		outputString.append("(");
		literal.getPredicate().accept(this);
		// Visit the arguments to print them. Print a space before each
		// argument.
		final int len = literal.numArgs();
		for (int i = 0; i < len; ++i) {
			outputString.append(' ');
			literal.getArg(i).accept(this);
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
		if (variable instanceof SkolemId) {
			if (!skolemIds.containsKey(variable)) {
				skolemIds.put((SkolemId) variable,
						((SkolemId) variable).getName(skolemIdCounter++));
			}
			outputString.append(skolemIds.get(variable));
		} else {
			processVariable(variable, false);
		}
	}

	private void processVariable(Variable variable, boolean lambdaArg) {
		if (lambdaArg || !variableIds.containsKey(variable)) {
			// Push the variable name.
			final String name = Variable.PREFIX
					+ String.valueOf(variableIdCounter++);
			variableIds.push(variable, name);
			outputString.append(name);
			outputString.append(Term.TYPE_SEPARATOR);
			outputString.append(variable.getType().getName());
			definedVariables.add(variable);
		} else {
			outputString.append(variableIds.peek(variable));
		}
	}

	public static class Printer implements ILogicalExpressionPrinter {

		@Override
		public String toString(LogicalExpression exp) {
			return LogicalExpressionToString.of(exp);
		}

	}

}
