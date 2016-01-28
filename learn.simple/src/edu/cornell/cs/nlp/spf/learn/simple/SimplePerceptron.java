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
package edu.cornell.cs.nlp.spf.learn.simple;

import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.learn.ILearner;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IParser;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Perceptron learner for parameter update only.
 *
 * @author Yoav Artzi
 * @param <DI>
 * @param <MR>
 */
public class SimplePerceptron implements
		ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>> {
	public static final ILogger							LOG	= LoggerFactory
																	.create(SimplePerceptron.class);

	private final int									numIterations;
	private final IParser<Sentence, LogicalExpression>	parser;
	private final IDataCollection<SingleSentence>		trainingData;

	public SimplePerceptron(int numIterations,
			IDataCollection<SingleSentence> trainingData,
			IParser<Sentence, LogicalExpression> parser) {
		this.numIterations = numIterations;
		this.trainingData = trainingData;
		this.parser = parser;
	}

	@Override
	public void train(Model<Sentence, LogicalExpression> model) {
		for (int iterationNumber = 0; iterationNumber < numIterations; ++iterationNumber) {
			// Training iteration, go over all training samples
			LOG.info("=========================");
			LOG.info("Training iteration %d", iterationNumber);
			LOG.info("=========================");
			int itemCounter = -1;

			for (final SingleSentence dataItem : trainingData) {
				final long startTime = System.currentTimeMillis();

				LOG.info("%d : ================== [%d]", ++itemCounter,
						iterationNumber);
				LOG.info("Sample type: %s", dataItem.getClass().getSimpleName());
				LOG.info("%s", dataItem);

				final IDataItemModel<LogicalExpression> dataItemModel = model
						.createDataItemModel(dataItem.getSample());
				final IParserOutput<LogicalExpression> parserOutput = parser
						.parse(dataItem.getSample(), dataItemModel);
				final List<? extends IDerivation<LogicalExpression>> bestParses = parserOutput
						.getBestDerivations();

				// Correct parse
				final List<? extends IDerivation<LogicalExpression>> correctParses = parserOutput
						.getMaxDerivations(new IFilter<Category<LogicalExpression>>() {

							@Override
							public boolean test(Category<LogicalExpression> e) {
								return dataItem.getLabel().equals(
										e.getSemantics());
							}
						});

				// Violating parses
				final List<IDerivation<LogicalExpression>> violatingBadParses = new LinkedList<IDerivation<LogicalExpression>>();
				for (final IDerivation<LogicalExpression> parse : bestParses) {
					// Doesn't distinguish between parses according to syntax.
					if (!dataItem.isCorrect(parse.getSemantics())) {
						violatingBadParses.add(parse);
						LOG.info("Bad parse: %s", parse.getSemantics());
					}
				}

				if (!violatingBadParses.isEmpty() && !correctParses.isEmpty()) {
					// Case we have bad best parses and a correct parse, need to
					// update.

					// Create the parameter update
					final IHashVector update = HashVectorFactory.create();

					// Positive update
					for (final IDerivation<LogicalExpression> parse : correctParses) {
						parse.getAverageMaxFeatureVector().addTimesInto(
								1.0 / correctParses.size(), update);
					}

					// Negative update
					for (final IDerivation<LogicalExpression> parse : violatingBadParses) {
						parse.getAverageMaxFeatureVector().addTimesInto(
								-1.0 * (1.0 / violatingBadParses.size()),
								update);
					}

					// Prune small entries from the update
					update.dropNoise();

					// Validate the update
					if (!model.isValidWeightVector(update)) {
						throw new IllegalStateException("invalid update: "
								+ update);
					}

					// Update the parameters vector
					LOG.info("Update: %s", update);
					update.addTimesInto(1.0, model.getTheta());
				} else if (correctParses.isEmpty()) {
					LOG.info("No correct parses. No update.");
				} else {
					LOG.info("Correct. No update.");
				}
				LOG.info("Sample processing time %.4f",
						(System.currentTimeMillis() - startTime) / 1000.0);
			}
		}
	}

}
