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
package edu.uw.cs.lil.tiny.ccg.categories;

/**
 * Category services, such as composition and application.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic type
 */
public interface ICategoryServices<MR> {
	
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
	 * @return null if the composition fails.
	 */
	Category<MR> compose(ComplexCategory<MR> primary,
			ComplexCategory<MR> secondary, int order);
	
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
	 * Returns an empty category.
	 */
	Category<MR> getEmptyCategory();
	
	/**
	 * Returns a NP category with no semantics.
	 */
	Category<MR> getNounPhraseCategory();
	
	/**
	 * Returns a S category with no semantics.
	 */
	Category<MR> getSentenceCategory();
	
	@Override
	int hashCode();
	
	/**
	 * Given a string representation (single line) parse it into a category.
	 */
	Category<MR> parse(String string);
	
	/**
	 * Parse the semantics from the given string.
	 */
	MR parseSemantics(String string);
	
}
