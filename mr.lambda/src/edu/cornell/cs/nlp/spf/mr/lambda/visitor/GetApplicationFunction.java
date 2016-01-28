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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.collections.PowerSet;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.comparators.SkolemIdInstanceWrapper;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMappingOverlay;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType;
import edu.cornell.cs.nlp.utils.collections.stackmap.IdentityFastStackMap;

/**
 * Given an an application argument and the result, tries to guess the
 * application function {@link LogicalExpression}. The process is constrained by
 * the maximum size of coordination subset to be considered. Furthermore, when
 * the argument appears more than once in the result, it's assumed all instances
 * are a result of the application reversed. Further limitations probably exist,
 * but no further analysis was done.
 *
 * @author Yoav Artzi
 */
public class GetApplicationFunction implements ILogicalExpressionVisitor {

	private final LogicalExpression	applicationArgument;
	private final Variable			applicationVariable;
	private int						currentDepth	= 0;
	private final int				maxDepth;
	private final int				maxSubsetSize;
	private LogicalExpression		result			= null;
	private boolean					subExpReplaced	= false;

	private GetApplicationFunction(LogicalExpression applicationArgument,
			int maxSubsetSize, int maxDepth) {
		// Use via 'of' function.
		this.applicationArgument = applicationArgument;
		this.maxSubsetSize = maxSubsetSize;
		this.maxDepth = maxDepth;
		this.applicationVariable = new Variable(applicationArgument.getType());
	}

	public static LogicalExpression of(LogicalExpression applicationResult,
			LogicalExpression applicationArgument, int maxSubsetSize) {
		return of(applicationResult, applicationArgument, maxSubsetSize,
				Integer.MAX_VALUE);
	}

	public static LogicalExpression of(LogicalExpression applicationResult,
			LogicalExpression applicationArgument, int maxSubsetSize,
			int maxDepth) {
		// This process relies heavily on how the logical forms look like, so we
		// normalize them in advance by wrapping with lambda terms.
		final LogicalExpression wrappedResult = LambdaWrapped
				.of(applicationResult);
		final LogicalExpression wrappedArgument = LambdaWrapped
				.of(applicationArgument);

		final GetApplicationFunction visitor = new GetApplicationFunction(
				wrappedArgument, maxSubsetSize, maxDepth);
		visitor.visit(wrappedResult);
		if (visitor.subExpReplaced) {
			final LogicalExpression function = Simplify.of(new Lambda(
					visitor.applicationVariable, visitor.result));
			assert wrappedResult.equals(ApplyAndSimplify.of(function,
					wrappedArgument));
			return function;
		} else {
			return null;
		}

	}

	/**
	 * Expose for unit test only.
	 */
	static LogicalExpression processSubExp(
			LogicalExpression applicationResultSubExp,
			LogicalExpression applicationArgument, Variable applicationVariable) {
		if (applicationResultSubExp instanceof Lambda) {
			// Might return 'true' for the body of the lambda term, but never
			// for the lambda term itself.
			return null;
		}

		// Strip all external lambda terms from the application argument and set
		// aside their number and variables.
		LogicalExpression strippedArgument = applicationArgument;
		final Map<Variable, LogicalExpression> strippedVariables = new HashMap<Variable, LogicalExpression>();
		final List<Variable> orderedVariableList = new LinkedList<Variable>();
		while (strippedArgument instanceof Lambda) {
			strippedVariables.put(((Lambda) strippedArgument).getArgument(),
					null);
			orderedVariableList.add(((Lambda) strippedArgument).getArgument());
			strippedArgument = ((Lambda) strippedArgument).getBody();
		}

		final ProcessSubExpression visitor = new ProcessSubExpression(
				strippedArgument, strippedVariables);
		visitor.visit(applicationResultSubExp);

		// Create the replacement logical expression, use the ordered list for
		// this.
		if (visitor.result) {
			if (orderedVariableList.isEmpty()) {
				return applicationVariable;
			} else {
				final LogicalExpression[] replacementLiteralArgs = new LogicalExpression[orderedVariableList
						.size()];
				int i = 0;
				for (final Variable variable : orderedVariableList) {
					final LogicalExpression arg = visitor.externalVariableMapping
							.get(variable);
					if (arg == null) {
						// This is an unlikely event, but can happen, if, for
						// example, the variable is never used.
						return null;
					}
					replacementLiteralArgs[i++] = arg;
				}
				return new Literal(applicationVariable, replacementLiteralArgs);
			}
		} else {
			return null;
		}
	}

	@Override
	public void visit(Lambda lambda) {
		++currentDepth;
		try {
			if (currentDepth > maxDepth) {
				result = lambda;
				return;
			}
			lambda.getBody().accept(this);
			if (result != lambda.getBody()) {
				result = new Lambda(lambda.getArgument(), result);
			} else {
				result = lambda;
			}
		} finally {
			--currentDepth;
		}
	}

	@Override
	public void visit(Literal literal) {
		++currentDepth;
		try {
			if (currentDepth > maxDepth) {
				result = literal;
				return;
			}

			final LogicalExpression replacementExpression = processSubExp(
					literal, applicationArgument, applicationVariable);
			if (replacementExpression != null) {
				result = replacementExpression;
				subExpReplaced = true;
				return;
			}

			final int len = literal.numArgs();
			if (literal.getPredicateType() instanceof RecursiveComplexType) {
				// Track what arguments were processed successfully, so they can
				// be
				// ignored later.
				final Integer[] argIndices = new Integer[len];
				for (int i = 0; i < len; ++i) {
					argIndices[i] = i;
				}
				final List<LogicalExpression> replacementArgs = new ArrayList<LogicalExpression>();
				boolean argChanged = false;
				if (literal.getPredicateType().isOrderSensitive()) {
					// Order sensitive recursive complex type. Can extract
					// subsets
					// of arguments, but must keep order.
					for (int subsetSize = 1; subsetSize < maxSubsetSize; ++subsetSize) {
						for (int start = 0; start < len - subsetSize; start++) {
							// Create a new literal using the original predicate
							// and
							// the subset.
							final LogicalExpression[] subsetArgs = new LogicalExpression[subsetSize];
							boolean skipSubset = false;
							for (int i = start; i < start + subsetSize; ++i) {
								final Integer argIndex = argIndices[i];
								if (argIndex == null) {
									skipSubset = true;
									break;
								} else {
									subsetArgs[i] = literal.getArg(argIndex);
								}
							}
							if (skipSubset) {
								continue;
							}
							// Try to process it.
							final LogicalExpression subsetReplacement = processSubExp(
									subsetSize == 1 ? subsetArgs[0]
											: new Literal(
													literal.getPredicate(),
													subsetArgs),
									applicationArgument, applicationVariable);
							if (subsetReplacement != null) {
								// Mark the arguments in the subset as
								// processed.
								for (int i = start; i < start + subsetSize; ++i) {
									argIndices[i] = null;
								}
								// Save the replacement expression to return it.
								replacementArgs.add(subsetReplacement);
								subExpReplaced = true;
								argChanged = true;
							}
						}
					}
				} else {
					// Order insensitive recursive complex type. Can extract
					// subsets
					// of arguments, regardless of order.
					for (final List<Integer> subset : new PowerSet<Integer>(
							argIndices)) {
						final int subsetSize = subset.size();
						if (subsetSize > 0 && subsetSize <= maxSubsetSize) {
							// Create a new literal using the original predicate
							// and
							// the subset.
							final LogicalExpression[] subsetArgs = new LogicalExpression[subsetSize];
							boolean skipSubset = false;
							for (int i = 0; i < subsetSize; ++i) {
								final Integer argIndex = subset.get(i);
								if (argIndex == null) {
									skipSubset = true;
									break;
								} else {
									subsetArgs[i] = literal.getArg(argIndex);
								}
							}
							if (skipSubset) {
								continue;
							}
							// Try to process it.
							final LogicalExpression subsetReplacement = processSubExp(
									subset.size() == 1 ? subsetArgs[0]
											: new Literal(
													literal.getPredicate(),
													subsetArgs),
									applicationArgument, applicationVariable);
							if (subsetReplacement != null) {
								// Mark the arguments in the subset as
								// processed.
								for (int i = 0; i < subsetSize; ++i) {
									argIndices[subset.get(i)] = null;
								}
								// Save the replacement expression to return it.
								replacementArgs.add(subsetReplacement);
								subExpReplaced = true;
								argChanged = true;
							}
						}
					}
				}

				// Visit the predicate and argument un-processed so far.
				literal.getPredicate().accept(this);
				final LogicalExpression newPredicate = result;
				for (final Integer index : argIndices) {
					if (index != null) {
						final LogicalExpression arg = literal.getArg(index);
						arg.accept(this);
						replacementArgs.add(result);
						if (arg != result) {
							argChanged = true;
						}

					}
				}

				if (argChanged) {
					result = new Literal(
							newPredicate,
							replacementArgs
									.toArray(new LogicalExpression[replacementArgs
											.size()]));
				} else if (literal.getPredicate() != newPredicate) {
					result = new Literal(newPredicate, literal);
				} else {
					result = literal;
				}
			} else {
				// Normal literal.
				literal.getPredicate().accept(this);
				final LogicalExpression newPredicate = result;
				final LogicalExpression[] newArgs = new LogicalExpression[len];
				boolean argChanged = false;
				for (int i = 0; i < len; ++i) {
					final LogicalExpression arg = literal.getArg(i);
					arg.accept(this);
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
		} finally {
			--currentDepth;
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		if (logicalConstant.equals(applicationArgument)) {
			result = applicationVariable;
			subExpReplaced = true;
		} else {
			result = logicalConstant;
		}

	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		// Nothing to do.
		result = variable;
	}

	/**
	 * Tests whether a candidate sub-expression is the result of an application
	 * replacement. Exposed for unit test only.
	 */
	static class ProcessSubExpression implements ILogicalExpressionVisitor {

		private LogicalExpression					argument;
		private Map<Variable, LogicalExpression>	externalVariableMapping;
		private boolean								result	= true;
		private ScopeMapping<Variable, Variable>	scope	= new ScopeMapping<Variable, Variable>(
																	new IdentityFastStackMap<Variable, Variable>(),
																	new IdentityFastStackMap<Variable, Variable>());

		public ProcessSubExpression(LogicalExpression strippedArgument,
				Map<Variable, LogicalExpression> strippedVariables) {
			this.argument = strippedArgument;
			this.externalVariableMapping = strippedVariables;
		}

		@Override
		public void visit(Lambda lambda) {
			if (isDirectlyMatched(lambda)) {
				return;
			}

			if (!(argument instanceof Lambda)) {
				result = false;
				return;
			}

			final Lambda argLambda = (Lambda) argument;

			// Update the variable scope mapping.
			scope.push(lambda.getArgument(), argLambda.getArgument());

			// If the variable of argLambda is used in the stripped variables
			// mapping, we need to remove it from there, since that mapping
			// relies
			// on an unscoped instance.
			final boolean removedFromStrippedVariables;
			final LogicalExpression storedValue;
			if (externalVariableMapping.containsKey(argLambda.getArgument())) {
				removedFromStrippedVariables = true;
				storedValue = externalVariableMapping.get(argLambda
						.getArgument());
			} else {
				removedFromStrippedVariables = false;
				storedValue = null;
			}

			// Visit the body.
			argument = argLambda.getBody();
			lambda.getBody().accept(this);

			if (removedFromStrippedVariables) {
				// Restore mapping, if changed.
				externalVariableMapping.put(argLambda.getArgument(),
						storedValue);
			}
		}

		@Override
		public void visit(Literal literal) {
			if (isDirectlyMatched(literal)) {
				return;
			}

			final int len = literal.numArgs();
			if (!(argument instanceof Literal)
					|| ((Literal) argument).numArgs() != len) {
				result = false;
				return;
			}

			final Literal argLiteral = (Literal) argument;

			if (literal.getPredicateType().isOrderSensitive()) {
				// Case order sensitive literal.
				argument = argLiteral.getPredicate();
				literal.getPredicate().accept(this);
				if (!result) {
					return;
				}
				for (int i = 0; i < len; ++i) {
					argument = argLiteral.getArg(i);
					literal.getArg(i).accept(this);
					if (!result) {
						return;
					}
				}
			} else {
				// Case order insensitive literal. Similar to how we compare
				// Literal objects.

				final ScopeMapping<Variable, Variable> originalScopeMapping = scope;
				final Map<Variable, LogicalExpression> originalExternalVariableMapping = externalVariableMapping;

				final LogicalExpression[] otherArgsCopy = argLiteral
						.argumentCopy();
				for (int j = 0; j < len; ++j) {
					boolean found = false;
					for (int i = 0; i < len; ++i) {
						if (otherArgsCopy[i] != null) {
							scope = new ScopeMappingOverlay<Variable, Variable>(
									originalScopeMapping,
									new IdentityFastStackMap<Variable, Variable>(),
									new IdentityFastStackMap<Variable, Variable>());
							final HashMap<Variable, LogicalExpression> temporaryMap = new HashMap<Variable, LogicalExpression>(
									originalExternalVariableMapping);
							externalVariableMapping = temporaryMap;
							argument = otherArgsCopy[i];
							literal.getArg(j).accept(this);
							externalVariableMapping = originalExternalVariableMapping;
							if (result) {
								found = true;
								otherArgsCopy[i] = null;
								((ScopeMappingOverlay<Variable, Variable>) scope)
										.applyToBase();
								originalExternalVariableMapping
										.putAll(temporaryMap);
								break;
							} else {
								// Reset the result.
								result = true;
							}
						}
					}
					if (!found) {
						result = false;
						return;
					}
				}
				scope = originalScopeMapping;
			}
		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			if (isDirectlyMatched(logicalConstant)) {
				return;
			}

			result = logicalConstant.equals(argument);
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			if (isDirectlyMatched(variable)) {
				return;
			}

			if (variable instanceof SkolemId) {
				if (!(argument instanceof SkolemId)) {
					result = false;
					return;
				}
				final SkolemId argSkolemId = (SkolemId) argument;
				final SkolemId id = (SkolemId) variable;
				final Variable mappedValue = scope.peek(id);
				if (mappedValue == argSkolemId
						&& scope.peekValue(mappedValue) == id) {
					return;
				} else if (argSkolemId instanceof SkolemIdInstanceWrapper) {
					throw new IllegalArgumentException(
							"skolem ID instance wrapper not supported");
				} else if (!scope.containsValue(argSkolemId)) {
					scope.push(id, argSkolemId);
				} else {
					result = false;
				}
			} else {
				if (!(argument instanceof Variable)) {
					result = false;
					return;
				}
				final Variable argVariable = (Variable) argument;
				final Variable mapValue = scope.peek(variable);
				if (mapValue == argument
						&& scope.peekValue(argVariable) == variable) {
					// Comparison through mapping of variables.
					return;
				} else if (!scope.containsValue(argVariable)) {
					// Case both are not mapped, do instance comparison for free
					// variables.
					result = argVariable == variable;
				} else {
					// Not equal.
					result = false;
					return;
				}
			}
		}

		/**
		 * Tries to directly match or create a mapping.
		 *
		 * @return 'true' if processed.
		 */
		private boolean isDirectlyMatched(LogicalExpression resultSubExp) {
			if (argument.numFreeVariables() == 0) {
				// If the current argument has no free variables, the
				// resultSubExp should equal it.
				result = argument.equals(resultSubExp);
				return true;
			}

			if (argument instanceof Variable
					&& externalVariableMapping.containsKey(argument)
					&& argument.getType().isExtendingOrExtendedBy(
							resultSubExp.getType())) {
				if (externalVariableMapping.get(argument) == null) {
					externalVariableMapping.put((Variable) argument,
							resultSubExp);
				} else if (!externalVariableMapping.get(argument).equals(
						resultSubExp)) {
					result = false;
				}
				return true;
			}
			return false;
		}

	}
}
