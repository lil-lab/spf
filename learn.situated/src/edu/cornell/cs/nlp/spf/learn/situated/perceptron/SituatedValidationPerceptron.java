/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.learn.situated.perceptron;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.situated.ISituatedDataItem;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.learn.situated.AbstractSituatedLearner;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.joint.IJointDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.IJointOutput;
import edu.cornell.cs.nlp.spf.parser.joint.IJointOutputLogger;
import edu.cornell.cs.nlp.spf.parser.joint.IJointParser;
import edu.cornell.cs.nlp.spf.parser.joint.model.IJointDataItemModel;
import edu.cornell.cs.nlp.spf.parser.joint.model.IJointModelImmutable;
import edu.cornell.cs.nlp.spf.parser.joint.model.JointModel;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

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
 * @param <DI>
 *            Training data item.
 */
public class SituatedValidationPerceptron<SAMPLE extends ISituatedDataItem<Sentence, ?>, MR, ESTEP, ERESULT, DI extends ILabeledDataItem<SAMPLE, ?>>
		extends AbstractSituatedLearner<SAMPLE, MR, ESTEP, ERESULT, DI> {
	public static final ILogger				LOG	= LoggerFactory
														.create(SituatedValidationPerceptron.class);
	private final boolean					hardUpdates;
	private final double					margin;

	private final IValidator<DI, ERESULT>	validator;

	private SituatedValidationPerceptron(
			int numIterations,
			double margin,
			IDataCollection<DI> trainingData,
			Map<DI, Pair<MR, ERESULT>> trainingDataDebug,
			int maxSentenceLength,
			int lexiconGenerationBeamSize,
			IJointParser<SAMPLE, MR, ESTEP, ERESULT> parser,
			boolean hardUpdates,
			IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger,
			IValidator<DI, ERESULT> validator,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>> genlex) {
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

	private IHashVector constructUpdate(
			List<IJointDerivation<MR, ERESULT>> violatingValidParses,
			List<IJointDerivation<MR, ERESULT>> violatingInvalidParses,
			JointModel<SAMPLE, MR, ESTEP> model) {
		// Create the parameter update
		final IHashVector update = HashVectorFactory.create();

		// Get the update for valid violating samples
		for (final IJointDerivation<MR, ERESULT> parse : violatingValidParses) {
			parse.getMeanMaxFeatures().addTimesInto(
					1.0 / violatingValidParses.size(), update);
		}

		// Get the update for the invalid violating samples
		for (final IJointDerivation<MR, ERESULT> parse : violatingInvalidParses) {
			parse.getMeanMaxFeatures().addTimesInto(
					-1.0 * (1.0 / violatingInvalidParses.size()), update);
		}

		// Prune small entries from the update
		update.dropNoise();

		// Validate the update
		if (!model.isValidWeightVector(update)) {
			throw new IllegalStateException("invalid update: " + update);
		}

		return update;
	}

	/**
	 * Split the list parses to valid and invalid ones.
	 *
	 * @param dataItem
	 * @param parseResults
	 * @return Pair of (good parses, bad parses)
	 */
	private Pair<List<IJointDerivation<MR, ERESULT>>, List<IJointDerivation<MR, ERESULT>>> createValidInvalidSets(
			DI dataItem,
			Collection<? extends IJointDerivation<MR, ERESULT>> parses) {
		final List<IJointDerivation<MR, ERESULT>> validParses = new LinkedList<IJointDerivation<MR, ERESULT>>();
		final List<IJointDerivation<MR, ERESULT>> invalidParses = new LinkedList<IJointDerivation<MR, ERESULT>>();
		double validScore = -Double.MAX_VALUE;
		for (final IJointDerivation<MR, ERESULT> parse : parses) {
			if (validate(dataItem, parse.getResult())) {
				if (hardUpdates) {
					// Case using hard updates, only keep the highest scored
					// valid ones
					if (parse.getViterbiScore() > validScore) {
						validScore = parse.getViterbiScore();
						validParses.clear();
						validParses.add(parse);
					} else if (parse.getViterbiScore() == validScore) {
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

	private Pair<List<IJointDerivation<MR, ERESULT>>, List<IJointDerivation<MR, ERESULT>>> marginViolatingSets(
			JointModel<SAMPLE, MR, ESTEP> model,
			List<IJointDerivation<MR, ERESULT>> validParses,
			List<IJointDerivation<MR, ERESULT>> invalidParses) {
		// Construct margin violating sets
		final List<IJointDerivation<MR, ERESULT>> violatingValidParses = new LinkedList<IJointDerivation<MR, ERESULT>>();
		final List<IJointDerivation<MR, ERESULT>> violatingInvalidParses = new LinkedList<IJointDerivation<MR, ERESULT>>();

		// Flags to mark that we inserted a parse into the violating
		// sets, so no need to check for its violation against others
		final boolean[] validParsesFlags = new boolean[validParses.size()];
		final boolean[] invalidParsesFlags = new boolean[invalidParses.size()];
		int validParsesCounter = 0;
		for (final IJointDerivation<MR, ERESULT> validParse : validParses) {
			int invalidParsesCounter = 0;
			for (final IJointDerivation<MR, ERESULT> invalidParse : invalidParses) {
				if (!validParsesFlags[validParsesCounter]
						|| !invalidParsesFlags[invalidParsesCounter]) {
					// Create the delta vector if needed, we do it only
					// once. This is why we check if we are going to
					// need it in the above 'if'.
					final IHashVector featureDelta = validParse
							.getMeanMaxFeatures().addTimes(-1.0,
									invalidParse.getMeanMaxFeatures());
					final double deltaScore = model.score(featureDelta);

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

		return Pair.of(violatingValidParses, violatingInvalidParses);

	}

	@Override
	protected void parameterUpdate(DI dataItem,
			IJointDataItemModel<MR, ESTEP> dataItemModel,
			JointModel<SAMPLE, MR, ESTEP> model, int dataItemNumber,
			int epochNumber) {

		// Parse with current model
		final IJointOutput<MR, ERESULT> parserOutput = parser.parse(
				dataItem.getSample(), dataItemModel);
		stats.mean("model parse", parserOutput.getInferenceTime() / 1000.0,
				"sec");
		parserOutputLogger.log(parserOutput, dataItemModel,
				String.format("%d-update", dataItemNumber));
		final List<? extends IJointDerivation<MR, ERESULT>> modelParses = parserOutput
				.getDerivations();
		final List<? extends IJointDerivation<MR, ERESULT>> bestModelParses = parserOutput
				.getMaxDerivations();

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
		LOG.info("Output is %s", parserOutput.isExact() ? "exact"
				: "approximate");

		// Split all parses to valid and invalid sets
		final Pair<List<IJointDerivation<MR, ERESULT>>, List<IJointDerivation<MR, ERESULT>>> validInvalidSetsPair = createValidInvalidSets(
				dataItem, modelParses);
		final List<IJointDerivation<MR, ERESULT>> validParses = validInvalidSetsPair
				.first();
		final List<IJointDerivation<MR, ERESULT>> invalidParses = validInvalidSetsPair
				.second();
		LOG.info("%d valid parses, %d invalid parses", validParses.size(),
				invalidParses.size());
		LOG.info("Valid parses:");
		for (final IJointDerivation<MR, ERESULT> parse : validParses) {
			logParse(dataItem, parse, true, true, dataItemModel);
		}

		// Record if the best is the gold standard, if such debug
		// information is available.
		if (bestModelParses.size() == 1
				&& isGoldDebugCorrect(dataItem, bestModelParses.get(0)
						.getResult())) {
			stats.appendSampleStat(dataItemNumber, epochNumber, GOLD_LF_IS_MAX);
		} else if (!validParses.isEmpty()) {
			// Record if a valid parse was found.
			stats.appendSampleStat(dataItemNumber, epochNumber, HAS_VALID_LF);
		}

		if (!validParses.isEmpty()) {
			stats.count("valid", epochNumber);
		}

		// Skip update if there are no valid or invalid parses
		if (validParses.isEmpty() || invalidParses.isEmpty()) {
			LOG.info("No valid/invalid parses -- skipping");
			return;
		}

		// Construct margin violating sets
		final Pair<List<IJointDerivation<MR, ERESULT>>, List<IJointDerivation<MR, ERESULT>>> marginViolatingSets = marginViolatingSets(
				model, validParses, invalidParses);
		final List<IJointDerivation<MR, ERESULT>> violatingValidParses = marginViolatingSets
				.first();
		final List<IJointDerivation<MR, ERESULT>> violatingInvalidParses = marginViolatingSets
				.second();
		LOG.info("%d violating valid parses, %d violating invalid parses",
				violatingValidParses.size(), violatingInvalidParses.size());
		if (violatingValidParses.isEmpty()) {
			LOG.info("There are no violating valid/invalid parses -- skipping");
			return;
		}
		LOG.info("Violating valid parses: ");
		for (final IJointDerivation<MR, ERESULT> pair : violatingValidParses) {
			logParse(dataItem, pair, true, true, dataItemModel);
		}
		LOG.info("Violating invalid parses: ");
		for (final IJointDerivation<MR, ERESULT> parse : violatingInvalidParses) {
			logParse(dataItem, parse, false, true, dataItemModel);
		}

		// Construct weight update vector
		final IHashVector update = constructUpdate(violatingValidParses,
				violatingInvalidParses, model);

		// Update the parameters vector
		LOG.info("Update: %s", update);
		update.addTimesInto(1.0, model.getTheta());
		stats.appendSampleStat(dataItemNumber, epochNumber, TRIGGERED_UPDATE);
		stats.count("update", epochNumber);
	}

	@Override
	protected boolean validate(DI dataItem, ERESULT hypothesis) {
		return validator.isValid(dataItem, hypothesis);
	}

	/**
	 * Builder for {@link SituatedValidationPerceptron}.
	 *
	 * @author Yoav Artzi
	 */
	public static class Builder<SAMPLE extends ISituatedDataItem<Sentence, ?>, MR, ESTEP, ERESULT, DI extends ILabeledDataItem<SAMPLE, ?>> {

		/**
		 * Required for lexical induction.
		 */
		private ICategoryServices<MR>												categoryServices			= null;

		/**
		 * GENLEX procedure. If 'null' skip lexical induction.
		 */
		private ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>>	genlex						= null;

		/**
		 * Use hard updates. Meaning: consider only highest-scored valid parses
		 * for parameter updates, instead of all valid parses.
		 */
		private boolean																hardUpdates					= false;

		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																	lexiconGenerationBeamSize	= 20;

		/** Margin to scale the relative loss function */
		private double																margin						= 1.0;

		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private int																	maxSentenceLength			= Integer.MAX_VALUE;
		/** Number of training iterations */
		private int																	numIterations				= 4;

		private final IJointParser<SAMPLE, MR, ESTEP, ERESULT>						parser;

		private IJointOutputLogger<MR, ESTEP, ERESULT>								parserOutputLogger			= new IJointOutputLogger<MR, ESTEP, ERESULT>() {

																													private static final long	serialVersionUID	= 4342845964338126692L;

																													@Override
																													public void log(
																															IJointOutput<MR, ERESULT> output,
																															IJointDataItemModel<MR, ESTEP> dataItemModel,
																															String tag) {
																														// Stub,
																														// do
																														// nothing.
																													}

																													@Override
																													public void log(
																															IParserOutput<MR> output,
																															IDataItemModel<MR> dataItemModel,
																															String tag) {
																														// Stub,
																														// do
																														// nothing.
																													}
																												};

		/** Training data */
		private final IDataCollection<DI>											trainingData;

		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<DI, Pair<MR, ERESULT>>											trainingDataDebug			= new HashMap<DI, Pair<MR, ERESULT>>();

		private final IValidator<DI, ERESULT>										validator;

		public Builder(IDataCollection<DI> trainingData,
				IJointParser<SAMPLE, MR, ESTEP, ERESULT> parser,
				IValidator<DI, ERESULT> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}

		public SituatedValidationPerceptron<SAMPLE, MR, ESTEP, ERESULT, DI> build() {
			return new SituatedValidationPerceptron<SAMPLE, MR, ESTEP, ERESULT, DI>(
					numIterations, margin, trainingData, trainingDataDebug,
					maxSentenceLength, lexiconGenerationBeamSize, parser,
					hardUpdates, parserOutputLogger, validator,
					categoryServices, genlex);
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setGenlex(
				ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>> genlex,
				ICategoryServices<MR> categoryServices) {
			this.genlex = genlex;
			this.categoryServices = categoryServices;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setHardUpdates(
				boolean hardUpdates) {
			this.hardUpdates = hardUpdates;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setMargin(double margin) {
			this.margin = margin;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setMaxSentenceLength(
				int maxSentenceLength) {
			this.maxSentenceLength = maxSentenceLength;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setNumTrainingIterations(
				int numTrainingIterations) {
			this.numIterations = numTrainingIterations;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setParserOutputLogger(
				IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger) {
			this.parserOutputLogger = parserOutputLogger;
			return this;
		}

		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setTrainingDataDebug(
				Map<DI, Pair<MR, ERESULT>> trainingDataDebug) {
			this.trainingDataDebug = trainingDataDebug;
			return this;
		}
	}

	public static class Creator<SAMPLE extends ISituatedDataItem<Sentence, ?>, MR, ESTEP, ERESULT, DI extends ILabeledDataItem<SAMPLE, ?>>
			implements
			IResourceObjectCreator<SituatedValidationPerceptron<SAMPLE, MR, ESTEP, ERESULT, DI>> {

		private final String	name;

		public Creator() {
			this("learner.weakp.valid.situated");
		}

		public Creator(String name) {
			this.name = name;
		}

		@SuppressWarnings("unchecked")
		@Override
		public SituatedValidationPerceptron<SAMPLE, MR, ESTEP, ERESULT, DI> create(
				Parameters params, IResourceRepository repo) {

			final IDataCollection<DI> trainingData = repo.get(params
					.get("data"));

			final Builder<SAMPLE, MR, ESTEP, ERESULT, DI> builder = new SituatedValidationPerceptron.Builder<SAMPLE, MR, ESTEP, ERESULT, DI>(
					trainingData,
					(IJointParser<SAMPLE, MR, ESTEP, ERESULT>) repo
							.get(ParameterizedExperiment.PARSER_RESOURCE),
					(IValidator<DI, ERESULT>) repo.get(params.get("validator")));

			if ("true".equals(params.get("hard"))) {
				builder.setHardUpdates(true);
			}

			if (params.contains("parseLogger")) {
				builder.setParserOutputLogger((IJointOutputLogger<MR, ESTEP, ERESULT>) repo
						.get(params.get("parseLogger")));
			}

			if (params.contains("genlex")) {
				builder.setGenlex(
						(ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>>) repo
								.get(params.get("genlex")),
						(ICategoryServices<MR>) repo
								.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
			}

			if (params.contains("genlexbeam")) {
				builder.setLexiconGenerationBeamSize(Integer.valueOf(params
						.get("genlexbeam")));
			}

			if (params.contains("margin")) {
				builder.setMargin(Double.valueOf(params.get("margin")));
			}

			if (params.contains("maxSentenceLength")) {
				builder.setMaxSentenceLength(Integer.valueOf(params
						.get("maxSentenceLength")));
			}

			if (params.contains("iter")) {
				builder.setNumTrainingIterations(Integer.valueOf(params
						.get("iter")));
			}

			return builder.build();
		}

		@Override
		public String type() {
			return name;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					SituatedValidationPerceptron.class)
					.setDescription(
							"Validation senstive perceptron for situated learning of models with situated inference (cite: Artzi and Zettlemoyer 2013)")
					.addParam("data", "id", "Training data")
					.addParam(
							"hard",
							"boolean",
							"Use hard updates (i.e., only use max scoring valid parses/evaluation as positive samples). Options: true, false. Default: false")
					.addParam("parseLogger", "id",
							"Parse logger for debug detailed logging of parses")
					.addParam("genlex", "ILexiconGenerator", "GENLEX procedure")
					.addParam("genlexbeam", "int",
							"Beam to use for GENLEX inference (parsing).")
					.addParam("margin", "double",
							"Margin to use for updates. Updates will be done when this margin is violated.")
					.addParam("maxSentenceLength", "int",
							"Max sentence length to process")
					.addParam("iter", "int", "Number of training iterations")
					.addParam("validator", "IValidator", "Validation function")
					.build();
		}

	}

}
