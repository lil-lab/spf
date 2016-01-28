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
package edu.cornell.cs.nlp.spf.test.stats;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;

public abstract class AbstractTestingStatistics<SAMPLE, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>>
		implements ITestingStatistics<SAMPLE, LABEL, DI> {

	private final String				metricName;
	private final String				prefix;
	protected final IStatistics<SAMPLE>	stats;

	public AbstractTestingStatistics(String prefix, String metricName,
			IStatistics<SAMPLE> stats) {
		this.prefix = prefix;
		this.metricName = metricName;
		this.stats = stats;
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder("=== ").append(
				getMetricName()).append(" statistics:\n");
		ret.append("Recall: ").append(stats.getCorrects()).append('/')
				.append(stats.getTotal()).append(" = ").append(stats.recall())
				.append('\n');
		ret.append("Precision: ").append(stats.getCorrects()).append('/')
				.append(stats.getTotal() - stats.getFailures()).append(" = ")
				.append(stats.precision()).append('\n');
		ret.append("F1: ").append(stats.f1()).append('\n');
		ret.append("SKIP Recall: ")
				.append(stats.getSloppyCorrects() + stats.getCorrects())
				.append('/').append(stats.getTotal()).append(" = ")
				.append(stats.sloppyRecall()).append('\n');
		ret.append("SKIP Precision: ")
				.append(stats.getSloppyCorrects() + stats.getCorrects())
				.append('/')
				.append(stats.getTotal() - stats.getSloppyFailures())
				.append(" = ").append(stats.sloppyPrecision()).append('\n');
		ret.append("SKIP F1: ").append(stats.sloppyF1());
		return ret.toString();
	}

	@Override
	public String toTabDelimitedString() {
		final StringBuilder ret = new StringBuilder(getPrefix())
				.append("\tmetric=").append(getMetricName()).append("\t");
		ret.append("recall=").append(stats.recall()).append('\t');
		ret.append("precision=").append(stats.precision()).append('\t');
		ret.append("f1=").append(stats.f1()).append('\t');
		ret.append("skippingRecall=").append(stats.sloppyRecall()).append('\t');
		ret.append("skippingPrecision=").append(stats.sloppyPrecision())
				.append('\t');
		ret.append("skippingF1=").append(stats.sloppyF1());
		return ret.toString();
	}

	protected String getMetricName() {
		return metricName;
	}

	protected String getPrefix() {
		return prefix == null ? "" : prefix;
	}

}
