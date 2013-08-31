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
package edu.uw.cs.lil.tiny.learn.situated.perceptron;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.PerceptronServices;
import edu.uw.cs.lil.tiny.learn.situated.AbstractSituatedLearner;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.IJointParse;
import edu.uw.cs.lil.tiny.parser.joint.IJointParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointModelImmutable;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

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
public class SituatedValidationPerceptron<STATE, MR, ESTEP, ERESULT, DI extends IDataItem<Pair<Sentence, STATE>>>
		extends AbstractSituatedLearner<STATE, MR, ESTEP, ERESULT, DI> {
	private static final ILogger					LOG	= LoggerFactory
																.create(SituatedValidationPerceptron.class);
	private final boolean							hardUpdates;
	private final double							margin;
	private final IValidator<DI, Pair<MR, ERESULT>>	validator;
	
	private SituatedValidationPerceptron(
			int numIterations,
			double margin,
			IDataCollection<DI> trainingData,
			Map<DI, Pair<MR, ERESULT>> trainingDataDebug,
			int maxSentenceLength,
			int lexiconGenerationBeamSize,
			IJointParser<Sentence, STATE, MR, ESTEP, ERESULT> parser,
			boolean hardUpdates,
			IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger,
			IValidator<DI, Pair<MR, ERESULT>> validator,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IJointModelImmutable<IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP>> genlex) {
		super(numIterations, trainingData, trainingDataDebug,
				maxSentenceLength, lexiconGenerationBeamSize, parser,
				parserOutputLogger, categoryServices, genlex);
		this.margin = margin;
		this.hardUpdates = hardUpdates;
		this.validator = validator;
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
			DI dataItem, Collection<? extends IJointParse<MR, ERESULT>> parses) {
		final List<IJointParse<MR, ERESULT>> validParses = new LinkedList<IJointParse<MR, ERESULT>>();
		final List<IJointParse<MR, ERESULT>> invalidParses = new LinkedList<IJointParse<MR, ERESULT>>();
		double validScore = -Double.MAX_VALUE;
		for (final IJointParse<MR, ERESULT> parse : parses) {
			if (validate(dataItem, parse.getResult())) {
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
			DI dataItem,
			IJointDataItemModel<MR, ESTEP> dataItemModel,
			JointModel<IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP> model,
			int itemCounter, int epochNumber) {
		
		// Parse with current model
		final IJointOutput<MR, ERESULT> parserOutput = parser.parse(dataItem,
				dataItemModel);
		stats.recordModelParsing(parserOutput.getInferenceTime());
		parserOutputLogger.log(parserOutput, dataItemModel);
		final List<? extends IJointParse<MR, ERESULT>> modelParses = parserOutput
				.getAllParses();
		final List<? extends IJointParse<MR, ERESULT>> bestModelParses = parserOutput
				.getBestParses();
		
		if (modelParses.isEmpty()) {
			// Skip the rest of the process if no complete parses
			// available
			LOG.info("No parses for: %s", dataItem);
			LOG.info("Skipping parameter update");
			return;
		}
		
		LOG.info("Created %d model parses for training sample",
				modelParses.size());
		LOG.info("Model parsing time: %.4fsec",
				parserOutput.getInferenceTime() / 1000.0);
		
		// Record if the best is the gold standard, if such debug
		// information is available
		if (bestModelParses.size() == 1
				&& isGoldDebugCorrect(dataItem, bestModelParses.get(0)
						.getResult())) {
			stats.goldIsOptimal(itemCounter, epochNumber);
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
	
	@Override
	protected boolean validate(DI dataItem, Pair<MR, ERESULT> hypothesis) {
		return validator.isValid(dataItem, hypothesis);
	}
	
	/**
	 * Builder for {@link SituatedValidationPerceptron}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Builder<STATE, MR, ESTEP, ERESULT, DI extends IDataItem<Pair<Sentence, STATE>>> {
		
		/**
		 * Required for lexical induction.
		 */
		private ICategoryServices<MR>																				categoryServices			= null;
		
		/**
		 * GENLEX procedure. If 'null' skip lexical induction.
		 */
		private ILexiconGenerator<DI, MR, IJointModelImmutable<IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP>>	genlex						= null;
		
		/**
		 * Use hard updates. Meaning: consider only highest-scored valid parses
		 * for parameter updates, instead of all valid parses.
		 */
		private boolean																								hardUpdates					= false;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																									lexiconGenerationBeamSize	= 20;
		
		/** Margin to scale the relative loss function */
		private double																								margin						= 1.0;
		
		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private int																									maxSentenceLength			= Integer.MAX_VALUE;
		/** Number of training iterations */
		private int																									numIterations				= 4;
		
		private final IJointParser<Sentence, STATE, MR, ESTEP, ERESULT>												parser;
		
		private IJointOutputLogger<MR, ESTEP, ERESULT>																parserOutputLogger			= new IJointOutputLogger<MR, ESTEP, ERESULT>() {
																																					
																																					public void log(
																																							IJointOutput<MR, ERESULT> output,
																																							IJointDataItemModel<MR, ESTEP> dataItemModel) {
																																						// Stub
																																						
																																					}
																																				};
		
		/** Training data */
		private final IDataCollection<DI>																			trainingData;
		
		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<DI, Pair<MR, ERESULT>>																			trainingDataDebug			= new HashMap<DI, Pair<MR, ERESULT>>();
		
		private final IValidator<DI, Pair<MR, ERESULT>>																validator;
		
		public Builder(IDataCollection<DI> trainingData,
				IJointParser<Sentence, STATE, MR, ESTEP, ERESULT> parser,
				IValidator<DI, Pair<MR, ERESULT>> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}
		
		public SituatedValidationPerceptron<STATE, MR, ESTEP, ERESULT, DI> build() {
			return new SituatedValidationPerceptron<STATE, MR, ESTEP, ERESULT, DI>(
					numIterations, margin, trainingData, trainingDataDebug,
					maxSentenceLength, lexiconGenerationBeamSize, parser,
					hardUpdates, parserOutputLogger, validator,
					categoryServices, genlex);
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setGenlex(
				ILexiconGenerator<DI, MR, IJointModelImmutable<IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP>> genlex,
				ICategoryServices<MR> categoryServices) {
			this.genlex = genlex;
			this.categoryServices = categoryServices;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setHardUpdates(
				boolean hardUpdates) {
			this.hardUpdates = hardUpdates;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setMargin(double margin) {
			this.margin = margin;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setMaxSentenceLength(
				int maxSentenceLength) {
			this.maxSentenceLength = maxSentenceLength;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setNumTrainingIterations(
				int numTrainingIterations) {
			this.numIterations = numTrainingIterations;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setParserOutputLogger(
				IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger) {
			this.parserOutputLogger = parserOutputLogger;
			return this;
		}
		
		public Builder<STATE, MR, ESTEP, ERESULT, DI> setTrainingDataDebug(
				Map<DI, Pair<MR, ERESULT>> trainingDataDebug) {
			this.trainingDataDebug = trainingDataDebug;
			return this;
		}
	}
}
