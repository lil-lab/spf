/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
package edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class TypeShiftingRule<Y> implements IBinaryParseRule<Y> {
	
	private final IBinaryParseRule<Y>		baseBianryRule;
	private final String					ruleName;
	private final String					ruleNameL;
	private final String					ruleNameLR;
	private final String					ruleNameR;
	private final ITypeShiftingFunction<Y>	typeShiftingFunction;
	
	public TypeShiftingRule(String ruleName,
			ITypeShiftingFunction<Y> typeShiftingFunction,
			IBinaryParseRule<Y> baseBianryRule) {
		this.ruleName = ruleName;
		this.typeShiftingFunction = typeShiftingFunction;
		this.baseBianryRule = baseBianryRule;
		this.ruleNameL = ruleName + "_l";
		this.ruleNameR = ruleName + "_l";
		this.ruleNameLR = ruleName + "_lr";
		
		// Verify overloading is valid
		if (!baseBianryRule.isOverLoadable()) {
			throw new IllegalStateException(
					"Invalid overloading of parsing rules");
		}
		
	}
	
	@Override
	public Collection<ParseRuleResult<Y>> apply(Category<Y> left,
			Category<Y> right, boolean isCompleteSentence) {
		final List<ParseRuleResult<Y>> results = new LinkedList<ParseRuleResult<Y>>();
		
		final Category<Y> raisedLeft = typeShiftingFunction.typeRaise(left);
		final Category<Y> raisedRight = typeShiftingFunction.typeRaise(right);
		
		if (raisedLeft != null) {
			// Raise only left
			doBaseApplication(raisedLeft, right, isCompleteSentence, ruleNameL,
					results);
		}
		
		if (raisedRight != null) {
			// Raise only right
			doBaseApplication(left, raisedRight, isCompleteSentence, ruleNameR,
					results);
		}
		
		if (raisedLeft != null && raisedRight != null) {
			// Raise both
			doBaseApplication(raisedLeft, raisedRight, isCompleteSentence,
					ruleNameLR, results);
		}
		
		return results;
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
		final TypeShiftingRule other = (TypeShiftingRule) obj;
		if (baseBianryRule == null) {
			if (other.baseBianryRule != null) {
				return false;
			}
		} else if (!baseBianryRule.equals(other.baseBianryRule)) {
			return false;
		}
		if (ruleName == null) {
			if (other.ruleName != null) {
				return false;
			}
		} else if (!ruleName.equals(other.ruleName)) {
			return false;
		}
		if (typeShiftingFunction == null) {
			if (other.typeShiftingFunction != null) {
				return false;
			}
		} else if (!typeShiftingFunction.equals(other.typeShiftingFunction)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((baseBianryRule == null) ? 0 : baseBianryRule.hashCode());
		result = prime * result
				+ ((ruleName == null) ? 0 : ruleName.hashCode());
		result = prime
				* result
				+ ((typeShiftingFunction == null) ? 0 : typeShiftingFunction
						.hashCode());
		return result;
	}
	
	@Override
	public boolean isOverLoadable() {
		return false;
	}
	
	private void doBaseApplication(Category<Y> left, Category<Y> right,
			boolean isCompleteSentence, String shiftingRuleName,
			List<ParseRuleResult<Y>> results) {
		for (final ParseRuleResult<Y> result : baseBianryRule.apply(left,
				right, isCompleteSentence)) {
			results.add(new ParseRuleResult<Y>(shiftingRuleName + "+"
					+ result.getRuleName(), result.getResultCategory()));
			if (isCompleteSentence) {
				// Case the span represented by the two categories is the
				// complete sentence, try to apply the type shifting rule to the
				// final result as well. Since the top-most category will never
				// be combined with anything, this is the only opportunity to
				// apply the type-shifting rule to it.
				final Category<Y> raisedResult = typeShiftingFunction
						.typeRaise(result.getResultCategory());
				if (raisedResult != null) {
					results.add(new ParseRuleResult<Y>(shiftingRuleName + "+"
							+ result.getRuleName() + "+" + ruleName + "_o",
							raisedResult));
				}
			}
			
		}
	}
	
}
