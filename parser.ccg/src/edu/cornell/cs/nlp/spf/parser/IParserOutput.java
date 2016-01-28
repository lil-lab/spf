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
package edu.cornell.cs.nlp.spf.parser;

import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Parsing output. Packs all parses.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation type.
 * @see IParser
 */
public interface IParserOutput<MR> {

	/**
	 * Get all complete derivations.
	 *
	 * @return
	 */
	List<? extends IDerivation<MR>> getAllDerivations();

	/**
	 * Get highest scoring complete derivations.
	 */
	List<? extends IDerivation<MR>> getBestDerivations();

	/**
	 * Get all complete valid derivations given the filter.
	 */
	List<? extends IDerivation<MR>> getDerivations(IFilter<Category<MR>> filter);

	/**
	 * Get all complete max scoring valid derivations (can get multiple parses,
	 * since syntax is not constrained).
	 */
	List<? extends IDerivation<MR>> getMaxDerivations(
			IFilter<Category<MR>> filter);

	/**
	 * Return parsing time in milliseconds.
	 */
	long getParsingTime();

	/**
	 * Indicates if inference was exact or approximate.
	 */
	boolean isExact();

}
