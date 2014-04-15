package edu.uw.cs.lil.tiny.mr.lambda.printers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jregex.Pattern;
import jregex.Replacer;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;

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
	
	@Override
	public void visit(Lambda lambda) {
		outputString.append("\\lambda ");
		lambda.getArgument().accept(this);
		outputString.append(". ");
		lambda.getBody().accept(this);
	}
	
	@Override
	public void visit(Literal literal) {
		if (LogicLanguageServices.isCoordinationPredicate(literal
				.getPredicate())) {
			// Case coordination predicate.
			final Iterator<LogicalExpression> iterator = literal.getArguments()
					.iterator();
			while (iterator.hasNext()) {
				iterator.next().accept(this);
				if (iterator.hasNext()) {
					outputString.append(' ');
					literal.getPredicate().accept(this);
					outputString.append(' ');
				}
			}
		} else {
			literal.getPredicate().accept(this);
			outputString.append('(');
			final Iterator<LogicalExpression> iterator = literal.getArguments()
					.iterator();
			while (iterator.hasNext()) {
				iterator.next().accept(this);
				if (iterator.hasNext()) {
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
		outputString.append(mapping.get(variable));
	}
	
	/**
	 * Escape the necessary characters for a Latex string and wrap it \textit.
	 */
	private String latexIt(String str) {
		return "\\textit{" + SPECIAL_CHARS_REPLACER.replace(str) + "}";
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
