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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.mr.IMeaningRepresentation;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.LogicalExpressionToString;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * The basic lambda calculus entity. Everything is an expression
 * 
 * @author Yoav Artzi
 */
public abstract class LogicalExpression implements
		IMeaningRepresentation<ILogicalExpressionVisitor>, Serializable {
	public static char				PARENTHESIS_CLOSE	= ')';
	public static char				PARENTHESIS_OPEN	= '(';
	private static final ILogger	LOG					= LoggerFactory
																.create(LogicalExpression.class);
	private static final long		serialVersionUID	= 751768060713295464L;
	
	/**
	 * Mutable cache for the hashing code. This field is for internal use only!
	 * It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private int						hashCodeCache;
	
	/**
	 * Mutable flag to indicate if the hash code cache is populated. This field
	 * is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object.
	 */
	private boolean					hashCodeCalculated	= false;
	
	public static LogicalExpression parse(String string) {
		return parse(string, LogicLanguageServices.getTypeRepository(),
				LogicLanguageServices.getTypeComparator());
	}
	
	public static LogicalExpression parse(String string, boolean lockOntology) {
		return parse(string, LogicLanguageServices.getTypeRepository(),
				LogicLanguageServices.getTypeComparator(), lockOntology);
	}
	
	protected static LogicalExpression doParse(String string,
			Map<String, Variable> variables, TypeRepository typeRepository,
			ITypeComparator typeComparator, boolean lockOntology) {
		if (string.startsWith(Lambda.PREFIX)) {
			return Lambda.doParse(string, variables, typeRepository,
					typeComparator, lockOntology);
		} else if (string.startsWith(Literal.PREFIX)) {
			return Literal.doParse(string, variables, typeRepository,
					typeComparator, lockOntology);
		} else {
			return Term.doParse(string, variables, typeRepository,
					typeComparator, lockOntology);
		}
	}
	
	/**
	 * Parse a logical expression from a string. Throw a
	 * {@link RuntimeException} when trying to create a new logical constant.
	 * 
	 * @param string
	 * @param typeRepository
	 * @return
	 */
	protected static LogicalExpression parse(String string,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		return parse(string, typeRepository, typeComparator, true);
	}
	
	/**
	 * Parse a logical expression from a string.
	 * 
	 * @param string
	 *            LISP formatted
	 * @param typeRepository
	 *            Typing system
	 * @param lockOntology
	 *            Don't allow creation of new constants. If 'true' a
	 *            {@link RuntimeException} will be thrown when a new constant is
	 *            encountered.
	 * @return logical expression
	 */
	protected static LogicalExpression parse(String string,
			TypeRepository typeRepository, ITypeComparator typeComparator,
			boolean lockOntology) {
		try {
			return doParse(string, new HashMap<String, Variable>(),
					typeRepository, typeComparator, lockOntology);
		} catch (final RuntimeException e) {
			LOG.error("Logical expression syntax error: %s", string);
			throw e;
		}
	}
	
	public abstract void accept(ILogicalExpressionVisitor visitor);
	
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof LogicalExpression
				&& obj.hashCode() == hashCode()
				&& doEquals(obj, new HashMap<Variable, Variable>());
	}
	
	abstract public Type getType();
	
	@Override
	final public int hashCode() {
		if (!hashCodeCalculated) {
			hashCodeCache = calcHashCode();
			hashCodeCalculated = true;
		}
		return hashCodeCache;
	}
	
	@Override
	final public String toString() {
		return LogicalExpressionToString.of(this);
	}
	
	protected abstract int calcHashCode();
	
	protected abstract boolean doEquals(Object obj,
			Map<Variable, Variable> variablesMapping);
	
	protected boolean equals(Object obj,
			Map<Variable, Variable> variablesMapping) {
		return obj != null && obj instanceof LogicalExpression
				&& obj.hashCode() == hashCode()
				&& doEquals(obj, variablesMapping);
	}
}
