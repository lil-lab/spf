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
package edu.uw.cs.lil.tiny.test;

import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.IDataCollection;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.parser.IParseResult;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.test.stats.ITestingStatistics;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class Tester<X, Y> implements ITester<X, Y> {
	private static final ILogger									LOG	= LoggerFactory
																				.create(Tester.class
																						.getName());
	
	private final IParser<X, Y>										parser;
	
	private final IFilter<ILabeledDataItem<X, Y>>					skipParsingFilter;
	
	private final IDataCollection<? extends ILabeledDataItem<X, Y>>	testData;
	
	private Tester(IDataCollection<? extends ILabeledDataItem<X, Y>> testData,
			IFilter<ILabeledDataItem<X, Y>> skipParsingFilter,
			IParser<X, Y> parser) {
		this.testData = testData;
		this.skipParsingFilter = skipParsingFilter;
		this.parser = parser;
		LOG.info("Init Tester:  size(testData)=%d", testData.size());
	}
	
	@Override
	public void test(IModelImmutable<X, Y> model, ITestingStatistics<X, Y> stats) {
		test(testData, model, stats);
	}
	
	private String lexToString(Iterable<LexicalEntry<Y>> lexicalEntries,
			IModelImmutable<X, Y> model) {
		final StringBuilder ret = new StringBuilder();
		ret.append("[LexEntries and scores:\n");
		for (final LexicalEntry<Y> entry : lexicalEntries) {
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
	
	private void logParse(ILabeledDataItem<X, Y> dataItem,
			IParseResult<Y> parse, boolean logLexicalItems, String tag,
			IModelImmutable<X, Y> model) {
		LOG.info("%s%s[S%.2f] %s",
				dataItem.getLabel().equals(parse.getY()) ? "* " : "  ",
				tag == null ? "" : tag + " ", parse.getScore(), parse);
		LOG.info("Calculated score: %f", parse.getAverageMaxFeatureVector()
				.vectorMultiply(model.getTheta()));
		LOG.info("Features: %s",
				model.getTheta()
						.printValues(parse.getAverageMaxFeatureVector()));
		if (logLexicalItems) {
			for (final LexicalEntry<Y> entry : parse.getMaxLexicalEntries()) {
				LOG.info("\t[%f] %s", model.score(entry), entry);
			}
		}
	}
	
	private void processSingleBestParse(ILabeledDataItem<X, Y> dataItem,
			IModelImmutable<X, Y> model,
			final IParserOutput<Y> modelParserOutput,
			final IParseResult<Y> parse, boolean withWordSkipping,
			ITestingStatistics<X, Y> stats) {
		final Set<LexicalEntry<Y>> lexicalEntries = parse
				.getMaxLexicalEntries();
		final Y label = parse.getY();
		
		// Update statistics
		if (withWordSkipping) {
			stats.recordParseWithSkipping(dataItem, dataItem.getLabel(),
					parse.getY());
		} else {
			stats.recordParse(dataItem, dataItem.getLabel(), parse.getY());
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
			final List<IParseResult<Y>> correctParses = modelParserOutput
					.getMaxParses(dataItem.getLabel());
			LOG.info("Had correct parses: %s", !correctParses.isEmpty());
			if (!correctParses.isEmpty()) {
				for (final IParseResult<Y> correctParse : correctParses) {
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
					diff.dropSmallEntries();
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
			IDataCollection<? extends ILabeledDataItem<X, Y>> dataset,
			IModelImmutable<X, Y> model, ITestingStatistics<X, Y> stats) {
		int itemCounter = 0;
		for (final ILabeledDataItem<X, Y> item : dataset) {
			++itemCounter;
			test(itemCounter, item, model, stats);
		}
	}
	
	private void test(int itemCounter, ILabeledDataItem<X, Y> dataItem,
			IModelImmutable<X, Y> model, ITestingStatistics<X, Y> stats) {
		LOG.info("%d : ==================", itemCounter);
		LOG.info("%s", dataItem);
		
		// Try a simple model parse
		final IParserOutput<Y> modelParserOutput = parser.parse(dataItem,
				model.createDataItemModel(dataItem));
		LOG.info("Test parsing time %.2f",
				modelParserOutput.getParsingTime() / 1000.0);
		
		final List<IParseResult<Y>> bestModelParses = modelParserOutput
				.getBestParses();
		if (bestModelParses.size() == 1) {
			// Case we have a single parse
			processSingleBestParse(dataItem, model, modelParserOutput,
					bestModelParses.get(0), false, stats);
		} else if (bestModelParses.size() > 1) {
			// Multiple top parses
			
			// Update statistics
			stats.recordParses(dataItem, dataItem.getLabel(), ListUtils.map(
					bestModelParses,
					new ListUtils.Mapper<IParseResult<Y>, Y>() {
						@Override
						public Y process(IParseResult<Y> obj) {
							return obj.getY();
						}
					}));
			
			// There are more than one equally high scoring
			// logical forms. If this is the case, we abstain
			// from returning a result.
			LOG.info("too many parses");
			LOG.info("%d parses:", bestModelParses.size());
			for (final IParseResult<Y> parse : bestModelParses) {
				logParse(dataItem, parse, false, null, model);
			}
			// Check if we had the correct parse and it just wasn't the best
			final List<IParseResult<Y>> correctParses = modelParserOutput
					.getMaxParses(dataItem.getLabel());
			LOG.info("Had correct parses: %s", !correctParses.isEmpty());
			if (!correctParses.isEmpty()) {
				for (final IParseResult<Y> correctParse : correctParses) {
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
			if (skipParsingFilter.isValid(dataItem)) {
				final IParserOutput<Y> parserOutputWithSkipping = parser.parse(
						dataItem, model.createDataItemModel(dataItem), true);
				LOG.info("EMPTY Parsing time %f",
						parserOutputWithSkipping.getParsingTime() / 1000.0);
				final List<IParseResult<Y>> bestEmptiesParses = parserOutputWithSkipping
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
							new ListUtils.Mapper<IParseResult<Y>, Y>() {
								@Override
								public Y process(IParseResult<Y> obj) {
									return obj.getY();
								}
							}));
					
					LOG.info("WRONG: %d parses", bestEmptiesParses.size());
					for (final IParseResult<Y> parse : bestEmptiesParses) {
						logParse(dataItem, parse, false, null, model);
					}
					// Check if we had the correct parse and it just wasn't
					// the best
					final List<IParseResult<Y>> correctParses = parserOutputWithSkipping
							.getMaxParses(dataItem.getLabel());
					LOG.info("Had correct parses: %s", !correctParses.isEmpty());
					if (!correctParses.isEmpty()) {
						for (final IParseResult<Y> correctParse : correctParses) {
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
	
	public static class Builder<X, Y> {
		
		private final IParser<X, Y>										parser;
		
		/** Filters which data items are valid for parsing with word skipping */
		private IFilter<ILabeledDataItem<X, Y>>							skipParsingFilter	= new IFilter<ILabeledDataItem<X, Y>>() {
																								
																								@Override
																								public boolean isValid(
																										ILabeledDataItem<X, Y> e) {
																									return true;
																								}
																							};
		
		private final IDataCollection<? extends ILabeledDataItem<X, Y>>	testData;
		
		public Builder(
				IDataCollection<? extends ILabeledDataItem<X, Y>> testData,
				IParser<X, Y> parser) {
			this.testData = testData;
			this.parser = parser;
		}
		
		public Tester<X, Y> build() {
			return new Tester<X, Y>(testData, skipParsingFilter, parser);
		}
		
		public IFilter<ILabeledDataItem<X, Y>> getSkipParsingFilter() {
			return skipParsingFilter;
		}
		
		public Builder<X, Y> setSkipParsingFilter(
				IFilter<ILabeledDataItem<X, Y>> skipParsingFilter) {
			this.skipParsingFilter = skipParsingFilter;
			return this;
		}
	}
	
}
