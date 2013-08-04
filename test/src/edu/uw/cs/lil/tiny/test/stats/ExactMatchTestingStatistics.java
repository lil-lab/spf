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
	private static final String		DEFAULT_METRIC_NAME		= "EXACT";
	
	private static final ILogger	LOG						= LoggerFactory
																	.create(ExactMatchTestingStatistics.class);
	
	/**
	 * The number of correct parses.
	 */
	private int						correctParses			= 0;
	
	/**
	 * The number of parses that provided no single best parse.
	 */
	private int						noParses				= 0;
	
	/**
	 * Total number of parses recorded.
	 */
	private int						numParses				= 0;
	
	/**
	 * The number of correct parses. With word skipping.
	 */
	private int						skippingCorrectParses	= 0;
	
	/**
	 * The number of parses that provided no single best parse. With word
	 * skipping.
	 */
	private int						skippingNoParses		= 0;
	
	/**
	 * The number of single best wrong parses. With word skipping.
	 */
	private int						skippingWrongParses		= 0;
	
	/**
	 * The number of single best wrong parses.
	 */
	private int						wrongParses				= 0;
	
	public ExactMatchTestingStatistics() {
		super(DEFAULT_METRIC_NAME);
	}
	
	public ExactMatchTestingStatistics(String prefix) {
		super(prefix, DEFAULT_METRIC_NAME);
	}
	
	public ExactMatchTestingStatistics(String prefix, String metricName) {
		super(prefix, metricName);
	}
	
	@Override
	public void recordNoParse(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold) {
		LOG.info("%s stats -- recording no parse", getMetricName());
		numParses++;
		noParses++;
	}
	
	@Override
	public void recordNoParseWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold) {
		LOG.info("%s stats -- recording no parse with skipping",
				getMetricName());
		skippingNoParses++;
	}
	
	@Override
	public void recordParse(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, LABEL label) {
		numParses++;
		if (gold.equals(label)) {
			LOG.info("%s stats -- recording correct parse: %s",
					getMetricName(), label);
			correctParses++;
		} else {
			LOG.info("%s stats -- recording wrong parse: %s", getMetricName(),
					label);
			wrongParses++;
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
			skippingCorrectParses++;
		} else {
			LOG.info("%s stats -- recording wrong parse with skipping: %s",
					getMetricName(), label);
			skippingWrongParses++;
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder("=== ").append(
				getMetricName()).append(" statistics:\n");
		ret.append("Recall: ").append(correctParses).append('/')
				.append(numParses).append(" = ").append(recall()).append('\n');
		ret.append("Precision: ").append(correctParses).append('/')
				.append(numParses - noParses).append(" = ").append(precision())
				.append('\n');
		ret.append("F1: ").append(f1()).append('\n');
		ret.append("SKIP Recall: ")
				.append(skippingCorrectParses + correctParses).append('/')
				.append(numParses).append(" = ").append(skippingRecall())
				.append('\n');
		ret.append("SKIP Precision: ")
				.append(skippingCorrectParses + correctParses).append('/')
				.append(numParses - skippingNoParses).append(" = ")
				.append(skippingPrecision()).append('\n');
		ret.append("SKIP F1: ").append(skippingF1());
		return ret.toString();
	}
	
	@Override
	public String toTabDelimitedString() {
		final StringBuilder ret = new StringBuilder(getPrefix())
				.append("\tmetric=").append(getMetricName()).append("\t");
		ret.append("recall=").append(recall()).append('\t');
		ret.append("precision=").append(precision()).append('\t');
		ret.append("f1=").append(f1()).append('\t');
		ret.append("skippingRecall=").append(skippingRecall()).append('\t');
		ret.append("skippingPrecision=").append(skippingPrecision())
				.append('\t');
		ret.append("skippingF1=").append(skippingF1());
		return ret.toString();
	}
	
	private double f1() {
		return (precision() + recall()) == 0.0 ? 0.0
				: (2 * precision() * recall()) / (precision() + recall());
	}
	
	private double precision() {
		return (numParses - noParses) == 0.0 ? 0.0
				: ((double) correctParses / (numParses - noParses));
	}
	
	private double recall() {
		return numParses == 0.0 ? 0.0 : (double) correctParses / numParses;
	}
	
	private double skippingF1() {
		return (skippingPrecision() + skippingRecall()) == 0.0 ? 0.0
				: (2 * skippingPrecision() * skippingRecall())
						/ (skippingPrecision() + skippingRecall());
	}
	
	private double skippingPrecision() {
		return (numParses - skippingNoParses) == 0.0 ? 0.0
				: (double) (skippingCorrectParses + correctParses)
						/ (numParses - skippingNoParses);
	}
	
	private double skippingRecall() {
		return numParses == 0.0 ? 0.0
				: (double) (skippingCorrectParses + correctParses) / numParses;
	}
	
}
