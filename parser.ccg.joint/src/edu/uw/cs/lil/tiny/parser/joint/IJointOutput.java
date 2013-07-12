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

import java.util.List;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.utils.composites.Pair;

/**
 * Output for joint inference of parsing and semantics evaluation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 * @param <ERESULT>
 */
public interface IJointOutput<MR, ERESULT> {
	
	/**
	 * All joint parses. Including failed executions.
	 * 
	 * @return
	 */
	List<IJointParse<MR, ERESULT>> getAllParses();
	
	/**
	 * All joint parses.
	 * 
	 * @param includeFails
	 *            exclude failed execution iff 'false'
	 * @return
	 */
	List<IJointParse<MR, ERESULT>> getAllParses(boolean includeFails);
	
	/**
	 * Internal output of the base parser.
	 * 
	 * @return
	 */
	IParserOutput<MR> getBaseParserOutput();
	
	List<IJointParse<MR, ERESULT>> getBestParses();
	
	List<IJointParse<MR, ERESULT>> getBestParses(boolean includeFails);
	
	List<IJointParse<MR, ERESULT>> getBestParsesFor(Pair<MR, ERESULT> label);
	
	List<IJointParse<MR, ERESULT>> getBestParsesForY(MR partialLabel);
	
	List<IJointParse<MR, ERESULT>> getBestParsesForZ(ERESULT partialLabel);
	
	/**
	 * Total inference time in milliseconds.
	 * 
	 * @return
	 */
	long getInferenceTime();
	
	List<LexicalEntry<MR>> getMaxLexicalEntries(Pair<MR, ERESULT> label);
	
	/**
	 * Get all parses for the given result.
	 * 
	 * @param label
	 * @return null if no parse has this label
	 */
	List<IJointParse<MR, ERESULT>> getParsesFor(Pair<MR, ERESULT> label);
	
	List<IJointParse<MR, ERESULT>> getParsesForY(MR partialLabel);
	
	List<IJointParse<MR, ERESULT>> getParsesForZ(ERESULT partialLabel);
}
