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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jregex.Pattern;
import jregex.Replacer;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;

/**
 * Produce a LATEX-formated string representing the logical form.
 *
 * @author Yoav Artzi
 */
public class LogicalExpressionToLatexString implements
		ILogicalExpressionVisitor {
	private static final Replacer					SPECIAL_CHARS_REPLACER	= new Pattern(
																					"([\\{\\}_^$])")
																					.replacer("\\\\$1");
	private static final String[]					VARIABLE_NAMES;
	private final Map<LogicalExpression, String>	mapping;
	private final StringBuilder						outputString			= new StringBuilder();
	private int										skolemIdCounter			= 1;
	private int										variableNameIndex		= 0;

	private int										variableSuffix			= 0;

	private LogicalExpressionToLatexString(
			Map<LogicalExpression, String> baseMapping) {
		this.mapping = new HashMap<LogicalExpression, String>(baseMapping);
	}

	static {
		final List<String> names = new ArrayList<String>();
		for (char c = 'z'; c >= 'a'; --c) {
			names.add(Character.toString(c));

		}
		VARIABLE_NAMES = names.toArray(new String[0]);

	}

	public static String of(LogicalExpression exp,
			Map<LogicalExpression, String> baseMapping) {
		final LogicalExpressionToLatexString visitor = new LogicalExpressionToLatexString(
				baseMapping);
		visitor.visit(exp);
		return visitor.outputString.toString();
	}

	/**
	 * Escape the necessary characters for a Latex string and wrap it \textit.
	 */
	private static String latexIt(String str) {
		return "\\textit{" + SPECIAL_CHARS_REPLACER.replace(str) + "}";
	}

	@Override
	public void visit(Lambda lambda) {
		outputString.append("\\lambda ");
		lambda.getArgument().accept(this);
		outputString.append(". ");
		lambda.getBody().accept(this);
	}

	@Override
	public void visit(Literal literal) {
		final int numArgs = literal.numArgs();
		if (LogicLanguageServices.isCoordinationPredicate(literal
				.getPredicate())) {
			// Case coordination predicate.
			for (int i = 0; i < numArgs; ++i) {
				literal.getArg(i).accept(this);
				if (i + 1 < numArgs) {
					outputString.append(' ');
					literal.getPredicate().accept(this);
					outputString.append(' ');
				}
			}
		} else if (numArgs > 1
				&& literal.getArg(0).getType()
						.equals(SkolemServices.getIDType())
				&& literal.getArg(1).getType()
						.equals(SkolemServices.getIDType())) {
			// Case skolem terms with referring ID: pred_{id}^{ref}(args).
			literal.getPredicate().accept(this);
			outputString.append("_{");
			literal.getArg(0).accept(this);
			outputString.append('}');
			outputString.append("^{");
			literal.getArg(1).accept(this);
			outputString.append('}');
			if (numArgs > 2) {
				outputString.append('(');
				for (int i = 2; i < numArgs; ++i) {
					literal.getArg(i).accept(this);
					if (i + 1 < numArgs) {
						outputString.append(", ");
					}
				}
				outputString.append(')');
			}
		} else if (numArgs > 0
				&& literal.getArg(0).getType()
						.equals(SkolemServices.getIDType())) {
			// Case skolem term without reference: pred_{id}(args).
			literal.getPredicate().accept(this);
			outputString.append("_{");
			literal.getArg(0).accept(this);
			outputString.append('}');
			if (numArgs > 1) {
				for (int i = 1; i < numArgs; ++i) {
					outputString.append('(');
					literal.getArg(i).accept(this);
					if (i + 1 < numArgs) {
						outputString.append(", ");
					}
				}
				outputString.append(')');
			}
		} else {
			literal.getPredicate().accept(this);
			outputString.append('(');
			for (int i = 0; i < numArgs; ++i) {
				literal.getArg(i).accept(this);
				if (i + 1 < numArgs) {
					outputString.append(", ");
				}
			}
			outputString.append(')');
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		if (mapping.containsKey(logicalConstant)) {
			outputString.append(mapping.get(logicalConstant));
		} else if (logicalConstant.getType().isComplex()) {
			outputString.append(latexIt(logicalConstant.getBaseName()));
		} else {
			outputString.append(latexIt(logicalConstant.getBaseName()
					.toUpperCase()));
		}
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (!mapping.containsKey(variable)) {
			if (variable instanceof SkolemId) {
				mapping.put(variable, Integer.toString(skolemIdCounter++));
			} else {
				if (variableNameIndex >= VARIABLE_NAMES.length) {
					++variableSuffix;
					variableNameIndex = 0;
				}
				mapping.put(
						variable,
						VARIABLE_NAMES[variableNameIndex++]
								+ (variableSuffix == 0 ? "" : Integer
										.valueOf(variableSuffix)));
			}
		}
		outputString.append(mapping.get(variable));
	}

	public static class Printer implements ILogicalExpressionPrinter {

		private final Map<LogicalExpression, String>	baseMapping;

		public Printer(Map<LogicalExpression, String> baseMapping) {
			this.baseMapping = baseMapping;
		}

		@Override
		public String toString(LogicalExpression exp) {
			return of(exp, baseMapping);
		}

		public static class Builder {
			private final Map<LogicalExpression, String>	baseMapping	= new HashMap<LogicalExpression, String>();

			public Builder addMapping(LogicalExpression exp, String string) {
				baseMapping.put(exp, string);
				return this;
			}

			public Printer build() {
				return new Printer(baseMapping);
			}

		}

	}

}
