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
package edu.uw.cs.lil.tiny.parser.joint;

import java.util.List;

import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.utils.composites.Pair;

public interface IJointOutput<LF, ERESULT> extends IParserOutput<LF> {
	
	/**
	 * All joint parses. Including failed executions.
	 * 
	 * @return
	 */
	List<? extends IJointParse<LF, ERESULT>> getAllJointParses();
	
	/**
	 * All joint parses.
	 * 
	 * @param includeFails
	 *            exclude failed execution iff 'false'
	 * @return
	 */
	List<? extends IJointParse<LF, ERESULT>> getAllJointParses(
			boolean includeFails);
	
	IParserOutput<LF> getBaseParserOutput();
	
	List<? extends IJointParse<LF, ERESULT>> getBestJointParses();
	
	List<? extends IJointParse<LF, ERESULT>> getBestJointParses(
			boolean includeFails);
	
	List<IJointParse<LF, ERESULT>> getBestParsesFor(Pair<LF, ERESULT> label);
	
	List<IJointParse<LF, ERESULT>> getBestParsesForY(LF partialLabel);
	
	List<IJointParse<LF, ERESULT>> getBestParsesForZ(ERESULT partialLabel);
	
	List<LexicalEntry<LF>> getMaxLexicalEntries(Pair<LF, ERESULT> label);
	
	/**
	 * Get all parses for the given result.
	 * 
	 * @param label
	 * @return null if no parse has this label
	 */
	List<IJointParse<LF, ERESULT>> getParsesFor(Pair<LF, ERESULT> label);
	
	List<IJointParse<LF, ERESULT>> getParsesForY(LF partialLabel);
	
	List<IJointParse<LF, ERESULT>> getParsesForZ(ERESULT partialLabel);
}
