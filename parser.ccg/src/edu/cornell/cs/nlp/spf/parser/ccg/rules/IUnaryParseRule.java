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
 * A unary parse rule. Consumes a single span and modifies it.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public interface IUnaryParseRule<MR> extends Serializable {

	/**
	 * Takes a single category and modifies it.
	 *
	 * @param span
	 *            Information on the span the rule is applied to.
	 */
	ParseRuleResult<MR> apply(Category<MR> category, SentenceSpan span);

	@Override
	boolean equals(Object obj);

	UnaryRuleName getName();

	@Override
	int hashCode();

	/**
	 * A quick test to check if the rule may apply to this category. This test
	 * is required to be efficient and return 'true' for all categories the rule
	 * may apply for (i.e., !{@link #isValidArgument(Category)} \implies
	 * {@link #apply(Category, SentenceSpan)} == null). Naively, this method can
	 * return {@link #apply(Category, SentenceSpan)} != null. However, while
	 * accurate, this is not efficient.
	 */
	boolean isValidArgument(Category<MR> category, SentenceSpan span);

}
