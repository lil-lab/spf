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
package edu.uw.cs.lil.tiny.test;

import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.test.stats.ITestingStatistics;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class Tester<SAMPLE extends IDataItem<?>, MR> implements
		ITester<SAMPLE, MR> {
	
	public static final ILogger												LOG	= LoggerFactory
																						.create(Tester.class
																								.getName());
	
	private final IParser<SAMPLE, MR>										parser;
	
	private final IFilter<SAMPLE>											skipParsingFilter;
	
	private final IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>>	testData;
	
	private Tester(
			IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>> testData,
			IFilter<SAMPLE> skipParsingFilter, IParser<SAMPLE, MR> parser) {
		this.testData = testData;
		this.skipParsingFilter = skipParsingFilter;
		this.parser = parser;
		LOG.info("Init Tester:  size(testData)=%d", testData.size());
	}
	
	@Override
	public void test(IModelImmutable<SAMPLE, MR> model,
			ITestingStatistics<SAMPLE, MR> stats) {
		test(testData, model, stats);
	}
	
	private String lexToString(Iterable<LexicalEntry<MR>> lexicalEntries,
			IModelImmutable<SAMPLE, MR> model) {
		final StringBuilder ret = new StringBuilder();
		ret.append("[LexEntries and scores:\n");
		for (final LexicalEntry<MR> entry : lexicalEntries) {
			ret.append("[").append(model.score(entry)).append("] ");
			ret.append(entry);
			ret.append(" [");
			ret.append(model.getTheta().printValues(
					model.computeFeatures(entry)));
			ret.append("]\n");
		}
		ret.append("]");
		return ret.toString();
	}
	
	private void logParse(ILabeledDataItem<SAMPLE, MR> dataItem,
			IDerivation<MR> parse, boolean logLexicalItems, String tag,
			IModelImmutable<SAMPLE, MR> model) {
		LOG.info("%s%s[S%.2f] %s",
				dataItem.getLabel().equals(parse.getSemantics()) ? "* " : "  ",
				tag == null ? "" : tag + " ", parse.getScore(), parse);
		LOG.info("Calculated score: %f", parse.getAverageMaxFeatureVector()
				.vectorMultiply(model.getTheta()));
		LOG.info("Features: %s",
				model.getTheta()
						.printValues(parse.getAverageMaxFeatureVector()));
		if (logLexicalItems) {
			for (final LexicalEntry<MR> entry : parse.getMaxLexicalEntries()) {
				LOG.info("\t[%f] %s", model.score(entry), entry);
			}
		}
	}
	
	private void processSingleBestParse(
			final ILabeledDataItem<SAMPLE, MR> dataItem,
			IModelImmutable<SAMPLE, MR> model,
			final IParserOutput<MR> modelParserOutput, final IDerivation<MR> parse,
			boolean withWordSkipping, ITestingStatistics<SAMPLE, MR> stats) {
		final Set<LexicalEntry<MR>> lexicalEntries = parse
				.getMaxLexicalEntries();
		final MR label = parse.getSemantics();
		
		// Update statistics
		if (withWordSkipping) {
			stats.recordParseWithSkipping(dataItem, dataItem.getLabel(),
					parse.getSemantics());
		} else {
			stats.recordParse(dataItem, dataItem.getLabel(),
					parse.getSemantics());
		}
		
		if (dataItem.isCorrect(label)) {
			// A correct parse
			LOG.info("CORRECT");
			LOG.info(lexToString(lexicalEntries, model));
		} else {
			// One parse, but a wrong one
			LOG.info("WRONG: %s", label);
			LOG.info(lexToString(lexicalEntries, model));
			
			// Check if we had the correct parse and it just wasn't the best
			final List<? extends IDerivation<MR>> correctParses = modelParserOutput
					.getMaxParses(new IFilter<MR>() {
						
						@Override
						public boolean isValid(MR e) {
							return dataItem.getLabel().equals(e);
						}
					});
			LOG.info("Had correct parses: %s", !correctParses.isEmpty());
			if (!correctParses.isEmpty()) {
				for (final IDerivation<MR> correctParse : correctParses) {
					LOG.info(
							"Correct parse lexical items:\n%s",
							lexToString(correctParse.getMaxLexicalEntries(),
									model));
					LOG.info(
							"Correct feats: %s",
							model.getTheta().printValues(
									correctParse.getAverageMaxFeatureVector()));
					final IHashVector diff = correctParse
							.getAverageMaxFeatureVector().addTimes(-1.0,
									parse.getAverageMaxFeatureVector());
					diff.dropNoise();
					LOG.info("Diff: %s", model.getTheta().printValues(diff));
				}
			}
			LOG.info(
					"Feats: %s",
					model.getTheta().printValues(
							parse.getAverageMaxFeatureVector()));
		}
	}
	
	private void test(
			IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>> dataset,
			IModelImmutable<SAMPLE, MR> model,
			ITestingStatistics<SAMPLE, MR> stats) {
		int itemCounter = 0;
		for (final ILabeledDataItem<SAMPLE, MR> item : dataset) {
			++itemCounter;
			test(itemCounter, item, model, stats);
		}
	}
	
	private void test(int itemCounter,
			final ILabeledDataItem<SAMPLE, MR> dataItem,
			IModelImmutable<SAMPLE, MR> model,
			ITestingStatistics<SAMPLE, MR> stats) {
		LOG.info("%d : ==================", itemCounter);
		LOG.info("%s", dataItem);
		
		// Try a simple model parse
		final IParserOutput<MR> modelParserOutput = parser.parse(
				dataItem.getSample(),
				model.createDataItemModel(dataItem.getSample()));
		LOG.info("Test parsing time %.2f",
				modelParserOutput.getParsingTime() / 1000.0);
		
		final List<? extends IDerivation<MR>> bestModelParses = modelParserOutput
				.getBestParses();
		if (bestModelParses.size() == 1) {
			// Case we have a single parse
			processSingleBestParse(dataItem, model, modelParserOutput,
					bestModelParses.get(0), false, stats);
		} else if (bestModelParses.size() > 1) {
			// Multiple top parses
			
			// Update statistics
			stats.recordParses(dataItem, dataItem.getLabel(), ListUtils.map(
					bestModelParses, new ListUtils.Mapper<IDerivation<MR>, MR>() {
						@Override
						public MR process(IDerivation<MR> obj) {
							return obj.getSemantics();
						}
					}));
			
			// There are more than one equally high scoring
			// logical forms. If this is the case, we abstain
			// from returning a result.
			LOG.info("too many parses");
			LOG.info("%d parses:", bestModelParses.size());
			for (final IDerivation<MR> parse : bestModelParses) {
				logParse(dataItem, parse, false, null, model);
			}
			// Check if we had the correct parse and it just wasn't the best
			final List<? extends IDerivation<MR>> correctParses = modelParserOutput
					.getMaxParses(new IFilter<MR>() {
						
						@Override
						public boolean isValid(MR e) {
							return dataItem.getLabel().equals(e);
						}
					});
			
			LOG.info("Had correct parses: %s", !correctParses.isEmpty());
			if (!correctParses.isEmpty()) {
				for (final IDerivation<MR> correctParse : correctParses) {
					LOG.info(
							"Correct parse lexical items:\n%s",
							lexToString(correctParse.getMaxLexicalEntries(),
									model));
					LOG.info("Correct feats: %s",
							correctParse.getAverageMaxFeatureVector());
				}
			}
		} else {
			// No parses
			LOG.info("no parses");
			
			// Update stats
			stats.recordNoParse(dataItem, dataItem.getLabel());
			
			// Potentially re-parse with word skipping
			if (skipParsingFilter.isValid(dataItem.getSample())) {
				final IParserOutput<MR> parserOutputWithSkipping = parser
						.parse(dataItem.getSample(),
								model.createDataItemModel(dataItem.getSample()),
								true);
				LOG.info("EMPTY Parsing time %f",
						parserOutputWithSkipping.getParsingTime() / 1000.0);
				final List<? extends IDerivation<MR>> bestEmptiesParses = parserOutputWithSkipping
						.getBestParses();
				
				if (bestEmptiesParses.size() == 1) {
					processSingleBestParse(dataItem, model,
							parserOutputWithSkipping, bestEmptiesParses.get(0),
							true, stats);
				} else if (bestEmptiesParses.isEmpty()) {
					// No parses
					LOG.info("no parses");
					
					stats.recordNoParseWithSkipping(dataItem,
							dataItem.getLabel());
				} else {
					// too many parses or no parses
					stats.recordParsesWithSkipping(dataItem, dataItem
							.getLabel(), ListUtils.map(bestEmptiesParses,
							new ListUtils.Mapper<IDerivation<MR>, MR>() {
								@Override
								public MR process(IDerivation<MR> obj) {
									return obj.getSemantics();
								}
							}));
					
					LOG.info("WRONG: %d parses", bestEmptiesParses.size());
					for (final IDerivation<MR> parse : bestEmptiesParses) {
						logParse(dataItem, parse, false, null, model);
					}
					// Check if we had the correct parse and it just wasn't
					// the best
					final List<? extends IDerivation<MR>> correctParses = parserOutputWithSkipping
							.getMaxParses(new IFilter<MR>() {
								
								@Override
								public boolean isValid(MR e) {
									return dataItem.getLabel().equals(e);
								}
							});
					LOG.info("Had correct parses: %s", !correctParses.isEmpty());
					if (!correctParses.isEmpty()) {
						for (final IDerivation<MR> correctParse : correctParses) {
							LOG.info(
									"Correct parse lexical items:\n%s",
									lexToString(
											correctParse.getMaxLexicalEntries(),
											model));
							LOG.info("Correct feats: %s",
									correctParse.getAverageMaxFeatureVector());
						}
					}
				}
			} else {
				LOG.info("Skipping word-skip parsing due to length");
				stats.recordNoParseWithSkipping(dataItem, dataItem.getLabel());
			}
		}
	}
	
	public static class Builder<SAMPLE extends IDataItem<?>, MR> {
		
		private final IParser<SAMPLE, MR>										parser;
		
		/** Filters which data items are valid for parsing with word skipping */
		private IFilter<SAMPLE>													skipParsingFilter	= new IFilter<SAMPLE>() {
																										
																										@Override
																										public boolean isValid(
																												SAMPLE e) {
																											return true;
																										}
																									};
		
		private final IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>>	testData;
		
		public Builder(
				IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>> testData,
				IParser<SAMPLE, MR> parser) {
			this.testData = testData;
			this.parser = parser;
		}
		
		public Tester<SAMPLE, MR> build() {
			return new Tester<SAMPLE, MR>(testData, skipParsingFilter, parser);
		}
		
		public Builder<SAMPLE, MR> setSkipParsingFilter(
				IFilter<SAMPLE> skipParsingFilter) {
			this.skipParsingFilter = skipParsingFilter;
			return this;
		}
	}
	
	public static class Creator<SAMPLE extends IDataItem<?>, MR> implements
			IResourceObjectCreator<Tester<SAMPLE, MR>> {
		
		@SuppressWarnings("unchecked")
		@Override
		public Tester<SAMPLE, MR> create(Parameters parameters,
				IResourceRepository resourceRepo) {
			
			// Get the testing set
			final IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>> testSet;
			{
				// [yoav] [17/10/2011] Store in Object to javac known bug
				final Object dataCollection = resourceRepo
						.getResource(parameters.get("data"));
				if (dataCollection == null
						|| !(dataCollection instanceof IDataCollection<?>)) {
					throw new RuntimeException(
							"Unknown or non labeled dataset: "
									+ parameters.get("data"));
				} else {
					testSet = (IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>>) dataCollection;
				}
			}
			
			if (!parameters.contains("parser")) {
				throw new IllegalStateException(
						"tester now requires you to provide a parser");
			}
			
			final Tester.Builder<SAMPLE, MR> builder = new Tester.Builder<SAMPLE, MR>(
					testSet,
					(IParser<SAMPLE, MR>) resourceRepo.getResource(parameters
							.get("parser")));
			
			if (parameters.get("skippingFilter") != null) {
				builder.setSkipParsingFilter((IFilter<SAMPLE>) resourceRepo
						.getResource(parameters.get("skippingFilter")));
			}
			
			return builder.build();
		}
		
		@Override
		public String type() {
			return "tester";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), Tester.class)
					.setDescription(
							"Model tester. Tests inference using the model on some testing data")
					.addParam("data", "id",
							"IDataCollection that holds ILabaledDataItem entries")
					.addParam("parser", "id", "Parser object")
					.addParam("skippingFilter", "id",
							"IFilter used to decide which data items to skip")
					.build();
		}
		
	}
	
}
