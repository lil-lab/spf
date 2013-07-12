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
package edu.uw.cs.lil.learn.validation.perceptron;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.learn.validation.AbstractLearner;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.lexicalgen.ILexGenDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.learn.PerceptronServices;
import edu.uw.cs.lil.tiny.parser.IOutputLogger;
import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.test.ITester;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Validation-based perceptron learner. See Artzi and Zettlemoyer 2013 for
 * detailed description.
 * <p>
 * Parameter update step inspired by: Natasha Singh-Miller and Michael Collins.
 * 2007. Trigger-based Language Modeling using a Loss-sensitive Perceptron
 * Algorithm. In proceedings of ICASSP 2007.
 * </p>
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation type.
 */
public class ValidationPerceptron<MR> extends
		AbstractLearner<IParserOutput<MR>, MR> {
	private static final ILogger						LOG	= LoggerFactory
																	.create(ValidationPerceptron.class);
	/**
	 * Only consider highest scoring valid parses for correct parses for
	 * parameter update.
	 */
	private final boolean								hardUpdates;
	
	/**
	 * Update criterion margin.
	 */
	private final double								margin;
	private final IParser<Sentence, MR>					parser;
	private final IValidator<IDataItem<Sentence>, MR>	validator;
	
	private ValidationPerceptron(
			int numIterations,
			double margin,
			IDataCollection<? extends ILexGenDataItem<Sentence, MR>> trainingData,
			Map<IDataItem<Sentence>, MR> trainingDataDebug,
			int maxSentenceLength, int lexiconGenerationBeamSize,
			IParser<Sentence, MR> parser, boolean hardUpdates,
			boolean lexiconLearning, IOutputLogger<MR> parserOutputLogger,
			IValidator<IDataItem<Sentence>, MR> validator,
			ITester<Sentence, MR> tester, boolean conflateGenlexAndPrunedParses) {
		super(numIterations, trainingData, trainingDataDebug,
				maxSentenceLength, lexiconGenerationBeamSize, parser,
				lexiconLearning, parserOutputLogger, tester,
				conflateGenlexAndPrunedParses);
		this.margin = margin;
		this.parser = parser;
		this.hardUpdates = hardUpdates;
		this.validator = validator;
		LOG.info(
				"Init ValidationPerceptron: numIterations=%d, margin=%f, trainingData.size()=%d, trainingDataDebug.size()=%d, maxSentenceLength=%d ...",
				numIterations, margin, trainingData.size(),
				trainingDataDebug.size(), maxSentenceLength);
		LOG.info("Init ValidationPerceptron: ... lexiconGenerationBeamSize=%d",
				lexiconGenerationBeamSize);
		LOG.info("Init ValidationPerceptron: ... conflateParses=%s",
				conflateGenlexAndPrunedParses ? "true" : "false");
	}
	
	/**
	 * Collect valid and invalid parses.
	 * 
	 * @param dataItem
	 * @param realOutput
	 * @param goodOutput
	 * @return
	 */
	private Pair<List<IParse<MR>>, List<IParse<MR>>> createValidInvalidSets(
			ILexGenDataItem<Sentence, MR> dataItem,
			IParserOutput<MR> realOutput, IParserOutput<MR> goodOutput) {
		
		final List<IParse<MR>> validParses = new LinkedList<IParse<MR>>();
		final List<IParse<MR>> invalidParses = new LinkedList<IParse<MR>>();
		
		// Track invalid parses, so we won't aggregate a parse more than once --
		// this is an approximation, but it's a best effort
		final Set<IParse<MR>> invalidSemantics = new HashSet<IParse<MR>>();
		
		// Collect invalid parses from readlOutput
		for (final IParse<MR> parse : realOutput.getAllParses()) {
			if (!validate(dataItem, parse.getSemantics())) {
				invalidParses.add(parse);
				invalidSemantics.add(parse);
			}
		}
		
		// Collect valid and invalid parses from goodOutput
		double validScore = -Double.MAX_VALUE;
		for (final IParse<MR> parse : goodOutput.getAllParses()) {
			if (validate(dataItem, parse.getSemantics())) {
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
			} else if (!invalidSemantics.contains(parse)) {
				invalidParses.add(parse);
			}
		}
		return Pair.of(validParses, invalidParses);
	}
	
	@Override
	protected void parameterUpdate(ILexGenDataItem<Sentence, MR> dataItem,
			IParserOutput<MR> realOutput, IParserOutput<MR> goodOutput,
			Model<Sentence, MR> model, int itemCounter, int epochNumber) {
		
		final IDataItemModel<MR> dataItemModel = model
				.createDataItemModel(dataItem);
		
		// Split all parses to valid and invalid sets
		final Pair<List<IParse<MR>>, List<IParse<MR>>> validInvalidSetsPair = createValidInvalidSets(
				dataItem, realOutput, goodOutput);
		final List<IParse<MR>> validParses = validInvalidSetsPair.first();
		final List<IParse<MR>> invalidParses = validInvalidSetsPair.second();
		LOG.info("%d valid parses, %d invalid parses", validParses.size(),
				invalidParses.size());
		LOG.info("Valid parses:");
		for (final IParse<MR> parse : validParses) {
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
		final Pair<List<IParse<MR>>, List<IParse<MR>>> marginViolatingSets = PerceptronServices
				.marginViolatingSets(model, margin, validParses, invalidParses);
		final List<IParse<MR>> violatingValidParses = marginViolatingSets
				.first();
		final List<IParse<MR>> violatingInvalidParses = marginViolatingSets
				.second();
		LOG.info("%d violating valid parses, %d violating invalid parses",
				violatingValidParses.size(), violatingInvalidParses.size());
		if (violatingValidParses.isEmpty()) {
			LOG.info("There are no violating valid/invalid parses -- skipping");
			return;
		}
		LOG.info("Violating valid parses: ");
		for (final IParse<MR> pair : violatingValidParses) {
			logParse(dataItem, pair, true, true, dataItemModel);
		}
		LOG.info("Violating invalid parses: ");
		for (final IParse<MR> parse : violatingInvalidParses) {
			logParse(dataItem, parse, false, true, dataItemModel);
		}
		
		// TODO Verify that all used lexical entries have features in the
		// model. To do that, simply re-add them all to the model.
		// for (final IParse<MR> parse : modelParses) {
		// model.addLexEntries(parse.getMaxLexicalEntries());
		// }
		
		// Construct weight update vector
		final IHashVector update = PerceptronServices.constructUpdate(
				violatingValidParses, violatingInvalidParses, model);
		
		// Update the parameters vector
		LOG.info("Update: %s", update);
		update.addTimesInto(1.0, model.getTheta());
		stats.triggeredUpdate(itemCounter, epochNumber);
		
	}
	
	@Override
	protected IParserOutput<MR> parse(ILexGenDataItem<Sentence, MR> dataItem,
			IDataItemModel<MR> dataItemModel) {
		return parser.parse(dataItem, dataItemModel);
	}
	
	@Override
	protected IParserOutput<MR> parse(ILexGenDataItem<Sentence, MR> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> dataItemModel) {
		return parser.parse(dataItem, pruningFilter, dataItemModel);
	}
	
	@Override
	protected IParserOutput<MR> parse(ILexGenDataItem<Sentence, MR> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> dataItemModel,
			ILexicon<MR> generatedLexicon, int beamSize) {
		return parser.parse(dataItem, dataItemModel, false, generatedLexicon);
	}
	
	@Override
	protected boolean validate(IDataItem<Sentence> dataItem, MR hypothesis) {
		return validator.isValid(dataItem, hypothesis);
	}
	
	/**
	 * Builder for {@link ValidationPerceptron}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Builder<MR> {
		
		/**
		 * Recycle the lexical induction parser output as the pruned one for
		 * parameter update.
		 */
		private boolean															conflateGenlexAndPrunedParses	= false;
		
		/**
		 * Use hard updates. Meaning: consider only highest-scored valid parses
		 * for parameter updates, instead of all valid parses.
		 */
		private boolean															hardUpdates						= false;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int																lexiconGenerationBeamSize		= 20;
		
		/**
		 * Learn a lexicon.
		 */
		private boolean															lexiconLearning					= true;
		
		/** Margin to scale the relative loss function */
		private double															margin							= 1.0;
		
		/**
		 * Max sentence length. Sentence longer than this value will be skipped
		 * during training
		 */
		private int																maxSentenceLength				= Integer.MAX_VALUE;
		
		/** Number of training iterations */
		private int																numIterations					= 4;
		
		private final IParser<Sentence, MR>										parser;
		private IOutputLogger<MR>												parserOutputLogger				= new IOutputLogger<MR>() {
																													
																													public void log(
																															IParserOutput<MR> output,
																															IDataItemModel<MR> dataItemModel) {
																														// Stub
																														
																													}
																												};
		
		private ITester<Sentence, MR>											tester							= null;
		
		/** Training data */
		private final IDataCollection<? extends ILexGenDataItem<Sentence, MR>>	trainingData;
		
		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<IDataItem<Sentence>, MR>									trainingDataDebug				= new HashMap<IDataItem<Sentence>, MR>();
		
		private final IValidator<IDataItem<Sentence>, MR>						validator;
		
		public Builder(
				IDataCollection<? extends ILexGenDataItem<Sentence, MR>> trainingData,
				IParser<Sentence, MR> parser,
				IValidator<IDataItem<Sentence>, MR> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}
		
		public ValidationPerceptron<MR> build() {
			return new ValidationPerceptron<MR>(numIterations, margin,
					trainingData, trainingDataDebug, maxSentenceLength,
					lexiconGenerationBeamSize, parser, hardUpdates,
					lexiconLearning, parserOutputLogger, validator, tester,
					conflateGenlexAndPrunedParses);
		}
		
		public Builder<MR> setConflateGenlexAndPrunedParses(
				boolean conflateGenlexAndPrunedParses) {
			this.conflateGenlexAndPrunedParses = conflateGenlexAndPrunedParses;
			return this;
		}
		
		public Builder<MR> setHardUpdates(boolean hardUpdates) {
			this.hardUpdates = hardUpdates;
			return this;
		}
		
		public Builder<MR> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}
		
		public Builder<MR> setLexiconLearning(boolean lexiconLearning) {
			this.lexiconLearning = lexiconLearning;
			return this;
		}
		
		public Builder<MR> setMargin(double margin) {
			this.margin = margin;
			return this;
		}
		
		public Builder<MR> setMaxSentenceLength(int maxSentenceLength) {
			this.maxSentenceLength = maxSentenceLength;
			return this;
		}
		
		public Builder<MR> setNumTrainingIterations(int numTrainingIterations) {
			this.numIterations = numTrainingIterations;
			return this;
		}
		
		public Builder<MR> setParserOutputLogger(
				IOutputLogger<MR> parserOutputLogger) {
			this.parserOutputLogger = parserOutputLogger;
			return this;
		}
		
		public Builder<MR> setTester(ITester<Sentence, MR> tester) {
			this.tester = tester;
			return this;
		}
		
		public Builder<MR> setTrainingDataDebug(
				Map<IDataItem<Sentence>, MR> trainingDataDebug) {
			this.trainingDataDebug = trainingDataDebug;
			return this;
		}
	}
}
