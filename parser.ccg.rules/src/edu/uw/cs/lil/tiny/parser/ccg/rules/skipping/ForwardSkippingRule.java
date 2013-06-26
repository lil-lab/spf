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
package edu.uw.cs.lil.tiny.parser.ccg.rules.skipping;

import java.util.Collection;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

/**
 * This is used to skip words tagged with an empty category:
 * <p>
 * <li>
 * <ul>
 * X EMP => X
 * </ul>
 * </li>
 * </p>
 * 
 * @author Yoav Artzi
 */
public class ForwardSkippingRule<Y> extends AbstractSkippingRule<Y> {
	private static final String	RULE_NAME	= "fskip";
	
	public ForwardSkippingRule(ICategoryServices<Y> categoryServices) {
		super(RULE_NAME, categoryServices);
	}
	
	@Override
	public Collection<ParseRuleResult<Y>> apply(Category<Y> left,
			Category<Y> right, boolean isCompleteSentence) {
		return attemptSkipping(left, right, false);
	}
	
}
