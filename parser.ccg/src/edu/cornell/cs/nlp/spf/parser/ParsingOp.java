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
package edu.cornell.cs.nlp.spf.parser;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;

/**
 * Represents a single parsing operation with no score or features computed.
 * This object is used to pack the information available to the filter used for
 * parsing (if one is used).
 *
 * @author Yoav Artzi
 */
public class ParsingOp<MR> {

	private final Category<MR>	category;
	private final RuleName		rule;
	private final SentenceSpan		span;

	public ParsingOp(Category<MR> category, SentenceSpan span, RuleName rule) {
		assert category != null;
		assert span != null;
		assert rule != null;
		this.category = category;
		this.span = span;
		this.rule = rule;
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
		@SuppressWarnings("unchecked")
		final ParsingOp<MR> other = (ParsingOp<MR>) obj;
		if (!category.equals(other.category)) {
			return false;
		}
		if (!rule.equals(other.rule)) {
			return false;
		}
		if (!span.equals(other.span)) {
			return false;
		}
		return true;
	}

	public Category<MR> getCategory() {
		return category;
	}

	public RuleName getRule() {
		return rule;
	}

	public SentenceSpan getSpan() {
		return span;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (category == null ? 0 : category.hashCode());
		result = prime * result + (rule == null ? 0 : rule.hashCode());
		result = prime * result + (span == null ? 0 : span.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return new StringBuilder(span.toString()).append(" ").append(rule)
				.append(" -> ").append(category).toString();
	}

}
