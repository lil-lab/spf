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
package edu.cornell.cs.nlp.spf.parser.ccg.cky;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphDerivation;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParserOutput;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.filter.FilterUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Parser output of the CKY parser, including the chart and all possible parses.
 *
 * @param <MR>
 *            Type of meaning representation
 * @author Yoav Artzi
 */
public class CKYParserOutput<MR> implements IGraphParserOutput<MR> {

	/** All complete parses */
	private final List<CKYDerivation<MR>>	allParses;

	/** Max scoring complete parses */
	private final List<CKYDerivation<MR>>	bestParses;

	/** The CKY chart */
	private final Chart<MR>					chart;

	/** Total parsing time */
	private final long						parsingTime;

	public CKYParserOutput(Chart<MR> chart, long parsingTime) {
		this.chart = chart;
		this.parsingTime = parsingTime;
		this.allParses = Collections.unmodifiableList(chart.getParseResults());
		this.bestParses = Collections
				.unmodifiableList(findBestParses(allParses));
	}

	private static <MR> List<CKYDerivation<MR>> findBestParses(
			List<CKYDerivation<MR>> all) {
		return findBestParses(all, null);
	}

	private static <MR> List<CKYDerivation<MR>> findBestParses(
			List<CKYDerivation<MR>> all, IFilter<Category<MR>> filter) {
		final List<CKYDerivation<MR>> best = new LinkedList<CKYDerivation<MR>>();
		double bestScore = -Double.MAX_VALUE;
		for (final CKYDerivation<MR> p : all) {
			if (filter == null || filter.test(p.getCategory())) {
				if (p.getScore() == bestScore) {
					best.add(p);
				}
				if (p.getScore() > bestScore) {
					bestScore = p.getScore();
					best.clear();
					best.add(p);
				}
			}
		}
		return best;
	}

	@Override
	public List<CKYDerivation<MR>> getAllDerivations() {
		return allParses;
	}

	@Override
	public List<CKYDerivation<MR>> getBestDerivations() {
		return bestParses;
	}

	public Chart<MR> getChart() {
		return chart;
	}

	@Override
	public List<CKYDerivation<MR>> getDerivations(
			final IFilter<Category<MR>> filter) {
		final List<CKYDerivation<MR>> parses = new ArrayList<CKYDerivation<MR>>(
				allParses);
		CollectionUtils.filterInPlace(parses,
				new IFilter<IGraphDerivation<MR>>() {

					@Override
					public boolean test(IGraphDerivation<MR> e) {
						return filter.test(e.getCategory());
					}
				});
		return parses;
	}

	@Override
	public List<CKYDerivation<MR>> getMaxDerivations(
			IFilter<Category<MR>> filter) {
		return findBestParses(allParses, filter);
	}

	@Override
	public long getParsingTime() {
		return parsingTime;
	}

	@Override
	public boolean isExact() {
		return chart.getPrunedSpans().isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public IHashVector logExpectedFeatures() {
		return logExpectedFeatures(FilterUtils.<Category<MR>> stubTrue());
	}

	/** {@inheritDoc} */
	@Override
	public IHashVector logExpectedFeatures(IFilter<Category<MR>> filter) {
		return chart.logExpectedFeatures(filter);
	}

	/** {@inheritDoc} */
	@Override
	public IHashVector logExpectedFeatures(IScorer<Category<MR>> initialScorer) {
		return chart.logExpectedFeatures(initialScorer);
	}

	/** {@inheritDoc} */
	@Override
	public double logNorm() {
		return logNorm(FilterUtils.<Category<MR>> stubTrue());
	}

	/** {@inheritDoc} */
	@Override
	public double logNorm(IFilter<Category<MR>> filter) {
		return chart.logNorm(filter);
	}

}
