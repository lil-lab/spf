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
package edu.uw.cs.lil.tiny.parser;

import java.util.List;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;

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
	 * Get all complete parses.
	 * 
	 * @return
	 */
	List<? extends IParse<MR>> getAllParses();
	
	/**
	 * Get highest scoring complete parses.
	 * 
	 * @return
	 */
	List<? extends IParse<MR>> getBestParses();
	
	/**
	 * Get all lexical entries participating in all max scoring parses for the
	 * given semantics.
	 * 
	 * @param semantics
	 * @return
	 */
	List<LexicalEntry<MR>> getMaxLexicalEntries(MR semantics);
	
	/**
	 * Get all complete max scoring parses for the given semantics (can get
	 * multiple parses, since syntax is not constrained).
	 * 
	 * @param semantics
	 * @return
	 */
	List<? extends IParse<MR>> getMaxParses(MR semantics);
	
	/**
	 * Return parsing time in milliseconds.
	 * 
	 * @return
	 */
	long getParsingTime();
}
