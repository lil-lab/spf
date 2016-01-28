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
package edu.cornell.cs.nlp.spf.ccg.categories;

import java.io.Serializable;

/**
 * Category services, such as composition and application.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic type
 */
public interface ICategoryServices<MR> extends Serializable {

	/**
	 * Apply the function category to the argument category.
	 *
	 * @return null if the application fails.
	 */
	Category<MR> apply(ComplexCategory<MR> function, Category<MR> argument);

	/**
	 * Semantic application.
	 */
	MR apply(MR function, MR argument);

	/**
	 * Compose the given categories, so the logical forms will compose to
	 * primary(secondary).
	 *
	 * @param corss
	 *            Do cross composition (crossed directionality).
	 * @return null if the composition fails.
	 */
	Category<MR> compose(ComplexCategory<MR> primary,
			ComplexCategory<MR> secondary, int order, boolean cross);

	/**
	 * Semantic composition.
	 *
	 * @param order
	 *            The order of the composition. Meaning, the depth of the shared
	 *            variable in the secondary.
	 */
	MR compose(MR primary, MR secondary, int order);

	@Override
	boolean equals(Object obj);

	/**
	 * Returns the empty category.
	 */
	Category<MR> getEmptyCategory();

	@Override
	int hashCode();

	/**
	 * Given a string representation (single line) read a category from it.
	 */
	Category<MR> read(String string);

	/**
	 * Read semantics from a string.
	 */
	MR readSemantics(String string);

}
