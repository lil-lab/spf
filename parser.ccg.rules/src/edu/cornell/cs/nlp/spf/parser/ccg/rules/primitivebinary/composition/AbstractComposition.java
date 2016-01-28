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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * An abstract rule for composition.
 *
 * @author Yoav Artzi
 */
public abstract class AbstractComposition<MR> implements IBinaryParseRule<MR> {
	public static final ILogger			LOG					= LoggerFactory
																	.create(AbstractComposition.class);

	public static String				RULE_LABEL			= "comp";

	private static final String			CROSS_PREFIX		= "x";

	private static final long			serialVersionUID	= 5577624888908150245L;

	private final ICategoryServices<MR>	categoryServices;

	private final boolean				cross;

	private final RuleName				name;

	private final int					order;

	public AbstractComposition(String label, Direction direction, int order,
			ICategoryServices<MR> categoryServices, boolean cross) {
		this.order = order;
		this.cross = cross;
		this.name = RuleName.create(cross ? getCrossingCompositionLabel()
				: label, direction, order);
		this.categoryServices = categoryServices;
	}

	public static String getCrossingCompositionLabel() {
		return CROSS_PREFIX + RULE_LABEL;
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
	 * Composition combination.
	 *
	 * @param backward
	 *            'true' if the composition direction is reversed.
	 */
	protected ParseRuleResult<MR> doComposition(Category<MR> primary,
			Category<MR> secondary, boolean backward) {
		LOG.debug("applying %s, primary=%s, secondary=%s", name, primary,
				secondary);
		// Verify both are complex categories.
		if (primary instanceof ComplexCategory
				&& secondary instanceof ComplexCategory) {
			final ComplexCategory<MR> primaryCategory = (ComplexCategory<MR>) primary;
			final ComplexCategory<MR> secondaryCategory = (ComplexCategory<MR>) secondary;

			// Verify the directionality of the secondary argument.
			if (!(primaryCategory.getSlash() == (backward ? Slash.BACKWARD
					: Slash.FORWARD))) {
				return null;
			}

			final Category<MR> result = categoryServices.compose(
					primaryCategory, secondaryCategory, order, cross);
			LOG.debug("... result=%s", result);
			if (result != null) {
				return new ParseRuleResult<MR>(name, result);
			}
		}

		return null;
	}
}
