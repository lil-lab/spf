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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition;

import java.util.ArrayList;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;

public class CompositionCreator<MR> implements
		IResourceObjectCreator<BinaryRuleSet<MR>> {

	private String	type;

	public CompositionCreator() {
		this("rule.composition");
	}

	public CompositionCreator(String type) {
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BinaryRuleSet<MR> create(Parameters params, IResourceRepository repo) {
		final int maxOrder = params.getAsInteger("maxOrder", 1);
		final List<IBinaryParseRule<MR>> rules = new ArrayList<IBinaryParseRule<MR>>();

		for (int i = 1; i <= maxOrder; ++i) {
			rules.add(new ForwardComposition<MR>((ICategoryServices<MR>) repo
					.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					i, false));
			rules.add(new BackwardComposition<MR>((ICategoryServices<MR>) repo
					.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					i, false));
		}

		if (params.getAsBoolean("crossing", false)) {
			rules.add(new ForwardComposition<MR>((ICategoryServices<MR>) repo
					.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					1, true));
			rules.add(new BackwardComposition<MR>((ICategoryServices<MR>) repo
					.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					1, true));
		}

		return new BinaryRuleSet<MR>(rules);
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public ResourceUsage usage() {
		return ResourceUsage
				.builder(type, AbstractComposition.class)
				.addParam("crossing", Boolean.class,
						"Create crossing composition rules (default: false)")
				.addParam("maxOrder", Integer.class,
						"Maximum composition order (default: 1, 3 should be enough for English)")
				.build();
	}

}
