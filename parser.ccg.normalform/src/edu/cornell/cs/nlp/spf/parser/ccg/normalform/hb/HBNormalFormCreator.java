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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform.hb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.INormalFormConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator.Builder;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleSet;

public class HBNormalFormCreator implements
		IResourceObjectCreator<NormalFormValidator> {
	private final String	type;

	public HBNormalFormCreator() {
		this("nf.hb");
	}

	public HBNormalFormCreator(String type) {
		this.type = type;
	}

	private static Set<RuleName> getRuleNameSet(List<String> ids,
			IResourceRepository repo) {
		final Set<RuleName> ruleNames = new HashSet<RuleName>();
		for (final String id : ids) {
			final Object resource = repo.get(id);
			if (resource instanceof BinaryRuleSet) {
				for (final IBinaryParseRule<?> rule : (BinaryRuleSet<?>) resource) {
					ruleNames.add(rule.getName());
				}
			} else if (resource instanceof UnaryRuleSet) {
				for (final IUnaryParseRule<?> rule : (UnaryRuleSet<?>) resource) {
					ruleNames.add(rule.getName());
				}
			} else if (resource instanceof IBinaryParseRule) {
				ruleNames.add(((IBinaryParseRule<?>) resource).getName());
			} else if (resource instanceof IUnaryParseRule) {
				ruleNames.add(((IUnaryParseRule<?>) resource).getName());
			}
		}
		return ruleNames;
	}

	@Override
	public NormalFormValidator create(Parameters params,
			IResourceRepository repo) {
		final Builder builder = new NormalFormValidator.Builder();

		builder.addConstraint(new HBComposedConstraint(params
				.contains("coordination") ? getRuleNameSet(
				params.getSplit("coordination"), repo) : null, params
				.getAsBoolean("typeRaising", false)));

		for (final String id : params.getSplit("extraConstraints")) {
			builder.addConstraint((INormalFormConstraint) repo.get(id));
		}

		return builder.build();
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type, NormalFormValidator.class)
				.setDescription(
						"Normal form parsing constraints following Hockenmaier and Bisk 2010.")
				.addParam("typeRaising", Boolean.class,
						"Use type-raising constraints (4-5).")
				.addParam(
						"coordination",
						IBinaryParseRule.class,
						"Parse rules to force coordination constraint on (only applicable if type-raising constraints are enabled).")
				.addParam("extraConstraints", INormalFormConstraint.class,
						"List of extra constraints to include.").build();
	}
}
