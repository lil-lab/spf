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
package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.graph.IGraphDerivation;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.filter.FilterUtils;
import edu.uw.cs.utils.filter.IFilter;

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
	private final Chart<MR>				chart;
	
	/** Total parsing time */
	private final long					parsingTime;
	
	public CKYParserOutput(Chart<MR> chart, long parsingTime) {
		this.chart = chart;
		this.parsingTime = parsingTime;
		this.allParses = Collections.unmodifiableList(chart.getParseResults());
		this.bestParses = Collections
				.unmodifiableList(findBestParses(allParses));
	}
	
	private static <MR> List<CKYDerivation<MR>> findBestParses(List<CKYDerivation<MR>> all) {
		return findBestParses(all, null);
	}
	
	private static <MR> List<CKYDerivation<MR>> findBestParses(
			List<CKYDerivation<MR>> all, IFilter<MR> filter) {
		final List<CKYDerivation<MR>> best = new LinkedList<CKYDerivation<MR>>();
		double bestScore = -Double.MAX_VALUE;
		for (final CKYDerivation<MR> p : all) {
			if (filter == null || filter.isValid(p.getSemantics())) {
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
	public List<CKYDerivation<MR>> getAllParses() {
		return allParses;
	}
	
	public List<CKYDerivation<MR>> getBestParses() {
		return bestParses;
	}
	
	public Chart<MR> getChart() {
		return chart;
	}
	
	@Override
	public List<? extends IGraphDerivation<MR>> getMaxParses(IFilter<MR> filter) {
		return findBestParses(allParses, filter);
	}
	
	@Override
	public List<? extends IGraphDerivation<MR>> getParses(final IFilter<MR> filter) {
		final List<? extends IGraphDerivation<MR>> parses = new ArrayList<IGraphDerivation<MR>>(
				allParses);
		CollectionUtils.filterInPlace(parses, new IFilter<IGraphDerivation<MR>>() {
			
			@Override
			public boolean isValid(IGraphDerivation<MR> e) {
				return filter.isValid(e.getSemantics());
			}
		});
		return parses;
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
		return logExpectedFeatures(FilterUtils.<MR> stubTrue());
	}
	
	/** {@inheritDoc} */
	@Override
	public IHashVector logExpectedFeatures(IFilter<MR> filter) {
		return chart.logExpectedFeatures(filter);
	}
	
	/** {@inheritDoc} */
	@Override
	public IHashVector logExpectedFeatures(IScorer<MR> initialScorer) {
		return chart.logExpectedFeatures(initialScorer);
	}
	
	/** {@inheritDoc} */
	@Override
	public double logNorm() {
		return logNorm(FilterUtils.<MR> stubTrue());
	}
	
	/** {@inheritDoc} */
	@Override
	public double logNorm(IFilter<MR> filter) {
		return chart.logNorm(filter);
	}
	
}
