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
package edu.cornell.cs.nlp.spf.parser.ccg.rules;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;

/**
 * Binary CCG parse rule.
 *
 * @author Yoav Artzi
 */
public interface IBinaryParseRule<MR> extends Serializable {

	/**
	 * Takes two categories, left and right, as input. Assumes these categories
	 * are adjacent.
	 *
	 * @param span
	 *            Information on the span the rule is applied to.
	 */
	ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right,
			SentenceSpan span);

	@Override
	boolean equals(Object obj);

	RuleName getName();

	@Override
	int hashCode();
}
