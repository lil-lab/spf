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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform.hb;

import java.util.Set;

import edu.cornell.cs.nlp.spf.parser.ccg.normalform.INormalFormConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IArrayRuleNameSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ITypeRaisingRule.TypeRaisingNameServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.AbstractApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.AbstractComposition;

/**
 * NF constraints 1-6, Hockenmaier and Bisk (2010). This single constraint
 * efficiently covers all six HB NF constraints.
 *
 * @author Yoav Artzi
 */
public class HBComposedConstraint implements INormalFormConstraint {

	private static final long	serialVersionUID	= -2162040594671268854L;
	private final Set<RuleName>	coordinationRuleNames;
	private final boolean		includeTypeRaising;

	public HBComposedConstraint(Set<RuleName> coordinationRuleNames,
			boolean includeTypeRaising) {
		this.coordinationRuleNames = coordinationRuleNames;
		this.includeTypeRaising = includeTypeRaising;
	}

	@Override
	public boolean isValid(IArrayRuleNameSet leftGeneratingRules,
			IArrayRuleNameSet rightGeneratingRules, RuleName consideredRule) {
		// Primary, or functor category.
		final IArrayRuleNameSet primaryRules;
		// Secondary, or argument category.
		final IArrayRuleNameSet secondaryRules;
		final Direction consideredDirection = consideredRule.getDirection();
		final String consideredRuleLabel = consideredRule.getLabel();
		final int consideredOrder = consideredRule.getOrder();
		final boolean consideredIsComposition = consideredRuleLabel
				.equals(AbstractComposition.RULE_LABEL);
		final boolean consideredIsApplication = consideredRuleLabel
				.equals(AbstractApplication.RULE_LABEL);
		if (consideredDirection.equals(Direction.FORWARD)) {
			primaryRules = leftGeneratingRules;
			secondaryRules = rightGeneratingRules;
		} else if (consideredDirection.equals(Direction.BACKWARD)) {
			primaryRules = rightGeneratingRules;
			secondaryRules = leftGeneratingRules;
		} else {
			throw new IllegalStateException("invalid direction");
		}

		// Constraint #1 is applicable only to first order (order=0) composition
		// and application.
		final boolean constraint1IsApplicable = consideredIsApplication
				|| consideredIsComposition && consideredOrder == 0;

		// Constraint #2 is applicable to any composition rule.
		final boolean constraint2IsApplicable = consideredIsComposition;

		// Constraint #5 is only applicable when the primary is an application.
		final boolean constraint5IsApplicable = consideredIsApplication;

		// Iterate over primary rules to validate the constraints.
		boolean primaryRaised = false;
		if (constraint1IsApplicable || constraint2IsApplicable
				|| includeTypeRaising) {
			final int len = primaryRules.numRuleNames();
			for (int i = 0; i < len; ++i) {
				final RuleName rule = primaryRules.getRuleName(i);
				final boolean isComposition = rule.getLabel()
						.equals(AbstractComposition.RULE_LABEL);
				final boolean directionEqual = consideredDirection
						.equals(rule.getDirection());

				// Constraint #1.
				if (constraint1IsApplicable && isComposition
						&& directionEqual) {
					return false;
				}

				// Constraint #2.
				if (constraint2IsApplicable && isComposition
						&& rule.getOrder() == 0 && directionEqual) {
					return false;
				}

				if (includeTypeRaising) {
					if (TypeRaisingNameServices.isTypeRaising(rule)
							&& TypeRaisingNameServices.getDirection(rule)
									.equals(consideredDirection)) {
						// Constraint #5.
						if (constraint5IsApplicable) {
							return false;
						}
						// If the primary is raised, mark it for later
						// constraints.
						primaryRaised = true;
					}
				}
			}
		}

		// Constraint #3 is applicable to both composition and application.
		final boolean constraint3IsApplicable = consideredIsComposition;

		// Constraint #4 is only applicable when the primary is raised and the
		// considered rule is a composition.
		final boolean constraint4IsApplicable = includeTypeRaising
				&& primaryRaised && consideredIsComposition;

		// Iterate over secondary rules to validate the constraints.
		if (constraint3IsApplicable || constraint4IsApplicable) {
			final int len = secondaryRules.numRuleNames();
			for (int i = 0; i < len; ++i) {
				final RuleName rule = secondaryRules.getRuleName(i);
				final boolean isComposition = rule.getLabel()
						.equals(AbstractComposition.RULE_LABEL);
				final boolean directionEqual = consideredDirection
						.equals(rule.getDirection());
				final int order = rule.getOrder();

				// Constraint #3.
				if (constraint3IsApplicable && isComposition
						&& consideredOrder > order && directionEqual) {
					return false;
				}

				// Constraint #4.
				if (constraint4IsApplicable && isComposition && !directionEqual
						&& order > consideredOrder) {
					return false;
				}

			}
		}

		return true;
	}

	@Override
	public boolean isValid(IArrayRuleNameSet generatingRules,
			RuleName consideredRule) {
		// Constraint #6. The only unary constraint.
		if (coordinationRuleNames == null
				|| !TypeRaisingNameServices.isTypeRaising(consideredRule)) {
			return true;
		}

		final int len = generatingRules.numRuleNames();
		for (int i = 0; i < len; ++i) {
			if (coordinationRuleNames
					.contains(generatingRules.getRuleName(i))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return "hb2010nf";
	}
}
