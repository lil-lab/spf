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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.io.StringReader;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.LispReader;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

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

	public static final ILogger		LOG					= LoggerFactory
																.create(Lambda.class);

	public static final String		PREFIX				= LogicalExpression.PARENTHESIS_OPEN
																+ HEAD_STRING;
	private static final long		serialVersionUID	= -9074603389979811699L;

	private final Variable			argument;

	private final LogicalExpression	body;

	private final Set<Variable>		freeVariables;

	private final ComplexType		type;

	public Lambda(Variable argument, LogicalExpression body) {
		this(argument, body, LogicLanguageServices.getTypeRepository());
	}

	@SuppressWarnings("unchecked")
	private Lambda(Variable argument, LogicalExpression body,
			TypeRepository typeRepository) {
		assert argument != null;
		assert body != null;
		this.argument = argument;
		this.body = body;
		this.type = typeRepository.getTypeCreateIfNeeded(body.getType(),
				argument.getType());
		assert type != null && type.isComplex() : String.format(
				"Invalid lambda type: arg=%s, body=%s, inferred type=%s",
				argument, body, type);
		if (body.numFreeVariables() == 1 && body.containsFreeVariable(argument)) {
			// Case single free variables in the body and it's the argument, so
			// we have no free variables.
			this.freeVariables = ReferenceSets.EMPTY_SET;
		} else {
			final ReferenceSet<Variable> variables = new ReferenceOpenHashSet<Variable>(
					body.getFreeVariables());
			variables.remove(argument);
			this.freeVariables = ReferenceSets.unmodifiable(variables);
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
		result = prime * result + (argument == null ? 0 : argument.hashCode());
		result = prime * result + (body == null ? 0 : body.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean containsFreeVariable(Variable variable) {
		return freeVariables.contains(variable);
	}

	@Override
	public boolean containsFreeVariables(Set<Variable> variables) {
		if (freeVariables.isEmpty()) {
			return false;
		}

		final Set<Variable> bigSet;
		final Set<Variable> smallSet;
		if (freeVariables.size() >= variables.size()) {
			bigSet = freeVariables;
			smallSet = variables;
		} else {
			bigSet = variables;
			smallSet = freeVariables;
		}

		for (final Variable variable : smallSet) {
			if (bigSet.contains(variable)) {
				return true;
			}
		}
		return false;
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
	public Set<Variable> getFreeVariables() {
		return freeVariables;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public int numFreeVariables() {
		return freeVariables.size();
	}

	@Override
	protected boolean doEquals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		if (this == exp) {
			// Since skolem IDs from this literal may be used in other parts of
			// the logical form, we need to create a mapping of them. As the
			// instances are identical, we can just update the mapping by
			// creating a mapping from each SkolemId to itself.
			if (!freeVariables.isEmpty()) {
				for (final Variable freeVariable : freeVariables) {
					if (freeVariable instanceof SkolemId) {
						mapping.push(freeVariable, freeVariable);
					}
				}
			}
			return true;
		}
		if (getClass() != exp.getClass()) {
			return false;
		}
		final Lambda other = (Lambda) exp;
		if (!type.equals(other.type)) {
			return false;
		}

		if (argument.getType().equals(other.argument.getType())) {
			// If the types are equal and both are not null, add the mapping for
			// the comparison of the body.
			mapping.push(argument, other.argument);
		} else {
			return false;
		}

		final boolean ret = body.equals(other.body, mapping);

		// Remove mapping.
		mapping.pop(argument);

		return ret;
	}

	public static class Reader implements IReader<Lambda> {

		@Override
		public Lambda read(String string,
				ScopeMapping<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader) {

			try {
				final LispReader lispReader = new LispReader(new StringReader(
						string));

				// The first argument is the 'lambda' keyword. We just ignore
				// it.
				lispReader.next();

				// The second argument is the variable definition.
				final Pair<String, Variable> variableDef = Variable
						.readVariableDefintion(lispReader.next(),
								typeRepository);

				if (variableDef == null) {
					throw new LogicalExpressionRuntimeException(
							"Invalid lambda argument: " + string);
				}

				// Update the scope mapping.
				mapping.push(variableDef.first(), variableDef.second());

				// The next argument is the body expression.
				final LogicalExpression lambdaBody = reader.read(
						lispReader.next(), mapping, typeRepository,
						typeComparator);

				// Verify that we don't have any more elements.
				if (lispReader.hasNext()) {
					throw new LogicalExpressionRuntimeException(String.format(
							"Invalid lambda expression: %s", string));
				}

				// Remove the variable from the mapping.
				if (mapping.pop(variableDef.first()) == null) {
					throw new LogicalExpressionRuntimeException(
							"Failed to remove variable from mapping. Something werid is happening: "
									+ string);
				}

				return new Lambda(variableDef.second(), lambdaBody);
			} catch (final RuntimeException e) {
				LOG.error("Lambda syntax error: %s", string);
				throw e;
			}

		}

		@Override
		public boolean test(String string) {
			return string.startsWith(Lambda.PREFIX);
		}

	}

}
