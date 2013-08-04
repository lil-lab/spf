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
package edu.uw.cs.lil.tiny.mr.lambda;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.tiny.utils.LispReader;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lambda expression with a single argument and a logical expression as the
 * body.
 * 
 * @author Yoav Artzi
 */
public class Lambda extends LogicalExpression {
	/**
	 * The head string for a lambda expression.
	 */
	public static final String		HEAD_STRING			= "lambda";
	
	public static final String		PREFIX				= LogicalExpression.PARENTHESIS_OPEN
																+ HEAD_STRING;
	
	private static final ILogger	LOG					= LoggerFactory
																.create(Lambda.class);
	private static final long		serialVersionUID	= -9074603389979811699L;
	private final Variable			argument;
	private final LogicalExpression	body;
	
	private final ComplexType		type;
	
	public Lambda(Variable argument, LogicalExpression body) {
		this(argument, body, LogicLanguageServices.getTypeRepository());
	}
	
	private Lambda(Variable argument, LogicalExpression body,
			TypeRepository typeRepository) {
		this.argument = argument;
		this.body = body;
		this.type = typeRepository.getTypeCreateIfNeeded(body.getType(),
				argument.getType());
		if (type == null || !type.isComplex()) {
			throw new LogicalExpressionRuntimeException(String.format(
					"Invalid lambda type: arg=%s, body=%s, inferred type=%s",
					argument, body, type));
		}
	}
	
	/**
	 * @see LogicalExpression#parse(String)
	 * @param string
	 * @return
	 */
	protected static Lambda doParse(String string,
			Map<String, Variable> variables, TypeRepository typeRepository,
			ITypeComparator typeComparator, boolean lockOntology) {
		try {
			final LispReader lispReader = new LispReader(new StringReader(
					string));
			
			// The first argument is the 'lambda' keyword. We just ignore it.
			lispReader.next();
			
			// Remember the size of our variable mapping table, to verify we
			// added
			// one later
			final int variablesOrgSize = variables.size();
			
			// The second argument is the name of the variable introduces
			final LogicalExpression varExpression = LogicalExpression.doParse(
					lispReader.next(), variables, typeRepository,
					typeComparator, lockOntology);
			
			if (!(varExpression instanceof Variable)) {
				throw new LogicalExpressionRuntimeException(
						"Invalid lambda argument: " + string);
			}
			
			final Variable variable = (Variable) varExpression;
			
			// To verify the creation of a variable, we just compare the
			// size of variables mapping table before and after. We can do that
			// because we don't allow overwrite.
			if (variablesOrgSize + 1 != variables.size()) {
				throw new LogicalExpressionRuntimeException(
						"Lambda expression must introduce a new variable: "
								+ string);
			}
			
			// The next argument is the body expression
			final LogicalExpression lambdaBody = LogicalExpression.doParse(
					lispReader.next(), variables, typeRepository,
					typeComparator, lockOntology);
			
			// Verify that we don't have any more elements
			if (lispReader.hasNext()) {
				throw new LogicalExpressionRuntimeException(String.format(
						"Invalid lambda expression: %s", string));
			}
			
			// Need to remove the variable from the table. Since we keep them in
			// a
			// map, this is going to be ugly, but we will tolerate it since it's
			// not
			// really going to be a large map.
			final Iterator<Entry<String, Variable>> variablesIterator = variables
					.entrySet().iterator();
			boolean removed = false;
			while (variablesIterator.hasNext() && !removed) {
				if (variablesIterator.next().getValue() == variable) {
					variablesIterator.remove();
					removed = true;
				}
			}
			if (!removed) {
				throw new LogicalExpressionRuntimeException(
						"Failed to remove variable from mapping. Something werid is happening: "
								+ string);
			}
			
			return new Lambda(variable, lambdaBody);
		} catch (final RuntimeException e) {
			LOG.error("Lambda syntax error: %s", string);
			throw e;
		}
		
	}
	
	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((argument == null) ? 0 : argument.hashCode());
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		// The parent object will create the variable mapping and use it for
		// equal. We can shortcut this process by quickly checking we have a
		// Lambda object.
		return obj instanceof Lambda && super.equals(obj);
	}
	
	public Variable getArgument() {
		return argument;
	}
	
	public LogicalExpression getBody() {
		return body;
	}
	
	public ComplexType getComplexType() {
		return type;
	}
	
	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	protected boolean doEquals(Object obj,
			Map<Variable, Variable> variablesMapping) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Lambda other = (Lambda) obj;
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		
		if (argument == null) {
			if (other.argument != null) {
				return false;
			}
		} else if (other.argument != null) {
			// If the types are equal and both are not null, add the mapping for
			// the comparison of the body
			variablesMapping.put(argument, other.argument);
		}
		
		boolean ret = true;
		
		if (body == null) {
			if (other.body != null) {
				ret = false;
			}
		} else {
			ret = body.equals(other.body, variablesMapping);
		}
		
		// Remove mapping
		variablesMapping.remove(argument);
		
		return ret;
	}
}
