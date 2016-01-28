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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.typshifting;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

/**
 * Type shifting rule that operates by applying a pre-defined category to the
 * input category.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class ApplicationTypeShifting<MR> implements IUnaryParseRule<MR> {

	private static final long			serialVersionUID	= -6282371109224310054L;
	private final ICategoryServices<MR>	categoryServices;
	private final Syntax				inputSyntax;
	private final boolean				matchSyntax;
	private final UnaryRuleName			ruleName;
	protected final ComplexCategory<MR>	function;
	protected final boolean				sentenceEndOnly;
	protected final boolean				sentenceStartOnly;

	public ApplicationTypeShifting(String label, ComplexCategory<MR> function,
			ICategoryServices<MR> categoryServices, boolean sentenceStartOnly,
			boolean sentenceEndOnly, boolean matchSyntax) {
		assert function.getSemantics() != null;
		this.matchSyntax = matchSyntax;
		this.sentenceStartOnly = sentenceStartOnly;
		this.sentenceEndOnly = sentenceEndOnly;
		this.ruleName = UnaryRuleName.create(label);
		this.function = function;
		this.categoryServices = categoryServices;
		this.inputSyntax = function.getSyntax().getRight();
	}

	@Override
	public ParseRuleResult<MR> apply(Category<MR> category, SentenceSpan span) {
		if (sentenceStartOnly && !span.isStart()) {
			return null;
		}

		if (sentenceEndOnly && !span.isEnd()) {
			return null;
		}

		if (matchSyntax && !inputSyntax.equals(category.getSyntax())) {
			return null;
		}

		final Category<MR> shifted = categoryServices.apply(function, category);
		if (shifted == null) {
			return null;
		} else {
			return new ParseRuleResult<MR>(ruleName, shifted);
		}
	}

	@Override
	public UnaryRuleName getName() {
		return ruleName;
	}

	@Override
	public boolean isValidArgument(Category<MR> category, SentenceSpan span) {
		if (sentenceStartOnly && !span.isStart()) {
			return false;
		}

		if (sentenceEndOnly && !span.isEnd()) {
			return false;
		}

		if (matchSyntax) {
			return inputSyntax.equals(category.getSyntax());
		} else {
			return inputSyntax.unify(category.getSyntax()) != null;
		}
	}

	@Override
	public String toString() {
		return ruleName.toString();
	}

	public static class Creator<MR> implements
			IResourceObjectCreator<ApplicationTypeShifting<MR>> {

		private final String	type;

		public Creator() {
			this("rule.shifting.generic.application");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ApplicationTypeShifting<MR> create(Parameters params,
				IResourceRepository repo) {
			final ICategoryServices<MR> categoryServices = repo
					.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE);
			final boolean startOnly = params.getAsBoolean("startOnly", false)
					|| params.getAsBoolean("completeOnly", false);
			final boolean endOnly = params.getAsBoolean("endOnly", false)
					|| params.getAsBoolean("completeOnly", false);
			return new ApplicationTypeShifting<MR>(params.get("name"),
					(ComplexCategory<MR>) categoryServices.read(params
							.get("function")), categoryServices, startOnly,
					endOnly, params.getAsBoolean("matchSyntax", false));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					ApplicationTypeShifting.class)
					.addParam("name", String.class, "Rule name")
					.addParam("completeOnly", Boolean.class,
							"Apply to the complete span only (default: false)")
					.addParam("startOnly", Boolean.class,
							"Apply only to span at the start of the sentence (default: false)")
					.addParam("endOnly", Boolean.class,
							"Apply only to span at the end of the sentence (default: false)")
					.addParam(
							"matchSyntax",
							Boolean.class,
							"Require syntax to be equal to the function input syntax, rather than just unify (default: false)")
					.addParam("function", ComplexCategory.class,
							"Function category.").build();
		}

	}

}
