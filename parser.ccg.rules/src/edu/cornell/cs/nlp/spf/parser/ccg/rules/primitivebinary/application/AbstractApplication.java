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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;

/**
 * An abstract rule for logical application.
 *
 * @author Yoav Artzi
 */
public abstract class AbstractApplication<MR> implements IBinaryParseRule<MR> {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -584019468747141102L;
	public static final String				RULE_LABEL	= "apply";
	private final RuleName					name;
	protected final ICategoryServices<MR>	categoryServices;

	public AbstractApplication(String label, Direction direction,
			ICategoryServices<MR> categoryServices) {
		this.name = RuleName.create(label, direction);
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
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public RuleName getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (categoryServices == null ? 0 : categoryServices.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return name.toString();
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
	protected ParseRuleResult<MR> doApplication(Category<MR> function,
			Category<MR> argument, boolean backward) {
		if (function instanceof ComplexCategory) {
			final ComplexCategory<MR> functionCategory = (ComplexCategory<MR>) function;

			// Check direction of function slash
			if (functionCategory.getSlash() == (backward ? Slash.BACKWARD
					: Slash.FORWARD)) {
				// Do application and create new cell
				final Category<MR> result = categoryServices.apply(
						functionCategory, argument);
				if (result != null) {
					return new ParseRuleResult<MR>(name, result);
				}
			}
		}

		return null;
	}

}
