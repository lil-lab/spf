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
package edu.uw.cs.lil.tiny.learn.validation.stocgrad;

import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.validation.AbstractLearner;
import edu.uw.cs.lil.tiny.parser.IOutputLogger;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParser;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.test.ITester;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Validation-based stochastic gradient learner.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public class ValidationStocGrad<SAMPLE, DI extends IDataItem<SAMPLE>, MR>
		extends AbstractLearner<SAMPLE, DI, IGraphParserOutput<MR>, MR> {
	private static final ILogger			LOG						= LoggerFactory
																			.create(ValidationStocGrad.class);
	
	private final double					alpha0;
	
	private final double					c;
	
	private final IGraphParser<SAMPLE, MR>	parser;
	
	private int								stocGradientNumUpdates	= 0;
	
	private final IValidator<DI, MR>		validator;
	
	private ValidationStocGrad(
			int numIterations,
			IDataCollection<DI> trainingData,
			Map<DI, MR> trainingDataDebug,
			int maxSentenceLength,
			int lexiconGenerationBeamSize,
			IGraphParser<SAMPLE, MR> parser,
			IOutputLogger<MR> parserOutputLogger,
			double alpha0,
			double c,
			IValidator<DI, MR> validator,
			ITester<SAMPLE, MR> tester,
			boolean conflateGenlexAndPrunedParses,
			boolean errorDriven,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IModelImmutable<IDataItem<SAMPLE>, MR>> genlex,
			IFilter<DI> processingFilter) {
		super(numIterations, trainingData, trainingDataDebug,
				lexiconGenerationBeamSize, parserOutputLogger, tester,
				conflateGenlexAndPrunedParses, errorDriven, categoryServices,
				genlex, processingFilter);
		this.parser = parser;
		this.alpha0 = alpha0;
		this.c = c;
		this.validator = validator;
		LOG.info(
				"Init ValidationPerceptron: numIterations=%d, trainingData.size()=%d, trainingDataDebug.size()=%d, maxSentenceLength=%d ...",
				numIterations, trainingData.size(), trainingDataDebug.size(),
				maxSentenceLength);
		LOG.info("Init ValidationPerceptron: ... lexiconGenerationBeamSize=%d",
				lexiconGenerationBeamSize);
		LOG.info(
				"Init ValidationPerceptron: ... conflateParses=%s, erroDriven=%s",
				conflateGenlexAndPrunedParses ? "true" : "false",
				errorDriven ? "true" : "false");
		LOG.info("Init ValidationPerceptron: ... c=%f, alpha0=%f", c, alpha0);
	}
	
	@Override
	public void train(Model<IDataItem<SAMPLE>, MR> model) {
		stocGradientNumUpdates = 0;
		super.train(model);
	}
	
	@Override
	protected void parameterUpdate(final DI dataItem,
			IGraphParserOutput<MR> realOutput,
			IGraphParserOutput<MR> goodOutput,
			Model<IDataItem<SAMPLE>, MR> model, int itemCounter, int epochNumber) {
		
		if (realOutput.getAllParses().isEmpty()
				|| goodOutput.getAllParses().isEmpty()) {
			// Case not parses in one of the two outputs, skip the update
			LOG.info("Skipping parameter update, no parses");
			return;
		}
		
		// Create the update
		final IHashVector update = HashVectorFactory.create();
		
		// Step A: Compute the positive half of the update: conditioned on
		// getting successful validation
		
		final IFilter<MR> filter = new IFilter<MR>() {
			@Override
			public boolean isValid(MR e) {
				return validate(dataItem, e);
			}
		};
		final double conditionedNorm = goodOutput.norm(filter);
		if (conditionedNorm == 0.0) {
			// No positive update, skip the update
			LOG.info("No positive update");
			return;
		} else {
			// Case have complete valid parses
			final IHashVector expectedFeatures = goodOutput
					.expectedFeatures(filter);
			expectedFeatures.divideBy(conditionedNorm);
			LOG.info("Positive update: %s", expectedFeatures);
			expectedFeatures.addTimesInto(1.0, update);
			stats.hasValidParse(itemCounter, epochNumber);
		}
		
		// Step B: Compute the negative half of the update: expectation under
		// the current model
		final double norm = realOutput.norm();
		if (norm != 0.0) {
			// Case have complete parses
			final IHashVector expectedFeatures = realOutput.expectedFeatures();
			expectedFeatures.divideBy(norm);
			LOG.info("Negative update: %s", expectedFeatures);
			expectedFeatures.addTimesInto(-1.0, update);
		} else {
			LOG.info("No negative update");
		}
		
		// Step C: Apply the update
		
		// Validate the update
		if (!model.isValidWeightVector(update)) {
			throw new IllegalStateException("invalid update: " + update);
		}
		
		// Scale the update
		final double scale = alpha0 / (1.0 + c * stocGradientNumUpdates);
		update.multiplyBy(scale);
		update.dropSmallEntries();
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
			LOG.error("Bad update: %s -- norm: %f.4f -- feats: %s", update,
					norm, null);
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
	protected IGraphParserOutput<MR> parse(DI dataItem,
			IDataItemModel<MR> dataItemModel) {
		return parser.parse(dataItem, dataItemModel);
	}
	
	@Override
	protected IGraphParserOutput<MR> parse(DI dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> dataItemModel) {
		return parser.parse(dataItem, pruningFilter, dataItemModel);
	}
	
	@Override
	protected IGraphParserOutput<MR> parse(DI dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> dataItemModel,
			ILexicon<MR> generatedLexicon, int beamSize) {
		return parser.parse(dataItem, pruningFilter, dataItemModel, false,
				generatedLexicon, beamSize);
	}
	
	@Override
	protected boolean validate(DI dataItem, MR hypothesis) {
		return validator.isValid(dataItem, hypothesis);
	}
	
	public static class Builder<SAMPLE, DI extends IDataItem<SAMPLE>, MR> {
		
		/**
		 * Used to define the temperature of parameter updates. temp =
		 * alpha_0/(1+c*tot_number_of_training_instances)
		 */
		private double																alpha0							= 1.0;
		
		/**
		 * Used to define the temperature of parameter updates. temp =
		 * alpha_0/(1+c*tot_number_of_training_instances)
		 */
		private double																c								= 0.0001;
		
		/**
		 * Required for lexicon learning.
		 */
		private ICategoryServices<MR>												categoryServices				= null;
		
		/**
		 * Recycle the lexical induction parser output as the pruned one for
		 * parameter update.
		 */
		private boolean																conflateGenlexAndPrunedParses	= false;
		private boolean																errorDriven						= false;
		
		/**
		 * Processing filter, if 'false', skip sample.
		 */
		private IFilter<DI>															filter							= new IFilter<DI>() {
																														
																														@Override
																														public boolean isValid(
																																DI e) {
																															return true;
																														}
																													};
		
		/**
		 * GENLEX procedure. If 'null' skips lexicon induction.
		 */
		private ILexiconGenerator<DI, MR, IModelImmutable<IDataItem<SAMPLE>, MR>>	genlex;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																	lexiconGenerationBeamSize		= 20;
		
		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private final int															maxSentenceLength				= Integer.MAX_VALUE;
		
		/** Number of training iterations */
		private int																	numIterations					= 4;
		
		private final IGraphParser<SAMPLE, MR>										parser;
		
		private IOutputLogger<MR>													parserOutputLogger				= new IOutputLogger<MR>() {
																														
																														public void log(
																																IParserOutput<MR> output,
																																IDataItemModel<MR> dataItemModel) {
																															// Stub
																															
																														}
																													};
		
		private ITester<SAMPLE, MR>													tester							= null;
		
		/** Training data */
		private final IDataCollection<DI>											trainingData;
		
		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<DI, MR>															trainingDataDebug				= new HashMap<DI, MR>();
		
		private final IValidator<DI, MR>											validator;
		
		public Builder(IDataCollection<DI> trainingData,
				IGraphParser<SAMPLE, MR> parser, IValidator<DI, MR> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}
		
		public ValidationStocGrad<SAMPLE, DI, MR> build() {
			return new ValidationStocGrad<SAMPLE, DI, MR>(numIterations,
					trainingData, trainingDataDebug, maxSentenceLength,
					lexiconGenerationBeamSize, parser, parserOutputLogger,
					alpha0, c, validator, tester,
					conflateGenlexAndPrunedParses, errorDriven,
					categoryServices, genlex, filter);
		}
		
		public Builder<SAMPLE, DI, MR> setAlpha0(double alpha0) {
			this.alpha0 = alpha0;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setC(double c) {
			this.c = c;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setConflateGenlexAndPrunedParses(
				boolean conflateGenlexAndPrunedParses) {
			this.conflateGenlexAndPrunedParses = conflateGenlexAndPrunedParses;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setErrorDriven(boolean errorDriven) {
			this.errorDriven = errorDriven;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setGenlex(
				ILexiconGenerator<DI, MR, IModelImmutable<IDataItem<SAMPLE>, MR>> genlex,
				ICategoryServices<MR> categoryServices) {
			this.genlex = genlex;
			this.categoryServices = categoryServices;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setNumIterations(int numIterations) {
			this.numIterations = numIterations;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setParserOutputLogger(
				IOutputLogger<MR> parserOutputLogger) {
			this.parserOutputLogger = parserOutputLogger;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setProcessingFilter(IFilter<DI> filter) {
			this.filter = filter;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setTester(ITester<SAMPLE, MR> tester) {
			this.tester = tester;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setTrainingDataDebug(
				Map<DI, MR> trainingDataDebug) {
			this.trainingDataDebug = trainingDataDebug;
			return this;
		}
		
	}
}
