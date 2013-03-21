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
package edu.uw.cs.lil.tiny.learn.weakp.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.data.IDataCollection;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.lexicalgen.ILexicalGenerationDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.learn.weakp.WeaklySupervisedPerceptronStats;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry.Origin;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.IJointParse;
import edu.uw.cs.lil.tiny.parser.joint.IJointParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Trigger-sensitive perceptron.
 * <p>
 * Parameter update step inspired by: Natasha Singh-Miller and Michael Collins.
 * 2007. Trigger-based Language Modeling using a Loss-sensitive Perceptron
 * Algorithm. In proceedings of ICASSP 2007.
 * </p>
 * 
 * @author Yoav Artzi
 * @param <LF>
 */
public class JointValidationSensitivePerceptron<STATE, LF, ESTEP, ERESULT>
		implements
		ILearner<Sentence, LF, JointModel<Sentence, STATE, LF, ESTEP>> {
	private static final ILogger																	LOG	= LoggerFactory
																												.create(JointValidationSensitivePerceptron.class);
	/**
	 * Consider all valid parses for violations and update or only the highest
	 * scoring valid ones.
	 */
	private final boolean																			hardUpdates;
	/**
	 * Generator for lexical entries from evidence.
	 */
	private final int																				lexiconGenerationBeamSize;
	private final boolean																			lexiconLearning;
	private final double																			margin;
	private final int																				maxSentenceLength;
	private final int																				numIterations;
	private final IJointParser<Sentence, STATE, LF, ESTEP, ERESULT>									parser;
	private final IJointOutputLogger<LF, ESTEP, ERESULT>											parserOutputLogger;
	private final WeaklySupervisedPerceptronStats													stats;
	private final IDataCollection<? extends ILexicalGenerationDataItem<Pair<Sentence, STATE>, LF>>	trainingData;
	private final Map<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>>							trainingDataDebug;
	private final IValidator<Pair<Sentence, STATE>, Pair<LF, ERESULT>>								validator;
	
	private JointValidationSensitivePerceptron(
			int numIterations,
			double margin,
			IDataCollection<? extends ILexicalGenerationDataItem<Pair<Sentence, STATE>, LF>> trainingData,
			Map<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>> trainingDataDebug,
			int maxSentenceLength, int lexiconGenerationBeamSize,
			IJointParser<Sentence, STATE, LF, ESTEP, ERESULT> parser,
			boolean hardUpdates,
			IValidator<Pair<Sentence, STATE>, Pair<LF, ERESULT>> validator,
			boolean lexiconLearning,
			IJointOutputLogger<LF, ESTEP, ERESULT> parserOutputLogger) {
		this.numIterations = numIterations;
		this.margin = margin;
		this.trainingData = trainingData;
		this.trainingDataDebug = trainingDataDebug;
		this.maxSentenceLength = maxSentenceLength;
		this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
		this.parser = parser;
		this.hardUpdates = hardUpdates;
		this.validator = validator;
		this.lexiconLearning = lexiconLearning;
		this.parserOutputLogger = parserOutputLogger;
		this.stats = new WeaklySupervisedPerceptronStats(numIterations,
				trainingData.size());
		LOG.info(
				"Init JointValidationSensitivePerceptron: numIterations=%d, margin=%f, trainingData.size()=%d, trainingDataDebug.size()=%d, maxSentenceLength=%d ...",
				numIterations, margin, trainingData.size(),
				trainingDataDebug.size(), maxSentenceLength);
		LOG.info(
				"Init JointValidationSensitivePerceptron: ... lexiconGenerationBeamSize=%d",
				lexiconGenerationBeamSize);
		LOG.info("Init JointValidationSensitivePerceptron: ... validator=%s",
				validator);
	}
	
	public void train(JointModel<Sentence, STATE, LF, ESTEP> model) {
		// Epochs iteration
		for (int iterationNumber = 0; iterationNumber < numIterations; ++iterationNumber) {
			// Training iteration, go over all training samples
			LOG.info("=========================");
			LOG.info("Training iteration %d", iterationNumber);
			LOG.info("=========================");
			int itemCounter = -1;
			
			// Iterating over training data
			for (final ILexicalGenerationDataItem<Pair<Sentence, STATE>, LF> dataItem : trainingData) {
				// Process a specific training sample
				
				final long startTime = System.currentTimeMillis();
				
				LOG.info("%d : ================== [%d]", ++itemCounter,
						iterationNumber);
				LOG.info("Sample type: %s", dataItem.getClass().getSimpleName());
				LOG.info("%s", dataItem);
				
				if (dataItem.getSample().first().getTokens().size() > maxSentenceLength) {
					LOG.warn("Training sample too long, skipping");
					continue;
				}
				
				final IJointDataItemModel<LF, ESTEP> dataItemModel = model
						.createJointDataItemModel(dataItem);
				
				if (lexiconLearning) {
					// Step I: parse with a generated lexicon and add the best
					// parses to the lexicon.
					
					// Generate lexical entries
					final long genStartTime = System.currentTimeMillis();
					final ILexicon<LF> generatedLexicon = dataItem
							.generateLexicon();
					LOG.info("Generated lexicon size = %d",
							generatedLexicon.size());
					
					if (generatedLexicon.size() > 0) {
						
						final IJointOutput<LF, ERESULT> generateLexiconParserOutput = parser
								.parse(dataItem, dataItemModel, false,
										generatedLexicon,
										lexiconGenerationBeamSize);
						
						final long genTime = System.currentTimeMillis()
								- genStartTime;
						
						parserOutputLogger.log(generateLexiconParserOutput,
								dataItemModel);
						
						stats.recordGenerationParsing(genTime);
						final List<? extends IJointParse<LF, ERESULT>> generationParses = new LinkedList<IJointParse<LF, ERESULT>>(
								generateLexiconParserOutput.getAllJointParses());
						
						LOG.info("Lexicon generation parsing time: %.4fsec",
								genTime / 1000.0);
						LOG.info(
								"Created %d lexicon generation parses for training sample",
								generationParses.size());
						
						// Prune invalid generation parser
						CollectionUtils.filterInPlace(generationParses,
								new IFilter<IJointParse<LF, ERESULT>>() {
									@Override
									public boolean isValid(
											IJointParse<LF, ERESULT> e) {
										return validator.isValid(dataItem,
												e.getResult());
									}
								});
						
						LOG.info("Removed %d invalid parses",
								generateLexiconParserOutput.getAllJointParses()
										.size() - generationParses.size());
						
						// Collect best generation parses and their scores
						final List<IJointParse<LF, ERESULT>> bestGenerationParses = new LinkedList<IJointParse<LF, ERESULT>>();
						double currentMaxModelScore = -Double.MAX_VALUE;
						LOG.info("Generation parses:");
						for (final IJointParse<LF, ERESULT> parse : generationParses) {
							if (parse.getScore() > currentMaxModelScore) {
								currentMaxModelScore = parse.getScore();
								bestGenerationParses.clear();
								bestGenerationParses.add(parse);
							} else if (parse.getScore() == currentMaxModelScore) {
								bestGenerationParses.add(parse);
							}
						}
						
						// Log lexicon generation parses
						LOG.info(
								"%d valid best parses for lexical generation:",
								bestGenerationParses.size());
						for (final IJointParse<LF, ERESULT> parse : bestGenerationParses) {
							logParse(dataItem, parse, true, true, dataItemModel);
							LOG.info(
									"Feature weights: %s",
									model.getTheta().printValues(
											parse.getAverageMaxFeatureVector()));
						}
						
						// Add the lexical items that were the best during
						// lexical generation and were valid
						int newLexicalEntries = 0;
						for (final IJointParse<LF, ERESULT> parse : bestGenerationParses) {
							for (final LexicalEntry<LF> entry : parse
									.getMaxLexicalEntries()) {
								if (model
										.addLexEntry(entry
												.cloneWithDifferentOrigin(Origin.LEARNED))) {
									++newLexicalEntries;
									LOG.info(
											"Added LexicalEntry to model: %s [%s]",
											entry,
											model.getTheta()
													.printValues(
															model.computeFeatures(entry)));
								}
								// Add the linked entries
								for (final LexicalEntry<LF> linkedEntry : entry
										.getLinkedEntries()) {
									if (model
											.addLexEntry(linkedEntry
													.cloneWithDifferentOrigin(Origin.LEARNED))) {
										++newLexicalEntries;
										LOG.info(
												"Added (linked) LexicalEntry to model: %s [%s]",
												linkedEntry,
												model.getTheta()
														.printValues(
																model.computeFeatures(linkedEntry)));
									}
								}
							}
						}
						stats.numNewLexicalEntries(itemCounter,
								iterationNumber, newLexicalEntries);
					} else {
						LOG.info("Skipped GENLEX step. No generated lexical items.");
					}
				}
				
				// Step II: using the model with current lexicon. Create
				// valid/invalid sets and update on violations.
				final IJointOutput<LF, ERESULT> modelParserOutput = parser
						.parse(dataItem, dataItemModel);
				stats.recordModelParsing(modelParserOutput.getParsingTime());
				
				parserOutputLogger.log(modelParserOutput, dataItemModel);
				
				final List<? extends IJointParse<LF, ERESULT>> modelParses = modelParserOutput
						.getAllJointParses();
				
				LOG.info("Created %d model parses for training sample",
						modelParses.size());
				LOG.info("Model parsing time: %.4fsec",
						modelParserOutput.getParsingTime() / 1000.0);
				
				// Record if the best is the gold standard, if known
				final List<? extends IJointParse<LF, ERESULT>> bestModelParses = modelParserOutput
						.getBestJointParses();
				if (bestModelParses.size() == 1
						&& isGoldDebugCorrect(dataItem, bestModelParses.get(0)
								.getResult())) {
					stats.goldIsOptimal(itemCounter, iterationNumber);
				}
				
				if (modelParses.isEmpty()) {
					LOG.warn("No model parses for: %s", dataItem);
					continue;
				}
				
				// Iterate over all parses (including these that failed to
				// execute) and 'add' their lexical entries to the model. Must
				// do this to make sure they appear in the feature vectors to
				// compute both the margin and the update.
				for (final IJointParse<LF, ERESULT> parse : modelParses) {
					model.addLexEntries(parse.getMaxLexicalEntries());
				}
				
				// Create the good and bad sets
				final Pair<List<IJointParse<LF, ERESULT>>, List<IJointParse<LF, ERESULT>>> validInvalidSetsPair = createValidInvalidSets(
						dataItem, modelParses);
				final List<IJointParse<LF, ERESULT>> validParses = validInvalidSetsPair
						.first();
				final List<IJointParse<LF, ERESULT>> invalidParses = validInvalidSetsPair
						.second();
				
				LOG.info("%d valid parses, %d invalid parses",
						validParses.size(), invalidParses.size());
				
				if (!validParses.isEmpty()) {
					stats.hasValidParse(itemCounter, iterationNumber);
				}
				
				LOG.info("Valid parses:");
				for (final IJointParse<LF, ERESULT> parse : validParses) {
					logParse(dataItem, parse, true, true, dataItemModel);
				}
				
				if (validParses.isEmpty() || invalidParses.isEmpty()) {
					LOG.info("No valid/invalid parses -- skipping");
					continue;
				}
				
				// Violating sets
				final List<IJointParse<LF, ERESULT>> violatingValidParses = new LinkedList<IJointParse<LF, ERESULT>>();
				final List<IJointParse<LF, ERESULT>> violatingInvalidParses = new LinkedList<IJointParse<LF, ERESULT>>();
				
				// These flags are used to mark that we inserted a parse into
				// the violating sets, so no need to check for its violation
				// against others
				final boolean[] validParsesFlags = new boolean[validParses
						.size()];
				final boolean[] invalidParsesFlags = new boolean[invalidParses
						.size()];
				int validParsesCounter = 0;
				for (final IJointParse<LF, ERESULT> validParse : validParses) {
					int invalidParsesCounter = 0;
					for (final IJointParse<LF, ERESULT> invalidParse : invalidParses) {
						if (!validParsesFlags[validParsesCounter]
								|| !invalidParsesFlags[invalidParsesCounter]) {
							// Create the delta vector if needed, we do it only
							// once. This is why we check if we are going to
							// need it in the above 'if'.
							final IHashVector featureDelta = validParse
									.getAverageMaxFeatureVector()
									.addTimes(
											-1.0,
											invalidParse
													.getAverageMaxFeatureVector());
							final double deltaScore = featureDelta
									.vectorMultiply(model.getTheta());
							
							// Test valid parse for insertion into violating
							// valid parses
							if (!validParsesFlags[validParsesCounter]) {
								// Case this valid sample is still not in the
								// violating set
								if (deltaScore < margin * featureDelta.l1Norm()) {
									// Case of violation
									// Add to the violating set
									violatingValidParses.add(validParse);
									// Mark flag, so we won't test it again
									validParsesFlags[validParsesCounter] = true;
								}
							}
							
							// Test invalid parse for insertion into
							// violating invalid parses
							if (!invalidParsesFlags[invalidParsesCounter]) {
								// Case this invalid sample is still not in
								// the violating set
								if (deltaScore < margin * featureDelta.l1Norm()) {
									// Case of violation
									// Add to the violating set
									violatingInvalidParses.add(invalidParse);
									// Mark flag, so we won't test it again
									invalidParsesFlags[invalidParsesCounter] = true;
								}
							}
						}
						
						// Increase the counter, as we move to the next sample
						++invalidParsesCounter;
					}
					// Increase the counter, as we move to the next sample
					++validParsesCounter;
				}
				
				LOG.info(
						"%d violating valid parses, %d violating invalid parses",
						violatingValidParses.size(),
						violatingInvalidParses.size());
				
				if (violatingValidParses.isEmpty()) {
					LOG.info("There are no violating valid/invalid parses -- skipping");
					continue;
				}
				
				LOG.info("Violating valid parses: ");
				for (final IJointParse<LF, ERESULT> pair : violatingValidParses) {
					logParse(dataItem, pair, true, true, dataItemModel);
				}
				
				LOG.info("Violating invalid parses: ");
				for (final IJointParse<LF, ERESULT> parse : violatingInvalidParses) {
					logParse(dataItem, parse, false, true, dataItemModel);
				}
				
				// Create the parameter update
				final IHashVector update = HashVectorFactory.create();
				
				// Get the update for valid violating samples
				for (final IJointParse<LF, ERESULT> parse : violatingValidParses) {
					parse.getAverageMaxFeatureVector().addTimesInto(
							1.0 / violatingValidParses.size(), update);
				}
				
				// Get the update for the invalid violating samples
				for (final IJointParse<LF, ERESULT> parse : violatingInvalidParses) {
					parse.getAverageMaxFeatureVector().addTimesInto(
							-1.0 * (1.0 / violatingInvalidParses.size()),
							update);
				}
				
				// Prune small entries from the update
				update.dropSmallEntries();
				
				// Validate the update
				if (!model.isValidWeightVector(update)) {
					throw new IllegalStateException("invalid update: " + update);
				}
				
				// Update the parameters vector
				LOG.info("Update: %s", update);
				update.addTimesInto(1.0, model.getTheta());
				stats.triggeredUpdate(itemCounter, iterationNumber);
				
				stats.processed(itemCounter, iterationNumber);
				LOG.info("Total sample handling time: %.4fsec",
						(System.currentTimeMillis() - startTime) / 1000.0);
			}
			
			LOG.info("Iteration stats:");
			LOG.info(stats);
		}
	}
	
	/**
	 * Split the list parses to valid and invalid ones.
	 * 
	 * @param dataItem
	 * @param parseResults
	 * @return Pair of (good parses, bad parses)
	 */
	private Pair<List<IJointParse<LF, ERESULT>>, List<IJointParse<LF, ERESULT>>> createValidInvalidSets(
			IDataItem<Pair<Sentence, STATE>> dataItem,
			Collection<? extends IJointParse<LF, ERESULT>> parses) {
		final List<IJointParse<LF, ERESULT>> validParses = new LinkedList<IJointParse<LF, ERESULT>>();
		final List<IJointParse<LF, ERESULT>> invalidParses = new LinkedList<IJointParse<LF, ERESULT>>();
		double validScore = -Double.MAX_VALUE;
		for (final IJointParse<LF, ERESULT> parse : parses) {
			if (validator.isValid(dataItem, parse.getResult())) {
				if (hardUpdates) {
					// Case using hard updates, only keep the highest scored
					// valid ones
					if (parse.getScore() > validScore) {
						validScore = parse.getScore();
						validParses.clear();
						validParses.add(parse);
					} else if (parse.getScore() == validScore) {
						validParses.add(parse);
					}
				} else {
					validParses.add(parse);
				}
			} else {
				invalidParses.add(parse);
			}
		}
		return Pair.of(validParses, invalidParses);
	}
	
	private boolean isGoldDebugCorrect(
			IDataItem<Pair<Sentence, STATE>> dataItem, Pair<LF, ERESULT> label) {
		if (trainingDataDebug.containsKey(dataItem)) {
			return trainingDataDebug.get(dataItem).equals(label);
		} else {
			return false;
		}
	}
	
	private void logParse(IDataItem<Pair<Sentence, STATE>> dataItem,
			IJointParse<LF, ERESULT> parse, Boolean valid, boolean verbose,
			IDataItemModel<LF> dataItemModel) {
		logParse(dataItem, parse, valid, verbose, null, dataItemModel);
	}
	
	private void logParse(IDataItem<Pair<Sentence, STATE>> dataItem,
			IJointParse<LF, ERESULT> parse, Boolean valid, boolean verbose,
			String tag, IDataItemModel<LF> dataItemModel) {
		final boolean isGold;
		if (isGoldDebugCorrect(dataItem, parse.getResult())) {
			isGold = true;
		} else {
			isGold = false;
		}
		LOG.info("%s%s[%.2f%s] %s", isGold ? "* " : "  ", tag == null ? ""
				: tag + " ", parse.getScore(), valid == null ? ""
				: (valid ? ", V" : ", X"), parse);
		if (verbose) {
			for (final LexicalEntry<LF> entry : parse.getMaxLexicalEntries()) {
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
	 * Builder for {@link JointValidationSensitivePerceptron}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Builder<STATE, LF, ESTEP, ERESULT> {
		
		/**
		 * Use hard updates. Meaning: consider only highest-scored valid parses
		 * for parameter updates, instead of all valid parses.
		 */
		private boolean																					hardUpdates					= false;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																						lexiconGenerationBeamSize	= 20;
		
		/**
		 * Learn a lexicon.
		 */
		private boolean																					lexiconLearning				= true;
		
		/** Margin to scale the relative loss function */
		private double																					margin						= 1.0;
		
		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private int																						maxSentenceLength			= Integer.MAX_VALUE;
		
		/** Number of training iterations */
		private int																						numIterations				= 4;
		private final IJointParser<Sentence, STATE, LF, ESTEP, ERESULT>									parser;
		
		private IJointOutputLogger<LF, ESTEP, ERESULT>													parserOutputLogger			= new IJointOutputLogger<LF, ESTEP, ERESULT>() {
																																		
																																		public void log(
																																				IJointOutput<LF, ERESULT> output,
																																				IJointDataItemModel<LF, ESTEP> dataItemModel) {
																																			// Stub
																																			
																																		}
																																	};
		
		/** Training data */
		private final IDataCollection<? extends ILexicalGenerationDataItem<Pair<Sentence, STATE>, LF>>	trainingData;
		
		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>>								trainingDataDebug			= new HashMap<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>>();
		
		private final IValidator<Pair<Sentence, STATE>, Pair<LF, ERESULT>>								validator;
		
		public Builder(
				IDataCollection<? extends ILexicalGenerationDataItem<Pair<Sentence, STATE>, LF>> trainingData,
				IJointParser<Sentence, STATE, LF, ESTEP, ERESULT> parser,
				IValidator<Pair<Sentence, STATE>, Pair<LF, ERESULT>> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}
		
		public JointValidationSensitivePerceptron<STATE, LF, ESTEP, ERESULT> build() {
			return new JointValidationSensitivePerceptron<STATE, LF, ESTEP, ERESULT>(
					numIterations, margin, trainingData, trainingDataDebug,
					maxSentenceLength, lexiconGenerationBeamSize, parser,
					hardUpdates, validator, lexiconLearning, parserOutputLogger);
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setHardUpdates(
				boolean hardUpdates) {
			this.hardUpdates = hardUpdates;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setLexiconLearning(
				boolean lexiconLearning) {
			this.lexiconLearning = lexiconLearning;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setMargin(double margin) {
			this.margin = margin;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setMaxSentenceLength(
				int maxSentenceLength) {
			this.maxSentenceLength = maxSentenceLength;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setNumTrainingIterations(
				int numTrainingIterations) {
			this.numIterations = numTrainingIterations;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setParserOutputLogger(
				IJointOutputLogger<LF, ESTEP, ERESULT> parserOutputLogger) {
			this.parserOutputLogger = parserOutputLogger;
			return this;
		}
		
		public Builder<STATE, LF, ESTEP, ERESULT> setTrainingDataDebug(
				Map<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>> trainingDataDebug) {
			this.trainingDataDebug = trainingDataDebug;
			return this;
		}
	}
	
	/**
	 * A relative loss function as given in the paper with the notation
	 * \delta_i. It's always non-negative and scores all the "good" parse
	 * results with zero, while all the rest have a score that is bigger than
	 * one.
	 * 
	 * @author Yoav Artzi
	 * @param <Y>
	 *            Parser output representation.
	 */
	public static class RelativeLossFunction {
		final double	minLoss;
		
		public RelativeLossFunction(double minLoss) {
			this.minLoss = minLoss;
		}
		
		public double loss(double absLoss) {
			return absLoss - minLoss;
		}
	}
	
}
