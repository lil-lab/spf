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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.steps;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.IOverloadedParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.OverloadedRuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

/**
 * A single CKY parse step.
 *
 * @author Yoav Artzi
 * @param <MR>
 */
public class CKYParseStep<MR> extends AbstractCKYStep<MR> {

	public CKYParseStep(Category<MR> root, Cell<MR> leftChild,
			Cell<MR> rightChild, boolean isFullParse, RuleName ruleName,
			int start, int end) {
		super(root, leftChild, rightChild, isFullParse, ruleName, start, end);
	}

	@Override
	public CKYParseStep<MR> overloadWithUnary(
			ParseRuleResult<MR> unaryRuleResult, boolean fullParseAfterUnary) {
		if (!(unaryRuleResult.getRuleName() instanceof UnaryRuleName)) {
			throw new IllegalStateException(
					"Provided result is not from a unary rule: "
							+ unaryRuleResult);
		}
		return new Overloaded<MR>(unaryRuleResult.getResultCategory(),
				getChildCell(0), isUnary() ? null : getChildCell(1),
				fullParseAfterUnary, getRuleName().overload(
						(UnaryRuleName) unaryRuleResult.getRuleName()),
				getStart(), getEnd(), getRoot());
	}

	@Override
	public IWeightedCKYStep<MR> overloadWithUnary(
			ParseRuleResult<MR> unaryRuleResult, boolean fullParseAfterUnary,
			IDataItemModel<MR> model) {
		final CKYParseStep<MR> overloaded = overloadWithUnary(unaryRuleResult,
				fullParseAfterUnary);
		return new WeightedCKYParseStep<MR>(overloaded, model);
	}

	private static class Overloaded<MR> extends CKYParseStep<MR> implements
			IOverloadedParseStep<MR> {

		private final Category<MR>	intermediate;

		private Overloaded(Category<MR> root, Cell<MR> leftChild,
				Cell<MR> rightChild, boolean isFullParse,
				OverloadedRuleName ruleName, int start, int end,
				Category<MR> intermediate) {
			super(root, leftChild, rightChild, isFullParse, ruleName, start,
					end);
			assert intermediate != null;
			this.intermediate = intermediate;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			final Overloaded other = (Overloaded) obj;
			if (!intermediate.equals(other.intermediate)) {
				return false;
			}
			return true;
		}

		@Override
		public Category<MR> getIntermediate() {
			return intermediate;
		}

		@Override
		public OverloadedRuleName getRuleName() {
			return (OverloadedRuleName) super.getRuleName();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ (intermediate == null ? 0 : intermediate.hashCode());
			return result;
		}

		@Override
		public CKYParseStep<MR> overloadWithUnary(
				ParseRuleResult<MR> unaryRuleResult, boolean fullParseAfterUnary) {
			throw new IllegalStateException(
					"Can't overload an already overloaded step");
		}

	}

}
