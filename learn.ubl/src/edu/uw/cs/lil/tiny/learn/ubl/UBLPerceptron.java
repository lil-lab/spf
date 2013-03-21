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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataCollection;
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
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.test.Tester;
import edu.uw.cs.lil.tiny.test.stats.ExactMatchTestingStatistics;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * UBL learner using a Perceptron learning regime.
 * 
 * @author Yoav Artzi
 */
public class UBLPerceptron extends AbstractUBL {
	public static final ILogger	LOG	= LoggerFactory.create(UBLPerceptron.class);
	
	private final double		alpha0;
	private final double		c;
	private final int			epochs;
	
	public UBLPerceptron(
			Tester<Sentence, LogicalExpression> tester,
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> train,
			ICategoryServices<LogicalExpression> categoryServices,
			boolean expandLexicon, double alpha0, double c, int epochs,
			int maxSentLen, IUBLSplitter splitter,
			AbstractCKYParser<LogicalExpression> parser) {
		super(tester, train, categoryServices, expandLexicon, maxSentLen,
				splitter, parser);
		this.alpha0 = alpha0;
		this.c = c;
		this.epochs = epochs;
	}
	
	@Override
	public void train(Model<Sentence, LogicalExpression> model) {
		int stocGradientNumUpdates = 0;
		
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
				
				final IDataItemModel<LogicalExpression> dataItemModel = model
						.createDataItemModel(dataItem);
				
				if (tokens.size() > maxSentenceLength) {
					LOG.warn("Sentence too long - skipping");
					continue;
				}
				total++;
				
				// compute first half of parameter update:
				// add the features for the best correct parse
				
				final CKYParserOutput<LogicalExpression> parserTrueSemOutput = parser
						.parse(dataItem, Pruner.create(dataItem), dataItemModel);
				
				LOG.info("Constrained (given true sem) parsing time %f",
						parserTrueSemOutput.getParsingTime() / 1000.0);
				
				if (getSingleBestParseFor(dataItem.getLabel(),
						parserTrueSemOutput) == null) {
					continue;
				}
				
				if (expandLexicon) {
					splitAndMergeLexNew(dataItem,
							parserTrueSemOutput.getChart(), model);
				}
				
				final List<LexicalEntry<LogicalExpression>> lex = parserTrueSemOutput
						.getMaxLexicalEntries(dataItem.getLabel());
				
				// in factored learning, we have to add these to the model
				// so that they get in the lexicalentry features set
				model.addLexEntries(lex);
				
				LOG.info("Using:");
				LOG.info(lexToString(lex, model));
				
				// TODO [yoav] [withluke] [posttyping] Ugly. Need to fix this.
				// Not sure what exactly is going on here.
				final IHashVector goodfeats = getSingleBestParseFor(
						dataItem.getLabel(),
						(new CKYParserOutput<LogicalExpression>(
								parserTrueSemOutput.getChart(), dataItemModel,
								-1))).getAverageMaxFeatureVector();
				
				// This parse is using the new expanded lexicon. Produces
				// all possible parses, not constrained by the semantics.
				final CKYParserOutput<LogicalExpression> parserOutput = parser
						.parse(dataItem, dataItemModel);
				
				LOG.info("Unconditioned parsing time %f",
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
					final List<LexicalEntry<LogicalExpression>> lexUsed = parserOutput
							.getMaxLexicalEntries(bestOutput);
					
					// in factored learning, we have to add these to the model
					// so that they get in the lexicalentry features set
					model.addLexEntries(lexUsed);
					
					LOG.info("Using:");
					LOG.info(lexToString(lexUsed, model));
					if (lex.isEmpty()) {
						LOG.error("ERROR: empty lex");
					}
					correct++;
				} else {
					LOG.info("WRONG: ");
					for (final IParseResult<LogicalExpression> wrongOutput : bestList) {
						LOG.info(wrongOutput.getY().toString());
						final List<LexicalEntry<LogicalExpression>> lexUsed = parserOutput
								.getMaxLexicalEntries(wrongOutput.getY());
						
						// we have to add these to the model so that they get in
						// the lexicalentry feature sets
						model.addLexEntries(lexUsed);
						
						LOG.info("Using:");
						LOG.info(lexToString(lexUsed, model));
						if (lex.isEmpty()) {
							LOG.error("ERROR: empty lex");
						}
					}
					wrong++;
				}
				
				// compute second half of parameter update:
				// these are the features for the best wrong parse(s)
				final IHashVector badfeats = HashVectorFactory.create();
				for (final IParseResult<LogicalExpression> parse : bestList) {
					// TODO [yoav] BUG : need to 'add' all the lexical entries
					// to the lexicon to create features for them
					parse.getAverageMaxFeatureVector().addTimesInto(1.0,
							badfeats);
				}
				badfeats.divideBy(bestList.size());
				
				final IHashVector update = HashVectorFactory.create();
				goodfeats.addTimesInto(1.0, update);
				badfeats.addTimesInto(-1.0, update);
				
				// now do the update
				final double scale = alpha0
						/ (1.0 + c * stocGradientNumUpdates);
				update.multiplyBy(scale);
				update.dropSmallEntries();
				stocGradientNumUpdates++;
				
				LOG.info("Scale: %f", scale);
				// LOG.info("plus feats: %s", goodfeats);
				// LOG.info("minus feats: %s", badfeats);
				LOG.info("Update: %s", update);
				
				if (!update.isBad()) {
					if (!update.valuesInRange(-100, 100)) {
						LOG.warn(
								"Large update. First feats: %s, second feats: %s",
								goodfeats, badfeats);
					}
					
					// Validate the update
					if (!model.isValidWeightVector(update)) {
						throw new IllegalStateException("invalid update: "
								+ update);
					}
					
					// Do the update
					update.addTimesInto(1, model.getTheta());
				} else {
					LOG.error("Bad update: %s", update);
					LOG.error(model.getTheta().printValues(update));
				}
			} // end for each training example
			
			if (tester != null) {
				LOG.info("Testing:");
				final ExactMatchTestingStatistics<Sentence, LogicalExpression> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression>();
				tester.test(model, stats);
				LOG.info("%s", stats);
			}
			
		} // end epochs loop
		
	}
	
	private void splitAndMergeLexNew(
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Chart<LogicalExpression> chart,
			Model<Sentence, LogicalExpression> model) {
		
		// these loops go over every nonterminal in the highest scoring parse in
		// the chart
		chart.setMaxes(dataItem.getLabel());
		final int size = dataItem.getSample().getTokens().size();
		for (int begin = 0; begin < size; begin++) {
			for (int len = 0; len < size - begin; len++) {
				Cell<LogicalExpression> cell;
				final Iterator<Cell<LogicalExpression>> i = chart
						.getSpanIterator(begin, begin + len);
				while (i.hasNext()) {
					cell = i.next();
					if (cell.getIsMax()) {
						splitMergeAddToChart(cell, begin, begin + len, chart,
								dataItem, model);
					}
				}
			}
		}
		chart.recomputeInsideScore(model.createDataItemModel(dataItem));
	}
	
	private void splitMergeAddToChart(Cell<LogicalExpression> cell, int begin,
			int end, Chart<LogicalExpression> chart,
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			Model<Sentence, LogicalExpression> model) {
		// try all possible splits and take the one with the great score
		// increase
		final Category<LogicalExpression> rootCat = cell.getCategroy();
		final List<String> rootTokens = dataItem.getSample().getTokens()
				.subList(cell.getStart(), cell.getEnd() + 1);
		
		final Set<SplittingPair> allSplits = splitter.getSplits(rootCat);
		
		final AbstractCellFactory<LogicalExpression> cellFactory = chart
				.getCellFactory();
		
		for (final SplittingPair split : allSplits) {
			final Category<LogicalExpression> left = split.getLeft();
			final Category<LogicalExpression> right = split.getRight();
			
			// iterate the break point for divide the phrase (words)
			for (int sp = begin; sp < end; sp++) {
				// make parse cells for the new lexical entries
				LexicalEntry<LogicalExpression> leftLex = new LexicalEntry<LogicalExpression>(
						rootTokens.subList(0, (sp - begin) + 1), left,
						SPLITTING_LEXICAL_ORIGIN);
				LexicalEntry<LogicalExpression> rightLex = new LexicalEntry<LogicalExpression>(
						rootTokens.subList((sp - begin) + 1, rootTokens.size()),
						right, SPLITTING_LEXICAL_ORIGIN);
				
				if (model.hasLexEntry(leftLex) && model.hasLexEntry(rightLex)) {
					continue;
				}
				
				// System.out.print(leftLex + " : " + rightLex);
				
				leftLex = FactoredLexicon.factor(leftLex);
				rightLex = FactoredLexicon.factor(rightLex);
				
				// NOTE: we do not add the cell to the chart below. this is
				// because we will be doing lots of splits and evaluating how
				// much each would help on the same chart, without actually
				// adding each potential option (or rebuilding the chart each
				// time, etc)
				
				Cell<LogicalExpression> leftCell = cellFactory.create(leftLex,
						begin, sp);
				Cell<LogicalExpression> rightCell = cellFactory.create(
						rightLex, sp + 1, end);
				
				final IDataItemModel<LogicalExpression> dataItemModel = model
						.createDataItemModel(dataItem);
				
				chart.add(leftCell, dataItemModel);
				leftCell = chart.getCell(leftCell);
				if (leftCell == null) {
					continue;
				}
				chart.add(rightCell, dataItemModel);
				rightCell = chart.getCell(rightCell);
				if (rightCell == null) {
					continue;
				}
				
				// now, make the new root cell
				final Cell<LogicalExpression> r = cellFactory.create(rootCat,
						leftCell, rightCell, "splitMerge");
				chart.add(r, dataItemModel);
			}
		}
	}
	
}
