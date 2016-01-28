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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.Unification;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetApplicationArgument;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetApplicationFunction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.AbstractApplication;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Abstract application rule that supports generating one of the arguments given
 * the result and the other.
 *
 * @author Yoav Artzi
 */
public abstract class AbstractReversibleApplication extends
		AbstractApplication<LogicalExpression> implements
		IBinaryReversibleParseRule<LogicalExpression> {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 113249788916519430L;

	public static final ILogger	LOG	= LoggerFactory
											.create(AbstractReversibleApplication.class);

	private final int			depthLimit;

	private final int			maxSubsetSize;

	private final boolean		nfConstraint;

	/**
	 * This set of syntactic attributes is used when computing the argument
	 * during reverse application. It's used to generalize the syntax of the
	 * argument.
	 */
	private final Set<String>	syntacticAttributes;

	public AbstractReversibleApplication(String label, Direction direction,
			ICategoryServices<LogicalExpression> categoryServices,
			int maxSubsetSize, int depthLimit, boolean nfConstraint,
			Set<String> syntacticAttributes) {
		super(label, direction, categoryServices);
		this.maxSubsetSize = maxSubsetSize;
		this.depthLimit = depthLimit;
		this.nfConstraint = nfConstraint;
		this.syntacticAttributes = Collections
				.unmodifiableSet(syntacticAttributes);
		LOG.info(
				"Init %s :: depthLimit=%d, maxSubSize=%d, syntacticAttributes=%s",
				this.getClass().getSimpleName(), depthLimit, maxSubsetSize,
				syntacticAttributes);
	}

	private static boolean isValidSyntaxTransformation(Syntax resultSyntax,
			Syntax functionReturnSyntax) {
		if (resultSyntax instanceof ComplexSyntax
				&& functionReturnSyntax instanceof ComplexSyntax) {
			final ComplexSyntax resultComplex = (ComplexSyntax) resultSyntax;
			final ComplexSyntax funcReturnComplex = (ComplexSyntax) functionReturnSyntax;
			return resultComplex.getSlash()
					.equals(funcReturnComplex.getSlash())
					&& isValidSyntaxTransformation(resultComplex.getLeft(),
							funcReturnComplex.getLeft())
					&& isValidSyntaxTransformation(resultComplex.getRight(),
							funcReturnComplex.getRight());
		} else if (resultSyntax instanceof SimpleSyntax
				&& functionReturnSyntax instanceof SimpleSyntax) {
			return resultSyntax.equals(functionReturnSyntax)
					|| functionReturnSyntax.hasAttributeVariable();
		} else {
			return false;
		}
	}

	/**
	 * Verification method used for assertions only. This verified that the rule
	 * applies and the syntax is as expected. The semantics is verified by the
	 * classes that do this work.
	 *
	 * @param span
	 */
	private boolean verifyReverseApplication(
			Category<LogicalExpression> function,
			Category<LogicalExpression> argument,
			Category<LogicalExpression> result, boolean forward, SentenceSpan span) {
		final ParseRuleResult<LogicalExpression> actualResult;
		if (forward) {
			actualResult = apply(function, argument, span);
		} else {
			actualResult = apply(argument, function, span);
		}
		return actualResult != null
				&& actualResult.getResultCategory().getSyntax()
						.equals(result.getSyntax());
	}

	protected Set<Category<LogicalExpression>> doReverseApplicationFromArgument(
			Category<LogicalExpression> argument,
			Category<LogicalExpression> result, boolean forward, SentenceSpan span) {
		if (argument.getSemantics() != null && result.getSemantics() != null) {

			final LogicalExpression function = GetApplicationFunction.of(
					result.getSemantics(), argument.getSemantics(),
					maxSubsetSize, depthLimit);
			if (function != null) {

				// This test is expensive, and most will pass it, but actually
				// fail the semantic operation above, so better do it here.
				if (nfConstraint) {
					// Case enforcing normal-form constraint (meaning: don't let
					// a type-raised argument consume the function next to it).
					if (argument.getSyntax() instanceof ComplexSyntax) {
						final Slash argSlash = ((ComplexSyntax) argument
								.getSyntax()).getSlash();
						if (forward && argSlash.equals(Slash.BACKWARD)
								|| !forward && argSlash.equals(Slash.FORWARD)) {
							if (GetApplicationArgument.of(
									argument.getSemantics(),
									result.getSemantics()) != null) {
								return Collections.emptySet();
							}
						}
					}
				}

				// Create all supported (possible?) function syntax categories.
				final List<Syntax> functionSyntaxForms = new ArrayList<Syntax>(
						2);

				// Create the simplest syntactic form without any
				// generalization.
				final ComplexSyntax functionBaseSyntax = new ComplexSyntax(
						result.getSyntax(), argument.getSyntax(),
						forward ? Slash.FORWARD : Slash.BACKWARD);
				functionSyntaxForms.add(functionBaseSyntax);

				// Generalize the syntax to the case where the function contains
				// a variable which was determined by the argument. This is the
				// only generalization we support (the only required?).

				// Only do this when the base function syntax contains no
				// variables.
				if (!functionBaseSyntax.hasAttributeVariable()) {
					// Get the set of attributes that are shared by both sides
					// of the function syntax.
					final HashSet<String> mutualAttributes = new HashSet<String>(
							functionBaseSyntax.getLeft().getAttributes());
					mutualAttributes.retainAll(functionBaseSyntax.getRight()
							.getAttributes());
					// Iterate over all mutual attributes. For each one try to
					// replace all occurrences with a variable. This is an
					// approximation to replacing each possible subset that is
					// bigger than two and covers both sides of the slash.
					for (final String mutualAttribute : mutualAttributes) {
						functionSyntaxForms.add(functionBaseSyntax
								.replaceAttribute(mutualAttribute,
										Syntax.VARIABLE_ATTRIBUTE));
					}
				}

				// Generalize the syntax to the case where the argument of the
				// output function doesn't contain an attribute (i.e., is less
				// specific than the given argument). Only do this when the
				// argument is simple.
				if (functionBaseSyntax.getRight() instanceof SimpleSyntax) {
					final SimpleSyntax simpleArg = (SimpleSyntax) functionBaseSyntax
							.getRight();
					if (simpleArg.getAttribute() != null
							&& !simpleArg.getAttribute().equals(
									Syntax.VARIABLE_ATTRIBUTE)) {
						functionSyntaxForms.add(new ComplexSyntax(
								functionBaseSyntax.getLeft(), simpleArg
										.stripAttributes(), functionBaseSyntax
										.getSlash()));
					}
				}

				// Create the function categories. Pair the semantics with each
				// of the possible syntactic categories.
				final Set<Category<LogicalExpression>> functionCategories = new HashSet<>();
				for (final Syntax syntax : functionSyntaxForms) {
					final Category<LogicalExpression> functionCategory = Category
							.create(syntax, function);
					assert verifyReverseApplication(functionCategory, argument,
							result, forward, span) : String
							.format("Invalid reverse application from argument: argument=%s, result=%s, forward=%s, generated function=%s",
									argument, result, forward, functionCategory);
					functionCategories.add(functionCategory);
				}

				return functionCategories;
			}
		}
		return Collections.emptySet();
	}

	protected Set<Category<LogicalExpression>> doReverseApplicationFromFunction(
			Category<LogicalExpression> function,
			Category<LogicalExpression> result, boolean forward, SentenceSpan span) {
		// Verify syntax and slash direction matches.
		if (function instanceof ComplexCategory
				&& function.getSemantics() != null
				&& result.getSemantics() != null
				&& (forward
						&& ((ComplexCategory<LogicalExpression>) function)
								.getSyntax().getSlash().equals(Slash.FORWARD) || !forward
						&& ((ComplexCategory<LogicalExpression>) function)
								.getSyntax().getSlash().equals(Slash.BACKWARD))) {
			final ComplexSyntax functionSyntax = ((ComplexCategory<LogicalExpression>) function)
					.getSyntax();

			// The left syntax of the function should be equal to the result
			// syntax, unless both sides of the function syntax have a variable
			// (which could be set by an application). To find the variable
			// assignment, we unify of the left and the result, the resulting
			// assignment is used to create the argument syntax by setting the
			// variable of the right.
			final Syntax argumentBaseSyntax;
			final boolean argumentSetSyntaxVariable;
			final Syntax resultSyntax = result.getSyntax();
			if (functionSyntax.getLeft().equals(resultSyntax)) {
				argumentBaseSyntax = functionSyntax.getRight();
				argumentSetSyntaxVariable = false;
			} else if (functionSyntax.getRight().hasAttributeVariable()
					&& functionSyntax.getLeft().hasAttributeVariable()
					&& isValidSyntaxTransformation(resultSyntax,
							functionSyntax.getLeft())) {
				final Unification unification = functionSyntax.getLeft().unify(
						resultSyntax);
				if (unification != null && unification.isVariableAssigned()) {
					argumentBaseSyntax = functionSyntax.getRight().setVariable(
							unification.getVariableAssignment());
					if (argumentBaseSyntax == null) {
						LOG.debug(
								"Variable setting failed: function=%s, result=%s, forward=%s",
								function, result, forward);
						return Collections.emptySet();
					}
					argumentSetSyntaxVariable = true;
				} else {
					return Collections.emptySet();
				}
			} else {
				return Collections.emptySet();
			}

			final List<Syntax> argumentSyntaxForms = new ArrayList<Syntax>(3);
			argumentSyntaxForms.add(argumentBaseSyntax);

			// Generalize the argument syntactic form.

			// If the argument syntax is complex and its outer most argument is
			// simple and has an attribute, add a version without this
			// attribute. Only apply this generalization if the argument wasn't
			// use to set a variable in the function.
			if (argumentBaseSyntax instanceof ComplexSyntax
					&& !argumentSetSyntaxVariable) {
				final ComplexSyntax complex = (ComplexSyntax) argumentBaseSyntax;
				if (complex.getRight() instanceof SimpleSyntax) {
					final SimpleSyntax simpleArg = (SimpleSyntax) complex
							.getRight();
					if (simpleArg.getAttribute() != null
							&& !simpleArg.getAttribute().equals(
									Syntax.VARIABLE_ATTRIBUTE)) {
						argumentSyntaxForms.add(new ComplexSyntax(complex
								.getLeft(), simpleArg.stripAttributes(),
								complex.getSlash()));
					}
				}
			}

			// If the argument syntax is simple and it has no attribute, add
			// versions with all possible attributes for syntactic form.
			if (argumentBaseSyntax instanceof SimpleSyntax
					&& ((SimpleSyntax) argumentBaseSyntax).getAttribute() == null
					&& !argumentSetSyntaxVariable) {
				final SimpleSyntax simple = (SimpleSyntax) argumentBaseSyntax;
				for (final String attribute : syntacticAttributes) {
					final SimpleSyntax cloned = simple
							.cloneWithAttribute(attribute);
					if (cloned != null) {
						argumentSyntaxForms.add(cloned);
					}
				}
			}

			final LogicalExpression argument = GetApplicationArgument.of(
					function.getSemantics(), result.getSemantics());
			if (argument != null) {
				final Set<Category<LogicalExpression>> argumentCategories = new HashSet<Category<LogicalExpression>>();
				for (final Syntax syntax : argumentSyntaxForms) {
					final Category<LogicalExpression> argumentCategory = Category
							.create(syntax, argument);
					assert verifyReverseApplication(function, argumentCategory,
							result,
							getName().getDirection().equals(Direction.FORWARD),
							span) : String
							.format("Invalid reverse application from function: function=%s, result=%s, forward=%s, generated argument=%s",
									function, result, forward, argumentCategory);
					argumentCategories.add(argumentCategory);
				}
				return argumentCategories;
			}
		}
		return Collections.emptySet();
	}

}
