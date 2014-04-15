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
package edu.uw.cs.lil.tiny.learn.situated.stocgrad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector.ValueFunction;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.situated.AbstractSituatedLearner;
import edu.uw.cs.lil.tiny.learn.situated.perceptron.SituatedValidationPerceptron;
import edu.uw.cs.lil.tiny.parser.joint.IJointDerivation;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointModelImmutable;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Situated validation-based stochastic gradient learner.
 * 
 * @author Yoav Artzi
 * @param <STATE>
 * @param <MR>
 * @param <ESTEP>
 * @param <ERESULT>
 */
public class SituatedValidationStocGrad<SAMPLE extends ISituatedDataItem<Sentence, ?>, MR, ESTEP, ERESULT, DI extends ILabeledDataItem<SAMPLE, ?>>
		extends AbstractSituatedLearner<SAMPLE, MR, ESTEP, ERESULT, DI> {
	public static final ILogger									LOG						= LoggerFactory
																								.create(SituatedValidationStocGrad.class);
	private final double										alpha0;
	
	private final double										c;
	
	private final IJointGraphParser<SAMPLE, MR, ESTEP, ERESULT>	graphParser;
	
	private int													stocGradientNumUpdates	= 0;
	
	/**
	 * Since the logical form is marginalized for computing the normalization
	 * constant and probabilities, the validator has access only to the final
	 * result of the execution. This is in contrast to the validator in
	 * {@link SituatedValidationPerceptron}.
	 */
	private final IValidator<DI, ERESULT>						validator;
	
	private SituatedValidationStocGrad(
			int numIterations,
			IDataCollection<DI> trainingData,
			Map<DI, Pair<MR, ERESULT>> trainingDataDebug,
			int maxSentenceLength,
			int lexiconGenerationBeamSize,
			IJointGraphParser<SAMPLE, MR, ESTEP, ERESULT> parser,
			IJointOutputLogger<MR, ESTEP, ERESULT> parserOutputLogger,
			double alpha0,
			double c,
			IValidator<DI, ERESULT> validator,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>> genlex) {
		super(numIterations, trainingData, trainingDataDebug,
				maxSentenceLength, lexiconGenerationBeamSize, parser,
				parserOutputLogger, categoryServices, genlex);
		this.graphParser = parser;
		this.alpha0 = alpha0;
		this.c = c;
		this.validator = validator;
		LOG.info(
				"Init SituatedValidationSensitiveStocGrad: numIterations=%d,trainingData.size()=%d, trainingDataDebug.size()=%d, maxSentenceLength=%d ...",
				numIterations, trainingData.size(), trainingDataDebug.size(),
				maxSentenceLength);
		LOG.info(
				"Init SituatedValidationSensitiveStocGrad: ... lexiconGenerationBeamSize=%d, alpah0=%f, c=%f",
				lexiconGenerationBeamSize, alpha0, c);
	}
	
	@Override
	public void train(JointModel<SAMPLE, MR, ESTEP> model) {
		stocGradientNumUpdates = 0;
		super.train(model);
	}
	
	@Override
	protected void parameterUpdate(final DI dataItem,
			IJointDataItemModel<MR, ESTEP> dataItemModel,
			JointModel<SAMPLE, MR, ESTEP> model, int itemCounter,
			int epochNumber) {
		
		// Parse with current model
		final IJointGraphOutput<MR, ERESULT> parserOutput = graphParser.parse(
				dataItem.getSample(), dataItemModel);
		stats.recordModelParsing(parserOutput.getInferenceTime());
		parserOutputLogger.log(parserOutput, dataItemModel);
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
		
		// Record if the best is the gold standard, if such debug
		// information is available
		if (bestModelParses.size() == 1
				&& isGoldDebugCorrect(dataItem, bestModelParses.get(0)
						.getResult())) {
			stats.goldIsOptimal(itemCounter, epochNumber);
		}
		
		// Create the update
		final IHashVector update = HashVectorFactory.create();
		
		// Step A: Compute the positive half of the update: conditioned on
		// getting successful validation
		final IFilter<ERESULT> filter = new IFilter<ERESULT>() {
			@Override
			public boolean isValid(ERESULT e) {
				return validate(dataItem, e);
			}
		};
		final double logConditionedNorm = parserOutput.logNorm(filter);
		if (logConditionedNorm == Double.NEGATIVE_INFINITY) {
			// No positive update, skip the update.
			return;
		} else {
			// Case have complete valid parses.
			final IHashVector expectedFeatures = parserOutput
					.logExpectedFeatures(filter);
			expectedFeatures.add(-logConditionedNorm);
			expectedFeatures.applyFunction(new ValueFunction() {
				
				@Override
				public double apply(double value) {
					return Math.exp(value);
				}
			});
			expectedFeatures.dropNoise();
			expectedFeatures.addTimesInto(1.0, update);
			LOG.info("Positive update: %s", expectedFeatures);
		}
		
		// Step B: Compute the negative half of the update: expectation under
		// the current model
		final double logNorm = parserOutput.logNorm();
		if (logNorm == Double.NEGATIVE_INFINITY) {
			LOG.info("No negative update");
		} else {
			// Case have complete parses.
			final IHashVector expectedFeatures = parserOutput
					.logExpectedFeatures();
			expectedFeatures.add(-logNorm);
			expectedFeatures.applyFunction(new ValueFunction() {
				
				@Override
				public double apply(double value) {
					return Math.exp(value);
				}
			});
			expectedFeatures.dropNoise();
			expectedFeatures.addTimesInto(-1.0, update);
			LOG.info("Negative update: %s", expectedFeatures);
		}
		
		// Step C: Apply the update
		
		// Validate the update
		if (!model.isValidWeightVector(update)) {
			throw new IllegalStateException("invalid update: " + update);
		}
		
		// Scale the update
		final double scale = alpha0 / (1.0 + c * stocGradientNumUpdates);
		update.multiplyBy(scale);
		update.dropNoise();
		stocGradientNumUpdates++;
		LOG.info("Scale: %f", scale);
		if (update.size() == 0) {
			LOG.info("No update");
		} else {
			LOG.info("Update: %s", update);
			stats.triggeredUpdate(itemCounter, epochNumber);
		}
		
		// Check for NaNs and super large updates
		if (update.isBad()) {
			LOG.error("Bad update: %s -- log-norm: %f.4f -- feats: %s", update,
					logNorm, null);
			LOG.error(model.getTheta().printValues(update));
			throw new IllegalStateException("bad update");
		} else {
			if (!update.valuesInRange(-100, 100)) {
				LOG.warn("Large update");
			}
			// Do the update
			update.addTimesInto(1, model.getTheta());
		}
	}
	
	@Override
	protected boolean validate(DI dataItem, ERESULT hypothesis) {
		return validator.isValid(dataItem, hypothesis);
	}
	
	public static class Builder<SAMPLE extends ISituatedDataItem<Sentence, ?>, MR, ESTEP, ERESULT, DI extends ILabeledDataItem<SAMPLE, ?>> {
		
		/**
		 * Used to define the temperature of parameter updates. temp =
		 * alpha_0/(1+c*tot_number_of_training_instances)
		 */
		private double																alpha0						= 0.1;
		
		/**
		 * Used to define the temperature of parameter updates. temp =
		 * alpha_0/(1+c*tot_number_of_training_instances)
		 */
		private double																c							= 0.0001;
		
		/**
		 * Required for lexical induction.
		 */
		private ICategoryServices<MR>												categoryServices			= null;
		
		/**
		 * GENLEX procedure. If 'null' skip lexical induction.
		 */
		private ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>>	genlex						= null;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																	lexiconGenerationBeamSize	= 20;
		
		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private int																	maxSentenceLength			= Integer.MAX_VALUE;
		
		/** Number of training iterations */
		private int																	numIterations				= 4;
		
		private final IJointGraphParser<SAMPLE, MR, ESTEP, ERESULT>					parser;
		
		private IJointOutputLogger<MR, ESTEP, ERESULT>								parserOutputLogger			= new IJointOutputLogger<MR, ESTEP, ERESULT>() {
																													
																													public void log(
																															IJointOutput<MR, ERESULT> output,
																															IJointDataItemModel<MR, ESTEP> dataItemModel) {
																														// Stub
																														
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
				IJointGraphParser<SAMPLE, MR, ESTEP, ERESULT> parser,
				IValidator<DI, ERESULT> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}
		
		public SituatedValidationStocGrad<SAMPLE, MR, ESTEP, ERESULT, DI> build() {
			return new SituatedValidationStocGrad<SAMPLE, MR, ESTEP, ERESULT, DI>(
					numIterations, trainingData, trainingDataDebug,
					maxSentenceLength, lexiconGenerationBeamSize, parser,
					parserOutputLogger, alpha0, c, validator, categoryServices,
					genlex);
		}
		
		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setAlpha0(double alpha0) {
			this.alpha0 = alpha0;
			return this;
		}
		
		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setC(double c) {
			this.c = c;
			return this;
		}
		
		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setGenlex(
				ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>> genlex,
				ICategoryServices<MR> categoryServices) {
			this.genlex = genlex;
			this.categoryServices = categoryServices;
			return this;
		}
		
		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}
		
		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setMaxSentenceLength(
				int maxSentenceLength) {
			this.maxSentenceLength = maxSentenceLength;
			return this;
		}
		
		public Builder<SAMPLE, MR, ESTEP, ERESULT, DI> setNumIterations(
				int numIterations) {
			this.numIterations = numIterations;
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
			IResourceObjectCreator<SituatedValidationStocGrad<SAMPLE, MR, ESTEP, ERESULT, DI>> {
		
		private final String	name;
		
		public Creator() {
			this("learner.situated.valid.stocgrad");
		}
		
		public Creator(String name) {
			this.name = name;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public SituatedValidationStocGrad<SAMPLE, MR, ESTEP, ERESULT, DI> create(
				Parameters params, IResourceRepository repo) {
			
			final IDataCollection<DI> trainingData = repo.getResource(params
					.get("data"));
			
			final Builder<SAMPLE, MR, ESTEP, ERESULT, DI> builder = new SituatedValidationStocGrad.Builder<SAMPLE, MR, ESTEP, ERESULT, DI>(
					trainingData,
					(IJointGraphParser<SAMPLE, MR, ESTEP, ERESULT>) repo
							.getResource(ParameterizedExperiment.PARSER_RESOURCE),
					(IValidator<DI, ERESULT>) repo.getResource(params
							.get("validator")));
			
			if (params.contains("parseLogger")) {
				builder.setParserOutputLogger((IJointOutputLogger<MR, ESTEP, ERESULT>) repo
						.getResource(params.get("parseLogger")));
			}
			
			if (params.contains("genlex")) {
				builder.setGenlex(
						(ILexiconGenerator<DI, MR, IJointModelImmutable<SAMPLE, MR, ESTEP>>) repo
								.getResource(params.get("genlex")),
						(ICategoryServices<MR>) repo
								.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
			}
			
			if (params.contains("genlexbeam")) {
				builder.setLexiconGenerationBeamSize(Integer.valueOf(params
						.get("genlexbeam")));
			}
			
			if (params.contains("maxSentenceLength")) {
				builder.setMaxSentenceLength(Integer.valueOf(params
						.get("maxSentenceLength")));
			}
			
			if (params.contains("iter")) {
				builder.setNumIterations(Integer.valueOf(params.get("iter")));
			}
			
			if (params.contains("c")) {
				builder.setC(Double.valueOf(params.get("c")));
			}
			
			if (params.contains("alpha0")) {
				builder.setAlpha0(Double.valueOf(params.get("alpha0")));
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
					SituatedValidationStocGrad.class)
					.setDescription(
							"Validation senstive stochastic gradient for situated learning of models with situated inference (cite: Artzi and Zettlemoyer 2013)")
					.addParam(
							"c",
							"double",
							"Learing rate c parameter, temperature=alpha_0/(1+c*tot_number_of_training_instances)")
					.addParam(
							"alpha0",
							"double",
							"Learing rate alpha0 parameter, temperature=alpha_0/(1+c*tot_number_of_training_instances)")
					.addParam("validator", "IValidator", "Validation function")
					.addParam("data", "id", "Training data")
					.addParam("genlex", "ILexiconGenerator", "GENLEX procedure")
					.addParam("parseLogger", "id",
							"Parse logger for debug detailed logging of parses")
					.addParam("genlexbeam", "int",
							"Beam to use for GENLEX inference (parsing).")
					.addParam("maxSentenceLength", "int",
							"Max sentence length to process")
					.addParam("iter", "int", "Number of training iterations")
					.build();
		}
		
	}
}
