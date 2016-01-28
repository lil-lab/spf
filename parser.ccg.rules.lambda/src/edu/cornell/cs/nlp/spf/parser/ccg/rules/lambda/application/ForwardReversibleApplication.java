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

import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;

/**
 * Forward application for {@link LogicalExpression} that supports reversed
 * application.
 *
 * @author Yoav Artzi
 * @see ForwardApplication
 */
public class ForwardReversibleApplication extends AbstractReversibleApplication {

	private static final long	serialVersionUID	= -1733360640855739302L;

	public ForwardReversibleApplication(
			ICategoryServices<LogicalExpression> categoryServices,
			int maxSubsetSize, int depthLimit, boolean nfReversing,
			Set<String> syntacticAttributes) {
		super(RULE_LABEL, Direction.FORWARD, categoryServices, maxSubsetSize,
				depthLimit, nfReversing, syntacticAttributes);
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> left,
			Category<LogicalExpression> right, SentenceSpan span) {
		return doApplication(left, right, false);
	}

	@Override
	public Set<Category<LogicalExpression>> reverseApplyLeft(
			Category<LogicalExpression> left,
			Category<LogicalExpression> result, SentenceSpan span) {
		return doReverseApplicationFromFunction(left, result, true, span);
	}

	@Override
	public Set<Category<LogicalExpression>> reverseApplyRight(
			Category<LogicalExpression> right,
			Category<LogicalExpression> result, SentenceSpan span) {
		return doReverseApplicationFromArgument(right, result, true, span);
	}

	public static class Creator implements
			IResourceObjectCreator<ForwardReversibleApplication> {

		private final String	type;

		public Creator() {
			this("rule.application.forward.reversible");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ForwardReversibleApplication create(Parameters params,
				IResourceRepository repo) {
			return new ForwardReversibleApplication(
					(ICategoryServices<LogicalExpression>) repo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					params.getAsInteger("maxSubsetSize", 3), params
							.getAsInteger("maxDepth", Integer.MAX_VALUE),
					params.getAsBoolean("nfReversing", true),
					new HashSet<String>(params.getSplit("attributes")));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, ForwardReversibleApplication.class)
					.addParam(
							"maxDepth",
							Integer.class,
							"Max depth for extraction of argument when generating a function from an argument and a result (default: no limit)")
					.setDescription(
							"Forward application with reversing methods")
					.addParam("maxSubsetSize", Integer.class,
							"Max size of arguments to group together from recursive literals (default: 3)")
					.addParam(
							"attributes",
							String.class,
							"A set of syntactic attributes to use when generalizing the syntactic form during reverse application")
					.addParam(
							"nfReversing",
							Boolean.class,
							"Force normal-form type-raised function constraint during application reversing (default: true)")
					.build();
		}
	}

}
