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
package edu.uw.cs.lil.tiny.parser.ccg.rules;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;

public class BinaryRulesSet<Y> implements IBinaryParseRule<Y> {
	
	private final List<IBinaryParseRule<Y>>	rules;
	
	public BinaryRulesSet(List<IBinaryParseRule<Y>> rules) {
		this.rules = rules;
	}
	
	@Override
	public Collection<ParseRuleResult<Y>> apply(Category<Y> left,
			Category<Y> right, boolean completeSentence) {
		final List<ParseRuleResult<Y>> results = new LinkedList<ParseRuleResult<Y>>();
		
		for (final IBinaryParseRule<Y> rule : rules) {
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
		for (final IBinaryParseRule<Y> rule : rules) {
			if (!rule.isOverLoadable()) {
				return false;
			}
		}
		return true;
	}
	
}
