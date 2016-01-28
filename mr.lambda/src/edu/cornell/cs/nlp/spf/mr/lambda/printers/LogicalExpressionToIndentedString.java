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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Term;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.HasFreeVariables;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.utils.string.StringUtils;

/**
 * Creates a string representation for a {@link LogicalExpression}.
 *
 * @author Yoav Artzi
 */
public class LogicalExpressionToIndentedString implements
		ILogicalExpressionVisitor {
	private final static String			DEFAULT_IDENT		= "\t";
	private int							currentDepth		= 0;
	private final Set<Variable>			definedVariables	= new HashSet<Variable>();
	private final String				indentation;
	private int							skolemIdCounter		= 1;
	private final Map<SkolemId, String>	skolemIds			= new HashMap<SkolemId, String>();
	private final List<Variable>		variablesNamingList	= new LinkedList<Variable>();
	protected final StringBuilder		outputString		= new StringBuilder();

	protected LogicalExpressionToIndentedString(String indentation) {
		this.indentation = indentation;
	}

	public static String of(LogicalExpression expression) {
		return of(expression, DEFAULT_IDENT);
	}

	public static String of(LogicalExpression expression, String indentation) {
		final LogicalExpressionToIndentedString visitor = new LogicalExpressionToIndentedString(
				indentation);
		visitor.visit(expression);

		// Remove empty lines, which can happyen in cases like (lambda $0:e
		// (p:<e,<e,t>> $0 (a:<<e,t>,e (lambda $1:e (boo:<e,t> foo:e))))).
		return visitor.outputString.toString().replaceAll(
				"\n(" + indentation + ")+\n", "\n");
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
		final int len = literal.numArgs();
		if (LogicLanguageServices.isCoordinationPredicate(literal
				.getPredicate())) {
			outputString.append("(");
			literal.getPredicate().accept(this);
			// Visit the arguments to print them. Print a space before each
			// argument.
			++currentDepth;
			for (int i = 0; i < len; ++i) {
				outputString.append("\n"
						+ StringUtils.multiply(indentation, currentDepth));
				literal.getArg(i).accept(this);
			}
			--currentDepth;
			outputString.append(')');
		} else if (!HasFreeVariables.of(literal, true)
				&& outputString.length() > 0) {
			++currentDepth;
			outputString.append("\n"
					+ StringUtils.multiply(indentation, currentDepth));
			outputString.append("(");
			literal.getPredicate().accept(this);
			// Visit the arguments to print them. Print a space before each
			// argument.
			// ++currentDepth;
			for (int i = 0; i < len; ++i) {
				// outputString.append("\n"
				// + StringUtils.multiply(indentation, currentDepth));
				outputString.append(' ');
				literal.getArg(i).accept(this);
			}
			// --currentDepth;
			--currentDepth;
			outputString.append(')');
		} else {
			outputString.append("(");
			literal.getPredicate().accept(this);
			// Visit the arguments to print them. Print a space before each
			// argument.
			for (int i = 0; i < len; ++i) {
				outputString.append(' ');
				literal.getArg(i).accept(this);
			}
			outputString.append(')');
		}
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
			outputString.append(getVariableName(variable));
			if (!definedVariables.contains(variable)) {
				outputString.append(Term.TYPE_SEPARATOR);
				outputString.append(variable.getType().getName());
				definedVariables.add(variable);
			}
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

		private final String	indentation;

		public Printer() {
			this(DEFAULT_IDENT);
		}

		public Printer(String indentation) {
			this.indentation = indentation;
		}

		@Override
		public String toString(LogicalExpression exp) {
			return LogicalExpressionToIndentedString.of(exp, indentation);
		}

	}

}
