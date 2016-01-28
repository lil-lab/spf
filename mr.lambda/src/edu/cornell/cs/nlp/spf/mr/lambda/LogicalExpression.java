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
package edu.cornell.cs.nlp.spf.mr.lambda;

import java.io.Serializable;
import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.IMeaningRepresentation;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

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
	private transient int		hashCodeCache;

	/**
	 * Mutable flag to indicate if the hash code cache is populated. This field
	 * is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object.
	 */
	private transient boolean	hashCodeCalculated	= false;

	public static LogicalExpression read(String string) {
		return LogicalExpressionReader.from(string);
	}

	@Override
	public abstract void accept(ILogicalExpressionVisitor visitor);

	public abstract boolean containsFreeVariable(Variable variable);

	public abstract boolean containsFreeVariables(Set<Variable> variables);

	/**
	 * Comparison with existing mapping and a hashcode short-circuiting.
	 *
	 * @param exp
	 *            Compared object.
	 * @param mapping
	 *            Existing logical expression mapping between this logical
	 *            expression and the target.
	 */
	public boolean equals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		return exp != null && exp.hashCode() == hashCode()
				&& doEquals(exp, mapping);
	}

	/**
	 * Logical expression equals() creates an empty mapping and then compares to
	 * the given object, while tracking variables. Before allocating the
	 * mapping, tries to fail quickly by comparing the hash codes, which are
	 * cached.
	 */
	@Override
	public boolean equals(Object obj) {
		// Try to use the hash code to quickly fail on most non-equal objects.
		return obj instanceof LogicalExpression && obj.hashCode() == hashCode()
				&& LogicLanguageServices.isEqual(this, (LogicalExpression) obj);
	}

	public abstract Set<Variable> getFreeVariables();

	abstract public Type getType();

	@Override
	final public int hashCode() {
		if (!hashCodeCalculated) {
			hashCodeCache = calcHashCode();
			hashCodeCalculated = true;
		}
		return hashCodeCache;
	}

	public abstract int numFreeVariables();

	@Override
	final public String toString() {
		return LogicLanguageServices.toString(this);
	}

	protected abstract int calcHashCode();

	/**
	 * Comparison with mapping.
	 *
	 * @param exp
	 *            Compared object.
	 * @param mapping
	 *            Map of logical expressions from source expression to target.
	 */
	protected abstract boolean doEquals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping);
}
