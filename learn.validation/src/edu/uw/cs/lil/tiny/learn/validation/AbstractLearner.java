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
package edu.uw.cs.lil.tiny.learn.validation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry.Origin;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.ILossDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.learn.OnlineLearningStats;
import edu.uw.cs.lil.tiny.learn.validation.perceptron.ValidationPerceptron;
import edu.uw.cs.lil.tiny.learn.validation.stocgrad.ValidationStocGrad;
import edu.uw.cs.lil.tiny.parser.IOutputLogger;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.test.ITester;
import edu.uw.cs.lil.tiny.test.stats.ExactMatchTestingStatistics;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Validation-based learner. See Artzi and Zettlemoyer 2013 for detailed
 * description. While the algorithm in the paper is situated, this one is not.
 * For a situated version see the package edu.uw.cs.lil.tiny.learn.situated.
 * <p>
 * Parameter update step inspired by: Natasha Singh-Miller and Michael Collins.
 * 2007. Trigger-based Language Modeling using a Loss-sensitive Perceptron
 * Algorithm. In proceedings of ICASSP 2007.
 * </p>
 * 
 * @author Yoav Artzi
 * @see ValidationPerceptron
 * @see ValidationStocGrad
 */
public abstract class AbstractLearner<SAMPLE extends IDataItem<?>, DI extends ILabeledDataItem<SAMPLE, ?>, PO extends IParserOutput<MR>, MR>
		implements ILearner<SAMPLE, DI, Model<SAMPLE, MR>> {
	public static final ILogger												LOG	= LoggerFactory
																						.create(AbstractLearner.class);
	
	private final ICategoryServices<MR>										categoryServices;
	
	/**
	 * Recycle the lexical induction parser output as the pruned one for
	 * parameter update.
	 */
	private final boolean													conflateGenlexAndPrunedParses;
	
	/**
	 * Number of training epochs.
	 */
	private final int														epochs;
	
	/**
	 * The learner is error driven, meaning: if it can parse a sentence, it will
	 * skip lexical induction.
	 */
	private final boolean													errorDriven;
	
	/**
	 * GENLEX procedure. If 'null', skip lexicon learning.
	 */
	private final ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>>	genlex;
	
	/**
	 * Parser beam size for lexical generation.
	 */
	private final int														lexiconGenerationBeamSize;
	
	private final IFilter<DI>												processingFilter;
	
	/**
	 * Tester to use after each epoch.
	 */
	private final ITester<SAMPLE, MR>										tester;
	
	/**
	 * Training data.
	 */
	private final IDataCollection<DI>										trainingData;
	
	/**
	 * Mapping of training data samples to their gold labels.
	 */
	private final Map<DI, MR>												trainingDataDebug;
	
	/**
	 * Parser output logger.
	 */
	protected final IOutputLogger<MR>										parserOutputLogger;
	
	/**
	 * Learning statistics.
	 */
	protected final OnlineLearningStats										stats;
	
	protected AbstractLearner(int numIterations,
			IDataCollection<DI> trainingData, Map<DI, MR> trainingDataDebug,
			int lexiconGenerationBeamSize,
			IOutputLogger<MR> parserOutputLogger, ITester<SAMPLE, MR> tester,
			boolean conflateGenlexAndPrunedParses, boolean errorDriven,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>> genlex,
			IFilter<DI> processingFilter) {
		this.epochs = numIterations;
		this.trainingData = trainingData;
		this.trainingDataDebug = trainingDataDebug;
		this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
		this.parserOutputLogger = parserOutputLogger;
		this.tester = tester;
		this.conflateGenlexAndPrunedParses = conflateGenlexAndPrunedParses;
		this.errorDriven = errorDriven;
		this.categoryServices = categoryServices;
		this.genlex = genlex;
		this.processingFilter = processingFilter;
		this.stats = new OnlineLearningStats(numIterations, trainingData.size());
	}
	
	@Override
	public void train(Model<SAMPLE, MR> model) {
		// Epochs
		for (int epochNumber = 0; epochNumber < epochs; ++epochNumber) {
			// Training epoch, iterate over all training samples
			LOG.info("=========================");
			LOG.info("Training epoch %d", epochNumber);
			LOG.info("=========================");
			int itemCounter = -1;
			
			// Iterating over training data
			for (final DI dataItem : trainingData) {
				// Process a single training sample
				
				// Record start time
				final long startTime = System.currentTimeMillis();
				
				// Log sample header
				LOG.info("%d : ================== [%d]", ++itemCounter,
						epochNumber);
				LOG.info("Sample type: %s", dataItem.getClass().getSimpleName());
				LOG.info("%s", dataItem);
				
				// Skip sample, if over the length limit
				if (!processingFilter.isValid(dataItem)) {
					LOG.info("Skipped training sample, due to processing filter");
					continue;
				}
				
				try {
					// Data item model
					final IDataItemModel<MR> dataItemModel = model
							.createDataItemModel(dataItem.getSample());
					
					// ///////////////////////////
					// Step I: Parse with current model. If we get a valid
					// parse, update parameters.
					// ///////////////////////////
					
					// Parse with current model and record some statistics
					final PO parserOutput = parse(dataItem, dataItemModel);
					stats.recordModelParsing(parserOutput.getParsingTime());
					parserOutputLogger.log(parserOutput, dataItemModel);
					
					final List<? extends IDerivation<MR>> modelParses = parserOutput
							.getAllParses();
					final List<? extends IDerivation<MR>> bestModelParses = parserOutput
							.getBestParses();
					
					LOG.info("Model parsing time: %.4fsec",
							parserOutput.getParsingTime() / 1000.0);
					LOG.info("Output is %s", parserOutput.isExact() ? "exact"
							: "approximate");
					LOG.info("Created %d model parses for training sample:",
							modelParses.size());
					for (final IDerivation<MR> parse : modelParses) {
						logParse(dataItem, parse,
								validate(dataItem, parse.getSemantics()), true,
								dataItemModel);
					}
					
					// Record if the best is the gold standard, if such debug
					// information is available
					if (bestModelParses.size() == 1
							&& isGoldDebugCorrect(dataItem, bestModelParses
									.get(0).getSemantics())) {
						stats.goldIsOptimal(itemCounter, epochNumber);
					}
					
					// Create a list of all valid parses
					final List<? extends IDerivation<MR>> validParses = getValidParses(
							parserOutput, dataItem);
					
					// If has a valid parse, call parameter update procedure
					// and continue
					if (!validParses.isEmpty() && errorDriven) {
						parameterUpdate(dataItem, parserOutput, parserOutput,
								model, itemCounter, epochNumber);
						continue;
					}
					
					// ///////////////////////////
					// Step II: Generate new lexical entries, prune and update
					// the model. Keep the parser output for Step III.
					// ///////////////////////////
					
					if (genlex == null) {
						// Skip the example if not doing lexicon learning
						continue;
					}
					
					final PO generationParserOutput = lexicalInduction(
							dataItem, dataItemModel, model, itemCounter,
							epochNumber);
					
					// ///////////////////////////
					// Step III: Update parameters
					// ///////////////////////////
					
					if (conflateGenlexAndPrunedParses
							&& generationParserOutput != null) {
						parameterUpdate(dataItem, parserOutput,
								generationParserOutput, model, itemCounter,
								epochNumber);
					} else {
						final PO prunedParserOutput = parse(dataItem,
								createPruningFilter(dataItem), dataItemModel);
						LOG.info("Constrained parsing time: %.4fsec",
								prunedParserOutput.getParsingTime() / 1000.0);
						parameterUpdate(dataItem, parserOutput,
								prunedParserOutput, model, itemCounter,
								epochNumber);
					}
					
				} finally {
					// Record statistics
					stats.processed(itemCounter, epochNumber);
					LOG.info("Total sample handling time: %.4fsec",
							(System.currentTimeMillis() - startTime) / 1000.0);
				}
			}
			
			// Output epoch statistics
			LOG.info("Epoch stats:");
			LOG.info(stats);
			
			// Intermediate testing with exact match statistics only
			if (tester != null) {
				LOG.info("Testing:");
				final ExactMatchTestingStatistics<SAMPLE, MR> testingStats = new ExactMatchTestingStatistics<SAMPLE, MR>();
				tester.test(model, testingStats);
				LOG.info("%s", testingStats);
			}
			
		}
	}
	
	/**
	 * Create a pruning filter for parsing for the given data item.
	 * 
	 * @param dataItem
	 * @return
	 */
	private IFilter<MR> createPruningFilter(final DI dataItem) {
		if (dataItem instanceof ILossDataItem) {
			return new IFilter<MR>() {
				
				@SuppressWarnings("unchecked")
				@Override
				public boolean isValid(MR e) {
					if (dataItem instanceof ILossDataItem) {
						return !((ILossDataItem<Sentence, MR>) dataItem)
								.prune(e);
					} else {
						return true;
					}
				}
			};
		} else {
			return new IFilter<MR>() {
				@Override
				public boolean isValid(MR e) {
					return true;
				}
			};
		}
	}
	
	private List<? extends IDerivation<MR>> getValidParses(PO parserOutput,
			final DI dataItem) {
		final List<? extends IDerivation<MR>> parses = new LinkedList<IDerivation<MR>>(
				parserOutput.getAllParses());
		
		// Use validation function to prune generation parses
		CollectionUtils.filterInPlace(parses, new IFilter<IDerivation<MR>>() {
			@Override
			public boolean isValid(IDerivation<MR> e) {
				return validate(dataItem, e.getSemantics());
			}
		});
		return parses;
	}
	
	private PO lexicalInduction(final DI dataItem,
			IDataItemModel<MR> dataItemModel, Model<SAMPLE, MR> model,
			int dataItemNumber, int epochNumber) {
		// Generate lexical entries
		final ILexicon<MR> generatedLexicon = genlex.generate(dataItem, model,
				categoryServices);
		LOG.info("Generated lexicon size = %d", generatedLexicon.size());
		
		if (generatedLexicon.size() > 0) {
			// Case generated lexical entries
			
			// Create pruning filter, if the data item fits
			final IFilter<MR> pruningFilter = createPruningFilter(dataItem);
			
			// Parse with generated lexicon
			final PO parserOutput = parse(dataItem, pruningFilter,
					dataItemModel, generatedLexicon, lexiconGenerationBeamSize);
			
			// Log lexical generation parsing time
			stats.recordGenerationParsing(parserOutput.getParsingTime());
			LOG.info("Lexicon induction parsing time: %.4fsec",
					parserOutput.getParsingTime() / 1000.0);
			LOG.info("Output is %s", parserOutput.isExact() ? "exact"
					: "approximate");
			
			// Log generation parser output
			parserOutputLogger.log(parserOutput, dataItemModel);
			
			LOG.info(
					"Created %d lexicon generation parses for training sample",
					parserOutput.getAllParses().size());
			
			// Get valid lexical generation parses
			final List<? extends IDerivation<MR>> validParses = getValidParses(
					parserOutput, dataItem);
			LOG.info("Removed %d invalid parses", parserOutput.getAllParses()
					.size() - validParses.size());
			
			// Collect max scoring valid generation parses
			final List<IDerivation<MR>> bestGenerationParses = new LinkedList<IDerivation<MR>>();
			double currentMaxModelScore = -Double.MAX_VALUE;
			for (final IDerivation<MR> parse : validParses) {
				if (parse.getScore() > currentMaxModelScore) {
					currentMaxModelScore = parse.getScore();
					bestGenerationParses.clear();
					bestGenerationParses.add(parse);
				} else if (parse.getScore() == currentMaxModelScore) {
					bestGenerationParses.add(parse);
				}
			}
			LOG.info("%d valid best parses for lexical generation:",
					bestGenerationParses.size());
			for (final IDerivation<MR> parse : bestGenerationParses) {
				logParse(dataItem, parse, true, true, dataItemModel);
				LOG.info(
						"Feature weights: %s",
						model.getTheta().printValues(
								parse.getAverageMaxFeatureVector()));
			}
			
			// Update the model's lexicon with generated lexical
			// entries from the max scoring valid generation parses
			int newLexicalEntries = 0;
			for (final IDerivation<MR> parse : bestGenerationParses) {
				for (final LexicalEntry<MR> entry : parse
						.getMaxLexicalEntries()) {
					if (model.addLexEntry(entry
							.cloneWithDifferentOrigin(Origin.LEARNED))) {
						++newLexicalEntries;
						LOG.info(
								"Added LexicalEntry to model: %s [%s]",
								entry,
								model.getTheta().printValues(
										model.computeFeatures(entry)));
					}
					// Lexical generators might link related lexical
					// entries, so if we add the original one, we
					// should also add all its linked ones
					for (final LexicalEntry<MR> linkedEntry : entry
							.getLinkedEntries()) {
						if (model.addLexEntry(linkedEntry
								.cloneWithDifferentOrigin(Origin.LEARNED))) {
							++newLexicalEntries;
							LOG.info(
									"Added (linked) LexicalEntry to model: %s [%s]",
									linkedEntry,
									model.getTheta().printValues(
											model.computeFeatures(linkedEntry)));
						}
					}
				}
			}
			// Record statistics
			stats.numNewLexicalEntries(dataItemNumber, epochNumber,
					newLexicalEntries);
			
			return parserOutput;
		} else {
			// Skip lexical induction
			LOG.info("Skipped GENLEX step. No generated lexical items.");
			return null;
		}
	}
	
	protected boolean isGoldDebugCorrect(DI dataItem, MR label) {
		if (trainingDataDebug.containsKey(dataItem)) {
			return trainingDataDebug.get(dataItem).equals(label);
		} else {
			return false;
		}
	}
	
	protected void logParse(DI dataItem, IDerivation<MR> parse, Boolean valid,
			boolean verbose, IDataItemModel<MR> dataItemModel) {
		logParse(dataItem, parse, valid, verbose, null, dataItemModel);
	}
	
	protected void logParse(DI dataItem, IDerivation<MR> parse, Boolean valid,
			boolean verbose, String tag, IDataItemModel<MR> dataItemModel) {
		final boolean isGold;
		if (isGoldDebugCorrect(dataItem, parse.getSemantics())) {
			isGold = true;
		} else {
			isGold = false;
		}
		LOG.info("%s%s[%.2f%s] %s", isGold ? "* " : "  ", tag == null ? ""
				: tag + " ", parse.getScore(), valid == null ? ""
				: (valid ? ", V" : ", X"), parse);
		if (verbose) {
			for (final LexicalEntry<MR> entry : parse.getMaxLexicalEntries()) {
				LOG.info(
						"\t[%f] %s [%s]",
						dataItemModel.score(entry),
						entry,
						dataItemModel.getTheta().printValues(
								dataItemModel.computeFeatures(entry)));
			}
			LOG.info("Rules used: %s",
					ListUtils.join(parse.getMaxRulesUsed(), ", "));
			LOG.info(dataItemModel.getTheta().printValues(
					parse.getAverageMaxFeatureVector()));
		}
	}
	
	/**
	 * Parameter update method.
	 * 
	 * @param dataItem
	 * @param realOutput
	 * @param goodOutput
	 * @param model
	 */
	protected abstract void parameterUpdate(DI dataItem, PO realOutput,
			PO goodOutput, Model<SAMPLE, MR> model, int itemCounter,
			int epochNumber);
	
	/**
	 * Unconstrained parsing method.
	 * 
	 * @param dataItem
	 * @param dataItemModel
	 * @return
	 */
	protected abstract PO parse(DI dataItem, IDataItemModel<MR> dataItemModel);
	
	/**
	 * Constrained parsing method.
	 * 
	 * @param dataItem
	 * @param pruningFilter
	 * @param dataItemModel
	 * @param generatedLexicon
	 * @param beamSize
	 * @return
	 */
	protected abstract PO parse(DI dataItem, IFilter<MR> pruningFilter,
			IDataItemModel<MR> dataItemModel);
	
	/**
	 * Constrained parsing method for lexical generation.
	 * 
	 * @param dataItem
	 * @param pruningFilter
	 * @param dataItemModel
	 * @param generatedLexicon
	 * @param beamSize
	 * @return
	 */
	protected abstract PO parse(DI dataItem, IFilter<MR> pruningFilter,
			IDataItemModel<MR> dataItemModel, ILexicon<MR> generatedLexicon,
			int beamSize);
	
	/**
	 * Validation method.
	 * 
	 * @param dataItem
	 * @param hypothesis
	 * @return
	 */
	abstract protected boolean validate(DI dataItem, MR hypothesis);
}
