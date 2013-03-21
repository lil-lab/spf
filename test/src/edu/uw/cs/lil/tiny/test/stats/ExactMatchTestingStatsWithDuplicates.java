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
package edu.uw.cs.lil.tiny.test.stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.utils.counter.Counter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class ExactMatchTestingStatsWithDuplicates<X, Y> extends
		AbstractTestingStatistics<X, Y> {
	private static final String		DEFAULT_METRIC_NAME		= "DUPLICATE_EXACT";
	
	private static final ILogger	LOG						= LoggerFactory
																	.create(ExactMatchTestingStatsWithDuplicates.class);
	
	private final Map<X, Counter>	correctParses			= new HashMap<X, Counter>();
	private final Map<X, Counter>	noParses				= new HashMap<X, Counter>();
	private final Map<X, Counter>	numParses				= new HashMap<X, Counter>();
	private final Set<X>			sentences				= new HashSet<X>();
	private final Map<X, Counter>	skippingCorrectParses	= new HashMap<X, Counter>();
	private final Map<X, Counter>	skippingNoParses		= new HashMap<X, Counter>();
	private final Map<X, Counter>	skippingWrongParses		= new HashMap<X, Counter>();
	private final Map<X, Counter>	wrongParses				= new HashMap<X, Counter>();
	
	public ExactMatchTestingStatsWithDuplicates() {
		super(DEFAULT_METRIC_NAME);
	}
	
	public ExactMatchTestingStatsWithDuplicates(String prefix) {
		super(prefix, DEFAULT_METRIC_NAME);
	}
	
	public ExactMatchTestingStatsWithDuplicates(String prefix, String metricName) {
		super(prefix, metricName);
	}
	
	public int getCount(X sample, Map<X, Counter> map) {
		if (map.containsKey(sample)) {
			return map.get(sample).value();
		} else {
			return 0;
		}
	}
	
	public void inc(X sample, Map<X, Counter> map) {
		if (map.containsKey(sample)) {
			map.get(sample).inc();
		} else {
			map.put(sample, new Counter(1));
		}
	}
	
	@Override
	public void recordNoParse(IDataItem<X> dataItem, Y gold) {
		LOG.info("%s stats -- recording no parse", getMetricName());
		final X sample = dataItem.getSample();
		sentences.add(sample);
		inc(sample, numParses);
		inc(sample, noParses);
	}
	
	@Override
	public void recordNoParseWithSkipping(IDataItem<X> dataItem, Y gold) {
		LOG.info("%s stats -- recording no parse with skipping", getMetricName());
		final X sample = dataItem.getSample();
		sentences.add(sample);
		inc(sample, skippingNoParses);
	}
	
	@Override
	public void recordParse(IDataItem<X> dataItem, Y gold, Y label) {
		final X sample = dataItem.getSample();
		sentences.add(sample);
		inc(sample, numParses);
		if (gold.equals(label)) {
			LOG.info("%s stats -- recording correct parse: %s", getMetricName(),
					label);
			inc(sample, correctParses);
		} else {
			LOG.info("%s stats -- recording wrong parse: %s", getMetricName(),
					label);
			inc(sample, wrongParses);
		}
	}
	
	@Override
	public void recordParses(IDataItem<X> dataItem, Y gold, List<Y> labels) {
		recordNoParse(dataItem, gold);
	}
	
	@Override
	public void recordParsesWithSkipping(IDataItem<X> dataItem, Y gold,
			List<Y> labels) {
		recordNoParseWithSkipping(dataItem, gold);
	}
	
	@Override
	public void recordParseWithSkipping(IDataItem<X> dataItem, Y gold, Y label) {
		final X sample = dataItem.getSample();
		sentences.add(sample);
		if (gold.equals(label)) {
			LOG.info("%s stats -- recording correct parse with skipping: %s",
					getMetricName(), label);
			inc(sample, skippingCorrectParses);
		} else {
			LOG.info("%s stats -- recording wrong parse with skipping: %s",
					getMetricName(), label);
			inc(sample, skippingWrongParses);
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder("=== ").append(
				getMetricName()).append(" statistics:\n");
		ret.append("Recall: ").append(correctParses()).append('/')
				.append(numParses()).append(" = ").append(recall())
				.append('\n');
		ret.append("Precision: ").append(correctParses()).append('/')
				.append(numParses() - noParses()).append(" = ")
				.append(precision()).append('\n');
		ret.append("F1: ").append(f1()).append('\n');
		ret.append("SKIP Recall: ")
				.append(skippingCorrectParses() + correctParses()).append('/')
				.append(numParses()).append(" = ").append(skippingRecall())
				.append('\n');
		ret.append("SKIP Precision: ")
				.append(skippingCorrectParses() + correctParses()).append('/')
				.append(numParses() - skippingNoParses()).append(" = ")
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
	
	private double correctParses() {
		double ret = 0.0;
		for (final Map.Entry<X, Counter> entry : numParses.entrySet()) {
			ret += (double) getCount(entry.getKey(), correctParses)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	private double f1() {
		return (precision() + recall()) == 0.0 ? 0.0
				: (2 * precision() * recall()) / (precision() + recall());
	}
	
	private double noParses() {
		double ret = 0.0;
		for (final Map.Entry<X, Counter> entry : numParses.entrySet()) {
			ret += (double) getCount(entry.getKey(), noParses)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	private double numParses() {
		return numParses.size();
	}
	
	private double precision() {
		return (numParses() - noParses()) == 0.0 ? 0.0
				: (correctParses() / (numParses() - noParses()));
	}
	
	private double recall() {
		return numParses() == 0.0 ? 0.0 : correctParses() / numParses();
	}
	
	private double skippingCorrectParses() {
		double ret = 0.0;
		for (final Map.Entry<X, Counter> entry : numParses.entrySet()) {
			ret += (double) getCount(entry.getKey(), skippingCorrectParses)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	private double skippingF1() {
		return (skippingPrecision() + skippingRecall()) == 0.0 ? 0.0
				: (2 * skippingPrecision() * skippingRecall())
						/ (skippingPrecision() + skippingRecall());
	}
	
	private double skippingNoParses() {
		double ret = 0.0;
		for (final Map.Entry<X, Counter> entry : numParses.entrySet()) {
			ret += (double) getCount(entry.getKey(), skippingNoParses)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	private double skippingPrecision() {
		return (numParses() - skippingNoParses()) == 0.0 ? 0.0
				: (skippingCorrectParses() + correctParses())
						/ (numParses() - skippingNoParses());
	}
	
	private double skippingRecall() {
		return numParses() == 0.0 ? 0.0
				: (skippingCorrectParses() + correctParses()) / numParses();
	}
	
}
