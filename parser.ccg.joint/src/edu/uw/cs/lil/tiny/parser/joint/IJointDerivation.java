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
package edu.uw.cs.lil.tiny.parser.joint;

import java.util.LinkedHashSet;
import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;

/**
 * Single joint inference derivation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics meaning formal representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 */
public interface IJointDerivation<MR, ERESULT> {
	
	/**
	 * All lexical entries used in base parses.
	 */
	LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntries();
	
	/**
	 * All lexical entries used in max-scoring base parses, which participate in
	 * max-scoring derivations.
	 */
	LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries();
	
	/**
	 * All rules applied in max-scoring base parses, which participate in
	 * max-scoring derivations.
	 */
	LinkedHashSet<RuleUsageTriplet> getMaxParsingRules();
	
	/**
	 * Semantics meaning representations (MR) that participate in all
	 * max-scoring derivations from these packed into this derivation.
	 */
	List<MR> getMaxSemantics();
	
	/**
	 * Mean features over all max-scoring base parses, which participate in
	 * max-scoring derivations.
	 */
	IHashVectorImmutable getMeanMaxFeatures();
	
	/**
	 * The final joint inference result that is the root of all derivations
	 * represented.
	 */
	ERESULT getResult();
	
	/**
	 * Complete derivation viterbi score.
	 */
	double getViterbiScore();
	
}
