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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform.unaryconstraint;

import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.INormalFormConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IArrayRuleNameSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * NF constraint to ban unary operations after certain binary ones. Usually this
 * constraint should be used for binary rules that already include
 * type-raising/shifting.
 *
 * @author Yoav Artzi
 */
public class UnaryConstraint implements INormalFormConstraint {
	public static final ILogger	LOG					= LoggerFactory
															.create(UnaryConstraint.class);

	private static final long	serialVersionUID	= 383212050871379726L;

	private final Set<RuleName>	rules;

	public UnaryConstraint(Set<RuleName> rules) {
		this.rules = rules;
		LOG.info("Init %s :: rules=%s", UnaryConstraint.class.getSimpleName(),
				rules);
	}

	public static UnaryConstraint create(Set<IBinaryParseRule<?>> rules) {
		return new UnaryConstraint(new HashSet<RuleName>(ListUtils.map(rules,
				obj -> obj.getName())));
	}

	@Override
	public boolean isValid(IArrayRuleNameSet leftGeneratingRules,
			IArrayRuleNameSet rightGeneratingRules, RuleName consideredRule) {
		return true;
	}

	@Override
	public boolean isValid(IArrayRuleNameSet generatingRules,
			RuleName consideredRule) {
		final int len = generatingRules.numRuleNames();
		for (int i = 0; i < len; ++i) {
			if (rules.contains(generatingRules.getRuleName(i))) {
				return false;
			}
		}
		return true;
	}

	public static class Creator implements
			IResourceObjectCreator<UnaryConstraint> {

		private final String	type;

		public Creator() {
			this("nf.constraint.unary");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public UnaryConstraint create(Parameters params,
				IResourceRepository repo) {
			final Set<IBinaryParseRule<?>> rules = new HashSet<IBinaryParseRule<?>>();
			for (final String id : params.getSplit("rules")) {
				final Object rule = repo.get(id);
				if (rule instanceof IBinaryParseRule) {
					rules.add((IBinaryParseRule<?>) rule);
				} else if (rule instanceof BinaryRuleSet) {
					for (final IBinaryParseRule<?> singleRule : (BinaryRuleSet<?>) rule) {
						rules.add(singleRule);
					}
				} else {
					throw new IllegalArgumentException(
							"Invalid rule argument: " + id);
				}
			}
			return UnaryConstraint.create(rules);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, UnaryConstraint.class)
					.addParam("rules", IBinaryParseRule.class,
							"List of binary rules that can't be followed by unary rules")
					.build();
		}

	}

}
