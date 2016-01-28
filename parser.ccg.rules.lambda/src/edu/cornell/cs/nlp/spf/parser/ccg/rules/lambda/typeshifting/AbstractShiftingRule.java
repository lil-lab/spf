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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting;

import java.util.Collections;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.Unification;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;
import edu.cornell.cs.nlp.utils.collections.ArrayUtils;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

/**
 * Abstract class for type shifting rules of the type XX -> Y\Y or XX -> Y/Y.
 *
 * @author Yoav Artzi
 */
public abstract class AbstractShiftingRule
		implements IUnaryReversibleParseRule<LogicalExpression> {
	private static final long							serialVersionUID	= 4776985602709482216L;
	private final ICategoryServices<LogicalExpression>	categoryServices;
	private final SimpleSyntax							sourceSyntax;
	private final SimpleSyntax							targetBaseSyntax;
	private final Slash									targetSlash;
	protected final UnaryRuleName						name;

	public AbstractShiftingRule(String name, SimpleSyntax sourceSyntax,
			SimpleSyntax targetBaseSyntax, Slash targetSlash,
			ICategoryServices<LogicalExpression> categoryServices) {
		this.sourceSyntax = sourceSyntax;
		assert targetBaseSyntax.getAttribute() == null;
		this.targetBaseSyntax = targetBaseSyntax
				.cloneWithAttribute(Syntax.VARIABLE_ATTRIBUTE);
		this.targetSlash = targetSlash;
		this.categoryServices = categoryServices;
		this.name = UnaryRuleName.create(name);
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> category, SentenceSpan span) {
		final Unification sourceUnification = sourceSyntax
				.unify(category.getSyntax());
		if (sourceUnification != null) {
			final LogicalExpression raisedSemantics = typeShiftSemantics(
					category.getSemantics());
			if (raisedSemantics != null) {
				final Syntax base;
				if (((SimpleSyntax) sourceUnification.getUnifiedSyntax())
						.getAttribute() == null) {
					base = targetBaseSyntax;
				} else {
					base = targetBaseSyntax.cloneWithAttribute(
							((SimpleSyntax) sourceUnification
									.getUnifiedSyntax()).getAttribute());
				}
				if (base == null) {
					return null;
				}
				return new ParseRuleResult<LogicalExpression>(name,
						Category.create(
								new ComplexSyntax(base, base, targetSlash),
								raisedSemantics));
			}
		}
		return null;
	}

	@Override
	public UnaryRuleName getName() {
		return name;
	}

	@Override
	public boolean isValidArgument(Category<LogicalExpression> category,
			SentenceSpan span) {
		return sourceSyntax.unify(category.getSyntax()) != null;
	}

	@Override
	public Set<Category<LogicalExpression>> reverseApply(
			Category<LogicalExpression> result, SentenceSpan span) {
		if (result instanceof ComplexCategory) {
			final ComplexSyntax resultSyntax = ((ComplexCategory<LogicalExpression>) result)
					.getSyntax();
			if (resultSyntax.getSlash().equals(targetSlash)) {
				final Unification unifyLeft = targetBaseSyntax
						.unify(resultSyntax.getLeft());
				final Unification unifyRight = targetBaseSyntax
						.unify(resultSyntax.getRight());
				if (unifyLeft != null && unifyRight != null
						&& unifyLeft.getUnifiedSyntax()
								.equals(unifyRight.getUnifiedSyntax())) {
					// Create an argument category with semantics of (lambda
					// $0:x
					// true:t). Consuming this as argument and simplifying
					// should
					// reverse the shifting safely (and somewhat efficiently).
					final LogicalExpression semantics = result.getSemantics();
					if (semantics instanceof Lambda) {
						final Variable argument = ((Lambda) semantics)
								.getArgument();
						if (argument.getType().isComplex()
								&& LogicLanguageServices.getTypeRepository()
										.getTruthValueType().equals(argument
												.getType().getRange())) {
							final LogicalExpression reversedSemantics = categoryServices
									.apply(semantics, new Lambda(
											new Variable(argument.getType()
													.getDomain()),
											LogicLanguageServices.getTrue()));
							if (reversedSemantics != null) {
								final SimpleSyntax syntax = ((SimpleSyntax) unifyLeft
										.getUnifiedSyntax())
												.getAttribute() == null
														? sourceSyntax
														: sourceSyntax
																.cloneWithAttribute(
																		((SimpleSyntax) unifyLeft
																				.getUnifiedSyntax())
																						.getAttribute());
								if (syntax != null) {
									return SetUtils.createSingleton(Category
											.create(syntax, reversedSemantics));
								}
							}
						}
					}
				}
			}
		}
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		return name.toString();
	}

	/**
	 * (lambda $0:x (g $0)) ==> (lambda $0:<x,t> (lambda $1:x (and:<t*,t> ($0
	 * $1) (g $1))))
	 */
	protected LogicalExpression typeShiftSemantics(LogicalExpression sem) {
		final Type semType = sem.getType();
		final Type range = semType.getRange();

		if (semType.isComplex() && range.equals(LogicLanguageServices
				.getTypeRepository().getTruthValueType())) {

			// Make sure the expression is wrapped with lambda operators, since
			// the variables are required
			final Lambda lambda = (Lambda) sem;

			// Variable for the new outer lambda
			final Variable outerVariable = new Variable(LogicLanguageServices
					.getTypeRepository().getTypeCreateIfNeeded(
							LogicLanguageServices.getTypeRepository()
									.getTruthValueType(),
							lambda.getArgument().getType()));

			// Create the literal applying the function to the original
			// argument
			final LogicalExpression[] args = new LogicalExpression[1];
			args[0] = lambda.getArgument();
			final Literal newLiteral = new Literal(outerVariable, args);

			// Create the conjunction of newLitral and the original body
			final Literal conjunction = new Literal(
					LogicLanguageServices.getConjunctionPredicate(),
					ArrayUtils.create(newLiteral, lambda.getBody()));

			// The new inner lambda
			final Lambda innerLambda = new Lambda(lambda.getArgument(),
					conjunction);

			// The new outer lambda
			final Lambda outerLambda = new Lambda(outerVariable, innerLambda);

			// Simplify the output and return it
			final LogicalExpression ret = Simplify.of(outerLambda);

			return ret;
		}

		return null;
	}
}
