/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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

import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;

/**
 * Parser for sentences {@link Sentence}.
 * 
 * @author Yoav Artzi
 * @param <Y>
 *            The representation of the parser output
 * @see IParser
 */
public interface IParserOutput<Y> {
	
	/**
	 * Get all complete parses.
	 * 
	 * @return
	 */
	List<IParseResult<Y>> getAllParses();
	
	List<IParseResult<Y>> getBestParses();
	
	List<LexicalEntry<Y>> getMaxLexicalEntries(Y label);
	
	List<IParseResult<Y>> getMaxParses(Y label);
	
	long getParsingTime();
}
