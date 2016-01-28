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

import java.util.List;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Testing statistics for the exact match metric.
 *
 * @author Yoav Artzi
 * @param <SAMPLE>
 *            Testing sample.
 * @param <LABEL>
 *            Provided label.
 */
public class ExactMatchTestingStatistics<SAMPLE, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>>
		extends AbstractTestingStatistics<SAMPLE, LABEL, DI> {
	public static final ILogger	LOG					= LoggerFactory
															.create(ExactMatchTestingStatistics.class);

	private static final String	DEFAULT_METRIC_NAME	= "EXACT";

	public ExactMatchTestingStatistics() {
		this(null);
	}

	public ExactMatchTestingStatistics(String prefix) {
		this(prefix, DEFAULT_METRIC_NAME);
	}

	public ExactMatchTestingStatistics(String prefix, String metricName) {
		this(prefix, metricName, new SimpleStats<SAMPLE>(DEFAULT_METRIC_NAME));
	}

	public ExactMatchTestingStatistics(String prefix, String metricName,
			IStatistics<SAMPLE> stats) {
		super(prefix, metricName, stats);
	}

	@Override
	public void recordNoParse(DI dataItem) {
		LOG.info("%s stats -- recording no parse", getMetricName());
		stats.recordFailure(dataItem.getSample());
	}

	@Override
	public void recordNoParseWithSkipping(DI dataItem) {
		LOG.info("%s stats -- recording no parse with skipping",
				getMetricName());
		stats.recordSloppyFailure(dataItem.getSample());
	}

	@Override
	public void recordParse(DI dataItem, LABEL candidate) {
		if (dataItem.getLabel().equals(candidate)) {
			LOG.info("%s stats -- recording correct parse: %s",
					getMetricName(), candidate);
			stats.recordCorrect(dataItem.getSample());
		} else {
			LOG.info("%s stats -- recording wrong parse: %s", getMetricName(),
					candidate);
			stats.recordIncorrect(dataItem.getSample());
		}
	}

	@Override
	public void recordParses(DI dataItem, List<LABEL> candidates) {
		recordNoParse(dataItem);
	}

	@Override
	public void recordParsesWithSkipping(DI dataItem, List<LABEL> labels) {
		recordNoParseWithSkipping(dataItem);
	}

	@Override
	public void recordParseWithSkipping(DI dataItem, LABEL candidate) {
		if (dataItem.getLabel().equals(candidate)) {
			LOG.info("%s stats -- recording correct parse with skipping: %s",
					getMetricName(), candidate);
			stats.recordSloppyCorrect(dataItem.getSample());
		} else {
			LOG.info("%s stats -- recording wrong parse with skipping: %s",
					getMetricName(), candidate);
			stats.recordSloppyIncorrect(dataItem.getSample());
		}
	}

	public static class Creator<SAMPLE, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>>
			implements
			IResourceObjectCreator<ExactMatchTestingStatistics<SAMPLE, LABEL, DI>> {

		private String	type;

		public Creator() {
			this("test.stats.exact");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ExactMatchTestingStatistics<SAMPLE, LABEL, DI> create(
				Parameters params, IResourceRepository repo) {
			return new ExactMatchTestingStatistics<SAMPLE, LABEL, DI>(
					params.get("prefix"), params.get("name",
							DEFAULT_METRIC_NAME));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, ExactMatchTestingStatistics.class)
					.addParam("prefix", String.class,
							"Prefix string used to identify this metric")
					.addParam(
							"name",
							String.class,
							"Metric name (default: " + DEFAULT_METRIC_NAME
									+ ")").build();
		}

	}

}
