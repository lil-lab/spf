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
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.Unification;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
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
 * Type shifting: S -> S/AP. Required for coordination which includes a
 * preposition, such as:
 * <ul>
 * <li>face and move to the hatrack</li>
 * <li>landing in or taking off from SEATAC you see the lakes</li>
 * </ul>
 *
 * @author Yoav Artzi
 */
public class SententialAdverbialTypeShifting implements
		IUnaryReversibleParseRule<LogicalExpression> {
	private static final Syntax							S_FS_AP_SYNTAX		= new ComplexSyntax(
																					Syntax.S,
																					Syntax.AP,
																					Slash.FORWARD);

	private static final long							serialVersionUID	= -1636614240253688432L;

	private final ICategoryServices<LogicalExpression>	categoryServices;

	private final UnaryRuleName							name				= UnaryRuleName
																					.create("shift_s_ap");

	public SententialAdverbialTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices) {
		this.categoryServices = categoryServices;
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> category, SentenceSpan span) {
		final Unification baseUnification = Syntax.S
				.unify(category.getSyntax());
		if (baseUnification != null) {
			final LogicalExpression raisedSemantics = typeShiftSemantics(category
					.getSemantics());
			if (raisedSemantics != null) {
				return new ParseRuleResult<LogicalExpression>(name,
						Category.create(
								new ComplexSyntax(baseUnification
										.getUnifiedSyntax(), Syntax.AP,
										Slash.FORWARD), raisedSemantics));
			}
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
		final SententialAdverbialTypeShifting other = (SententialAdverbialTypeShifting) obj;
		if (categoryServices == null) {
			if (other.categoryServices != null) {
				return false;
			}
		} else if (!categoryServices.equals(other.categoryServices)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public UnaryRuleName getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (categoryServices == null ? 0 : categoryServices.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean isValidArgument(Category<LogicalExpression> category,
			SentenceSpan span) {
		return Syntax.S.unify(category.getSyntax()) != null;
	}

	@Override
	public Set<Category<LogicalExpression>> reverseApply(
			Category<LogicalExpression> result, SentenceSpan span) {
		final Unification syntaxUnification = result.getSyntax().unify(
				S_FS_AP_SYNTAX);
		if (result instanceof ComplexCategory && syntaxUnification != null) {
			// Create an argument category with semantics of (lambda $0:x
			// true:t). Consuming this as argument and simplifying should
			// reverse the shifting safely (and somewhat efficiently).
			final LogicalExpression semantics = result.getSemantics();
			if (semantics instanceof Lambda) {
				final Variable argument = ((Lambda) semantics).getArgument();
				if (argument.getType().isComplex()
						&& LogicLanguageServices.getTypeRepository()
								.getTruthValueType()
								.equals(argument.getType().getRange())) {
					final LogicalExpression reversedSemantics = categoryServices
							.apply(semantics, new Lambda(new Variable(argument
									.getType().getDomain()),
									LogicLanguageServices.getTrue()));
					if (reversedSemantics != null) {
						return SetUtils.createSingleton(Category.create(
								((ComplexSyntax) syntaxUnification
										.getUnifiedSyntax()).getLeft(),
								reversedSemantics));
					}
				}
			}

		}
		return Collections.emptySet();
	}

	/**
	 * (lambda $0:x (g $0)) ==> (lambda $0:<x,t> (lambda $1:x (and:<t*,t> ($0
	 * $1) (g $1))))
	 */
	protected LogicalExpression typeShiftSemantics(LogicalExpression sem) {
		final Type semType = sem.getType();
		final Type range = semType.getRange();

		if (semType.isComplex()
				&& range.equals(LogicLanguageServices.getTypeRepository()
						.getTruthValueType())) {

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

	public static class Creator implements
			IResourceObjectCreator<SententialAdverbialTypeShifting> {

		private final String	type;

		public Creator() {
			this("rule.shifting.sentence.ap");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public SententialAdverbialTypeShifting create(Parameters params,
				IResourceRepository repo) {
			return new SententialAdverbialTypeShifting(
					(ICategoryServices<LogicalExpression>) repo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					SententialAdverbialTypeShifting.class).build();
		}

	}
}
