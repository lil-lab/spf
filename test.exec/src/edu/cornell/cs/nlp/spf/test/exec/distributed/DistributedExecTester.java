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
package edu.cornell.cs.nlp.spf.test.exec.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.exec.IExec;
import edu.cornell.cs.nlp.spf.exec.IExecOutput;
import edu.cornell.cs.nlp.spf.exec.IExecution;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.reliabledist.EnvironmentConfig;
import edu.cornell.cs.nlp.spf.reliabledist.JobFuture;
import edu.cornell.cs.nlp.spf.reliabledist.ReliableManager;
import edu.cornell.cs.nlp.spf.test.exec.IExecTester;
import edu.cornell.cs.nlp.spf.test.stats.ITestingStatistics;
import edu.cornell.cs.nlp.utils.filter.FilterUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Distributed generic execution tester for {@link IExec}. Uses TinyDist for
 * distributing inference.
 *
 * @author Yoav Artzi
 * @see IExec
 * @param <SAMPLE>
 *            Inference sample.
 * @param <DI>
 *            Data item.
 * @param <RESULT>
 *            Inference result.
 */
public class DistributedExecTester<SAMPLE extends IDataItem<Sentence>, RESULT, DI extends ILabeledDataItem<SAMPLE, RESULT>>
		implements IExecTester<SAMPLE, RESULT, DI> {
	public static final ILogger		LOG	= LoggerFactory
												.create(DistributedExecTester.class
														.getName());

	private final ReliableManager	manager;

	private final IFilter<SAMPLE>	skipExecutionFilter;

	protected DistributedExecTester(IFilter<SAMPLE> skipParsingFilter,
			ReliableManager manager) {
		this.skipExecutionFilter = skipParsingFilter;
		this.manager = manager;
		LOG.info("Init %s", DistributedExecTester.class);
	}

	@Override
	public void test(IExec<SAMPLE, RESULT> exec, IDataCollection<DI> data,
			ITestingStatistics<SAMPLE, RESULT, DI> stats) {
		final long startTime = System.currentTimeMillis();

		// Set the environment.
		final AbstractExecTestEnvironment<SAMPLE, RESULT> enviroment;
		if (manager.getEnviroment() instanceof AbstractExecTestEnvironment) {
			enviroment = manager.getEnviroment();
		} else {
			enviroment = new ExecTestEnvironment<SAMPLE, RESULT>();
			manager.setupEnviroment(enviroment);
		}
		final List<EnvironmentConfig<?>> update = new ArrayList<>(2);
		update.add(enviroment.updateExec(exec));
		update.add(enviroment.updateSkipExecutionFilter(skipExecutionFilter));
		if (!manager.updateEnviroment(update)) {
			LOG.error("Failed to update environment");
			return;
		}

		// Sort data length in ascending order. This allows us to first send the
		// hardest jobs out, potentially distributing the work better.
		final List<DI> sortedData = new ArrayList<DI>(data.size());
		for (final DI dataItem : data) {
			sortedData.add(dataItem);
		}
		sortedData.sort((o1, o2) -> Integer.compare(o2.getSample().getSample()
				.getTokens().size(), o1.getSample().getSample().getTokens()
				.size()));

		// Distribute inference.
		final List<JobFuture<TestJobResult<RESULT>>> futures = new ArrayList<JobFuture<TestJobResult<RESULT>>>(
				sortedData.size());
		for (final DI dataItem : sortedData) {
			futures.add(manager.execute(createTestJob(dataItem)));
		}

		// Wait for all jobs to finish.
		boolean working = true;
		final long distStartTime = System.currentTimeMillis();
		while (working) {
			working = false;
			int completed = 0;
			JobFuture<TestJobResult<RESULT>> remainingFuture = null;
			for (final JobFuture<TestJobResult<RESULT>> future : futures) {
				if (!future.isDone()) {
					remainingFuture = future;
					working = true;
				} else {
					++completed;
				}
			}
			LOG.info("Completed %d/%d (%.3fsec)", completed, futures.size(),
					(System.currentTimeMillis() - distStartTime) / 1000.0);
			if (remainingFuture != null) {
				try {
					remainingFuture.get(10, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					// Ignore.
				} catch (final ExecutionException e) {
					// Ignore.
				} catch (final TimeoutException e) {
					// Ignore.
				}
			}
		}
		LOG.info("TinyDist complete (%f.3sec)",
				(System.currentTimeMillis() - distStartTime) / 1000.0);

		final Iterator<DI> dataIterator = sortedData.iterator();
		final Iterator<JobFuture<TestJobResult<RESULT>>> futureIterator = futures
				.iterator();
		int itemCounter = 0;
		long computeTime = 0;
		while (dataIterator.hasNext()) {
			++itemCounter;
			final DI dataItem = dataIterator.next();
			final JobFuture<TestJobResult<RESULT>> future = futureIterator
					.next();

			LOG.info("%d : ==================", itemCounter);
			LOG.info("%s", dataItem);

			try {
				LOG.info(future.getLog());
			} catch (final InterruptedException e) {
				LOG.error("Failed to get log due to an exception: %s", e);
			}

			final TestJobResult<RESULT> result;
			try {
				result = future.get();
			} catch (final InterruptedException e) {
				LOG.error("Job failed: %s", e);
				continue;
			} catch (final ExecutionException e) {
				LOG.error("Job failed: %s", e);
				continue;
			}

			test(dataItem, result, stats);
			computeTime += result.processingTime;
		}

		// Log speedup.
		final long realTotalTime = System.currentTimeMillis() - startTime;
		LOG.info("Distribution speedup:");
		LOG.info("Real time: %.3f, compute time: %.3f, speedup: %.3f",
				realTotalTime / 1000.0, computeTime / 1000.0, computeTime
						/ (double) realTotalTime);
	}

	private void processSingleBestParse(DI dataItem,
			ResultWrapper<RESULT> result, boolean sloppy,
			ITestingStatistics<SAMPLE, RESULT, DI> stats) {
		// Update statistics.
		if (sloppy) {
			stats.recordParseWithSkipping(dataItem, result.getResult());
		} else {
			stats.recordParse(dataItem, result.getResult());
		}
	}

	private void test(DI dataItem, TestJobResult<RESULT> result,
			ITestingStatistics<SAMPLE, RESULT, DI> stats) {

		LOG.info("Execution time %.2f", result.processingTime / 1000.0);

		if (result.maxScoringResults == null) {
			// Case simple inference failed, and sloppy inference skipped due to
			// filter.
			LOG.info("No results from simple inference, skipping sloppy execution due to filter");
			stats.recordNoParseWithSkipping(dataItem);
			stats.recordNoParse(dataItem);
		} else if (result.sloppy) {
			// Case simple inference failed, did sloppy inference.
			LOG.info("No results from simple inference, doing sloppy inference");

			// Update stats with no result for simple inference.
			stats.recordNoParse(dataItem);

			if (result.maxScoringResults.size() == 1) {
				processSingleBestParse(dataItem,
						result.maxScoringResults.get(0), true, stats);
			} else if (result.maxScoringResults.isEmpty()) {
				// No results.
				LOG.info("No results from sloppy inference");

				stats.recordNoParseWithSkipping(dataItem);
			} else {
				// Too many results.
				LOG.info("Multiple max scoring results from sloppy inference.");
				stats.recordParsesWithSkipping(
						dataItem,
						result.maxScoringResults.stream()
								.map(wrapper -> wrapper.getResult())
								.collect(Collectors.toList()));
			}
		} else {
			// Simple inference (not sloppy).
			LOG.info("Simple inference");

			if (result.maxScoringResults.size() == 1) {
				// Case we have a single execution.
				processSingleBestParse(dataItem,
						result.maxScoringResults.get(0), false, stats);
			} else if (result.maxScoringResults.size() > 1) {
				// Update statistics.
				LOG.info("Multiple max scoring results.");
				stats.recordParses(
						dataItem,
						result.maxScoringResults.stream()
								.map(wrapper -> wrapper.getResult())
								.collect(Collectors.toList()));
			} else {
				// This should never happen. If we fail to generate any results,
				// we should do sloppy inference or skip sloppy (both handled
				// above).
				LOG.error("Simple inference only with no results -- probably a bug");
			}
		}
	}

	protected Function<AbstractExecTestEnvironment<SAMPLE, RESULT>, TestJobResult<RESULT>> createTestJob(
			DI dataItem) {
		return new TestJob<SAMPLE, RESULT, DI>(dataItem);
	}

	public static class Creator<SAMPLE extends IDataItem<Sentence>, RESULT, DI extends ILabeledDataItem<SAMPLE, RESULT>>
			implements
			IResourceObjectCreator<DistributedExecTester<SAMPLE, RESULT, DI>> {
		private final String	resourceName;

		public Creator() {
			this("tester.exec.dist");
		}

		public Creator(String resourceName) {
			this.resourceName = resourceName;
		}

		@Override
		public DistributedExecTester<SAMPLE, RESULT, DI> create(
				Parameters params, IResourceRepository repo) {
			final IFilter<SAMPLE> filter;
			if (params.contains("sloppyFilter")) {
				filter = repo.get(params.get("sloppyFilter"));
			} else {
				filter = FilterUtils.stubTrue();
			}
			return new DistributedExecTester<SAMPLE, RESULT, DI>(filter,
					(ReliableManager) repo.get(params.get("manager")));
		}

		@Override
		public String type() {
			return resourceName;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					DistributedExecTester.class)
					.addParam(
							"sloppyFilter",
							"id",
							"IFilter used to decide what data items to skip when doing sloppy inference (e.g., skipping words)")
					.addParam("manager", ReliableManager.class,
							"TintDist reliable manager").build();
		}

	}

	protected static class ResultWrapper<RESULT> implements Serializable {
		private static final long			serialVersionUID	= -5433917228047711586L;
		private final IHashVectorImmutable	features;
		private final RESULT				result;

		public ResultWrapper(RESULT result, IHashVectorImmutable features) {
			this.result = result;
			this.features = features;
		}

		public IHashVectorImmutable getFeatures() {
			return features;
		}

		public RESULT getResult() {
			return result;
		}
	}

	protected static class TestJob<SAMPLE extends IDataItem<?>, RESULT, DI extends ILabeledDataItem<SAMPLE, RESULT>>
			implements
			Function<AbstractExecTestEnvironment<SAMPLE, RESULT>, TestJobResult<RESULT>>,
			Serializable {

		public static final ILogger	LOG					= LoggerFactory
																.create(TestJob.class);

		private static final long	serialVersionUID	= -3244603620467529689L;

		protected final DI			dataItem;

		public TestJob(DI dataItem) {
			this.dataItem = dataItem;
		}

		@Override
		public TestJobResult<RESULT> apply(
				AbstractExecTestEnvironment<SAMPLE, RESULT> env) {

			// Try a simple model parse
			final IExecOutput<RESULT> execOutput = env.getExec().execute(
					dataItem.getSample());
			LOG.info("Test execution time %.2f",
					execOutput.getExecTime() / 1000.0);

			final List<IExecution<RESULT>> bestExecs = execOutput
					.getMaxExecutions();

			if (!bestExecs.isEmpty()) {
				LOG.info("%d max scoring execution results", bestExecs.size());

				if (bestExecs.size() == 1) {
					LOG.info(bestExecs.get(0).toString(true));
				}

				return new TestJobResult<RESULT>(bestExecs
						.stream()
						.map(o -> new ResultWrapper<>(o.getResult(), o
								.getFeatures())).collect(Collectors.toList()),
						false, execOutput.getExecTime(), bestExecs.get(0)
								.score());
			} else if (env.getSkipExecutionFilter().test(dataItem.getSample())) {
				final IExecOutput<RESULT> sloppyExecOutput = env.getExec()
						.execute(dataItem.getSample(), true);
				LOG.info("SLOPPY execution time %f",
						sloppyExecOutput.getExecTime() / 1000.0);
				final List<IExecution<RESULT>> bestSloppyExecutions = sloppyExecOutput
						.getMaxExecutions();
				LOG.info("%d sloppy max scoring execution results",
						bestSloppyExecutions.size());
				if (bestSloppyExecutions.size() == 1) {
					LOG.info("Single best sloppy execution:");
					LOG.info(bestSloppyExecutions.get(0).toString(true));
				} else if (!bestSloppyExecutions.isEmpty()) {
					LOG.info("Logging first one only");
					LOG.info(bestSloppyExecutions.get(0).toString(true));
				}
				return new TestJobResult<RESULT>(bestSloppyExecutions
						.stream()
						.map(o -> new ResultWrapper<>(o.getResult(), o
								.getFeatures())).collect(Collectors.toList()),
						true, execOutput.getExecTime()
								+ sloppyExecOutput.getExecTime(),
						bestSloppyExecutions.isEmpty() ? null
								: bestSloppyExecutions.get(0).score());
			} else {
				LOG.info("Skipping sloppy execution due to filter");
				return new TestJobResult<RESULT>(Collections.emptyList(),
						false, execOutput.getExecTime(), null);
			}
		}

	}

	protected static class TestJobResult<RESULT> implements Serializable {

		private static final long					serialVersionUID	= 6523717614664036780L;
		private final Double						maxScore;
		private final List<ResultWrapper<RESULT>>	maxScoringResults;
		private final long							processingTime;
		private final boolean						sloppy;

		public TestJobResult(List<ResultWrapper<RESULT>> maxScoringResults,
				boolean sloppy, long processingTime, Double maxScore) {
			this.maxScoringResults = maxScoringResults;
			this.sloppy = sloppy;
			this.processingTime = processingTime;
			this.maxScore = maxScore;
		}

		public Double getMaxScore() {
			return maxScore;
		}

		public List<ResultWrapper<RESULT>> getMaxScoringResults() {
			return maxScoringResults;
		}

		public long getProcessingTime() {
			return processingTime;
		}

		public boolean isSloppy() {
			return sloppy;
		}

	}

}
