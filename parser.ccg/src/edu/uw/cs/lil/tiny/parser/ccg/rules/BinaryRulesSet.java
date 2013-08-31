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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.collections.ListUtils;

public class BinaryRulesSet<MR> implements IBinaryParseRule<MR> {
	
	private final List<IBinaryParseRule<MR>>	rules;
	
	public BinaryRulesSet(List<IBinaryParseRule<MR>> rules) {
		this.rules = rules;
	}
	
	@Override
	public Collection<ParseRuleResult<MR>> apply(Category<MR> left,
			Category<MR> right, boolean completeSentence) {
		final List<ParseRuleResult<MR>> results = new LinkedList<ParseRuleResult<MR>>();
		
		for (final IBinaryParseRule<MR> rule : rules) {
			results.addAll(rule.apply(left, right, completeSentence));
		}
		
		return results;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final BinaryRulesSet other = (BinaryRulesSet) obj;
		if (rules == null) {
			if (other.rules != null) {
				return false;
			}
		} else if (!rules.equals(other.rules)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rules == null) ? 0 : rules.hashCode());
		return result;
	}
	
	@Override
	public boolean isOverLoadable() {
		for (final IBinaryParseRule<MR> rule : rules) {
			if (!rule.isOverLoadable()) {
				return false;
			}
		}
		return true;
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<BinaryRulesSet<MR>> {
		
		private String	type;
		
		public Creator() {
			this("rule.set");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public BinaryRulesSet<MR> create(Parameters params,
				final IResourceRepository repo) {
			return new BinaryRulesSet<MR>(ListUtils.map(
					params.getSplit("rules"),
					new ListUtils.Mapper<String, IBinaryParseRule<MR>>() {
						
						@Override
						public IBinaryParseRule<MR> process(String obj) {
							return repo.getResource(obj);
						}
					}));
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
							"Binary parse rules.").build();
		}
		
	}
	
}
