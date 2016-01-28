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
import java.util.HashSet;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;

public class ReversibleApplicationCreator implements
		IResourceObjectCreator<BinaryRuleSet<LogicalExpression>> {

	private final String	type;

	public ReversibleApplicationCreator() {
		this("rule.application.reversible");
	}

	public ReversibleApplicationCreator(String type) {
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BinaryRuleSet<LogicalExpression> create(Parameters params,
			IResourceRepository repo) {
		final List<IBinaryParseRule<LogicalExpression>> rules = new ArrayList<IBinaryParseRule<LogicalExpression>>(
				2);
		rules.add(new ForwardReversibleApplication(
				(ICategoryServices<LogicalExpression>) repo
						.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
				params.getAsInteger("maxSubsetSize", 3), params.getAsInteger(
						"maxDepth", Integer.MAX_VALUE), params.getAsBoolean(
						"nfReversing", true), new HashSet<String>(params
						.getSplit("attributes"))));
		rules.add(new BackwardReversibleApplication(
				(ICategoryServices<LogicalExpression>) repo
						.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
				params.getAsInteger("maxSubsetSize", 3), params.getAsInteger(
						"maxDepth", Integer.MAX_VALUE), params.getAsBoolean(
						"nfReversing", true), new HashSet<String>(params
						.getSplit("attributes"))));
		return new BinaryRuleSet<LogicalExpression>(rules);
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public ResourceUsage usage() {
		return ResourceUsage
				.builder(type, AbstractReversibleApplication.class)
				.addParam(
						"maxDepth",
						Integer.class,
						"Max depth for extraction of argument when generating a function from an argument and a result (default: no limit)")
				.setDescription(
						"Forward and backward application rules with reversing methods")
				.addParam(
						"nfReversing",
						Boolean.class,
						"Force normal-form type-raised function constraint during application reversing (default: true)")
				.addParam(
						"attributes",
						String.class,
						"A set of syntactic attributes to use when generalizing the syntactic form during reverse application")
				.addParam("maxSubsetSize", Integer.class,
						"Max size of arguments to group together from recursive literals (default: 3)")
				.build();
	}

}
