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
package edu.uw.cs.lil.tiny.parser.ccg.rules.skipping;

import java.util.Collections;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * This is an abstract rule used to skip words tagged with an empty category.
 * 
 * @author Yoav Artzi
 */
public abstract class AbstractSkippingRule<MR> implements IBinaryParseRule<MR> {
	
	private static final String	RULE_LABEL	= "skip";
	
	private final Category<MR>	emptyCategory;
	
	private final RuleName		name;
	
	public AbstractSkippingRule(Direction direction,
			ICategoryServices<MR> categoryServices) {
		this.name = RuleName.create(RULE_LABEL, direction);
		this.emptyCategory = categoryServices.getEmptyCategory();
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
		final AbstractSkippingRule other = (AbstractSkippingRule) obj;
		if (emptyCategory == null) {
			if (other.emptyCategory != null) {
				return false;
			}
		} else if (!emptyCategory.equals(other.emptyCategory)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
	
	@Override
	public RuleName getName() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((emptyCategory == null) ? 0 : emptyCategory.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return name.toString();
	}
	
	protected List<ParseRuleResult<MR>> attemptSkipping(Category<MR> left,
			Category<MR> right, boolean backward) {
		final boolean rightCategoryIsEmpty = right.equals(emptyCategory);
		final boolean leftCategoryIsEmpty = left.equals(emptyCategory);
		
		// Only create new cells if one is empty and the other is not
		if (leftCategoryIsEmpty ^ rightCategoryIsEmpty) {
			if (leftCategoryIsEmpty && backward) {
				// Case left is empty
				return ListUtils.createSingletonList(new ParseRuleResult<MR>(
						name, right));
			} else if (rightCategoryIsEmpty && !backward) {
				// Case right is empty
				return ListUtils.createSingletonList(new ParseRuleResult<MR>(
						name, left));
			}
		}
		
		return Collections.emptyList();
	}
}
