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
package edu.uw.cs.lil.tiny.learn.ubl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataCollection;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.IUBLSplitter;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.SplittingServices.SplittingPair;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.IParseResult;
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

public class UBLStocGradient extends AbstractUBL {
	private static final ILogger													LOG					= LoggerFactory
																												.create(UBLStocGradient.class);
	
	private final double															alpha0;
	private final Map<IDataItem<Sentence>, List<LexicalEntry<LogicalExpression>>>	bestLexicalEntries	= new HashMap<IDataItem<Sentence>, List<LexicalEntry<LogicalExpression>>>();
	private final double															c;
	private final boolean															conservativeSplitting;
	private final int																epochs;
	private final boolean															pruneLex;
	
	UBLStocGradient(
			Tester<Sentence, LogicalExpression> tester,
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> train,
			ICategoryServices<LogicalExpression> categoryServices,
			boolean expandLexicon, double alpha0, double c, int epochs,
			int maxSentLen, boolean pruneLex, IUBLSplitter splitter,
			AbstractCKYParser<LogicalExpression> parser,
			boolean conservativeSplitting) {
		super(tester, train, categoryServices, expandLexicon, maxSentLen,
				splitter, parser);
		this.alpha0 = alpha0;
		this.c = c;
		this.epochs = epochs;
		this.pruneLex = pruneLex;
		this.conservativeSplitting = conservativeSplitting;
	}
	
	@Override
	public void train(Model<Sentence, LogicalExpression> model) {
		int stocGradientNumUpdates = 0;
		
		final ILexicon<LogicalExpression> initialEntries = model.getLexicon()
				.copy();
		
		if (expandLexicon) {
			// Init lexicon with all sentential lexical entries. No need to do
			// it if we are not learning a lexicon by expanding it.
			model.addLexEntries(UBLServices.createSentenceLexicalEntries(
					trainData, categoryServices));
		}
		
		// for each pass over the data
		for (int epoch = 0; epoch < epochs; epoch++) {
			LOG.info("Training, iteration %d", epoch);
			
			int total = 0, correct = 0;
			int wrong = 0;
			
			// loop through the training examples
			// try to create lexical entries for each training example
			for (final ILabeledDataItem<Sentence, LogicalExpression> dataItem : trainData) {
				
				final IDataItemModel<LogicalExpression> dataItemModel = model
						.createDataItemModel(dataItem);
				
				// print running statistics and sample header
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
				
				// print the sentence and logical form
				LOG.info(dataItem.getSample().toString());
				LOG.info(dataItem.getLabel().toString());
				
				final List<String> tokens = dataItem.getSample().getTokens();
				
				if (tokens.size() > maxSentenceLength) {
					LOG.warn("Sentence too long - skipping");
					continue;
				}
				total++;
				
				if (expandLexicon) {
					// first, get all possible lexical entries from
					// a manipulation of the best parse.
					final List<Split> splits = makeLexEntriesChart(dataItem,
							model);
					
					if (!conservativeSplitting || splits.size() == 1) {
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
							// if (split.size() > 2 && split.get(2) != null) {
							// model.removeLexEntryKeepFeature(split.get(2));
							// }
						}
					} else {
						LOG.info("Skipped addings splits due to ties [%d]",
								splits.size());
						for (final Split split : splits) {
							LOG.info(split);
						}
						
					}
					
					LOG.info("Lexicon size: %d", model.getLexicon().size());
				}
				
				// This parse is using the new expanded lexicon. Produces
				// all possible parses, not constrained by the semantics.
				final CKYParserOutput<LogicalExpression> parserOutput = parser
						.parse(dataItem, dataItemModel);
				
				LOG.info("First parsing time %f",
						parserOutput.getParsingTime() / 1000.0);
				
				// Take the semantic form of the single best parse
				// final LogicalExpression best = parserOutput
				// .getBestSingleMeaningRepresentation();
				final List<IParseResult<LogicalExpression>> bestList = parserOutput
						.getBestParses();
				
				// this just collates and outputs the training
				// accuracy.
				if (bestList.size() == 1
						&& dataItem.isCorrect(bestList.get(0).getY())) {
					final LogicalExpression bestOutput = bestList.get(0).getY();
					LOG.info("CORRECT: %s", bestOutput);
					final List<LexicalEntry<LogicalExpression>> lex = parserOutput
							.getMaxLexicalEntries(bestOutput);
					LOG.info("Using:");
					LOG.info(lexToString(lex, model));
					// in factored learning, we have to add these to the model
					// so that they get in the lexicalentry features set
					model.addLexEntries(lex);
					if (lex.isEmpty()) {
						LOG.error("ERROR: empty lex");
					}
					correct++;
				} else {
					LOG.info("WRONG: ");
					for (final IParseResult<LogicalExpression> wrongOutput : bestList) {
						LOG.info(wrongOutput.getY().toString());
						final List<LexicalEntry<LogicalExpression>> lex = parserOutput
								.getMaxLexicalEntries(wrongOutput.getY());
						// in factored learning, we have to add these to the
						// model
						// so that they get in the lexicalentry features set
						model.addLexEntries(lex);
						LOG.info("Using:");
						LOG.info(lexToString(lex, model));
						if (lex.isEmpty()) {
							LOG.error("ERROR: empty lex");
						}
					}
					wrong++;
				}
				
				// Get the parse chart to calculate the norm for the update
				final Chart<LogicalExpression> firstChart = parserOutput
						.getChart();
				
				// compute first half of parameter update:
				// subtract the expectation of parameters
				// under the distribution that is conditioned
				// on the sentence alone.
				final double norm = firstChart.computeNorm();
				final IHashVector update = HashVectorFactory.create();
				IHashVector firstfeats = null;
				if (norm != 0.0) {
					firstfeats = firstChart.computeExpFeatVals(dataItemModel);
					firstfeats.divideBy(norm);
					firstfeats.dropSmallEntries();
					LOG.info("Negative update: %s", firstfeats);
					firstfeats.addTimesInto(-1.0, update);
				}
				
				// compute second half of parameter update:
				// add the expectation of parameters
				// under the distribution that is conditioned
				// on the sentence and correct logical form.
				
				// This parse is using the new expanded lexicon. Produces
				// all possible parses, not constrained by the semantics.
				final CKYParserOutput<LogicalExpression> parserTrueSemOutput = parser
						.parse(dataItem, Pruner.create(dataItem), dataItemModel);
				
				LOG.info("Second (given true sem) parsing time %f",
						parserTrueSemOutput.getParsingTime() / 1000.0);
				
				if (getSingleBestParseFor(dataItem.getLabel(),
						parserTrueSemOutput) == null) {
					continue;
				}
				
				final Chart<LogicalExpression> secondChart = parserTrueSemOutput
						.getChart();
				final double secnorm = secondChart.computeNorm(dataItem
						.getLabel());
				IHashVector secondfeats = null;
				if (norm != 0.0) {
					secondfeats = secondChart
							.computeExpFeatVals(
									categoryServices.getSentenceCategory()
											.cloneWithNewSemantics(
													dataItem.getLabel()),
									dataItemModel);
					secondfeats.divideBy(secnorm);
					
					secondfeats.dropSmallEntries();
					secondfeats.addTimesInto(1.0, update);
					LOG.info("Plus features: %s", secondfeats);
					final List<LexicalEntry<LogicalExpression>> lex = parserTrueSemOutput
							.getMaxLexicalEntries(dataItem.getLabel());
					// in factored learning, we have to add these to the model
					// so that they get in the lexicalentry features set
					
					// TODO [yoav] [askluke] We are only adding the best lexical
					// entries, but update towards *all* trees. This might
					// create an issue with some lexical entries not in the
					// model. Even worse, it is actually responsible for some
					// weird updates.
					
					model.addLexEntries(lex);
					bestLexicalEntries.put(dataItem, lex);
					LOG.info("Best LexEntries:");
					LOG.info(lexToString(lex, model));
					if (lex.size() == 0) {
						LOG.error("ERROR: empty lex");
					}
				} else {
					continue;
				}
				
				// now do the update
				final double scale = alpha0
						/ (1.0 + c * stocGradientNumUpdates);
				update.multiplyBy(scale);
				update.dropSmallEntries();
				stocGradientNumUpdates++;
				
				LOG.info("Scale: %f", scale);
				if (update.size() == 0) {
					LOG.info("No update");
				} else {
					LOG.info("Update: %s", update);
				}
				
				if (!update.isBad()) {
					if (!update.valuesInRange(-100, 100)) {
						LOG.warn(
								"Large update. First feats: %s, second feats: %s",
								firstfeats, secondfeats);
					}
					
					// Validate the update
					if (!model.isValidWeightVector(update)) {
						throw new IllegalStateException("invalid update: "
								+ update);
					}
					
					// Do the update
					update.addTimesInto(1, model.getTheta());
				} else {
					LOG.error("Bad update: %s -- norm: %f.4f -- feats: %s",
							update, norm, null);
					LOG.error(model.getTheta().printValues(update));
				}
			} // end for each training example
			
			// we can prune the lexical items that were not used
			// in a max scoring parse.
			if (pruneLex) {
				final ILexicon<LogicalExpression> toKeep = initialEntries
						.copy();
				for (final List<LexicalEntry<LogicalExpression>> entries : bestLexicalEntries
						.values()) {
					toKeep.addAll(entries);
				}
				model.getLexicon().retainAll(toKeep);
			}
			
			if (tester != null) {
				LOG.info("Testing:");
				final ExactMatchTestingStatistics<Sentence, LogicalExpression> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression>();
				tester.test(model, stats);
				LOG.info("%s", stats);
			}
		} // end epochs loop
		
		// Log best lexical entries and their scores
		LOG.info("Lexical entries used in top parses:");
		final Set<LexicalEntry<LogicalExpression>> topEntries = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final List<LexicalEntry<LogicalExpression>> entries : bestLexicalEntries
				.values()) {
			topEntries.addAll(entries);
		}
		for (final LexicalEntry<LogicalExpression> entry : topEntries) {
			LOG.info("%s [%f]", entry, model.score(entry));
		}
		
		// XXX [Luke] DEBUG try pruning the lexicon at the end
		final ILexicon<LogicalExpression> toKeep = initialEntries.copy();
		for (final List<LexicalEntry<LogicalExpression>> entries : bestLexicalEntries
				.values()) {
			toKeep.addAll(entries);
		}
		model.getLexicon().retainAll(toKeep);
	}
	
	/**
	 * This function returns a set of lexical entries from Split and Merge
	 * operations on the maximum scoring correct parse.
	 * 
	 * @param model
	 */
	private List<Split> makeLexEntriesChart(
			final ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Model<Sentence, LogicalExpression> model) {
		final CKYParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem, Pruner.create(dataItem),
				model.createDataItemModel(dataItem));
		
		final IParseResult<LogicalExpression> correctParse = getSingleBestParseFor(
				dataItem.getLabel(), parserOutput);
		
		// if there is no possible parse, create a new lexical item for the
		// whole sentence
		// (like during initialization) and add it to the lexicon. this should
		// only happen
		// when you are pruning the lexicon.
		if (correctParse == null) {
			final LexicalEntry<LogicalExpression> newLex = new LexicalEntry<LogicalExpression>(
					dataItem.getSample().getTokens(), categoryServices
							.getSentenceCategory().cloneWithNewSemantics(
									dataItem.getLabel()),
					SPLITTING_LEXICAL_ORIGIN);
			model.addLexEntry(FactoredLexicon.factor(newLex));
			return Collections.emptyList();
		}
		
		LOG.info("MakeLex parsing time %f",
				parserOutput.getParsingTime() / 1000.0);
		return splitAndMergeLex(dataItem, parserOutput.getChart(), model);
	}
	
	private List<Split> splitAndMergeLex(
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Chart<LogicalExpression> chart,
			Model<Sentence, LogicalExpression> model) {
		final List<Split> result = new LinkedList<Split>();
		
		// these loops go over every nonterminal in the highest scoring parse in
		// the chart
		chart.setMaxes(dataItem.getLabel());
		double mostImproved = 0.0;
		final int size = dataItem.getSample().getTokens().size();
		for (int begin = 0; begin < size; begin++) {
			for (int len = 0; len < size - begin; len++) {
				Cell<LogicalExpression> cell;
				final Iterator<Cell<LogicalExpression>> i = chart
						.getSpanIterator(begin, begin + len);
				while (i.hasNext()) {
					cell = i.next();
					if (cell.getIsMax()) {
						mostImproved = splitMerge(cell, begin, begin + len,
								result, chart, dataItem, model, mostImproved);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * For a given cell, create all possible splits and take the ones with the
	 * maximum score increase.
	 * 
	 * @param cell
	 * @param begin
	 * @param end
	 * @param maxEntries
	 * @param chart
	 * @param dataItem
	 * @param model
	 * @param mostImproved
	 * @return
	 */
	private double splitMerge(Cell<LogicalExpression> cell, int begin, int end,
			List<Split> maxEntries, Chart<LogicalExpression> chart,
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Model<Sentence, LogicalExpression> model, double mostImproved) {
		final Category<LogicalExpression> rootCategory = cell.getCategroy();
		final List<String> rootTokens = dataItem.getSample().getTokens()
				.subList(cell.getStart(), cell.getEnd() + 1);
		double maxImprove = mostImproved;
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
				LOG.debug("Split under consideration:\n%s", splitCell);
				
				// If the score is higher, retain this split. Adding to the
				// chart will be done later.
				if (improvement >= maxImprove) {
					if (improvement > maxImprove) {
						maxEntries.clear();
						maxImprove = improvement;
					}
					// Save the original entry as well, so we can remove it from
					// the lexicon later
					maxEntries.add(splitCell);
				}
			}
		}
		return maxImprove;
	}
	
	public static class Builder {
		
		/**
		 * These are used to define the temperature of parameter updates. temp =
		 * alpha_0/(1+c*tot_number_of_training_instances)
		 */
		private double																			alpha0					= 0.1;
		
		/**
		 * These are used to define the temperature of parameter updates. temp =
		 * alpha_0/(1+c*tot_number_of_training_instances)
		 */
		private double																			c						= 0.0001;
		
		private final ICategoryServices<LogicalExpression>										categoryServices;
		
		private boolean																			conservativeSplitting	= false;
		
		/**
		 * Number of training epochs (rounds).
		 */
		private int																				epochs					= 10;
		
		/**
		 * Expand the lexicon during learning using higher order unification.
		 */
		private boolean																			expandLexicon			= true;
		
		/**
		 * Maximum sentence length to process. Skip any sentence that is longer.
		 */
		private int																				maxSentLen				= 50;
		
		private final AbstractCKYParser<LogicalExpression>										parser;
		
		/** Prune the lexicon at the end of each training epoch? */
		private boolean																			pruneLex				= false;
		
		private final IUBLSplitter																splitter;
		
		private Tester<Sentence, LogicalExpression>												tester;
		
		private final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>	trainData;
		
		public Builder(
				IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> trainData,
				ICategoryServices<LogicalExpression> categoryServices,
				AbstractCKYParser<LogicalExpression> parser,
				IUBLSplitter splitter) {
			this.categoryServices = categoryServices;
			this.trainData = trainData;
			this.parser = parser;
			this.splitter = splitter;
		}
		
		public UBLStocGradient build() {
			return new UBLStocGradient(tester, trainData, categoryServices,
					expandLexicon, alpha0, c, epochs, maxSentLen, pruneLex,
					splitter, parser, conservativeSplitting);
		}
		
		public Builder setAlpha0(double alpha0) {
			this.alpha0 = alpha0;
			return this;
		}
		
		public Builder setC(double c) {
			this.c = c;
			return this;
		}
		
		public Builder setConservativeSplitting(boolean conservativeSplitting) {
			this.conservativeSplitting = conservativeSplitting;
			return this;
		}
		
		public Builder setEpochs(int epochs) {
			this.epochs = epochs;
			return this;
		}
		
		public Builder setExpandLexicon(boolean expandLexicon) {
			this.expandLexicon = expandLexicon;
			return this;
		}
		
		public Builder setMaxSentLen(int maxSentLen) {
			this.maxSentLen = maxSentLen;
			return this;
		}
		
		public Builder setPruneLex(boolean pruneLex) {
			this.pruneLex = pruneLex;
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
