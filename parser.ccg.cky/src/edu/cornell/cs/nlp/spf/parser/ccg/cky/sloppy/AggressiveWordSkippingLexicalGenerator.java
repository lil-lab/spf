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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy;

import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.parser.ISentenceLexiconGenerator;
import edu.cornell.cs.nlp.utils.collections.MapUtils;

/**
 * Generate an EMPTY lexical entry for each sub-sequence of tokens.
 *
 * @author Yoav Artzi
 */
public class AggressiveWordSkippingLexicalGenerator<DI extends Sentence, MR>
		implements ISentenceLexiconGenerator<DI, MR> {
	public static final String			SKIPPING_LEXICAL_ORIGIN	= "skipping";

	/**
	 *
	 */
	private static final long			serialVersionUID		= 2264930688349857861L;

	private final ICategoryServices<MR>	categoryServices;

	public AggressiveWordSkippingLexicalGenerator(
			ICategoryServices<MR> categoryServices) {
		this.categoryServices = categoryServices;
	}

	@Override
	public Set<LexicalEntry<MR>> generateLexicon(DI sample) {
		final Set<LexicalEntry<MR>> lexicalEntries = new HashSet<LexicalEntry<MR>>();
		final TokenSeq tokens = sample.getTokens();
		final int numTokens = tokens.size();
		for (int i = 0; i < numTokens; i++) {
			for (int j = i; j < numTokens; j++) {
				lexicalEntries.add(new LexicalEntry<MR>(tokens.sub(i, j + 1),
						categoryServices.getEmptyCategory(), true, MapUtils
								.createSingletonMap(
										LexicalEntry.ORIGIN_PROPERTY,
										SKIPPING_LEXICAL_ORIGIN)));
			}
		}
		return lexicalEntries;
	}

}
