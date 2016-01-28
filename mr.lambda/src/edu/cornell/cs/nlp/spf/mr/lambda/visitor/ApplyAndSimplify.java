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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionRuntimeException;
import edu.cornell.cs.nlp.spf.mr.lambda.Term;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Apply a functional expression to an argument. The result is simplified. The
 * visitor also takes care of instances where the two logical expressions use
 * the same variable object (by replacing as needed).
 *
 * @author Luke Zettlemoyer
 */
public class ApplyAndSimplify extends AbstrcatSimplify {

	/**
	 * The argument of the application operation, e.g. x in f(x).
	 */
	private final LogicalExpression	appliedToArg;

	/**
	 * The variables that represents the argument in the given logical
	 * expression
	 */
	private final Variable			rootVariable;

	/**
	 * This constructor is private as a small part of the logic is in the static
	 * {@link #of(LogicalExpression, LogicalExpression, boolean, boolean)}
	 * method. Therefore, this visitor is not independent.
	 */
	private ApplyAndSimplify(LogicalExpression appliedToArg,
			Variable rootVariable) {
		super(false);
		this.appliedToArg = appliedToArg;
		this.rootVariable = rootVariable;
	}

	public static LogicalExpression of(LogicalExpression func,
			LogicalExpression arg) {
		// Make sure the argument is safe. Meaning, the set of free variables in
		// both function and argument doens't intersect. For that purpose,
		// replace all such objects in the argument.
		final LogicalExpression safeArg = ReplaceFreeVariablesIfPresent.of(arg,
				func.getFreeVariables());
		return ofUnsafe(func, safeArg);
	}

	private static LogicalExpression literalApplication(Literal literal,
			LogicalExpression arg) {
		final int len = literal.numArgs();
		final LogicalExpression[] newArgs = new LogicalExpression[len + 1];
		literal.copyArgsIntoArray(newArgs, 0, 0, len);
		// Append the argument to the list of arguments in the literal, verify
		// that it doesn't contain any variables (identical objects) from the
		// literal.
		newArgs[len] = arg;
		return new Literal(literal.getPredicate(), newArgs);
	}

	private static LogicalExpression termApplication(Term exp,
			LogicalExpression arg) {
		final LogicalExpression[] arguments = new LogicalExpression[1];
		arguments[0] = arg;
		// Verify that the consuming function (the Term) doesn't contain any
		// variables (identical objects) from the given argument and create the
		// new literal.
		return new Literal(exp, arguments);
	}

	/**
	 * Does apply-and-simplify without replacing any free variables. This method
	 * should be used with extreme caution.
	 */
	static LogicalExpression ofUnsafe(LogicalExpression func,
			LogicalExpression arg) {
		// Verify type matching. The functor must be have a complex type, and
		// need to be in some kind of parent-child relationship with the
		// argument, as we allow flexible typing syntax-wise.
		if (!func.getType().isComplex()
				|| !LogicLanguageServices.getTypeComparator().verifyArgType(
						func.getType().getDomain(), arg.getType())) {
			// Case typing mismatch
			return null;
		} else if (func instanceof Lambda) {
			// Case the functor is a Lambda expression
			final Lambda lambda = (Lambda) func;
			final Variable variable = lambda.getArgument();

			final ApplyAndSimplify visitor = new ApplyAndSimplify(arg, variable);

			visitor.visit(lambda.getBody());

			return visitor.result;
		} else if (func instanceof Literal) {
			// Case the functor is a literal, append the argument to
			// the end of the arguments list
			return Simplify.of(literalApplication((Literal) func, arg));
		} else if (func instanceof Term) {
			// Case the functor is a variable or logical constant,
			// create the a literal with the functor as predicate and the
			// argument as the only argument in the argument list
			return Simplify.of(termApplication((Term) func, arg));
		} else {
			// Should never happen
			throw new LogicalExpressionRuntimeException(
					"Impossible condition: un-handled logical expression object");
		}
	}

	@Override
	public void visit(Lambda lambda) {
		if (lambda.containsFreeVariable(rootVariable)) {
			super.visit(lambda);
		} else {
			result = lambda;
		}
	}

	@Override
	public void visit(Literal literal) {
		if (literal.containsFreeVariable(rootVariable)) {
			super.visit(literal);
		} else {
			result = literal;
		}
	}

	@Override
	public void visit(Variable variable) {
		if (variable == rootVariable) {
			result = appliedToArg;
		} else {
			result = variable;
		}
	}

}
