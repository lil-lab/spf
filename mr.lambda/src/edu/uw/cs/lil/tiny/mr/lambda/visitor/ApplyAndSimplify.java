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
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpressionRuntimeException;
import edu.uw.cs.lil.tiny.mr.lambda.Term;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Apply a functional expression to an argument. The result is simplified. The
 * visitor also takes care of instances where the two logical expressions use
 * the same variable object (by replacing as needed).
 * 
 * @author Luke Zettlemoyer
 */
public class ApplyAndSimplify extends AbstrcatSimplify {
	/**
	 * Indicates if the arguments was applied once already. We need to track if
	 * the argument is used more than once in the consuming function.
	 */
	private boolean							appliedOnceAlready	= false;
	
	/**
	 * The argument of the application operation, e.g. x in f(x).
	 */
	private final LogicalExpression			appliedToArg;
	
	/**
	 * The variables of the argument {@link #appliedToArg}. We track them so we
	 * can make sure there's no overlap of variables between the two
	 * expressions. If such an overlap exists, we replace variables.
	 */
	private final Set<Variable>				argVars;
	
	/**
	 * Mapping of variables in the given logical expressions to new variables
	 * that don't overlap with the variables of the argument. This mapping
	 * contains only variables that were replaced.
	 */
	private final Map<Variable, Variable>	oldVariablesToNew	= new HashMap<Variable, Variable>();
	
	/**
	 * The variables that represents the argument in the given logical
	 * expression
	 */
	private final LogicalExpression			rootVariable;
	
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
		this.argVars = GetVariables.of(appliedToArg);
	}
	
	public static LogicalExpression of(LogicalExpression func,
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
			
			return visitor.tempReturn;
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
	
	private static LogicalExpression literalApplication(Literal literal,
			LogicalExpression arg) {
		final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
				literal.getArguments().size() + 1);
		newArgs.addAll(literal.getArguments());
		// Append the argument to the list of arguments in the literal, verify
		// that it doesn't contain any variables (identical objects) from the
		// literal
		newArgs.add(ReplaceVariablesIfPresent.of(arg, GetVariables.of(literal)));
		return new Literal(literal.getPredicate(), newArgs);
	}
	
	private static LogicalExpression termApplication(Term exp,
			LogicalExpression arg) {
		final List<LogicalExpression> arguments = new ArrayList<LogicalExpression>(
				1);
		arguments.add(arg);
		// Verify that the consuming function (the Term) doesn't contain any
		// variables (identical objects) from the given argument and create the
		// new literal
		return new Literal(GetVariables.of(arg).contains(exp) ? new Variable(
				exp.getType()) : exp, arguments);
	}
	
	@Override
	public void visit(Variable variable) {
		if (variable == rootVariable) {
			if (appliedOnceAlready) {
				// Case the argument was already used once in the consuming
				// function. Need to replace its variables, so these objects
				// won't be shared.
				tempReturn = ReplaceBoundVariables.of(appliedToArg);
			} else {
				appliedOnceAlready = true;
				tempReturn = appliedToArg;
			}
		} else if (argVars.contains(variable)) {
			if (!oldVariablesToNew.containsKey(variable)) {
				oldVariablesToNew.put(variable,
						new Variable(variable.getType()));
			}
			tempReturn = oldVariablesToNew.get(variable);
		} else {
			tempReturn = variable;
		}
	}
	
}
