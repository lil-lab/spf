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
package edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary;

import java.util.Collections;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * An abstract rule for logical application.
 * 
 * @author Luke Zettlemoyer
 * @author Tom Kwiatkowski
 * @author Yoav Artzi
 */
public abstract class AbstractApplication<Y> implements IBinaryParseRule<Y> {
	private final ICategoryServices<Y>	categoryServices;
	private final String				ruleName;
	
	public AbstractApplication(String ruleName,
			ICategoryServices<Y> categoryServices) {
		this.ruleName = ruleName;
		this.categoryServices = categoryServices;
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
		final AbstractApplication other = (AbstractApplication) obj;
		if (categoryServices == null) {
			if (other.categoryServices != null) {
				return false;
			}
		} else if (!categoryServices.equals(other.categoryServices)) {
			return false;
		}
		if (ruleName == null) {
			if (other.ruleName != null) {
				return false;
			}
		} else if (!ruleName.equals(other.ruleName)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((categoryServices == null) ? 0 : categoryServices.hashCode());
		result = prime * result
				+ ((ruleName == null) ? 0 : ruleName.hashCode());
		return result;
	}
	
	@Override
	public boolean isOverLoadable() {
		return true;
	}
	
	/**
	 * Application combination.
	 * 
	 * @param function
	 *            The function cell
	 * @param argument
	 *            The argument cell
	 * @param backward
	 *            'true' if we the application direction is reversed.
	 * @param cellFactory
	 * @param result
	 *            The result cell, if exists, will be added to this list
	 */
	protected List<ParseRuleResult<Y>> doApplication(Category<Y> function,
			Category<Y> argument, boolean backward) {
		if (function instanceof ComplexCategory) {
			final ComplexCategory<Y> functionCategory = (ComplexCategory<Y>) function;
			
			// Check direction of function slash
			if (functionCategory.getSlash() == (backward ? Slash.BACKWARD
					: Slash.FORWARD)) {
				// Do application and create new cell
				final Category<Y> result = categoryServices.apply(
						functionCategory, argument);
				if (result != null) {
					return ListUtils
							.createSingletonList(new ParseRuleResult<Y>(
									ruleName, result));
				}
			}
		}
		
		return Collections.emptyList();
	}
}
