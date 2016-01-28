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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.parser.ccg.rules.IArrayRuleNameSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;

/**
 * A single normal form constraint.
 *
 * @author Yoav Artzi
 */
public interface INormalFormConstraint extends Serializable {

	/**
	 * Binary parse step constraint validation.
	 *
	 * @param leftGeneratingRules
	 *            The set of rules that generated the left category argument of
	 *            considered parse step.
	 * @param rightGeneratingRules
	 *            The set of rules that generated the right category argument of
	 *            considered parse step.
	 * @param consideredRule
	 *            The rule of the considered parse step.
	 */
	boolean isValid(IArrayRuleNameSet leftGeneratingRules,
			IArrayRuleNameSet rightGeneratingRules, RuleName consideredRule);

	/**
	 * Unary parse step constraint validation.
	 *
	 * @param generatingRules
	 *            The set of rules that generated the category argument of the
	 *            considered unary parse step.
	 * @param consideredRule
	 *            The rule of the considered parse step.
	 */
	boolean isValid(IArrayRuleNameSet generatingRules, RuleName consideredRule);

}
