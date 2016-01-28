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
package edu.cornell.cs.nlp.spf.parser.graph;

import java.util.List;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Graph-based parser output. Supports compute expectationa and normalization
 * constant.
 *
 * @author Yoav Artzi
 * @param <MR>
 */
public interface IGraphParserOutput<MR> extends IParserOutput<MR> {

	@Override
	public List<? extends IGraphDerivation<MR>> getMaxDerivations(
			IFilter<Category<MR>> filter);

	@Override
	List<? extends IGraphDerivation<MR>> getAllDerivations();

	@Override
	List<? extends IGraphDerivation<MR>> getBestDerivations();

	@Override
	List<? extends IGraphDerivation<MR>> getDerivations(
			IFilter<Category<MR>> filter);

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
	IHashVector logExpectedFeatures(IFilter<Category<MR>> filter);

	/**
	 * Computes non-normalized log expected feature values over all complete
	 * parses. Outside scores of roots of complete parses are used using the
	 * gives scorer.To normalize, use the log normalization constant (
	 * {@link #logNorm()}).
	 */
	IHashVector logExpectedFeatures(IScorer<Category<MR>> initialScorer);

	/**
	 * Compute the log normalization constant over all complete parses.
	 */
	double logNorm();

	/**
	 * Compute the log normalization constant over all complete parses that pass
	 * the filter.
	 */
	double logNorm(IFilter<Category<MR>> filter);

}
