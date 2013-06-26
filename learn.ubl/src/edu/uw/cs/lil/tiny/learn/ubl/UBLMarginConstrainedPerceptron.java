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
package edu.uw.cs.lil.tiny.learn.ubl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.IUBLSplitter;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.SplittingServices.SplittingPair;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.lil.tiny.parser.Pruner;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.FactoredLexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.test.Tester;
import edu.uw.cs.lil.tiny.test.stats.ExactMatchTestingStatistics;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class UBLMarginConstrainedPerceptron extends AbstractUBL {
	private static final ILogger	LOG	= LoggerFactory
												.create(UBLMarginConstrainedPerceptron.class);
	private final boolean			conservativeSplitting;
	private final int				epochs;
	private final double			margin;
	private final boolean			pruneLexicon;
	private final Character[][]		sampleParsingStats;
	
	private UBLMarginConstrainedPerceptron(
			Tester<Sentence, LogicalExpression> tester,
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> trainingData,
			ICategoryServices<LogicalExpression> categoryServices,
			boolean expandLexicon, int maxSentLen, IUBLSplitter splitter,
			AbstractCKYParser<LogicalExpression> parser, int epochs,
			boolean conservativeSplitting, double margin, boolean pruneLexicon) {
		super(tester, trainingData, categoryServices, expandLexicon,
				maxSentLen, splitter, parser);
		this.epochs = epochs;
		this.conservativeSplitting = conservativeSplitting;
		this.margin = margin;
		this.pruneLexicon = pruneLexicon;
		this.sampleParsingStats = new Character[trainingData.size()][epochs];
	}
	
	@Override
	public void train(Model<Sentence, LogicalExpression> model) {
		
		// Log the training set
		LOG.info("==============================");
		LOG.info("=== Training sentences [%d]:", trainData.size());
		LOG.info("==============================");
		for (final IDataItem<Sentence> dataItem : trainData) {
			LOG.info("%s", dataItem.getSample());
		}
		LOG.info("==============================");
		
		final ILexicon<LogicalExpression> initialEntries = model.getLexicon()
				.copy();
		final Map<IDataItem<Sentence>, Iterable<LexicalEntry<LogicalExpression>>> bestLexicalEntries = new HashMap<IDataItem<Sentence>, Iterable<LexicalEntry<LogicalExpression>>>();
		
		if (expandLexicon) {
			// Init lexicon with all sentential lexical entries. No need to do
			// it if we are not learning a lexicon by expanding it.
			model.addLexEntries(UBLServices.createSentenceLexicalEntries(
					trainData, categoryServices));
		}
		
		// Iterations over the data
		for (int epoch = 0; epoch < epochs; ++epoch) {
			LOG.info("Training, iteration %d", epoch);
			
			int total = 0;
			int correct = 0;
			int wrong = 0;
			int sampleCounter = -1;
			
			// Iterate over training examples
			for (final ILabeledDataItem<Sentence, LogicalExpression> dataItem : trainData) {
				sampleCounter++;
				
				// Log statistics and current sample
				if (total != 0) {
					final double recall = (double) correct / total;
					final double precision = (double) correct
							/ (correct + wrong);
					LOG.info(
							"%d : =============== r: %.2f p: %.2f (epoch: %d)",
							total, recall, precision, epoch);
				} else {
					LOG.info("%d : ===============", total);
				}
				LOG.info(dataItem);
				
				final List<String> tokens = dataItem.getSample().getTokens();
				
				if (tokens.size() > maxSentenceLength) {
					LOG.warn("Sentence too long - skipping");
					continue;
				}
				
				final IDataItemModel<LogicalExpression> dataItemModel = model
						.createDataItemModel(dataItem);
				
				// Increase number of processed sentences
				total++;
				
				if (expandLexicon) {
					// Get all best scoring splits
					final List<Split> splits = getTopSplits(dataItem, model);
					
					if (!conservativeSplitting || splits.size() == 1) {
						LOG.info("Adding lexical entries from %d splits",
								splits.size());
						for (final Split split : splits) {
							LOG.info("Split: %s", split);
							if (!split.reusingRight
									&& split.right.hasLexicalMaxStep()) {
								model.addLexEntries(split.right
										.getViterbiLexicalEntries());
							}
							if (!split.reusingLeft
									&& split.left.hasLexicalMaxStep()) {
								model.addLexEntries(split.left
										.getViterbiLexicalEntries());
							}
						}
						LOG.info("Lexicon size: %d", model.getLexicon().size());
					} else {
						LOG.info("Skipped addings splits due to ties [%d]",
								splits.size());
						for (final Split split : splits) {
							LOG.info(split);
						}
					}
				}
				
				// First parse: constrained by the sentence and semantics
				final CKYParserOutput<LogicalExpression> constrainedParseOutput = parser
						.parse(dataItem, Pruner.create(dataItem), dataItemModel);
				LOG.info("Constrained parse time: %f",
						constrainedParseOutput.getParsingTime() / 1000.0);
				final IParse<LogicalExpression> correctParse = getSingleBestParseFor(
						dataItem.getLabel(), constrainedParseOutput);
				if (correctParse == null) {
					LOG.info("Failed to find a correct parse. Update skipped.");
					continue;
				}
				
				// Second parse: constrained by the sentence only
				final CKYParserOutput<LogicalExpression> unconstrainedParseOutput = parser
						.parse(dataItem, dataItemModel);
				LOG.info("Unconstrained parse time: %f",
						unconstrainedParseOutput.getParsingTime() / 1000.0);
				
				// Log current parses
				if (unconstrainedParseOutput.getBestParses().size() == 1
						&& unconstrainedParseOutput.getBestParses().get(0)
								.getSemantics().equals(dataItem.getLabel())) {
					LOG.info("CORRECT");
					LOG.info("score: %f", unconstrainedParseOutput
							.getBestParses().get(0).getScore());
					sampleParsingStats[sampleCounter][epoch] = 'C';
					++correct;
				} else {
					LOG.info("WRONG");
					if (unconstrainedParseOutput.getBestParses().isEmpty()) {
						LOG.info("no parses");
					} else {
						LOG.info("score: %f", unconstrainedParseOutput
								.getBestParses().get(0).getScore());
						sampleParsingStats[sampleCounter][epoch] = 'W';
						++wrong;
						if (unconstrainedParseOutput.getBestParses().size() == 1) {
							LOG.info("Best parse: %s", unconstrainedParseOutput
									.getBestParses().get(0).getSemantics());
							LOG.info("Best parse lexical entries:");
							LOG.info(lexToString(unconstrainedParseOutput
									.getBestParses().get(0)
									.getMaxLexicalEntries(), model));
						}
					}
				}
				LOG.info("Correct parse lexical entries:");
				final LinkedHashSet<LexicalEntry<LogicalExpression>> correctMaxLexicalEntries = correctParse
						.getMaxLexicalEntries();
				LOG.info(lexToString(correctMaxLexicalEntries, model));
				
				// Keep the best correct lexical items
				// TODO
				bestLexicalEntries.put(dataItem, correctMaxLexicalEntries);
				
				// Prepare the update
				
				// Get the score of the best correct parse.
				final double bestCorrectParseScore = correctParse.getScore();
				
				LOG.info("Best correct parse score: %f", bestCorrectParseScore);
				
				// Collect all incorrect parses which violate the margin.
				// Meaning: the score of their best parse is violating the
				// margin in respect to the best correct parse.
				final List<IParse<LogicalExpression>> violaltingParses = new LinkedList<IParse<LogicalExpression>>();
				boolean correctParseFoundInUnconstrained = false;
				for (final IParse<LogicalExpression> parse : unconstrainedParseOutput
						.getAllParses()) {
					if (parse.getSemantics().equals(dataItem.getLabel())) {
						correctParseFoundInUnconstrained = true;
					} else if (parse.getScore() + margin >= bestCorrectParseScore) {
						LOG.info("Violating parse with score %f",
								parse.getScore());
						violaltingParses.add(parse);
					}
					
				}
				
				LOG.info("%d margin violating parses", violaltingParses.size());
				
				// Only do an update if we have violating parses or if the
				// correct parse is not the unconstrained parser output
				if (!violaltingParses.isEmpty()
						|| !correctParseFoundInUnconstrained) {
					final IHashVector update = HashVectorFactory.create();
					
					// Make sure the lexical entries that will be used in the
					// positive update, are in the model
					model.addLexEntries(correctMaxLexicalEntries);
					// Positive update: use the first parse to get the best
					// tree that gives the correct logical form. To make sure,
					// add all its lexical entries to the model. The positive
					// update is the feature vector of the best parse.
					final IHashVector positiveUpdate = correctParse
							.getAverageMaxFeatureVector();
					LOG.info("Positive update: %s", positiveUpdate);
					positiveUpdate.addTimesInto(1.0, update);
					
					// Negative update: Weight the feature vectors
					// collected earlier and sum into a single vector.
					final IHashVector negativeUpdate = HashVectorFactory
							.create();
					for (final IParse<LogicalExpression> parse : violaltingParses) {
						// Make sure all the lexical entries are in the model
						model.addLexEntries(parse.getMaxLexicalEntries());
						
						parse.getAverageMaxFeatureVector().addTimesInto(
								1.0 / violaltingParses.size(), negativeUpdate);
					}
					LOG.info("Negative update: %s", negativeUpdate);
					negativeUpdate.addTimesInto(-1.0, update);
					
					// Do the update
					update.dropSmallEntries();
					
					// Validate the update
					if (!model.isValidWeightVector(update)) {
						throw new IllegalStateException("invalid update: "
								+ update);
					}
					
					LOG.info("Update: %s", update);
					update.addTimesInto(1.0, model.getTheta());
				}
			}
			final double recall = (double) correct / total;
			final double precision = (double) correct / (correct + wrong);
			LOG.info("Epoch %d completed. r: %.2f p: %.2f ", epoch, recall,
					precision);
			LOG.info("Parsing stats:\n%s", parsingStatsToString());
			
			if (tester != null) {
				LOG.info("Testing:");
				final ExactMatchTestingStatistics<Sentence, LogicalExpression> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression>();
				tester.test(model, stats);
				LOG.info("%s", stats);
			}
		}
		
		if (pruneLexicon) {
			LOG.info("Pre-pruning lexicon size: %d", model.getLexicon().size());
			final ILexicon<LogicalExpression> toKeep = initialEntries.copy();
			for (final Iterable<LexicalEntry<LogicalExpression>> entries : bestLexicalEntries
					.values()) {
				for (final LexicalEntry<LogicalExpression> entry : entries) {
					toKeep.add(entry);
				}
			}
			model.getLexicon().retainAll(toKeep);
			LOG.info("Lexicon size after pruning: %d", model.getLexicon()
					.size());
		}
	}
	
	/**
	 * For a given cell, collect highest scoring splits.
	 * 
	 * @param cell
	 * @param begin
	 * @param end
	 * @param currentTopSplits
	 * @param chart
	 * @param dataItem
	 * @param model
	 * @param currentBestImprovement
	 * @return
	 */
	private double collectTopSplits(Cell<LogicalExpression> cell, int begin,
			int end, List<Split> currentTopSplits,
			Chart<LogicalExpression> chart,
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Model<Sentence, LogicalExpression> model,
			double currentBestImprovement) {
		final Category<LogicalExpression> rootCategory = cell.getCategroy();
		final List<String> rootTokens = dataItem.getSample().getTokens()
				.subList(cell.getStart(), cell.getEnd() + 1);
		double maxImprove = currentBestImprovement;
		final AbstractCellFactory<LogicalExpression> cellFactory = chart
				.getCellFactory();
		
		// Get all splits for the root category
		final Set<SplittingPair> allSplits = splitter.getSplits(rootCategory);
		
		// Iterate over all possible splits
		for (final SplittingPair split : allSplits) {
			final Category<LogicalExpression> left = split.getLeft();
			final Category<LogicalExpression> right = split.getRight();
			
			// Iterate over all breaking points for dividing the phrase (tokens)
			for (int splittingPoint = begin; splittingPoint < end; splittingPoint++) {
				// Create parse cells for the new lexical entries
				// TODO Do we really need to factor them here?
				final LexicalEntry<LogicalExpression> leftEntry = FactoredLexicon
						.factor(new LexicalEntry<LogicalExpression>(rootTokens
								.subList(0, (splittingPoint - begin) + 1),
								left, SPLITTING_LEXICAL_ORIGIN));
				final LexicalEntry<LogicalExpression> rightEntry = FactoredLexicon
						.factor(new LexicalEntry<LogicalExpression>(rootTokens
								.subList((splittingPoint - begin) + 1,
										rootTokens.size()), right,
								SPLITTING_LEXICAL_ORIGIN));
				
				// If both created lexical entries exist in the model, skip this
				// split
				if (model.hasLexEntry(leftEntry)
						&& model.hasLexEntry(rightEntry)) {
					continue;
				}
				
				// NOTE: we do not add the cell to the chart below. this is
				// because we will be doing lots of splits and evaluating how
				// much each would help on the same chart, without actually
				// adding each potential option (or rebuilding the chart each
				// time, etc)
				
				// Create cells for the splits
				final Cell<LogicalExpression> newLeftCell = cellFactory.create(
						leftEntry, begin, splittingPoint);
				final Cell<LogicalExpression> newRightCell = cellFactory
						.create(rightEntry, splittingPoint + 1, end);
				
				// If equivalent cells exist in the chart and they have a higher
				// max score
				final Cell<LogicalExpression> leftCell;
				final boolean reusingLeft;
				final Cell<LogicalExpression> chartLeftCell = chart
						.getCell(newLeftCell);
				if (chartLeftCell == null
						|| chartLeftCell.getViterbiScore() >= newLeftCell
								.getViterbiScore()) {
					leftCell = newLeftCell;
					reusingLeft = false;
				} else {
					leftCell = chartLeftCell;
					reusingLeft = true;
				}
				
				final Cell<LogicalExpression> rightCell;
				final boolean reusingRight;
				final Cell<LogicalExpression> chartRightCell = chart
						.getCell(newRightCell);
				if (chartRightCell == null
						|| chartRightCell.getViterbiScore() >= newRightCell
								.getViterbiScore()) {
					rightCell = newRightCell;
					reusingRight = false;
				} else {
					rightCell = chartRightCell;
					reusingRight = true;
				}
				
				// Only consider this split if it adds a new lexical entry
				if ((reusingLeft || model.hasLexEntry(leftEntry))
						&& (reusingRight || model.hasLexEntry(rightEntry))) {
					continue;
				}
				
				// Create the new root cell
				final Cell<LogicalExpression> newRootCell = cellFactory.create(
						rootCategory, leftCell, rightCell, "splitMerge");
				
				final double improvement = newRootCell.getViterbiScore()
						- cell.getViterbiScore();
				final Split splitCell = new Split(leftCell, reusingLeft,
						rightCell, reusingRight, cell, improvement);
				LOG.dev("Split under consideration:\n%s", splitCell);
				
				// If the score is higher, retain this split. Adding to the
				// chart will be done later.
				if (improvement >= maxImprove) {
					if (improvement > maxImprove) {
						currentTopSplits.clear();
						maxImprove = improvement;
					}
					// Save the original entry as well, so we can remove it from
					// the lexicon later
					currentTopSplits.add(splitCell);
				}
			}
		}
		return maxImprove;
	}
	
	private List<Split> getTopSplits(
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Chart<LogicalExpression> chart,
			Model<Sentence, LogicalExpression> model) {
		final List<Split> topSplits = new LinkedList<Split>();
		final int size = dataItem.getSample().getTokens().size();
		
		// these loops go over every nonterminal in the highest scoring parse in
		// the chart
		
		// Set the cells participating in the highest scoring parse as maxes
		chart.setMaxes(dataItem.getLabel());
		
		// Iterate over all non-terminals in the highest scoring parse
		double bestImprovement = 0.0;
		for (int begin = 0; begin < size; begin++) {
			for (int len = 0; len < size - begin; len++) {
				final Iterator<Cell<LogicalExpression>> i = chart
						.getSpanIterator(begin, begin + len);
				while (i.hasNext()) {
					final Cell<LogicalExpression> cell = i.next();
					if (cell.getIsMax()) {
						bestImprovement = collectTopSplits(cell, begin, begin
								+ len, topSplits, chart, dataItem, model,
								bestImprovement);
					}
				}
			}
		}
		return topSplits;
	}
	
	private List<Split> getTopSplits(
			final ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Model<Sentence, LogicalExpression> model) {
		final CKYParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem, Pruner.create(dataItem),
				model.createDataItemModel(dataItem));
		
		LOG.info("getTopSplits parsing time %f",
				parserOutput.getParsingTime() / 1000.0);
		
		final IParse<LogicalExpression> correctParse = getSingleBestParseFor(
				dataItem.getLabel(), parserOutput);
		
		// If there's no correct parse, create a new sentential lexical item.
		// This can only happen when we prune the lexicon or remove
		// lexical entries once split.
		if (correctParse == null) {
			final LexicalEntry<LogicalExpression> newLex = new LexicalEntry<LogicalExpression>(
					dataItem.getSample().getTokens(), categoryServices
							.getSentenceCategory().cloneWithNewSemantics(
									dataItem.getLabel()),
					SPLITTING_LEXICAL_ORIGIN);
			model.addLexEntry(FactoredLexicon.factor(newLex));
			return Collections.emptyList();
		}
		
		return getTopSplits(dataItem, parserOutput.getChart(), model);
	}
	
	private String parsingStatsToString() {
		final StringBuilder sb = new StringBuilder();
		int sampleCounter = 0;
		for (final Character[] sampleStats : sampleParsingStats) {
			sb.append(String.format("%3d ", sampleCounter));
			for (final Character stat : sampleStats) {
				sb.append(stat == null ? ' ' : stat);
			}
			sb.append('\n');
			++sampleCounter;
		}
		return sb.toString();
	}
	
	public static class Builder {
		
		private final ICategoryServices<LogicalExpression>										categoryServices;
		private boolean																			conservativeSplitting	= false;
		private final int																		epochs;
		private boolean																			expandLexicon			= true;
		private final double																	margin;
		
		private int																				maxSentenceLength		= Integer.MAX_VALUE;
		
		private final AbstractCKYParser<LogicalExpression>										parser;
		private boolean																			pruneLexicon			= false;
		private final IUBLSplitter																splitter;
		private Tester<Sentence, LogicalExpression>												tester					= null;
		private final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>	trainingData;
		
		public Builder(
				ICategoryServices<LogicalExpression> categoryServices,
				IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> trainingData,
				double margin, int epochs, IUBLSplitter splitter,
				AbstractCKYParser<LogicalExpression> parser) {
			this.categoryServices = categoryServices;
			this.trainingData = trainingData;
			this.margin = margin;
			this.epochs = epochs;
			this.splitter = splitter;
			this.parser = parser;
		}
		
		public UBLMarginConstrainedPerceptron build() {
			return new UBLMarginConstrainedPerceptron(tester, trainingData,
					categoryServices, expandLexicon, maxSentenceLength,
					splitter, parser, epochs, conservativeSplitting, margin,
					pruneLexicon);
		}
		
		public Builder setConservativeSplitting(boolean conservativeSplitting) {
			this.conservativeSplitting = conservativeSplitting;
			return this;
		}
		
		public Builder setExpandLexicon(boolean expandLexicon) {
			this.expandLexicon = expandLexicon;
			return this;
		}
		
		public Builder setMaxSentenceLength(int maxSentenceLength) {
			this.maxSentenceLength = maxSentenceLength;
			return this;
		}
		
		public Builder setPruneLexicon(boolean pruneLexicon) {
			this.pruneLexicon = pruneLexicon;
			return this;
		}
		
		public Builder setTester(Tester<Sentence, LogicalExpression> tester) {
			this.tester = tester;
			return this;
		}
		
	}
	
	private static class Split {
		private final Cell<LogicalExpression>	left;
		private final Cell<LogicalExpression>	original;
		private final boolean					reusingLeft;
		private final boolean					reusingRight;
		private final Cell<LogicalExpression>	right;
		private final double					scoreImprovement;
		
		public Split(Cell<LogicalExpression> left, boolean reusingLeft,
				Cell<LogicalExpression> right, boolean reusingRight,
				Cell<LogicalExpression> original, double scoreImprovement) {
			this.left = left;
			this.reusingLeft = reusingLeft;
			this.reusingRight = reusingRight;
			this.original = original;
			this.right = right;
			this.scoreImprovement = scoreImprovement;
		}
		
		private static String cellToString(Cell<LogicalExpression> cell) {
			return new StringBuilder().append("[").append(cell.getStart())
					.append(", ").append(cell.getEnd()).append("] ")
					.append(cell.hasLexicalMaxStep() ? " (LEX) " : "")
					.append(cell.getCategroy()).toString();
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("[").append(
					scoreImprovement).append("] ");
			sb.append(cellToString(original)).append('\n');
			
			sb.append("\tL: ");
			if (reusingLeft) {
				sb.append("[reusing] ");
			}
			sb.append(cellToString(left)).append('\n');
			
			sb.append("\tR: ");
			if (reusingRight) {
				sb.append("[reusing] ");
			}
			sb.append(cellToString(right));
			
			return sb.toString();
		}
		
	}
	
}
