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
package edu.cornell.cs.nlp.spf.mr.lambda.exec.naive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.log.thread.InterruptedRuntimeException;

/**
 * Generic evaluation visitor for {@link LogicalExpression}. Stops when
 * executing thread receives an interrupt and throws a
 * {@link InterruptedRuntimeException}.
 *
 * @author Yoav Artzi
 */
public class Evaluation implements ILogicalExpressionVisitor {
	public static final ILogger			LOG			= LoggerFactory
															.create(Evaluation.class);
	private final Map<Variable, Object>	denotations	= new HashMap<Variable, Object>();
	private final IEvaluationServices	services;
	protected Object					result		= null;

	protected Evaluation(IEvaluationServices services) {
		this.services = services;
	}

	public static Object of(LogicalExpression exp, IEvaluationServices services) {
		final Evaluation visitor = new Evaluation(services);
		visitor.visit(exp);
		return visitor.result;
	}

	/**
	 * Decomposes a logical expression as a SELECT query.
	 *
	 * @param exp
	 * @return Pair of queried variables and SELECT body. If not a SELECT query,
	 *         returns null.
	 */
	private static Pair<List<Variable>, LogicalExpression> decomposeLogicalExpressionAsSelect(
			LogicalExpression exp) {
		LogicalExpression currentBody = exp;
		final List<Variable> queryVariables = new LinkedList<Variable>();
		while (currentBody instanceof Lambda) {
			final Lambda lambda = (Lambda) currentBody;
			if (lambda.getArgument().getType().isComplex()) {
				// Case argument is complex
				return null;
			} else {
				queryVariables.add(lambda.getArgument());
				currentBody = lambda.getBody();
			}
		}

		if (currentBody.getType().isComplex()) {
			return null;
		} else {
			return Pair.of(queryVariables, currentBody);
		}
	}

	private static void testInterruption() {
		if (Thread.interrupted()) {
			throw new InterruptedRuntimeException(new InterruptedException(
					"Evaluation interuppted"));
		}
	}

	@Override
	public void visit(Lambda lambda) {
		testInterruption();
		if (services.isCached(lambda)) {
			// Case is cache
			result = services.getFromCache(lambda);
		} else {
			visit(lambda, false);
			// Cache
			services.cacheResult(lambda, result);
		}
	}

	@Override
	public void visit(Literal literal) {
		testInterruption();
		// Try to get from cache
		if (services.isCached(literal)) {
			result = services.getFromCache(literal);
			return;
		}

		// If it's a coordination update the result variable with the
		// default return value (T for conjunction, F for disjunction)
		final int len = literal.numArgs();
		if (LogicLanguageServices.isCoordinationPredicate(literal
				.getPredicate())) {
			// Case coordination predicate, can short-circuit

			// Get short-circuiting argument value
			final Boolean shortCircuitingValue;
			if (LogicLanguageServices.getConjunctionPredicate().equals(
					literal.getPredicate())) {
				shortCircuitingValue = Boolean.FALSE;
			} else if (LogicLanguageServices.getDisjunctionPredicate().equals(
					literal.getPredicate())) {
				shortCircuitingValue = Boolean.TRUE;
			} else {
				throw new IllegalStateException(
						"unhandled coordination predicate: " + literal);
			}

			for (int i = 0; i < len; ++i) {
				literal.getArg(i).accept(this);
				if (result == null || shortCircuitingValue.equals(result)) {
					// Cache
					services.cacheResult(literal, result);

					return;
				}

			}

			// Case not short-circuited, so return the default value
			result = !shortCircuitingValue;
		} else {
			// Case not a coordination, no shortcuts, use domain executors to
			// evaluate

			// Iterate over the arguments
			final Object[] evalArgs = new Object[len];
			int counter = 0;
			for (int i = 0; i < len; ++i) {
				literal.getArg(i).accept(this);
				if (result == null) {
					// If failed to evaluate, propagate failure to literal

					// Cache
					services.cacheResult(literal, result);

					return;
				} else {
					evalArgs[counter] = result;
				}
				++counter;
			}

			// Execute predicate with arguments, return result
			result = services.evaluateLiteral(literal.getPredicate(), evalArgs);
		}

		// Cache
		services.cacheResult(literal, result);
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		testInterruption();
		// Try to get from cache
		if (services.isCached(logicalConstant)) {
			result = services.getFromCache(logicalConstant);
			return;
		}

		if (logicalConstant.equals(LogicLanguageServices.getTrue())) {
			result = true;
		} else if (logicalConstant.equals(LogicLanguageServices.getFalse())) {
			result = false;
		} else {
			// call domain services to process
			result = services.evaluateConstant(logicalConstant);
		}

		// Cache
		services.cacheResult(logicalConstant, result);
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		testInterruption();
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		testInterruption();
		// Variables are not cached, since their denotation constantly changes

		// Current denotation
		result = denotations.get(variable);
	}

	protected boolean isPartialLiteral(Literal literal) {
		// Count number of arguments on predicate type
		Type type = literal.getPredicateType();
		int numArgs = 0;
		while (type.isComplex()) {
			if (type instanceof RecursiveComplexType) {
				numArgs += ((RecursiveComplexType) type).getMinArgs();
				type = ((RecursiveComplexType) type).getFinalRange();
			} else {
				++numArgs;
				type = type.getRange();
			}
		}

		return literal.numArgs() < numArgs;
	}

	/**
	 * This method doesn't support caching. Calls to this method should handle
	 * caching.
	 *
	 * @param lambda
	 * @param shortcircuit
	 */
	protected void visit(Lambda lambda, boolean shortcircuit) {
		// Get the queried variables (the ones we are SELECTing on) and the body
		// of the select operation. The type of the logical expression has to be
		// <?,<?,<?...<?,t>..>>>, with all arguments being primitive types and
		// the final body being type 't'. The query variables are all these
		// arguments.
		final Pair<List<Variable>, LogicalExpression> selectDecomposition = decomposeLogicalExpressionAsSelect(lambda);
		if (selectDecomposition != null) {
			// Case SELECT expression
			final LogicalExpression queryBody = selectDecomposition.second();
			final List<Variable> queryVariables = selectDecomposition.first();

			// Collect all possible denotations
			final List<Iterable<?>> allDenotations = new ArrayList<Iterable<?>>(
					queryVariables.size());
			for (final Variable variable : queryVariables) {
				allDenotations.add(services.getAllDenotations(variable));
			}

			LOG.debug("Lambda SELECT execution: query_variables=%s, body=%s",
					queryVariables, queryBody);
			LOG.debug("Denotations: %s", allDenotations);

			final boolean truthTypedBody = LogicLanguageServices
					.getTypeRepository().getTruthValueType()
					.equals(queryBody.getType());

			// Try all possible combinations of denotations
			final LambdaResult lambdaResult = new LambdaResult(
					queryVariables.size());
			for (final List<?> tuple : CollectionUtils
					.cartesianProduct(allDenotations)) {
				// The two iterables are synced
				final Iterator<?> denotationsIterator = tuple.iterator();
				final Iterator<Variable> variablesIterator = queryVariables
						.iterator();
				while (denotationsIterator.hasNext()) {
					final Object denotation = denotationsIterator.next();
					final Variable variable = variablesIterator.next();
					denotations.put(variable, denotation);
					services.denotationChanged(variable);
				}
				LOG.debug("Denotation: %s", denotations);
				queryBody.accept(this);

				// Ignore denotations that evaluate the body to null, since it's
				// an indication towards invalid arrity or typing
				if (result != null
						&& (!truthTypedBody || Boolean.TRUE.equals(result))) {
					// Case truth-typed body, return only tuples that
					// evaluate to 'true', otherwise all tuples are welcome
					lambdaResult.addTuple(new Tuple(tuple.toArray(), result));
					if (shortcircuit) {
						break;
					}
				}
			}

			// Clean the cache for all variables
			for (final Variable variable : queryVariables) {
				services.denotationChanged(variable);
			}

			// Update result
			result = lambdaResult;

			// Remove variable from denotations map
			for (final Variable variable : queryVariables) {
				denotations.remove(variable);
			}
		} else {
			throw new IllegalArgumentException("invalid lambda: " + lambda);
		}
	}

}
