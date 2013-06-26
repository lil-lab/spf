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
package edu.uw.lil.cs.tiny.learn.situated.perceptron;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.IValidationDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.lexicalgen.ILexGenValidationDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.learn.PerceptronServices;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.IJointParse;
import edu.uw.cs.lil.tiny.parser.joint.IJointParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;
import edu.uw.lil.cs.tiny.learn.situated.AbstractSituatedLearner;

/**
 * Situated validation-based perceptron learner. See Artzi and Zettlemoyer 2013
 * for detailed description.
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
 */
public class SituatedValidationPerceptron<STATE, MR, ESTEP, ERESULT> extends
		AbstractSituatedLearner<STATE, MR, ESTEP, ERESULT> {
	private static final ILogger	LOG	= LoggerFactory
												.create(SituatedValidationPerceptron.class);
	/**
	 * Only consider highest scoring valid parses for correct parses for
	 * parameter update.
	 */
	private final boolean			hardUpdates;
	
	/**
	 * Update criterion margin.
	 */
	private final double			margin;
	
	private SituatedValidationPerceptron(
			int numIterations,
			double margin,
			IDataCollection<? extends ILexGenValidationDataItem<Pair<Sentence, STATE>, MR, Pair<MR, ERESULT>>> trainingData,
			Map<IDataItem<Pair<Sentence, STATE>>, Pair<MR, ERESULT>> trainingDataDebug,
			int maxSentenceLength, int lexiconGenerationBeamSize,
			IJointParser<Sentence, STATE, MR, ESTEP, ERESULT> parser,
			boolean hardUpdates, boolean lexiconLearning,
			IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger) {
		super(numIterations, trainingData, trainingDataDebug,
				maxSentenceLength, lexiconGenerationBeamSize, parser,
				lexiconLearning, parserOutputLogger);
		this.margin = margin;
		this.hardUpdates = hardUpdates;
		LOG.info(
				"Init SituatedValidationSensitivePerceptron: numIterations=%d, margin=%f, trainingData.size()=%d, trainingDataDebug.size()=%d, maxSentenceLength=%d ...",
				numIterations, margin, trainingData.size(),
				trainingDataDebug.size(), maxSentenceLength);
		LOG.info(
				"Init SituatedValidationSensitivePerceptron: ... lexiconGenerationBeamSize=%d",
				lexiconGenerationBeamSize);
	}
	
	/**
	 * Split the list parses to valid and invalid ones.
	 * 
	 * @param dataItem
	 * @param parseResults
	 * @return Pair of (good parses, bad parses)
	 */
	private Pair<List<IJointParse<MR, ERESULT>>, List<IJointParse<MR, ERESULT>>> createValidInvalidSets(
			IValidationDataItem<Pair<Sentence, STATE>, Pair<MR, ERESULT>> dataItem,
			Collection<? extends IJointParse<MR, ERESULT>> parses) {
		final List<IJointParse<MR, ERESULT>> validParses = new LinkedList<IJointParse<MR, ERESULT>>();
		final List<IJointParse<MR, ERESULT>> invalidParses = new LinkedList<IJointParse<MR, ERESULT>>();
		double validScore = -Double.MAX_VALUE;
		for (final IJointParse<MR, ERESULT> parse : parses) {
			if (dataItem.isValid(parse.getResult())) {
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
	
	@Override
	protected void parameterUpdate(
			IJointOutput<MR, ERESULT> modelParserOutput,
			ILexGenValidationDataItem<Pair<Sentence, STATE>, MR, Pair<MR, ERESULT>> dataItem,
			IJointDataItemModel<MR, ESTEP> dataItemModel,
			JointModel<Sentence, STATE, MR, ESTEP> model, int itemCounter,
			int epochNumber) {
		
		final List<? extends IJointParse<MR, ERESULT>> modelParses = modelParserOutput
				.getAllJointParses();
		
		// Skip the rest of the process if no complete parses available
		if (modelParses.isEmpty()) {
			LOG.info("No parses for: %s", dataItem);
			LOG.info("Skipping parameter update");
			return;
		}
		
		// Record if the best is the gold standard, if such debug
		// information is available
		final List<? extends IJointParse<MR, ERESULT>> bestModelParses = modelParserOutput
				.getBestJointParses();
		if (bestModelParses.size() == 1
				&& isGoldDebugCorrect(dataItem, bestModelParses.get(0)
						.getResult())) {
			stats.goldIsOptimal(itemCounter, epochNumber);
		}
		
		// Verify that all used lexical entries have features in the
		// model. To do that, simply re-add them all to the model.
		for (final IJointParse<MR, ERESULT> parse : modelParses) {
			model.addLexEntries(parse.getMaxLexicalEntries());
		}
		
		// Split all parses to valid and invalid sets
		final Pair<List<IJointParse<MR, ERESULT>>, List<IJointParse<MR, ERESULT>>> validInvalidSetsPair = createValidInvalidSets(
				dataItem, modelParses);
		final List<IJointParse<MR, ERESULT>> validParses = validInvalidSetsPair
				.first();
		final List<IJointParse<MR, ERESULT>> invalidParses = validInvalidSetsPair
				.second();
		LOG.info("%d valid parses, %d invalid parses", validParses.size(),
				invalidParses.size());
		LOG.info("Valid parses:");
		for (final IJointParse<MR, ERESULT> parse : validParses) {
			logParse(dataItem, parse, true, true, dataItemModel);
		}
		
		// Record if a valid parse was found
		if (!validParses.isEmpty()) {
			stats.hasValidParse(itemCounter, epochNumber);
		}
		
		// Skip update if there are no valid or invalid parses
		if (validParses.isEmpty() || invalidParses.isEmpty()) {
			LOG.info("No valid/invalid parses -- skipping");
			return;
		}
		
		// Construct margin violating sets
		final Pair<List<IJointParse<MR, ERESULT>>, List<IJointParse<MR, ERESULT>>> marginViolatingSets = PerceptronServices
				.marginViolatingSets(model, margin, validParses, invalidParses);
		final List<IJointParse<MR, ERESULT>> violatingValidParses = marginViolatingSets
				.first();
		final List<IJointParse<MR, ERESULT>> violatingInvalidParses = marginViolatingSets
				.second();
		LOG.info("%d violating valid parses, %d violating invalid parses",
				violatingValidParses.size(), violatingInvalidParses.size());
		if (violatingValidParses.isEmpty()) {
			LOG.info("There are no violating valid/invalid parses -- skipping");
			return;
		}
		LOG.info("Violating valid parses: ");
		for (final IJointParse<MR, ERESULT> pair : violatingValidParses) {
			logParse(dataItem, pair, true, true, dataItemModel);
		}
		LOG.info("Violating invalid parses: ");
		for (final IJointParse<MR, ERESULT> parse : violatingInvalidParses) {
			logParse(dataItem, parse, false, true, dataItemModel);
		}
		
		// Construct weight update vector
		final IHashVector update = PerceptronServices.constructUpdate(
				violatingValidParses, violatingInvalidParses, model);
		
		// Update the parameters vector
		LOG.info("Update: %s", update);
		update.addTimesInto(1.0, model.getTheta());
		stats.triggeredUpdate(itemCounter, epochNumber);
		
	}
	
	/**
	 * Builder for {@link SituatedValidationPerceptron}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Builder<STATE, LF, ESTEP, ERESULT> {
		
		/**
		 * Use hard updates. Meaning: consider only highest-scored valid parses
		 * for parameter updates, instead of all valid parses.
		 */
		private boolean																										hardUpdates					= false;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																											lexiconGenerationBeamSize	= 20;
		
		/**
		 * Learn a lexicon.
		 */
		private boolean																										lexiconLearning				= true;
		
		/** Margin to scale the relative loss function */
		private double																										margin						= 1.0;
		
		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private int																											maxSentenceLength			= Integer.MAX_VALUE;
		
		/** Number of training iterations */
		private int																											numIterations				= 4;
		private final IJointParser<Sentence, STATE, LF, ESTEP, ERESULT>														parser;
		
		private IJointOutputLogger<LF, ESTEP, ERESULT>																		parserOutputLogger			= new IJointOutputLogger<LF, ESTEP, ERESULT>() {
																																							
																																							public void log(
																																									IJointOutput<LF, ERESULT> output,
																																									IJointDataItemModel<LF, ESTEP> dataItemModel) {
																																								// Stub
																																								
																																							}
																																						};
		
		/** Training data */
		private final IDataCollection<? extends ILexGenValidationDataItem<Pair<Sentence, STATE>, LF, Pair<LF, ERESULT>>>	trainingData;
		
		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>>													trainingDataDebug			= new HashMap<IDataItem<Pair<Sentence, STATE>>, Pair<LF, ERESULT>>();
		
		public Builder(
				IDataCollection<? extends ILexGenValidationDataItem<Pair<Sentence, STATE>, LF, Pair<LF, ERESULT>>> trainingData,
				IJointParser<Sentence, STATE, LF, ESTEP, ERESULT> parser) {
			this.trainingData = trainingData;
			this.parser = parser;
		}
		
		public SituatedValidationPerceptron<STATE, LF, ESTEP, ERESULT> build() {
			return new SituatedValidationPerceptron<STATE, LF, ESTEP, ERESULT>(
					numIterations, margin, trainingData, trainingDataDebug,
					maxSentenceLength, lexiconGenerationBeamSize, parser,
					hardUpdates, lexiconLearning, parserOutputLogger);
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
}
