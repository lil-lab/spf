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
package edu.uw.cs.lil.tiny.learn.situated;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry.Origin;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.learn.OnlineLearningStats;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.IJointDerivation;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.IJointParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointModelImmutable;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Situated validation-based learner. See Artzi and Zettlemoyer 2013 for
 * detailed description.
 * <p>
 * Parameter update step inspired by: Natasha Singh-Miller and Michael Collins.
 * 2007. Trigger-based Language Modeling using a Loss-sensitive Perceptron
 * Algorithm. In proceedings of ICASSP 2007.
 * </p>
 * 
 * @author Yoav Artzi
 * @param <STATE>
 *            Type of initial state.
 * @param <MR>
 *            Meaning representation type.
 * @param <ESTEP>
 *            Type of execution step.
 * @param <ERESULT>
 *            Type of execution result.
 * @param <DI>
 *            Data item used for learning.
 */
public abstract class AbstractSituatedLearner<SAMPLE extends ISituatedDataItem<Sentence, ?>, MR, ESTEP, ERESULT, DI extends ILabeledDataItem<SAMPLE, ?>>
		implements ILearner<SAMPLE, DI, JointModel<SAMPLE, MR, ESTEP>> {
	public static final ILogger															LOG	= LoggerFactory
																									.create(AbstractSituatedLearner.class);
	private final ICategoryServices<MR>													categoryServices;
	
	/**
	 * Number of training epochs.
	 */
	private final int																	epochs;
	
	/**
	 * GENLEX procedure. If 'null' skip lexical induction.
	 */
	private final ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>>	genlex;
	
	/**
	 * Parser beam size for lexical generation.
	 */
	private final int																	lexiconGenerationBeamSize;
	
	/**
	 * Max sentence length to process. If longer, skip.
	 */
	private final int																	maxSentenceLength;
	
	/**
	 * Training data.
	 */
	private final IDataCollection<DI>													trainingData;
	
	/**
	 * Mapping of training data samples to their gold labels.
	 */
	private final Map<DI, Pair<MR, ERESULT>>											trainingDataDebug;
	
	/**
	 * Joint parser for inference.
	 */
	protected final IJointParser<SAMPLE, MR, ESTEP, ERESULT>							parser;
	/**
	 * Parser output logger.
	 */
	protected final IJointOutputLogger<MR, ESTEP, ERESULT>								parserOutputLogger;
	/**
	 * Learning statistics.
	 */
	protected final OnlineLearningStats													stats;
	
	protected AbstractSituatedLearner(
			int numIterations,
			IDataCollection<DI> trainingData,
			Map<DI, Pair<MR, ERESULT>> trainingDataDebug,
			int maxSentenceLength,
			int lexiconGenerationBeamSize,
			IJointParser<SAMPLE, MR, ESTEP, ERESULT> parser,
			IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>> genlex) {
		this.epochs = numIterations;
		this.trainingData = trainingData;
		this.trainingDataDebug = trainingDataDebug;
		this.maxSentenceLength = maxSentenceLength;
		this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
		this.parser = parser;
		this.parserOutputLogger = parserOutputLogger;
		this.categoryServices = categoryServices;
		this.genlex = genlex;
		this.stats = new OnlineLearningStats(numIterations, trainingData.size());
	}
	
	@Override
	public void train(JointModel<SAMPLE, MR, ESTEP> model) {
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
				if (dataItem.getSample().getSample().getTokens().size() > maxSentenceLength) {
					LOG.warn("Training sample too long, skipping");
					continue;
				}
				
				// Sample data item model
				final IJointDataItemModel<MR, ESTEP> dataItemModel = model
						.createJointDataItemModel(dataItem.getSample());
				
				// ///////////////////////////
				// Step I: Generate a large number of potential lexical entries,
				// parse to prune them and update the lexicon.
				// ///////////////////////////
				
				if (genlex != null) {
					lexicalInduction(dataItem, dataItemModel, model,
							itemCounter, epochNumber);
				}
				
				// ///////////////////////////
				// Step II: Update model parameters.
				// ///////////////////////////
				parameterUpdate(dataItem, dataItemModel, model, itemCounter,
						epochNumber);
				
				// Record statistics
				stats.processed(itemCounter, epochNumber);
				LOG.info("Total sample handling time: %.4fsec",
						(System.currentTimeMillis() - startTime) / 1000.0);
			}
			
			// Output epoch statistics
			LOG.info("Epoch stats:");
			LOG.info(stats);
		}
	}
	
	private void lexicalInduction(final DI dataItem,
			IJointDataItemModel<MR, ESTEP> dataItemModel,
			JointModel<SAMPLE, MR, ESTEP> model, int dataItemNumber,
			int epochNumber) {
		// Generate lexical entries
		final ILexicon<MR> generatedLexicon = genlex.generate(dataItem, model,
				categoryServices);
		LOG.info("Generated lexicon size = %d", generatedLexicon.size());
		
		if (generatedLexicon.size() > 0) {
			// Case generated lexical entries
			
			// Parse with generated lexicon
			final IJointOutput<MR, ERESULT> generateLexiconParserOutput = parser
					.parse(dataItem.getSample(), dataItemModel, false,
							generatedLexicon, lexiconGenerationBeamSize);
			
			// Log lexical generation parsing time
			stats.recordGenerationParsing(generateLexiconParserOutput
					.getInferenceTime());
			LOG.info("Lexicon induction parsing time: %.4fsec",
					generateLexiconParserOutput.getInferenceTime() / 1000.0);
			LOG.info("Output is %s",
					generateLexiconParserOutput.isExact() ? "exact"
							: "approximate");
			
			// Log generation parser output
			parserOutputLogger.log(generateLexiconParserOutput, dataItemModel);
			
			// Get lexical generation parses
			final List<? extends IJointDerivation<MR, ERESULT>> generationParses = new LinkedList<IJointDerivation<MR, ERESULT>>(
					generateLexiconParserOutput.getDerivations());
			LOG.info(
					"Created %d lexicon generation parses for training sample",
					generationParses.size());
			
			// Use validation function to prune generation parses
			CollectionUtils.filterInPlace(generationParses,
					new IFilter<IJointDerivation<MR, ERESULT>>() {
						@Override
						public boolean isValid(IJointDerivation<MR, ERESULT> e) {
							return validate(dataItem, e.getResult());
						}
					});
			LOG.info("Removed %d invalid parses", generateLexiconParserOutput
					.getDerivations().size() - generationParses.size());
			
			// Collect max scoring valid generation parses
			final List<IJointDerivation<MR, ERESULT>> bestGenerationParses = new LinkedList<IJointDerivation<MR, ERESULT>>();
			double currentMaxModelScore = -Double.MAX_VALUE;
			for (final IJointDerivation<MR, ERESULT> parse : generationParses) {
				if (parse.getViterbiScore() > currentMaxModelScore) {
					currentMaxModelScore = parse.getViterbiScore();
					bestGenerationParses.clear();
					bestGenerationParses.add(parse);
				} else if (parse.getViterbiScore() == currentMaxModelScore) {
					bestGenerationParses.add(parse);
				}
			}
			LOG.info("%d valid best parses for lexical generation:",
					bestGenerationParses.size());
			for (final IJointDerivation<MR, ERESULT> parse : bestGenerationParses) {
				logParse(dataItem, parse, true, true, dataItemModel);
				LOG.info("Feature weights: %s",
						model.getTheta()
								.printValues(parse.getMeanMaxFeatures()));
			}
			
			// Update the model's lexicon with generated lexical
			// entries from the max scoring valid generation parses
			int newLexicalEntries = 0;
			for (final IJointDerivation<MR, ERESULT> parse : bestGenerationParses) {
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
		} else {
			// Skip lexical induction
			LOG.info("Skipped GENLEX step. No generated lexical items.");
		}
	}
	
	protected boolean isGoldDebugCorrect(DI dataItem, ERESULT label) {
		if (trainingDataDebug.containsKey(dataItem)) {
			return trainingDataDebug.get(dataItem).equals(label);
		} else {
			return false;
		}
	}
	
	protected void logParse(DI dataItem, IJointDerivation<MR, ERESULT> parse,
			Boolean valid, boolean verbose, IDataItemModel<MR> dataItemModel) {
		logParse(dataItem, parse, valid, verbose, null, dataItemModel);
	}
	
	protected void logParse(DI dataItem, IJointDerivation<MR, ERESULT> parse,
			Boolean valid, boolean verbose, String tag,
			IDataItemModel<MR> dataItemModel) {
		final boolean isGold;
		if (isGoldDebugCorrect(dataItem, parse.getResult())) {
			isGold = true;
		} else {
			isGold = false;
		}
		LOG.info("%s%s[%.2f%s] %s", isGold ? "* " : "  ", tag == null ? ""
				: tag + " ", parse.getViterbiScore(), valid == null ? ""
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
					ListUtils.join(parse.getMaxParsingRules(), ", "));
			LOG.info(dataItemModel.getTheta().printValues(
					parse.getMeanMaxFeatures()));
		}
	}
	
	/**
	 * Parameter update method.
	 */
	protected abstract void parameterUpdate(DI dataItem,
			IJointDataItemModel<MR, ESTEP> dataItemModel,
			JointModel<SAMPLE, MR, ESTEP> model, int itemCounter,
			int epochNumber);
	
	abstract protected boolean validate(DI dataItem, ERESULT hypothesis);
}
