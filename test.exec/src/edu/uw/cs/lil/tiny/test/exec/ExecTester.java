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
package edu.uw.cs.lil.tiny.test.exec;

import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.exec.IExec;
import edu.uw.cs.lil.tiny.exec.IExecOutput;
import edu.uw.cs.lil.tiny.exec.IExecution;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.test.stats.ITestingStatistics;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Generic execution tester for {@link IExec}.
 * 
 * @author Yoav Artzi
 * @see IExec
 * @param <DI>
 * @param <RESULT>
 */
public class ExecTester<DI extends IDataItem<?>, RESULT> {
	public static final ILogger							LOG	= LoggerFactory
																	.create(ExecTester.class
																			.getName());
	
	private final IFilter<ILabeledDataItem<DI, RESULT>>	skipExecutionFilter;
	
	private ExecTester(IFilter<ILabeledDataItem<DI, RESULT>> skipParsingFilter) {
		this.skipExecutionFilter = skipParsingFilter;
		LOG.info("Init ExecTester");
	}
	
	public void test(IExec<DI, RESULT> exec,
			IDataCollection<? extends ILabeledDataItem<DI, RESULT>> dataset,
			ITestingStatistics<DI, RESULT> stats) {
		int itemCounter = 0;
		for (final ILabeledDataItem<DI, RESULT> item : dataset) {
			++itemCounter;
			test(itemCounter, item, exec, stats);
		}
	}
	
	private void processSingleBestParse(ILabeledDataItem<DI, RESULT> dataItem,
			final IExecOutput<RESULT> execOutput, IExecution<RESULT> execution,
			boolean sloppy, ITestingStatistics<DI, RESULT> stats) {
		final RESULT label = execution.getResult();
		
		// Update statistics
		if (sloppy) {
			stats.recordParseWithSkipping(dataItem, dataItem.getLabel(), label);
		} else {
			stats.recordParse(dataItem, dataItem.getLabel(), label);
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
	
	private void test(int itemCounter, ILabeledDataItem<DI, RESULT> dataItem,
			IExec<DI, RESULT> exec, ITestingStatistics<DI, RESULT> stats) {
		LOG.info("%d : ==================", itemCounter);
		LOG.info("%s", dataItem);
		
		// Try a simple model parse
		final IExecOutput<RESULT> execOutput = exec.execute(dataItem
				.getSample());
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
			stats.recordParses(dataItem, dataItem.getLabel(), ListUtils.map(
					bestExecs,
					new ListUtils.Mapper<IExecution<RESULT>, RESULT>() {
						@Override
						public RESULT process(IExecution<RESULT> obj) {
							return obj.getResult();
						}
					}));
			
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
			stats.recordNoParse(dataItem, dataItem.getLabel());
			
			// Potentially re-execute -- sloppy execution
			LOG.info("no parses");
			if (skipExecutionFilter.isValid(dataItem)) {
				final IExecOutput<RESULT> sloppyExecOutput = exec.execute(
						dataItem.getSample(), true);
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
					
					stats.recordNoParseWithSkipping(dataItem,
							dataItem.getLabel());
				} else {
					// too many results
					stats.recordParsesWithSkipping(dataItem, dataItem
							.getLabel(), ListUtils.map(bestSloppyExecutions,
							new ListUtils.Mapper<IExecution<RESULT>, RESULT>() {
								@Override
								public RESULT process(IExecution<RESULT> obj) {
									return obj.getResult();
								}
							}));
					
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
				stats.recordNoParseWithSkipping(dataItem, dataItem.getLabel());
			}
		}
	}
	
	public static class Builder<DI extends IDataItem<?>, RESULT> {
		
		/** Filters which data items are valid for parsing with word skipping */
		private IFilter<ILabeledDataItem<DI, RESULT>>	skipParsingFilter	= new IFilter<ILabeledDataItem<DI, RESULT>>() {
																				
																				@Override
																				public boolean isValid(
																						ILabeledDataItem<DI, RESULT> e) {
																					return true;
																				}
																			};
		
		public ExecTester<DI, RESULT> build() {
			return new ExecTester<DI, RESULT>(skipParsingFilter);
		}
		
		public Builder<DI, RESULT> setSkipParsingFilter(
				IFilter<ILabeledDataItem<DI, RESULT>> skipParsingFilter) {
			this.skipParsingFilter = skipParsingFilter;
			return this;
		}
	}
	
	public static class Creator<DI extends IDataItem<?>, RESULT> implements
			IResourceObjectCreator<ExecTester<DI, RESULT>> {
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
		public ExecTester<DI, RESULT> create(Parameters params,
				IResourceRepository repo) {
			final Builder<DI, RESULT> builder = new ExecTester.Builder<DI, RESULT>();
			
			if (params.contains("sloppyFilter")) {
				builder.setSkipParsingFilter((IFilter<ILabeledDataItem<DI, RESULT>>) repo
						.getResource(params.get("sloppyFilter")));
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
					.addParam(
							"sloppyFilter",
							"id",
							"IFilter used to decide what data items to skip when doing sloppy inference (e.g., skipping words)")
					.build();
		}
		
	}
	
}
