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
package edu.uw.cs.lil.tiny.test.stats;

import java.util.List;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Testing statistics for the exact match metric.
 * 
 * @author Yoav Artzi
 * @param <SAMPLE>
 * @param <MR>
 */
public class ExactMatchTestingStatistics<SAMPLE, LABEL> extends
		AbstractTestingStatistics<SAMPLE, LABEL> {
	public static final ILogger	LOG					= LoggerFactory
															.create(ExactMatchTestingStatistics.class);
	
	private static final String	DEFAULT_METRIC_NAME	= "EXACT";
	
	public ExactMatchTestingStatistics() {
		this(null);
	}
	
	public ExactMatchTestingStatistics(String prefix) {
		this(prefix, DEFAULT_METRIC_NAME,
				new SimpleStats<ILabeledDataItem<SAMPLE, LABEL>>(
						DEFAULT_METRIC_NAME));
	}
	
	public ExactMatchTestingStatistics(String prefix, String metricName,
			IStatistics<ILabeledDataItem<SAMPLE, LABEL>> stats) {
		super(prefix, metricName, stats);
	}
	
	@Override
	public void recordNoParse(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold) {
		LOG.info("%s stats -- recording no parse", getMetricName());
		stats.recordFailure(dataItem);
	}
	
	@Override
	public void recordNoParseWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold) {
		LOG.info("%s stats -- recording no parse with skipping",
				getMetricName());
		stats.recordSloppyFailure(dataItem);
	}
	
	@Override
	public void recordParse(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, LABEL label) {
		if (gold.equals(label)) {
			LOG.info("%s stats -- recording correct parse: %s",
					getMetricName(), label);
			stats.recordCorrect(dataItem);
		} else {
			LOG.info("%s stats -- recording wrong parse: %s", getMetricName(),
					label);
			stats.recordIncorrect(dataItem);
		}
	}
	
	@Override
	public void recordParses(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, List<LABEL> labels) {
		recordNoParse(dataItem, gold);
	}
	
	@Override
	public void recordParsesWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold,
			List<LABEL> labels) {
		recordNoParseWithSkipping(dataItem, gold);
	}
	
	@Override
	public void recordParseWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold, LABEL label) {
		if (gold.equals(label)) {
			LOG.info("%s stats -- recording correct parse with skipping: %s",
					getMetricName(), label);
			stats.recordSloppyCorrect(dataItem);
		} else {
			LOG.info("%s stats -- recording wrong parse with skipping: %s",
					getMetricName(), label);
			stats.recordSloppyIncorrect(dataItem);
		}
	}
	
}
