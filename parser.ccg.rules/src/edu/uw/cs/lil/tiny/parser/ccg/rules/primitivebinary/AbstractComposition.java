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
 * An abstract rule for logical composition.
 * 
 * @author Luke Zettlemoyer
 * @author Tom Kwiatkowski
 * @author Yoav Artzi
 */
public abstract class AbstractComposition<Y> implements IBinaryParseRule<Y> {
	private final ICategoryServices<Y>	categoryServices;
	
	private final String				ruleName;
	
	/**
	 * Flag to force Eisner normal form parsing.
	 */
	private final boolean				useEisnerNormalForm;
	
	public AbstractComposition(String ruleName,
			ICategoryServices<Y> categoryServices) {
		this(ruleName, categoryServices, false);
	}
	
	public AbstractComposition(String ruleName,
			ICategoryServices<Y> categoryServices, boolean useEisnerNormalForm) {
		this.ruleName = ruleName;
		this.categoryServices = categoryServices;
		this.useEisnerNormalForm = useEisnerNormalForm;
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
		final AbstractComposition other = (AbstractComposition) obj;
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
		if (useEisnerNormalForm != other.useEisnerNormalForm) {
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
		result = prime * result + (useEisnerNormalForm ? 1231 : 1237);
		return result;
	}
	
	@Override
	public boolean isOverLoadable() {
		return true;
	}
	
	/**
	 * Composition combination.
	 * 
	 * @param f
	 * @param g
	 * @param backward
	 *            'true' if the composition direction is reversed.
	 * @param cellFactory
	 * @param result
	 *            The result cell, if exist, is added to this list
	 */
	protected List<ParseRuleResult<Y>> doComposition(Category<Y> f,
			Category<Y> g, boolean backward) {
		// Verify both cell have complex categories
		if (f instanceof ComplexCategory && g instanceof ComplexCategory) {
			final ComplexCategory<Y> fCategory = (ComplexCategory<Y>) f;
			final ComplexCategory<Y> gCategory = (ComplexCategory<Y>) g;
			
			// Verify valid composition direction
			if (!fCategory.hasSlash(backward ? Slash.BACKWARD : Slash.FORWARD)
					|| !gCategory.hasSlash(backward ? Slash.BACKWARD
							: Slash.FORWARD)) {
				return Collections.emptyList();
			}
			if (useEisnerNormalForm) {
				// If Eisner normal form parsing is enforced, will validate the
				// direction of
				// the composition using the 'f' category.
				if (fCategory.getSlash() == Slash.BACKWARD
						&& fCategory.isFromLeftComp()) {
					return Collections.emptyList();
				}
				if (fCategory.getSlash() == Slash.FORWARD
						&& fCategory.isFromRightComp()) {
					return Collections.emptyList();
				}
			}
			
			final Category<Y> result = categoryServices.compose(fCategory,
					gCategory);
			if (result != null) {
				return ListUtils.createSingletonList(new ParseRuleResult<Y>(
						ruleName, result));
			}
		}
		
		return Collections.emptyList();
	}
}
