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
package edu.uw.cs.lil.tiny.parser.ccg.cky.chart;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.lil.tiny.parser.ccg.rules.UnaryRuleName;

/**
 * A single CKY parse step.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public class CKYParseStep<MR> extends AbstractCKYParseStep<MR> {
	
	public CKYParseStep(Category<MR> root, Cell<MR> child,
			boolean isFulleParse, RuleName ruleName, IDataItemModel<MR> model) {
		this(root, child, null, isFulleParse, ruleName, model);
	}
	
	public CKYParseStep(Category<MR> root, Cell<MR> leftChild,
			Cell<MR> rightChild, boolean isFullParse, RuleName ruleName,
			IDataItemModel<MR> model) {
		super(root, leftChild, rightChild, isFullParse, ruleName, model);
	}
	
	@Override
	public CKYParseStep<MR> cloneWithUnary(ParseRuleResult<MR> unaryRuleResult,
			IDataItemModel<MR> model, boolean fullParseAfterUnary) {
		if (!(unaryRuleResult.getRuleName() instanceof UnaryRuleName)) {
			throw new IllegalStateException(
					"Provided result is not from a unary rule: "
							+ unaryRuleResult);
		}
		return new CKYParseStep<MR>(
				unaryRuleResult.getResultCategory(),
				children.get(0),
				isUnary ? null : children.get(1),
				fullParseAfterUnary,
				ruleName.overload((UnaryRuleName) unaryRuleResult.getRuleName()),
				model);
	}
}
