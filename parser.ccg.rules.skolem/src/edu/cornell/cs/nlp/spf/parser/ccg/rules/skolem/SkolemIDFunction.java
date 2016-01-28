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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.skolem;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

/**
 * Set IDs for all skolem terms that have no free variables. Doesn't set
 * reference IDs. Assumes the ID is the first argument of the quantifier.
 *
 * @author Yoav Artzi
 */
public class SkolemIDFunction implements
		Function<Category<LogicalExpression>, Category<LogicalExpression>> {

	private final Set<LogicalConstant>	quantifiers;

	public SkolemIDFunction(Set<LogicalConstant> quantifiers) {
		this.quantifiers = quantifiers;
	}

	@Override
	public Category<LogicalExpression> apply(Category<LogicalExpression> input) {
		if (input.getSemantics() == null) {
			return input;
		}

		final SetIDs visitor = new SetIDs();
		visitor.visit(input.getSemantics());
		if (visitor.result == input.getSemantics()) {
			return input;
		} else {
			return input.cloneWithNewSemantics(visitor.result);
		}
	}

	public static class Creator implements
			IResourceObjectCreator<SkolemIDFunction> {

		private final String	type;

		public Creator() {
			this("function.skolem.id");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public SkolemIDFunction create(Parameters params,
				IResourceRepository repo) {
			return new SkolemIDFunction(
					SetUtils.createSingleton(LogicalConstant.read(params
							.get("quantifier"))));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, SkolemIDFunction.class)
					.addParam("quantifier", LogicalConstant.class,
							"Quantifiers that can be IDed.").build();
		}

	}

	private class SetIDs implements ILogicalExpressionVisitor {

		private Set<Variable>		freeVariables	= new HashSet<Variable>();
		private LogicalExpression	result			= null;

		@Override
		public void visit(Lambda lambda) {
			lambda.getBody().accept(this);
			if (lambda.getBody() != result) {
				result = new Lambda(lambda.getArgument(), result);
			} else {
				result = lambda;
			}
			// Remove the variable bound by this lambda operator.
			freeVariables.remove(lambda.getArgument());
		}

		@Override
		public void visit(Literal literal) {
			// If the literal doesn't change, we simply recycle it.
			final Set<Variable> aggregateFreeVariables = freeVariables;
			freeVariables = new HashSet<Variable>();

			// Visit the predicate.
			literal.getPredicate().accept(this);
			if (result == null) {
				return;
			}
			final LogicalExpression newPredicate = result;
			aggregateFreeVariables.addAll(freeVariables);

			// Go over the arguments.
			final int len = literal.numArgs();
			final LogicalExpression[] newArgs = new LogicalExpression[len];
			boolean argChanged = false;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(i);
				freeVariables = new HashSet<Variable>();
				arg.accept(this);
				aggregateFreeVariables.addAll(freeVariables);
				if (result == null) {
					return;
				}
				newArgs[i] = result;
				if (arg != result) {
					argChanged = true;
				}
			}

			final Literal updatedLiteral;
			if (argChanged) {
				updatedLiteral = new Literal(newPredicate, newArgs);
			} else if (newPredicate != literal.getPredicate()) {
				updatedLiteral = new Literal(newPredicate, literal);
			} else {
				updatedLiteral = literal;
			}

			// The set of free variables of the literal is the union of all free
			// variable sets of its sub-expressions (predicate and arguments).
			freeVariables = aggregateFreeVariables;

			if (freeVariables.isEmpty()
					&& quantifiers.contains(updatedLiteral.getPredicate())
					&& updatedLiteral.getArg(0).equals(
							SkolemServices.getIdPlaceholder())) {
				final LogicalExpression[] argsWithID = updatedLiteral
						.argumentCopy();
				argsWithID[0] = new SkolemId();
				result = new Literal(updatedLiteral.getPredicate(), argsWithID);
			} else {
				result = updatedLiteral;
			}

		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			// Nothing to do.
			result = logicalConstant;
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			// Add the variables to the free variables set.
			if (!(variable instanceof SkolemId)) {
				freeVariables.add(variable);
			}
			result = variable;
		}

	}

}
