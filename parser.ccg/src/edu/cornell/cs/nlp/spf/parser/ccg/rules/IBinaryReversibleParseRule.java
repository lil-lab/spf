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

import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;

/**
 * Adds reverse application methods to {@link IBinaryParseRule}. Reverse
 * application methods takes a the rule result and one of its children and
 * returns the missing {@link Category} child.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public interface IBinaryReversibleParseRule<MR> extends IBinaryParseRule<MR> {

	/**
	 * @param left
	 *            Left child.
	 * @param result
	 *            Rule result.
	 * @param span
	 *            Information on the span considered.
	 * @return Potential right children.
	 */
	Set<Category<MR>> reverseApplyLeft(Category<MR> left, Category<MR> result,
			SentenceSpan span);

	/**
	 * @param right
	 *            Right child.
	 * @param result
	 *            Rule result.
	 * @param span
	 *            Information on the span considered.
	 * @return Potential left children.
	 */
	Set<Category<MR>> reverseApplyRight(Category<MR> right,
			Category<MR> result, SentenceSpan span);

}
