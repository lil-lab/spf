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
package edu.cornell.cs.nlp.spf.parser.ccg.features.basic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.OverloadedRuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

/**
 * Computes features over using type-shifting rules.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item for inference.
 * @param <MR>
 *            Meaning representation.
 */
public class RuleUsageFeatureSet<DI extends IDataItem<?>, MR> implements
		IParseFeatureSet<DI, MR> {

	private static final String	FEATURE_TAG			= "RULE";

	private static final long	serialVersionUID	= -2924052883973590335L;
	private final Set<String>	ignoreSet;

	private final double		scale;

	private final boolean		unaryRulesOnly;

	public RuleUsageFeatureSet(double scale, boolean unaryRulesOnly,
			Set<String> ignoreSet) {
		this.scale = scale;
		this.unaryRulesOnly = unaryRulesOnly;
		this.ignoreSet = ignoreSet;
	}

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return Collections.emptySet();
	}

	@Override
	public void setFeatures(IParseStep<MR> obj, IHashVector feats, DI dataItem) {
		setFeats(obj.getRuleName(), feats);
	}

	private void setFeats(RuleName ruleName, IHashVector features) {
		if (ruleName instanceof OverloadedRuleName) {
			setFeats(((OverloadedRuleName) ruleName).getOverloadedRuleName(),
					features);
			setFeats(((OverloadedRuleName) ruleName).getUnaryRule(), features);
		} else {
			if (!ignoreSet.contains(ruleName.getLabel())
					&& (!unaryRulesOnly || ruleName instanceof UnaryRuleName)) {
				for (final String ruleLabel : RuleName.splitRuleLabel(ruleName
						.toString())) {
					features.set(FEATURE_TAG, ruleLabel,
							features.get(FEATURE_TAG, ruleLabel) + 1.0 * scale);
				}
			}
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR> implements
			IResourceObjectCreator<RuleUsageFeatureSet<DI, MR>> {

		@Override
		public RuleUsageFeatureSet<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			return new RuleUsageFeatureSet<DI, MR>(params.getAsDouble("scale",
					1.0), params.getAsBoolean("unaryRulesOnly", false),
					new HashSet<String>(params.getSplit("ignore")));
		}

		@Override
		public String type() {
			return "feat.rules.count";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), RuleUsageFeatureSet.class)
					.setDescription(
							"Count features for rule usage. Feature tag: RULE.")
					.addParam("unaryRulesOnly", Boolean.class,
							"Create features for unary rules only.")
					.addParam("scale", "double",
							"Scaling factor for scorer output").build();
		}

	}

}
