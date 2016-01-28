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
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Given a set of variables, replace instances of them in the given expression
 * which are free. Variables are compared based on instance. The returned
 * expression is semantically identical and is promised not to have any of the
 * variables in the given set as free variables.
 *
 * @author Yoav Artzi
 */
public class ReplaceFreeVariablesIfPresent implements ILogicalExpressionVisitor {
	private final Map<Variable, Variable>	oldVariablesToNew	= new Reference2ReferenceOpenHashMap<Variable, Variable>();
	private LogicalExpression				result				= null;
	private Set<Variable>					variables;

	/**
	 * Usage only through 'of' static method.
	 *
	 * @param variables
	 */
	private ReplaceFreeVariablesIfPresent(Set<Variable> variables) {
		this.variables = variables;
	}

	public static LogicalExpression of(LogicalExpression exp,
			Set<Variable> variables) {
		if (exp.numFreeVariables() == 0 || variables.isEmpty()) {
			return exp;
		}

		final ReplaceFreeVariablesIfPresent visitor = new ReplaceFreeVariablesIfPresent(
				variables);
		visitor.visit(exp);
		return visitor.getResult();
	}

	public LogicalExpression getResult() {
		return result;
	}

	@Override
	public void visit(Lambda lambda) {
		if (!lambda.containsFreeVariables(variables)) {
			result = lambda;
			return;
		}

		// If the argument is present in the variable set, create a new set
		// without and keep this set aside.
		final Set<Variable> originalSet = variables;
		if (variables.contains(lambda.getArgument())) {
			final Set<Variable> revisedVariables = new HashSet<Variable>(
					variables);
			revisedVariables.remove(lambda.getArgument());
			variables = revisedVariables;
		}

		// No need to visit the argument, so visit just the body.
		lambda.getBody().accept(this);
		final LogicalExpression newBody = result;

		// Reset the variables set, in case it was changed.
		variables = originalSet;

		// Recreate if changed, otherwise return the original.
		if (newBody != lambda.getBody()) {
			result = new Lambda(lambda.getArgument(), newBody);
		} else {
			result = lambda;
		}
	}

	@Override
	public void visit(Literal literal) {
		if (!literal.containsFreeVariables(variables)) {
			result = literal;
			return;
		}

		literal.getPredicate().accept(this);
		final LogicalExpression newPredicate = result;

		final int len = literal.numArgs();
		final LogicalExpression[] newArgs = new LogicalExpression[len];
		boolean argChanged = false;
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);
			arg.accept(this);
			newArgs[i] = result;
			if (arg != result) {
				argChanged = true;
			}
		}

		if (argChanged) {
			result = new Literal(newPredicate, newArgs);
		} else if (newPredicate != literal.getPredicate()) {
			result = new Literal(newPredicate, literal);
		} else {
			result = literal;
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		result = logicalConstant;
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (variables.contains(variable)) {
			if (!oldVariablesToNew.containsKey(variable)) {
				oldVariablesToNew.put(variable,
						new Variable(variable.getType()));
			}
			result = oldVariablesToNew.get(variable);
		} else {
			result = variable;
		}
	}
}
