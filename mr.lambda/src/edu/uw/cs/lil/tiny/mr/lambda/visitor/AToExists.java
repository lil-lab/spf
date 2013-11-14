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
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Given a logical expression, replaces all occurrences of a:<<e,t>,e> with an
 * existential quantifier (inserted at the right level). Currently works only
 * for entities (e.g., not for actions or events).
 * 
 * @author Yoav Artzi
 */
public class AToExists implements ILogicalExpressionVisitor {
	public static final ILogger																		LOG			= LoggerFactory
																														.create(AToExists.class);
	
	private static final Stack<Pair<Variable, ? extends LogicalExpression>>							EMPTY_STACK	= new Stack<Pair<Variable, ? extends LogicalExpression>>();
	private final LogicalExpression																	aPredicate;
	private final LogicalExpression																	existsPredicate;
	private Pair<? extends LogicalExpression, Stack<Pair<Variable, ? extends LogicalExpression>>>	result		= null;
	
	private AToExists(LogicalExpression existsPredicate,
			LogicalConstant aPredicate) {
		this.existsPredicate = existsPredicate;
		this.aPredicate = aPredicate;
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			LogicalExpression existsPredicate, LogicalConstant aPredicate,
			Map<Type, LogicalConstant> equalsPredicates) {
		final AToExists visitor = new AToExists(existsPredicate, aPredicate);
		visitor.visit(exp);
		
		// If the stack still contains variables, try to wrap the result and
		// return it
		final LogicalExpression output = visitor.noUpperLevelWrapIfPossible(
				visitor.result.first(), visitor.result.second());
		
		if (visitor.result.second().isEmpty()) {
			return Simplify.of(output);
		} else if (visitor.result.second().size() == 1) {
			// Case a single item on the stack, there are a few cases
			
			// Get the type of the current logical expression and the
			// appropriate equals
			final Type expType = LogicLanguageServices.getTypeRepository()
					.generalizeType(visitor.result.first().getType());
			final LogicalConstant equals = equalsPredicates.get(expType);
			
			if (equals != null) {
				// Create an 'equals' structure
				
				// The variable that threads the equals
				final Variable variable = new Variable(expType);
				
				// Create the equals literal
				final LogicalExpression equalsLiteral = new Literal(equals,
						ListUtils.createList(variable, visitor.result.first()));
				
				// Conjunction of the equals literal and the rest
				final Literal conjLiteral = new Literal(
						LogicLanguageServices.getConjunctionPredicate(),
						ListUtils.createList(visitor.result.second().peek()
								.second(), equalsLiteral));
				
				// Create the exists literal for the variable of the indefinite
				// quatifier
				final LogicalExpression existsLiteral = new Literal(
						visitor.existsPredicate,
						ListUtils
								.createSingletonList((LogicalExpression) new Lambda(
										visitor.result.second().peek().first(),
										conjLiteral)));
				
				// Create the final lambda operator, simplify and return
				return Simplify.of(new Lambda(variable, existsLiteral));
			} else if (isVacuousLambda(output)) {
				final Lambda lambda = (Lambda) output;
				
				// Create the existential
				final Literal existential = new Literal(
						existsPredicate,
						ListUtils
								.createSingletonList((LogicalExpression) new Lambda(
										visitor.result.second().peek().first(),
										visitor.result.second().peek().second())));
				
				return Simplify
						.of(new Lambda(lambda.getArgument(), existential));
			} else {
				LOG.error(
						"ERROR: No 'equals' for %s and not a vacuous lambda, failed to process: %s",
						expType, exp);
				throw new IllegalStateException(
						String.format(
								"ERROR: No 'equals' for %s and not a vacuous lambda, failed to process: %s",
								expType, exp));
			}
		} else {
			// Case conversion failed
			LOG.error("ERROR: Failed to convert indefinite quantifier to an existential one");
			return null;
		}
	}
	
	/**
	 * Check if the expression follows the template (lambda $0:x $1:y).
	 * 
	 * @param exp
	 * @return
	 */
	private static boolean isVacuousLambda(LogicalExpression exp) {
		if (exp instanceof Lambda) {
			final Variable arg = ((Lambda) exp).getArgument();
			final LogicalExpression body = ((Lambda) exp).getBody();
			if (body instanceof Variable && arg != body) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
		if (result.first() != lambda.getBody()) {
			// Case body changed
			result = Pair.of(new Lambda(lambda.getArgument(), result.first()),
					result.second());
		} else {
			result = Pair.of(lambda, result.second());
		}
	}
	
	@Override
	public void visit(Literal literal) {
		if (aPredicate.equals(literal.getPredicate())) {
			if (literal.getArguments().size() == 1) {
				final Lambda innerLambda = makeEntityToTruthLambda(literal
						.getArguments().get(0));
				if (innerLambda == null) {
					throw new IllegalStateException("Invalid A expression: "
							+ literal);
				}
				innerLambda.getBody().accept(this);
				final Stack<Pair<Variable, ? extends LogicalExpression>> currentStack = new Stack<Pair<Variable, ? extends LogicalExpression>>();
				// To avoid the case of variables shared through various
				// structures, replace the current variable with a new one in
				// the inner body
				final Variable newVariable = new Variable(innerLambda
						.getArgument().getType());
				currentStack.push(Pair.of(
						newVariable,
						ReplaceExpression.of(result.first(),
								innerLambda.getArgument(), newVariable)));
				// Append stack from sub tree
				currentStack.addAll(result.second());
				result = Pair.of(newVariable, currentStack);
			} else {
				throw new IllegalStateException("invalid A expression: "
						+ literal);
			}
		} else {
			literal.getPredicate().accept(this);
			final Pair<? extends LogicalExpression, Stack<Pair<Variable, ? extends LogicalExpression>>> newPredPair = result;
			
			final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
					literal.getArguments().size());
			final List<Stack<Pair<Variable, ? extends LogicalExpression>>> newStacks = new ArrayList<Stack<Pair<Variable, ? extends LogicalExpression>>>(
					literal.getArguments().size());
			boolean argsChanged = false;
			for (final LogicalExpression arg : literal.getArguments()) {
				arg.accept(this);
				newArgs.add(result.first());
				newStacks.add(result.second());
				if (arg != result.first()) {
					argsChanged = true;
				}
			}
			
			// Merge all stacks
			final Stack<Pair<Variable, ? extends LogicalExpression>> mergedStack = new Stack<Pair<Variable, ? extends LogicalExpression>>();
			for (final Stack<Pair<Variable, ? extends LogicalExpression>> stack : newStacks) {
				mergedStack.addAll(stack);
			}
			
			if (argsChanged || newPredPair.first() != literal.getPredicate()) {
				result = Pair.of(new Literal(newPredPair.first(), newArgs),
						mergedStack);
			} else {
				result = Pair.of(literal, mergedStack);
			}
		}
		
		// Try to wrap the literal with existential quanitifiers
		result = Pair.of(wrapIfPossible(result.first(), result.second()),
				result.second());
		
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Nothing to do
		result = Pair.of(logicalConstant, EMPTY_STACK);
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		// Nothing to do
		result = Pair.of(variable, EMPTY_STACK);
	}
	
	private boolean isEntityToTruthType(Type type) {
		if (type.isComplex()) {
			final Type genType = LogicLanguageServices.getTypeRepository()
					.generalizeType(type);
			return LogicLanguageServices.getTypeRepository().getEntityType()
					.equals(((ComplexType) genType).getDomain())
					&& LogicLanguageServices.getTypeRepository()
							.getTruthValueType()
							.equals(((ComplexType) genType).getRange());
		} else {
			return false;
		}
	}
	
	private Lambda makeEntityToTruthLambda(LogicalExpression exp) {
		if (exp instanceof Lambda && isEntityToTruthType(exp.getType())) {
			return (Lambda) exp;
		} else {
			return null;
		}
	}
	
	private LogicalExpression noUpperLevelWrapIfPossible(LogicalExpression exp,
			Stack<Pair<Variable, ? extends LogicalExpression>> stack) {
		if (stack.size() == 1
				&& exp.equals(stack.peek().first())
				&& LogicLanguageServices.getTypeRepository()
						.getTruthValueType()
						.equals(stack.peek().second().getType())) {
			final Pair<Variable, ? extends LogicalExpression> pop = stack.pop();
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					1);
			args.add(new Lambda(pop.first(), pop.second()));
			return new Literal(existsPredicate, args);
		} else {
			return exp;
		}
	}
	
	private LogicalExpression wrapIfPossible(LogicalExpression exp,
			Stack<Pair<Variable, ? extends LogicalExpression>> stack) {
		if (LogicLanguageServices.getTypeRepository().getTruthValueType()
				.equals(exp.getType())) {
			LogicalExpression ret = exp;
			while (!stack.isEmpty()) {
				final Pair<Variable, ? extends LogicalExpression> pop = stack
						.pop();
				final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
						1);
				args.add(new Lambda(pop.first(), new Literal(
						LogicLanguageServices.getConjunctionPredicate(),
						ListUtils.createList(pop.second(), ret))));
				ret = new Literal(existsPredicate, args);
			}
			return ret;
		} else {
			return exp;
		}
	}
	
}
