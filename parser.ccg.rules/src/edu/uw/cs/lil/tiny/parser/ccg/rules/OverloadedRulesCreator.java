/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
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
package edu.uw.cs.lil.tiny.parser.ccg.rules;

import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting.ITypeShiftingFunction;

/**
 * Creator for building a set of binary rules overloaded with type shifting
 * functions.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public class OverloadedRulesCreator<MR> implements
		IResourceObjectCreator<BinaryRulesSet<MR>> {
	
	private String	type;
	
	public OverloadedRulesCreator() {
		this("rule.set.overload");
	}
	
	public OverloadedRulesCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BinaryRulesSet<MR> create(Parameters params, IResourceRepository repo) {
		final RuleSetBuilder<MR> builder = new RuleSetBuilder<MR>();
		
		for (final String id : params.getSplit("rules")) {
			builder.add((IBinaryParseRule<MR>) repo.getResource(id));
		}
		
		for (final String id : params.getSplit("functions")) {
			builder.add((ITypeShiftingFunction<MR>) repo.getResource(id));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return ResourceUsage
				.builder(type, BinaryRulesSet.class)
				.addParam("rules", IBinaryParseRule.class,
						"Binary parse rules.")
				.addParam("functions", ITypeShiftingFunction.class,
						"Type shifting functions.").build();
	}
	
}
