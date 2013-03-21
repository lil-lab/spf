/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
 ******************************************************************************/
package edu.uw.cs.lil.tiny.parser.ccg.features.basic.resources;

import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.RuleUsageFeatureSet;

public class RuleUsageFeatureSetCreator<X, Y> implements
		IResourceObjectCreator<RuleUsageFeatureSet<X, Y>> {
	
	@Override
	public RuleUsageFeatureSet<X, Y> create(Parameters params,
			IResourceRepository repo) {
		return new RuleUsageFeatureSet<X, Y>(
				params.contains("scale") ? Double.valueOf(params.get("scale"))
						: 1.0);
	}
	
	@Override
	public String type() {
		return "feat.rules.count";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), RuleUsageFeatureSet.class)
				.setDescription(
						"Feature set that provides features that count the number of times type-shifting rules are used. Feature tag: RULE.")
				.addParam("scale", "double", "Scaling factor for scorer output")
				.build();
	}
	
}
