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
package edu.uw.cs.lil.tiny.parser.graph;

import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.filter.IFilter;

/**
 * Graph-based parser output. Supports compute expectationa and normalization
 * constant.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public interface IGraphParserOutput<MR> extends IParserOutput<MR> {
	
	@Override
	public List<? extends IGraphDerivation<MR>> getMaxParses(IFilter<MR> filter);
	
	@Override
	List<? extends IGraphDerivation<MR>> getAllParses();
	
	@Override
	List<? extends IGraphDerivation<MR>> getBestParses();
	
	@Override
	List<? extends IGraphDerivation<MR>> getParses(IFilter<MR> filter);
	
	/**
	 * Compute non-normalized log expected features values over all complete
	 * parses. To normalize, use the log normalization constant (
	 * {@link #logNorm()}).
	 */
	IHashVector logExpectedFeatures();
	
	/**
	 * Compute non-normalized log expected features values over all complete
	 * parses that pass the filter. To normalize, use the log normalization
	 * constant ( {@link #logNorm()}).
	 */
	IHashVector logExpectedFeatures(IFilter<MR> filter);
	
	/**
	 * Computes non-normalized log expected feature values over all complete
	 * parses. Outside scores of roots of complete parses are used using the
	 * gives scorer.To normalize, use the log normalization constant (
	 * {@link #logNorm()}).
	 */
	IHashVector logExpectedFeatures(IScorer<MR> initialScorer);
	
	/**
	 * Compute the log normalization constant over all complete parses.
	 */
	double logNorm();
	
	/**
	 * Compute the log normalization constant over all complete parses that pass
	 * the filter.
	 */
	double logNorm(IFilter<MR> filter);
	
}
