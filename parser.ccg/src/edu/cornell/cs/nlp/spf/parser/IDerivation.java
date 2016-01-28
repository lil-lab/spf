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

import java.util.LinkedHashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.parser.ccg.IWeightedParseStep;

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

	/**
	 * Get all parse steps that participate in this derivation.
	 */
	Set<? extends IWeightedParseStep<MR>> getAllSteps();

	IHashVectorImmutable getAverageMaxFeatureVector();

	/**
	 * @return The category at the root of the derivation.
	 */
	Category<MR> getCategory();

	/**
	 * Get all lexical entries from all maximally scoring trees in this parse.
	 */
	LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries();

	/**
	 * Get all rules applied within all maximally scoring trees in this parse.
	 */
	LinkedHashSet<RuleUsageTriplet> getMaxRulesUsed();

	/**
	 * Get all max scoring parse steps. Max scoring parse steps are steps that
	 * participate in max scoring trees in this derivation.
	 */
	LinkedHashSet<? extends IWeightedParseStep<MR>> getMaxSteps();

	/**
	 * Parse viterbi score. If this derivation packs multiple trees, it will
	 * return the score of the max scoring tree. If there are multiple trees
	 * with the max score, return the score of one of them.
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
