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
package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;
import edu.uw.cs.utils.collections.CollectionUtils;

/**
 * Generate an EMPTY lexical entry for each token.
 * 
 * @author Yoav Artzi
 */
public class SimpleWordSkippingLexicalGenerator<MR> implements
		ISentenceLexiconGenerator<MR> {
	public static final String			SKIPPING_LEXICAL_ORIGIN	= "skipping";
	
	private final ICategoryServices<MR>	categoryServices;
	
	public SimpleWordSkippingLexicalGenerator(
			ICategoryServices<MR> categoryServices) {
		this.categoryServices = categoryServices;
	}
	
	@Override
	public Set<LexicalEntry<MR>> generateLexicon(Sentence sample,
			Sentence evidence) {
		final Set<LexicalEntry<MR>> lexicalEntries = new HashSet<LexicalEntry<MR>>();
		final List<String> tokens = evidence.getTokens();
		for (int j = 0; j < tokens.size(); j++) {
			// Single token empty lexical entry
			lexicalEntries.add(new LexicalEntry<MR>(CollectionUtils.subList(
					tokens, j, j + 1), categoryServices.getEmptyCategory(),
					SKIPPING_LEXICAL_ORIGIN));
		}
		return lexicalEntries;
	}
	
}
