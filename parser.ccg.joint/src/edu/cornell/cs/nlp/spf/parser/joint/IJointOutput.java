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
package edu.cornell.cs.nlp.spf.parser.joint;

import java.util.List;

import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Output for joint inference of parsing and semantics evaluation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 * @see IJointParser
 */
public interface IJointOutput<MR, ERESULT> {
	
	/**
	 * Base parser output.
	 */
	IParserOutput<MR> getBaseParserOutput();
	
	/**
	 * All joint derivations. Including evaluations that terminate in
	 * <code>null</code>.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getDerivations();
	
	/**
	 * All joint derivations.
	 * 
	 * @param includeFails
	 *            Include evaluations that terminate in <code>null</code>.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getDerivations(
			boolean includeFails);
	
	/**
	 * All derivations that conclude with the provided result.
	 * 
	 * @param result
	 *            Not null.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getDerivations(ERESULT result);
	
	/**
	 * All derivations that are valid according to the supplied filter.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getDerivations(
			IFilter<ERESULT> filter);
	
	/**
	 * Total inference time in milliseconds.
	 */
	long getInferenceTime();
	
	/**
	 * All max-scoring derivations.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getMaxDerivations();
	
	/**
	 * All max-scoring derivations.
	 * 
	 * @param includeFails
	 *            Include evaluations that terminate in <code>null</code>.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getMaxDerivations(
			boolean includeFails);
	
	/**
	 * All max-scoring derivations for the given result.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getMaxDerivations(
			ERESULT result);
	
	/**
	 * All max-scoring joint derivations that the filter validates.
	 */
	List<? extends IJointDerivation<MR, ERESULT>> getMaxDerivations(
			IFilter<ERESULT> filter);
	
	/**
	 * Indicates if inference was exact or approximate.
	 */
	boolean isExact();
}
