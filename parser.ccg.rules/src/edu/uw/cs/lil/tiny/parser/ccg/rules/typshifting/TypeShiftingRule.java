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
package edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class TypeShiftingRule<MR> implements IBinaryParseRule<MR> {
	
	private final IBinaryParseRule<MR>		baseBianryRule;
	private final String					ruleNameL;
	private final String					ruleNameLR;
	private final String					ruleNameR;
	private final ITypeShiftingFunction<MR>	typeShiftingFunction;
	
	public TypeShiftingRule(ITypeShiftingFunction<MR> typeShiftingFunction,
			IBinaryParseRule<MR> baseBianryRule) {
		this.typeShiftingFunction = typeShiftingFunction;
		this.baseBianryRule = baseBianryRule;
		this.ruleNameL = typeShiftingFunction.getName() + "_l";
		this.ruleNameR = typeShiftingFunction.getName() + "_l";
		this.ruleNameLR = typeShiftingFunction.getName() + "_lr";
		
		// Verify overloading is valid
		if (!baseBianryRule.isOverLoadable()) {
			throw new IllegalStateException(
					"Invalid overloading of parsing rules");
		}
		
	}
	
	@Override
	public Collection<ParseRuleResult<MR>> apply(Category<MR> left,
			Category<MR> right, boolean isCompleteSentence) {
		final List<ParseRuleResult<MR>> results = new LinkedList<ParseRuleResult<MR>>();
		
		final Category<MR> raisedLeft = typeShiftingFunction.typeShift(left);
		final Category<MR> raisedRight = typeShiftingFunction.typeShift(right);
		
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
	
	private void doBaseApplication(Category<MR> left, Category<MR> right,
			boolean isCompleteSentence, String shiftingRuleName,
			List<ParseRuleResult<MR>> results) {
		for (final ParseRuleResult<MR> result : baseBianryRule.apply(left,
				right, isCompleteSentence)) {
			results.add(new ParseRuleResult<MR>(shiftingRuleName + "+"
					+ result.getRuleName(), result.getResultCategory()));
			if (isCompleteSentence) {
				// Case the span represented by the two categories is the
				// complete sentence, try to apply the type shifting rule to the
				// final result as well. Since the top-most category will never
				// be combined with anything, this is the only opportunity to
				// apply the type-shifting rule to it.
				final Category<MR> raisedResult = typeShiftingFunction
						.typeShift(result.getResultCategory());
				if (raisedResult != null) {
					results.add(new ParseRuleResult<MR>(shiftingRuleName + "+"
							+ result.getRuleName() + "+"
							+ typeShiftingFunction.getName() + "_o",
							raisedResult));
				}
			}
			
		}
	}
	
}
