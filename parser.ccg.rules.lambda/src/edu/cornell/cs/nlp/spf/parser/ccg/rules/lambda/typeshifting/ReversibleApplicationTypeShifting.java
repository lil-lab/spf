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
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetApplicationArgument;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.typshifting.ApplicationTypeShifting;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

public class ReversibleApplicationTypeShifting extends
		ApplicationTypeShifting<LogicalExpression> implements
		IUnaryReversibleParseRule<LogicalExpression> {

	private static final long	serialVersionUID	= -4450608095874427413L;

	public ReversibleApplicationTypeShifting(String label,
			ComplexCategory<LogicalExpression> function,
			ICategoryServices<LogicalExpression> categoryServices,
			boolean sentenceStartOnly, boolean sentenceEndOnly,
			boolean matchSyntax) {
		super(label, function, categoryServices, sentenceStartOnly,
				sentenceEndOnly, matchSyntax);
	}

	@Override
	public Set<Category<LogicalExpression>> reverseApply(
			Category<LogicalExpression> result, SentenceSpan span) {
		if (sentenceStartOnly && !span.isStart()) {
			return Collections.emptySet();
		}

		if (sentenceEndOnly && !span.isEnd()) {
			return Collections.emptySet();
		}

		if (result.getSemantics() != null
				&& function.getSyntax().getLeft().equals(result.getSyntax())) {
			final LogicalExpression argument = GetApplicationArgument.of(
					function.getSemantics(), result.getSemantics());
			if (argument != null) {
				final Category<LogicalExpression> argumentCategory = Category
						.create(function.getSyntax().getRight(), argument);
				assert verify(result, argumentCategory, span);
				return SetUtils.createSingleton(argumentCategory);
			}
		}
		return Collections.emptySet();
	}

	private boolean verify(Category<LogicalExpression> expected,
			Category<LogicalExpression> argumentCategory, SentenceSpan span) {
		final ParseRuleResult<LogicalExpression> result = apply(
				argumentCategory, span);
		return result != null && expected.equals(result.getResultCategory());
	}

	public static class Creator implements
			IResourceObjectCreator<ReversibleApplicationTypeShifting> {

		private final String	type;

		public Creator() {
			this("rule.shifting.generic.application.reversible");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ReversibleApplicationTypeShifting create(Parameters params,
				IResourceRepository repo) {
			final ICategoryServices<LogicalExpression> categoryServices = repo
					.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE);
			final boolean startOnly = params.getAsBoolean("startOnly", false)
					|| params.getAsBoolean("completeOnly", false);
			final boolean endOnly = params.getAsBoolean("endOnly", false)
					|| params.getAsBoolean("completeOnly", false);
			return new ReversibleApplicationTypeShifting(params.get("name"),
					(ComplexCategory<LogicalExpression>) categoryServices
							.read(params.get("function")), categoryServices,
					startOnly, endOnly, params.getAsBoolean("matchSyntax",
							false));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					ReversibleApplicationTypeShifting.class)
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
