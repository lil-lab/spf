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

/**
 * Rule name that includes a binary {@link RuleName} overloaded with a
 * {@link UnaryRuleName}.
 *
 * @author Yoav Artzi
 */
public class OverloadedRuleName extends RuleName {

	private static final long	serialVersionUID	= 1179884180224965745L;
	private final RuleName		overloadedRuleName;
	private final UnaryRuleName	unaryRule;

	protected OverloadedRuleName(UnaryRuleName unaryRule,
			RuleName overloadedRuleName) {
		super(overloadedRuleName.getLabel() + RULE_ADD + unaryRule.getLabel(),
				overloadedRuleName.getDirection(),
				overloadedRuleName.getOrder());
		assert!(overloadedRuleName instanceof OverloadedRuleName);
		this.unaryRule = unaryRule;
		this.overloadedRuleName = overloadedRuleName;
	}

	public RuleName getOverloadedRuleName() {
		return overloadedRuleName;
	}

	public UnaryRuleName getUnaryRule() {
		return unaryRule;
	}

}
