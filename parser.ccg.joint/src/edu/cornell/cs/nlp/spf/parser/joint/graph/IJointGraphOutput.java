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
package edu.cornell.cs.nlp.spf.parser.joint.graph;

import java.util.List;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParser;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParserOutput;
import edu.cornell.cs.nlp.spf.parser.joint.IJointOutput;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Output for joint inference of parsing and semantics evaluation using a
 * graph-based parser ({@link IGraphParser}).
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 * @see IJointOutput
 */
public interface IJointGraphOutput<MR, ERESULT> extends
		IJointOutput<MR, ERESULT> {
	
	@Override
	public IGraphParserOutput<MR> getBaseParserOutput();
	
	@Override
	public List<? extends IJointGraphDerivation<MR, ERESULT>> getDerivations();
	
	@Override
	public List<? extends IJointGraphDerivation<MR, ERESULT>> getDerivations(
			boolean includeFails);
	
	@Override
	public List<? extends IJointGraphDerivation<MR, ERESULT>> getDerivations(
			ERESULT result);
	
	@Override
	public List<? extends IJointGraphDerivation<MR, ERESULT>> getDerivations(
			IFilter<ERESULT> filter);
	
	@Override
	public List<? extends IJointGraphDerivation<MR, ERESULT>> getMaxDerivations(
			ERESULT result);
	
	@Override
	public List<? extends IJointGraphDerivation<MR, ERESULT>> getMaxDerivations(
			IFilter<ERESULT> filter);
	
	@Override
	List<? extends IJointGraphDerivation<MR, ERESULT>> getMaxDerivations();
	
	@Override
	List<? extends IJointGraphDerivation<MR, ERESULT>> getMaxDerivations(
			boolean includeFails);
	
	/**
	 * Compute non-normalized log expected features values over all derivations.
	 * To normalize, use the log normalization constant ( {@link #logNorm()}).
	 */
	IHashVector logExpectedFeatures();
	
	/**
	 * Compute non-normalized log expected features values over all derivations
	 * that pass the filter. To normalize, use the log normalization constant (
	 * {@link #logNorm()}).
	 */
	IHashVector logExpectedFeatures(IFilter<ERESULT> filter);
	
	/**
	 * Log normalization constant.
	 */
	double logNorm();
	
	/**
	 * Log normalization constant using a filter.
	 */
	double logNorm(IFilter<ERESULT> filter);
}
