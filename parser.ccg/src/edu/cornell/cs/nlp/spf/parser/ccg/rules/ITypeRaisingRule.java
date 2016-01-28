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

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;

/**
 * A type raising rule that can be restricted given the category it will be
 * combined with later. Type raising rules are very similar to unary rules, but
 * are integrated into binary rules to avoid creating all potential results.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public interface ITypeRaisingRule<MR, T> {

	public static final String	RULE_LABEL			= "T";

	public static final int		RULE_LABEL_LENGTH	= RULE_LABEL.length();

	/**
	 * Takes a single category and modifies it: X : a => T\(T/X) : \lambda f.
	 * f(a).
	 *
	 * @param innerAgument
	 *            The syntactic category of the inner argument (X above).
	 * @param finalResult
	 *            The syntactic category of the final result (T above).
	 * @param finalResultSemanticType
	 *            The semantic type of the final result.
	 */
	ParseRuleResult<MR> apply(Category<MR> category, Syntax innerAgument,
			Syntax finalResult, T finalResultSemanticType);

	@Override
	boolean equals(Object obj);

	RuleName getName();

	@Override
	int hashCode();

	/**
	 * A quick test to check if the rule may apply to this category. This test
	 * is required to be efficient and return 'true' for all categories the rule
	 * may apply for (i.e., !{@link #isValidArgument(Category)} \implies
	 * {@link #apply(Category, boolean)}.isEmpty()). Naively, this method can
	 * return {@link #apply(Category, boolean)}.isEmpty(). However, while
	 * accurate, this is not efficient.
	 */
	boolean isValidArgument(Category<MR> category);

	public static class TypeRaisingNameServices {

		public static UnaryRuleName createRuleName(Direction direction) {
			return UnaryRuleName.create(direction.toString() + RULE_LABEL);
		}

		public static Direction getDirection(RuleName ruleName) {
			final String label = ruleName.getLabel();
			return Direction.valueOf(label.substring(0, label.length()
					- RULE_LABEL_LENGTH));
		}

		public static boolean isTypeRaising(RuleName ruleName) {
			final String label = ruleName.getLabel();
			return ruleName instanceof UnaryRuleName
					&& label.endsWith(RULE_LABEL)
					&& Direction.valueOf(label.substring(0, label.length()
							- RULE_LABEL_LENGTH)) != null;
		}

	}

}
