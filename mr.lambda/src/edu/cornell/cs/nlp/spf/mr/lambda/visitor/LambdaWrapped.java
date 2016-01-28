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

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Term;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Wraps an expression with needed Lambda operators. The order of arguments is
 * determined by the wrapped expression, so that the typing signature remains
 * the same.
 *
 * @author Yoav Artzi
 */
public class LambdaWrapped implements ILogicalExpressionVisitor {
	private LogicalExpression	tempReturn	= null;

	private LambdaWrapped() {
		// Use through static 'of' method
	}

	public static LogicalExpression of(LogicalExpression exp) {
		final LambdaWrapped visitor = new LambdaWrapped();
		visitor.visit(exp);
		return visitor.tempReturn;
	}

	/**
	 * Assumes the type of exp is complex.
	 *
	 * @param exp
	 * @return
	 */
	private static Lambda wrap(LogicalExpression exp) {
		final Variable newVariable = new Variable(LogicLanguageServices
				.getTypeRepository().generalizeType(exp.getType().getDomain()));
		final LogicalExpression[] args = new LogicalExpression[1];
		args[0] = newVariable;
		LogicalExpression newLiteral = new Literal(exp, args);
		if (newLiteral.getType().isComplex()) {
			newLiteral = wrap(newLiteral);
		} else {
			newLiteral = Simplify.of(newLiteral);
		}
		return new Lambda(newVariable, newLiteral);
	}

	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
		if (tempReturn != lambda.getBody()) {
			tempReturn = new Lambda(lambda.getArgument(), tempReturn);
		} else {
			tempReturn = lambda;
		}
	}

	@Override
	public void visit(Literal literal) {
		final LogicalExpression newPred;
		if (literal.getPredicate() instanceof Term) {
			newPred = literal.getPredicate();
		} else {
			literal.getPredicate().accept(this);
			newPred = tempReturn;
		}

		boolean argChanged = false;
		final int len = literal.numArgs();
		final LogicalExpression[] newArgs = new LogicalExpression[len];
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);
			arg.accept(this);
			newArgs[i] = tempReturn;
			if (tempReturn != arg) {
				argChanged = true;
			}
		}

		final Literal updatedLiteral;
		if (!argChanged && literal.getPredicate() == newPred) {
			updatedLiteral = literal;
		} else if (argChanged) {
			updatedLiteral = new Literal(newPred, newArgs);
		} else {
			updatedLiteral = new Literal(newPred, literal);
		}

		if (updatedLiteral.getType().isComplex()) {
			tempReturn = wrap(updatedLiteral);
		} else {
			tempReturn = updatedLiteral;
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		if (logicalConstant.getType().isComplex()) {
			wrap(logicalConstant).accept(this);
		} else {
			tempReturn = logicalConstant;
		}
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		if (variable.getType().isComplex()) {
			wrap(variable).accept(this);
		} else {
			tempReturn = variable;
		}
	}
}
