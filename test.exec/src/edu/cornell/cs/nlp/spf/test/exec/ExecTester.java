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
package edu.cornell.cs.nlp.spf.test.exec;

import java.util.List;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.exec.IExec;
import edu.cornell.cs.nlp.spf.exec.IExecOutput;
import edu.cornell.cs.nlp.spf.exec.IExecution;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.test.stats.ITestingStatistics;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Generic execution tester for {@link IExec}.
 *
 * @author Yoav Artzi
 * @see IExec
 * @param <DI>
 * @param <RESULT>
 */
public class ExecTester<SAMPLE extends IDataItem<?>, RESULT, DI extends ILabeledDataItem<SAMPLE, RESULT>>
		implements IExecTester<SAMPLE, RESULT, DI> {
	public static final ILogger		LOG	= LoggerFactory
			.create(ExecTester.class.getName());

	protected final IFilter<SAMPLE>	skipExecutionFilter;

	protected ExecTester(IFilter<SAMPLE> skipParsingFilter) {
		this.skipExecutionFilter = skipParsingFilter;
		LOG.info("Init ExecTester");
	}

	@Override
	public void test(IExec<SAMPLE, RESULT> exec, IDataCollection<DI> dataset,
			ITestingStatistics<SAMPLE, RESULT, DI> stats) {
		int itemCounter = 0;
		for (final DI item : dataset) {
			++itemCounter;
			test(itemCounter, item, exec, stats);
		}
	}

	protected void processSingleBestParse(DI dataItem,
			final IExecOutput<RESULT> execOutput, IExecution<RESULT> execution,
			boolean sloppy, ITestingStatistics<SAMPLE, RESULT, DI> stats) {
		final RESULT label = execution.getResult();

		// Update statistics
		if (sloppy) {
			stats.recordParseWithSkipping(dataItem, label);
		} else {
			stats.recordParse(dataItem, label);
		}

		if (dataItem.isCorrect(label)) {
			// A correct parse
			LOG.info("CORRECT: %s", execution.toString(true));
		} else {
			// One parse, but a wrong one
			LOG.info("WRONG: %s", execution.toString(true));

			// Check if we had the correct parse and it just wasn't the best
			final List<IExecution<RESULT>> correctExecs = execOutput
					.getExecutions(dataItem.getLabel());
			LOG.info("Had correct result: %s", !correctExecs.isEmpty());
			for (final IExecution<RESULT> correctExec : correctExecs) {
				LOG.info(correctExec.toString(true));
			}

		}
	}

	protected void test(int itemCounter, DI dataItem,
			IExec<SAMPLE, RESULT> exec,
			ITestingStatistics<SAMPLE, RESULT, DI> stats) {
		LOG.info("%d : ==================", itemCounter);
		LOG.info("%s", dataItem);

		// Try a simple model parse
		final IExecOutput<RESULT> execOutput = exec
				.execute(dataItem.getSample());
		LOG.info("Test execution time %.2f", execOutput.getExecTime() / 1000.0);

		final List<IExecution<RESULT>> bestExecs = execOutput
				.getMaxExecutions();
		if (bestExecs.size() == 1) {
			// Case we have a single execution
			processSingleBestParse(dataItem, execOutput, bestExecs.get(0),
					false, stats);
		} else if (bestExecs.size() > 1) {
			// Multiple top executions

			// Update statistics
			stats.recordParses(dataItem,
					ListUtils.map(bestExecs, obj -> obj.getResult()));

			// There are more than one equally high scoring
			// logical forms. If this is the case, we abstain
			// from returning a result.
			LOG.info("too many results");
			LOG.info("%d results:", bestExecs.size());
			for (final IExecution<RESULT> execution : bestExecs) {
				LOG.info(execution.toString(true));
			}
			// Check if we had the correct parse and it just wasn't the best
			final List<IExecution<RESULT>> correctExecs = execOutput
					.getExecutions(dataItem.getLabel());
			LOG.info("Had correct result: %s", !correctExecs.isEmpty());
			for (final IExecution<RESULT> correctExec : correctExecs) {
				LOG.info(correctExec.toString(true));
			}
		} else {
			// No parses
			LOG.info("no results");

			// Update stats
			stats.recordNoParse(dataItem);

			// Potentially re-execute -- sloppy execution
			LOG.info("no parses");
			if (skipExecutionFilter.test(dataItem.getSample())) {
				final IExecOutput<RESULT> sloppyExecOutput = exec
						.execute(dataItem.getSample(), true);
				LOG.info("SLOPPY execution time %f",
						sloppyExecOutput.getExecTime() / 1000.0);
				final List<IExecution<RESULT>> bestSloppyExecutions = sloppyExecOutput
						.getMaxExecutions();

				if (bestSloppyExecutions.size() == 1) {
					processSingleBestParse(dataItem, sloppyExecOutput,
							bestSloppyExecutions.get(0), true, stats);
				} else if (bestSloppyExecutions.isEmpty()) {
					// No results
					LOG.info("no results");

					stats.recordNoParseWithSkipping(dataItem);
				} else {
					// too many results
					stats.recordParsesWithSkipping(dataItem, ListUtils
							.map(bestSloppyExecutions, obj -> obj.getResult()));

					LOG.info("WRONG: %d results", bestSloppyExecutions.size());
					for (final IExecution<RESULT> execution : bestSloppyExecutions) {
						LOG.info(execution.toString(true));
					}
					// Check if we had the correct execution and it just wasn't
					// the best
					final List<IExecution<RESULT>> correctExecs = sloppyExecOutput
							.getExecutions(dataItem.getLabel());
					LOG.info("Had correct result: %s", !correctExecs.isEmpty());
					for (final IExecution<RESULT> correctExec : correctExecs) {
						LOG.info(correctExec.toString(true));
					}
				}
			} else {
				LOG.info("Skipping sloppy execution due to filter");
				stats.recordNoParseWithSkipping(dataItem);
			}
		}
	}

	public static class Builder<SAMPLE extends IDataItem<?>, RESULT, DI extends ILabeledDataItem<SAMPLE, RESULT>> {

		/** Filters which data items are valid for parsing with word skipping */
		private IFilter<SAMPLE> skipParsingFilter = e -> true;

		public ExecTester<SAMPLE, RESULT, DI> build() {
			return new ExecTester<SAMPLE, RESULT, DI>(skipParsingFilter);
		}

		public Builder<SAMPLE, RESULT, DI> setSkipParsingFilter(
				IFilter<SAMPLE> skipParsingFilter) {
			this.skipParsingFilter = skipParsingFilter;
			return this;
		}
	}

	public static class Creator<SAMPLE extends IDataItem<?>, RESULT, DI extends ILabeledDataItem<SAMPLE, RESULT>>
			implements IResourceObjectCreator<ExecTester<SAMPLE, RESULT, DI>> {
		private static final String	DEFAULT_NAME	= "tester.exec";
		private final String		resourceName;

		public Creator() {
			this(DEFAULT_NAME);
		}

		public Creator(String resourceName) {
			this.resourceName = resourceName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ExecTester<SAMPLE, RESULT, DI> create(Parameters params,
				IResourceRepository repo) {
			final Builder<SAMPLE, RESULT, DI> builder = new ExecTester.Builder<SAMPLE, RESULT, DI>();

			if (params.contains("sloppyFilter")) {
				builder.setSkipParsingFilter(
						(IFilter<SAMPLE>) repo.get(params.get("sloppyFilter")));
			}

			return builder.build();
		}

		@Override
		public String type() {
			return resourceName;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), ExecTester.class)
					.addParam("sloppyFilter", "id",
							"IFilter used to decide what data items to skip when doing sloppy inference (e.g., skipping words)")
					.build();
		}

	}

}
