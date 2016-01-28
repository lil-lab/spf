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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform.eisner;

import edu.cornell.cs.nlp.spf.parser.ccg.normalform.INormalFormConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IArrayRuleNameSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.AbstractApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.AbstractComposition;

/**
 * Eisner NF constraint (Eisner, 1995). Handles two cases:
 * <ul>
 * <li>No constituent produced by >Bn, for n >= 1, ever serves as the primary
 * (left) argument to >Bn', for n' > 0 (n=0 is application).</li>
 * <li>No constituent produced by <Bn, any n >= 1, ever serves as the primary
 * (right) argument to <Bn', any n' > 0 (n=0 is application).</li>
 * </ul>
 *
 * @author Yoav Artzi
 */
public class EisnerConstraint implements INormalFormConstraint {

	private static final long	serialVersionUID	= -2104893862810439365L;

	@Override
	public boolean isValid(IArrayRuleNameSet leftGeneratingRules,
			IArrayRuleNameSet rightGeneratingRules, RuleName consideredRule) {
		// Considered rule must be application or composition.
		if (AbstractComposition.RULE_LABEL.equals(consideredRule.getLabel())
				|| AbstractApplication.RULE_LABEL.equals(consideredRule
						.getLabel())) {
			final IArrayRuleNameSet primaryGeneratingRules;
			if (Direction.FORWARD == consideredRule.getDirection()) {
				primaryGeneratingRules = leftGeneratingRules;
			} else if (Direction.BACKWARD == consideredRule.getDirection()) {
				primaryGeneratingRules = rightGeneratingRules;
			} else {
				throw new IllegalStateException("Invalid direction");
			}
			final int length = primaryGeneratingRules.numRuleNames();
			for (int i = 0; i < length; ++i) {
				final RuleName rule = primaryGeneratingRules.getRuleName(i);
				if (AbstractComposition.RULE_LABEL.equals(rule.getLabel())
						&& rule.getDirection() == consideredRule.getDirection()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean isValid(IArrayRuleNameSet generatingRules,
			RuleName consideredRule) {
		return true;
	}

}
