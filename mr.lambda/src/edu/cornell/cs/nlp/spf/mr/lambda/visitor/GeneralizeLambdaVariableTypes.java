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

import java.util.HashMap;
import java.util.Map;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Replace the types of all variables with the most generalized type.
 *
 * @author Yoav Artzi
 */
public class GeneralizeLambdaVariableTypes implements ILogicalExpressionVisitor {

	private final Map<Variable, Variable>	newVariableMapping	= new HashMap<Variable, Variable>();

	private LogicalExpression				result				= null;

	public static LogicalExpression of(LogicalExpression exp) {
		final GeneralizeLambdaVariableTypes visitor = new GeneralizeLambdaVariableTypes();
		visitor.visit(exp);
		return visitor.result;
	}

	@Override
	public void visit(Lambda lambda) {
		final Variable varibleToUse;
		final Type generalizedType = LogicLanguageServices.getTypeRepository()
				.generalizeType(lambda.getArgument().getType());
		if (lambda.getArgument().getType().equals(generalizedType)) {
			varibleToUse = lambda.getArgument();
		} else {
			varibleToUse = new Variable(generalizedType);
			newVariableMapping.put(lambda.getArgument(), varibleToUse);
		}
		lambda.getBody().accept(this);
		if (varibleToUse != lambda.getArgument() || lambda.getBody() != result) {
			result = new Lambda(varibleToUse, result);
		} else {
			result = lambda;
		}
	}

	@Override
	public void visit(Literal literal) {

		// Visit the predicate
		literal.getPredicate().accept(this);
		final LogicalExpression newPredicate;
		if (result == literal.getPredicate()) {
			newPredicate = literal.getPredicate();
		} else {
			if (result == null) {
				return;
			}
			newPredicate = result;
		}
		// Go over the arguments
		final int len = literal.numArgs();
		final LogicalExpression[] newArgs = new LogicalExpression[len];
		boolean argChanged = false;
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);
			arg.accept(this);
			if (result == null) {
				return;
			}
			newArgs[i] = result;
			if (result != arg) {
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
		// Nothing to do
		result = logicalConstant;
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (newVariableMapping.containsKey(variable)) {
			result = newVariableMapping.get(variable);
		} else {
			result = variable;
		}
	}

}
