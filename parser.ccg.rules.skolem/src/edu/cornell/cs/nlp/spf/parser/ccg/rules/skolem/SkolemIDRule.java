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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.skolem;

import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

/**
 * For <id,...>-typed quantifier with no ID set and no free variables, replace
 * the ID placeholder with a new unique ID. Assumes the ID is the first argument
 * of the quantifier.
 *
 * @author Yoav Artzi
 */
public class SkolemIDRule implements IUnaryParseRule<LogicalExpression> {

	public static final String		RULE_LABEL			= "id";
	private static final long		serialVersionUID	= -1434348692759639394L;
	private final SkolemIDFunction	function;
	private final UnaryRuleName		name;

	public SkolemIDRule(Set<LogicalConstant> quantifiers) {
		this.name = UnaryRuleName.create(RULE_LABEL);
		this.function = new SkolemIDFunction(quantifiers);
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> category, SentenceSpan span) {
		final Category<LogicalExpression> outputCategory = function
				.apply(category);
		if (outputCategory.getSemantics() != category.getSemantics()) {
			return new ParseRuleResult<LogicalExpression>(name, outputCategory);
		} else {
			return null;
		}
	}

	@Override
	public UnaryRuleName getName() {
		return name;
	}

	@Override
	public boolean isValidArgument(Category<LogicalExpression> category,
			SentenceSpan span) {
		// Every category is a valid argument. A better validity check is too
		// similar to simply applying the rule.
		return true;
	}
}
