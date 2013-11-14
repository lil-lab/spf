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
import edu.uw.cs.lil.tiny.mr.lambda.visitor.LambdaWrapped;
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
	public static final ILogger	LOG					= LoggerFactory
															.create(LogicalExpression.class);
	public static char			PARENTHESIS_CLOSE	= ')';
	public static char			PARENTHESIS_OPEN	= '(';
	private static final long	serialVersionUID	= 751768060713295464L;
	
	/**
	 * Mutable cache for the hashing code. This field is for internal use only!
	 * It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private int					hashCodeCache;
	
	/**
	 * Mutable flag to indicate if the hash code cache is populated. This field
	 * is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object.
	 */
	private boolean				hashCodeCalculated	= false;
	
	/**
	 * Parse a logical expression from a string.
	 * 
	 * @param string
	 *            LISP formatted.
	 */
	public static LogicalExpression parse(String string) {
		return parse(string, LogicLanguageServices.getTypeRepository(),
				LogicLanguageServices.getTypeComparator());
	}
	
	protected static LogicalExpression doParse(String string,
			Map<String, Variable> variables, TypeRepository typeRepository,
			ITypeComparator typeComparator) {
		if (string.startsWith(Lambda.PREFIX)) {
			return Lambda.doParse(string, variables, typeRepository,
					typeComparator);
		} else if (string.startsWith(Literal.PREFIX)) {
			return Literal.doParse(string, variables, typeRepository,
					typeComparator);
		} else {
			return Term.doParse(string, variables, typeRepository,
					typeComparator);
		}
	}
	
	/**
	 * Parse a logical expression from a string.
	 * 
	 * @param string
	 *            LISP formatted
	 * @param typeRepository
	 *            Typing system
	 * @return logical expression
	 */
	protected static LogicalExpression parse(String string,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		try {
			return LambdaWrapped.of(doParse(string,
					new HashMap<String, Variable>(), typeRepository,
					typeComparator));
		} catch (final RuntimeException e) {
			LOG.error("Logical expression syntax error: %s", string);
			throw e;
		}
	}
	
	public abstract void accept(ILogicalExpressionVisitor visitor);
	
	/**
	 * Logical expression equals() creates an empty mapping of variables and
	 * then compares to the given object, while tracking variables. Before
	 * allocating the variable map, tries to fail quickly by comparing the hash
	 * codes, which are cached.
	 */
	@Override
	public boolean equals(Object obj) {
		// Try to use the hash code to quickly fail on most non-equal objects.
		return obj instanceof LogicalExpression
				&& obj.hashCode() == hashCode()
				&& doEquals((LogicalExpression) obj,
						new HashMap<Variable, Variable>());
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
	
	/**
	 * Comparison with variable tracking.
	 * 
	 * @param exp
	 *            Compared object.
	 * @param variablesMapping
	 *            Map of variables from source expression to target.
	 */
	protected abstract boolean doEquals(LogicalExpression exp,
			Map<Variable, Variable> variablesMapping);
	
	/**
	 * Comparison with existing variable mapping and a hashcode
	 * short-circuiting.
	 * 
	 * @param exp
	 *            Compared object.
	 * @param variablesMapping
	 *            Existing variable mapping between this logical expression and
	 *            the target.
	 */
	protected boolean equals(LogicalExpression exp,
			Map<Variable, Variable> variablesMapping) {
		return exp != null && exp.hashCode() == hashCode()
				&& doEquals(exp, variablesMapping);
	}
}
