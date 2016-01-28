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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleSet;

/**
 * A set of reversible unary rules defined by a list of categories read from a
 * file.
 *
 * @author Yoav Artzi
 */
public class SimpleReversibleApplicationTypeShiftingCreator implements
		IResourceObjectCreator<UnaryRuleSet<LogicalExpression>> {

	private final String	type;

	public SimpleReversibleApplicationTypeShiftingCreator() {
		this("rule.shifting.generic.reversible.set");
	}

	public SimpleReversibleApplicationTypeShiftingCreator(String type) {
		this.type = type;
	}

	@Override
	public UnaryRuleSet<LogicalExpression> create(Parameters params,
			IResourceRepository repo) {
		final List<IUnaryParseRule<LogicalExpression>> rules = new LinkedList<IUnaryParseRule<LogicalExpression>>();

		final ICategoryServices<LogicalExpression> categoryServices = repo
				.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE);

		final boolean startOnly = params.getAsBoolean("startOnly", false)
				|| params.getAsBoolean("completeOnly", false);
		final boolean endOnly = params.getAsBoolean("endOnly", false)
				|| params.getAsBoolean("completeOnly", false);
		final boolean matchSyntax = params.getAsBoolean("matchSyntax", false);

		try (final BufferedReader reader = new BufferedReader(new FileReader(
				params.getAsFile("file")))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("//")) {
					continue;
				}

				final String[] split = line.split("\t", 2);
				final String ruleName = split[0];
				final ComplexCategory<LogicalExpression> category = (ComplexCategory<LogicalExpression>) categoryServices
						.read(split[1]);
				rules.add(new ReversibleApplicationTypeShifting(ruleName,
						category, categoryServices, startOnly, endOnly,
						matchSyntax));
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		return new UnaryRuleSet<LogicalExpression>(
				new ArrayList<IUnaryParseRule<LogicalExpression>>(rules));
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public ResourceUsage usage() {
		return ResourceUsage
				.builder(type, UnaryRuleSet.class)
				.setDescription(
						"A set of reversible unary rules defined by a list of categories read from a file")
				.addParam(
						"file",
						File.class,
						"File where each line contains the following tab seaprated fields: a rule name and a functional category")
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
				.build();
	}

}
