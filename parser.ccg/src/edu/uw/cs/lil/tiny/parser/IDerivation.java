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

import java.util.LinkedHashSet;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;

/**
 * Single derivation result. In some parsers a single derivation captures a
 * number of parse trees that lead to the same logical form.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation type.
 * @see IParser
 * @see IParserOutput
 */
public interface IDerivation<MR> {
	/**
	 * Collect all lexical entries from all trees in this parse.
	 */
	LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntries();
	
	IHashVectorImmutable getAverageMaxFeatureVector();
	
	/**
	 * Get all lexical entries from all maximally scoring trees in this parse.
	 */
	LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries();
	
	/**
	 * Get all rules applied within all maximally scoring trees in this parse.
	 */
	LinkedHashSet<RuleUsageTriplet> getMaxRulesUsed();
	
	/**
	 * Parse viterbi score.
	 */
	double getScore();
	
	/**
	 * The semantics at the root of the parse.
	 */
	MR getSemantics();
	
	/**
	 * Get the number of parses packed into this derivation.
	 */
	long numParses();
	
}
