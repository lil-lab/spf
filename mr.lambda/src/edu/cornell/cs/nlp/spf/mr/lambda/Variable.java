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

import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Lambda calculus variable.
 *
 * @author Yoav Artzi
 */
public class Variable extends Term {
	public static final ILogger	LOG					= LoggerFactory
															.create(Variable.class);
	public static final String	PREFIX				= "$";
	private static final long	serialVersionUID	= -2489052410662325680L;
	private final Set<Variable>	singleton;

	public Variable(Type type) {
		super(type);
		// Hyper specialized singleton.
		this.singleton = ReferenceSets.singleton(this);
	}

	/**
	 * Reads a variable definition into a pair of a {@link Variable} and a
	 * {@link String} (its name).
	 */
	static Pair<String, Variable> readVariableDefintion(String string,
			TypeRepository typeRepository) {
		final String[] split = string.split(Term.TYPE_SEPARATOR);
		if (split.length == 2) {
			final Type type = typeRepository.getTypeCreateIfNeeded(split[1]);
			if (type == null) {
				throw new LogicalExpressionRuntimeException(
						"Invalid type for variable: " + string);
			}
			return Pair.of(split[0], new Variable(type));
		} else {
			return null;
		}
	}

	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean containsFreeVariable(Variable variable) {
		return variable == this;
	}

	@Override
	public boolean containsFreeVariables(Set<Variable> variables) {
		return variables.contains(this);
	}

	/**
	 * When no external mapping is present, equals() should just make identity
	 * comparison.
	 */
	@Override
	public boolean equals(Object obj) {
		// Without variable mapping, variables are only equal when they are
		// identical
		return this == obj;
	}

	@Override
	public Set<Variable> getFreeVariables() {
		return singleton;
	}

	@Override
	public int numFreeVariables() {
		return 1;
	}

	@Override
	protected boolean doEquals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		if (!(exp instanceof Variable) || exp instanceof SkolemId) {
			return false;
		}

		final Variable mapValue = mapping.peek(this);
		if (mapValue == exp && mapping.peekValue(mapValue) == this) {
			// Comparison through mapping of variables.
			return true;
		} else if (!mapping.containsValue((Variable) exp)) {
			// Case both are not mapped, do instance comparison for free
			// variables.
			return exp == this;
		} else {
			// Not equal.
			return false;
		}
	}

	public static class Reader implements IReader<Variable> {

		@Override
		public Variable read(String string,
				ScopeMapping<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader) {

			try {
				final Pair<String, Variable> defintion = readVariableDefintion(
						string, typeRepository);
				if (defintion != null && !mapping.containsKey(string)) {
					mapping.push(defintion.first(), defintion.second());
					return defintion.second();
				} else if (defintion != null) {
					throw new LogicalExpressionRuntimeException(
							"Re-define a global variable: " + string);
				} else {
					// Case variable reference.
					if (mapping.containsKey(string)) {
						return (Variable) mapping.peek(string);
					} else {
						throw new LogicalExpressionRuntimeException(
								"Undefined variable reference: " + string);
					}
				}
			} catch (final RuntimeException e) {
				LOG.error("Variable error: %s", string);
				throw e;
			}

		}

		@Override
		public boolean test(String string) {
			return string.startsWith(Variable.PREFIX);
		}

	}

}
