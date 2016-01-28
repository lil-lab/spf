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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;

/**
 * A rule to skip words by ignoring empty categories.
 *
 * @author Yoav Artzi
 */
public class SkippingRule<MR> implements IBinaryParseRule<MR> {
	private static final String	RULE_LABEL			= "skip";

	private static final long	serialVersionUID	= -6360947546119425546L;

	private final boolean		backward;

	private final Category<MR>	emptyCategory;

	private final RuleName		name;

	public SkippingRule(Direction direction,
			ICategoryServices<MR> categoryServices) {
		this.name = RuleName.create(RULE_LABEL, direction);
		this.emptyCategory = categoryServices.getEmptyCategory();
		this.backward = direction.equals(Direction.BACKWARD);
	}

	@Override
	public ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right,
			SentenceSpan span) {
		if (backward && left.equals(emptyCategory)) {
			return new ParseRuleResult<MR>(name, right);
		} else if (!backward && right.equals(emptyCategory)) {
			return new ParseRuleResult<MR>(name, left);
		} else {
			return null;
		}
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
		final SkippingRule other = (SkippingRule) obj;
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
				+ (emptyCategory == null ? 0 : emptyCategory.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return name.toString();
	}

}
