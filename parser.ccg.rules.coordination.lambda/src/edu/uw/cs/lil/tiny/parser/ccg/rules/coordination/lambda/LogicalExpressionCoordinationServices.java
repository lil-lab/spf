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
package edu.uw.cs.lil.tiny.parser.ccg.rules.coordination.lambda;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ApplyAndSimplify;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ReplaceVariablesIfPresent;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.coordination.ICoordinationServices;

public class LogicalExpressionCoordinationServices implements
		ICoordinationServices<LogicalExpression> {
	
	private final LogicalConstant						baseConjunctionConstant;
	
	private final String								baseConjunctionName;
	
	private final LogicalConstant						baseDisjunctionConstant;
	private final String								baseDisjunctionName;
	
	private final ICategoryServices<LogicalExpression>	categoryServices;
	
	public LogicalExpressionCoordinationServices(
			LogicalConstant baseConjunctionConstant,
			LogicalConstant baseDisjunctionConstant,
			ICategoryServices<LogicalExpression> categoryServices) {
		this.baseConjunctionConstant = baseConjunctionConstant;
		this.baseDisjunctionConstant = baseDisjunctionConstant;
		this.categoryServices = categoryServices;
		this.baseConjunctionName = baseConjunctionConstant.getBaseName();
		this.baseDisjunctionName = baseDisjunctionConstant.getBaseName();
	}
	
	@Override
	public LogicalExpression applyCoordination(LogicalExpression function1,
			LogicalExpression coordination) {
		// Verify the coordination
		if (!(coordination instanceof Literal)
				|| !isCoordinator(((Literal) coordination).getPredicate(),
						((Literal) coordination).getArguments().size())) {
			return null;
		}
		
		final Literal literal = (Literal) coordination;
		final Type argType = ((ComplexType) literal.getPredicate().getType())
				.getDomain();
		
		// Verify the function is a Lambda operator and the type of the argument
		// fits the coordination
		final Lambda functionLambda;
		if (function1 instanceof Lambda) {
			functionLambda = (Lambda) function1;
		} else {
			return null;
		}
		if (!argType.isExtending(functionLambda.getArgument().getType())) {
			return null;
		}
		final Variable outerMostVariable = functionLambda.getArgument();
		
		// Identify shared arguments (Lambda operators and the variables they
		// bind), locate the outer-most truth-typed expression
		LogicalExpression innerBody = functionLambda.getBody();
		final List<Variable> sharedVariables = new LinkedList<Variable>();
		final Set<Variable> sharedVariablesSet = new HashSet<Variable>();
		while (innerBody instanceof Lambda) {
			sharedVariables.add(((Lambda) innerBody).getArgument());
			sharedVariablesSet.add(((Lambda) innerBody).getArgument());
			innerBody = ((Lambda) innerBody).getBody();
		}
		if (!innerBody.getType().equals(
				LogicLanguageServices.getTypeRepository().getTruthValueType())) {
			// Case the inner body is not truth-typed
			return null;
		}
		
		// Create a Lambda expression using only the outer Lambda operator and
		// the found truth-type expression as body
		final Lambda reducedFunction = new Lambda(outerMostVariable, innerBody);
		
		// Apply the recently created expression to all arguments
		final List<LogicalExpression> coordinationItems = new ArrayList<LogicalExpression>(
				literal.getArguments().size());
		for (final LogicalExpression coordinate : literal.getArguments()) {
			final LogicalExpression application = ApplyAndSimplify.of(
					reducedFunction, ReplaceVariablesIfPresent.of(coordinate,
							sharedVariablesSet));
			if (application == null) {
				return null;
			} else {
				coordinationItems.add(application);
			}
		}
		
		// Create a literal (conjunction or disjunction, depending on the
		// coordinator) by coordinating all the results of the application
		final Literal coordinationLiteral;
		if (isConjunctionCoordinator((LogicalConstant) literal.getPredicate())) {
			coordinationLiteral = new Literal(
					LogicLanguageServices.getConjunctionPredicate(),
					coordinationItems);
		} else if (isDisjunctionCoordinator((LogicalConstant) literal
				.getPredicate())) {
			coordinationLiteral = new Literal(
					LogicLanguageServices.getDisjunctionPredicate(),
					coordinationItems);
		} else {
			throw new IllegalStateException("invalid coordinator: "
					+ literal.getPredicate());
		}
		
		// Wrap with the shared Lambda operators and return the complete
		// expression
		LogicalExpression wrappedCoordination = coordinationLiteral;
		final ListIterator<Variable> iterator = sharedVariables
				.listIterator(sharedVariables.size());
		while (iterator.hasPrevious()) {
			wrappedCoordination = new Lambda(iterator.previous(),
					wrappedCoordination);
		}
		
		return Simplify.of(wrappedCoordination);
	}
	
	@Override
	public LogicalExpression createPartialCoordination(
			LogicalExpression coordinated, LogicalExpression coordinator) {
		// Create a binary predicate from coordinator
		if (!isBaseCoordinator(coordinator)) {
			return null;
		}
		
		// The type of the coordinated element is generalized to create the
		// coordination predicate
		final Type argType = LogicLanguageServices.getTypeRepository()
				.generalizeType(coordinated.getType());
		final LogicalConstant coordinationPredicate = createPredicate(
				(LogicalConstant) coordinator, 2, argType);
		
		// Create a literal using the predicate, with a variable as the
		// first argument and 'coordinated' as the second, and wrap the literal
		// with a lambda expression binding the varaible.
		final List<LogicalExpression> arguments = new ArrayList<LogicalExpression>(
				2);
		final Variable variable = new Variable(argType);
		arguments.add(variable);
		arguments.add(coordinated);
		return new Lambda(variable, new Literal(coordinationPredicate,
				arguments));
	}
	
	@Override
	public LogicalExpression createSimpleCoordination(
			LogicalExpression coordinated, LogicalExpression coordinator) {
		// Create a binary predicate from coordinator
		if (!isBaseCoordinator(coordinator)) {
			return null;
		}
		
		if (LogicLanguageServices.getTypeRepository().getTruthValueType()
				.equals(coordinated.getType())) {
			// Case coordinating truth-typed expressions
			final LogicalExpression coordinationPredicate;
			if (isConjunctionCoordinator((LogicalConstant) coordinator)) {
				coordinationPredicate = LogicLanguageServices
						.getConjunctionPredicate();
			} else if (isDisjunctionCoordinator((LogicalConstant) coordinator)) {
				coordinationPredicate = LogicLanguageServices
						.getDisjunctionPredicate();
			} else {
				throw new IllegalStateException("invalid coordinator: "
						+ coordinator);
			}
			
			final Variable variable = new Variable(LogicLanguageServices
					.getTypeRepository().getTruthValueType());
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					2);
			args.add(variable);
			args.add(coordinated);
			return Simplify.of(new Literal(coordinationPredicate, args));
		}
		
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final LogicalExpressionCoordinationServices other = (LogicalExpressionCoordinationServices) obj;
		if (baseConjunctionConstant == null) {
			if (other.baseConjunctionConstant != null) {
				return false;
			}
		} else if (!baseConjunctionConstant
				.equals(other.baseConjunctionConstant)) {
			return false;
		}
		if (baseConjunctionName == null) {
			if (other.baseConjunctionName != null) {
				return false;
			}
		} else if (!baseConjunctionName.equals(other.baseConjunctionName)) {
			return false;
		}
		if (baseDisjunctionConstant == null) {
			if (other.baseDisjunctionConstant != null) {
				return false;
			}
		} else if (!baseDisjunctionConstant
				.equals(other.baseDisjunctionConstant)) {
			return false;
		}
		if (baseDisjunctionName == null) {
			if (other.baseDisjunctionName != null) {
				return false;
			}
		} else if (!baseDisjunctionName.equals(other.baseDisjunctionName)) {
			return false;
		}
		if (categoryServices == null) {
			if (other.categoryServices != null) {
				return false;
			}
		} else if (!categoryServices.equals(other.categoryServices)) {
			return false;
		}
		return true;
	}
	
	@Override
	public LogicalExpression expandCoordination(LogicalExpression coordination) {
		if (coordination instanceof Literal
				&& isCoordinator(((Literal) coordination).getPredicate(),
						((Literal) coordination).getArguments().size())) {
			final Literal literal = (Literal) coordination;
			final Type argType = ((ComplexType) literal.getPredicate()
					.getType()).getDomain();
			final List<LogicalExpression> expandedArgs = new ArrayList<LogicalExpression>(
					literal.getArguments().size() + 1);
			// The variable referring to the new argument
			final LogicalExpression variable = new Variable(argType);
			expandedArgs.add(variable);
			expandedArgs.addAll(literal.getArguments());
			return new Literal(createPredicate(
					(LogicalConstant) literal.getPredicate(), literal
							.getArguments().size() + 1, argType), expandedArgs);
		} else {
			return null;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((baseConjunctionConstant == null) ? 0
						: baseConjunctionConstant.hashCode());
		result = prime
				* result
				+ ((baseConjunctionName == null) ? 0 : baseConjunctionName
						.hashCode());
		result = prime
				* result
				+ ((baseDisjunctionConstant == null) ? 0
						: baseDisjunctionConstant.hashCode());
		result = prime
				* result
				+ ((baseDisjunctionName == null) ? 0 : baseDisjunctionName
						.hashCode());
		result = prime
				* result
				+ ((categoryServices == null) ? 0 : categoryServices.hashCode());
		return result;
	}
	
	private LogicalConstant createPredicate(LogicalConstant constant,
			int numArgs, Type argType) {
		// Using truth type as the final return type, 't' is a simple
		// placeholder here, it's completely meaningless.
		Type type = LogicLanguageServices.getTypeRepository()
				.getTypeCreateIfNeeded(
						LogicLanguageServices.getTypeRepository()
								.getTruthValueType(), argType);
		for (int i = 1; i < numArgs; ++i) {
			type = LogicLanguageServices.getTypeRepository()
					.getTypeCreateIfNeeded(type, argType);
		}
		return LogicalConstant.createDynamic(
				LogicalConstant.makeName(constant.getBaseName(), type), type);
	}
	
	private boolean isBaseCoordinator(LogicalExpression predicate) {
		return baseDisjunctionConstant.equals(predicate)
				|| baseConjunctionConstant.equals(predicate);
	}
	
	private boolean isConjunctionCoordinator(LogicalConstant predicate) {
		return predicate.getBaseName().equals(baseConjunctionName);
	}
	
	private boolean isCoordinator(LogicalExpression predicate, int numArgs) {
		if (predicate instanceof LogicalConstant
				&& (isConjunctionCoordinator((LogicalConstant) predicate) || isDisjunctionCoordinator((LogicalConstant) predicate))
				&& predicate.getType() instanceof ComplexType) {
			final ComplexType predicateType = (ComplexType) predicate.getType();
			Type current = predicateType;
			int count = 0;
			while (current instanceof ComplexType) {
				++count;
				current = ((ComplexType) current).getRange();
			}
			return count == numArgs;
		} else {
			return false;
		}
	}
	
	private boolean isDisjunctionCoordinator(LogicalConstant predicate) {
		return predicate.getBaseName().equals(baseDisjunctionName);
	}
	
}
