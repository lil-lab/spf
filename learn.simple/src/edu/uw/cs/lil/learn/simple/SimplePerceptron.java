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
package edu.uw.cs.lil.learn.simple;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Perceptron learner for parameter update only.
 * 
 * @author Yoav Artzi
 * @param <X>
 * @param <Z>
 */
public class SimplePerceptron<X, Z> implements ILearner<X, Z, Model<X, Z>> {
	private static final ILogger									LOG	= LoggerFactory
																				.create(SimplePerceptron.class);
	
	private final int												numIterations;
	private final IParser<X, Z>										parser;
	private final IDataCollection<? extends ILabeledDataItem<X, Z>>	trainingData;
	
	public SimplePerceptron(int numIterations,
			IDataCollection<? extends ILabeledDataItem<X, Z>> trainingData,
			IParser<X, Z> parser) {
		this.numIterations = numIterations;
		this.trainingData = trainingData;
		this.parser = parser;
	}
	
	@Override
	public void train(Model<X, Z> model) {
		for (int iterationNumber = 0; iterationNumber < numIterations; ++iterationNumber) {
			// Training iteration, go over all training samples
			LOG.info("=========================");
			LOG.info("Training iteration %d", iterationNumber);
			LOG.info("=========================");
			int itemCounter = -1;
			
			for (final ILabeledDataItem<X, Z> dataItem : trainingData) {
				final long startTime = System.currentTimeMillis();
				
				LOG.info("%d : ================== [%d]", ++itemCounter,
						iterationNumber);
				LOG.info("Sample type: %s", dataItem.getClass().getSimpleName());
				LOG.info("%s", dataItem);
				
				final IDataItemModel<Z> dataItemModel = model
						.createDataItemModel(dataItem);
				final IParserOutput<Z> parserOutput = parser.parse(dataItem,
						dataItemModel);
				final List<IParse<Z>> bestParses = parserOutput
						.getBestParses();
				
				// Correct parse
				final List<IParse<Z>> correctParses = parserOutput
						.getMaxParses(dataItem.getLabel());
				
				// Violating parses
				final List<IParse<Z>> violatingBadParses = new LinkedList<IParse<Z>>();
				for (final IParse<Z> parse : bestParses) {
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
					for (final IParse<Z> parse : correctParses) {
						parse.getAverageMaxFeatureVector().addTimesInto(
								(1.0 / correctParses.size()), update);
					}
					
					// Negative update
					for (final IParse<Z> parse : violatingBadParses) {
						parse.getAverageMaxFeatureVector().addTimesInto(
								-1.0 * (1.0 / violatingBadParses.size()),
								update);
					}
					
					// Prune small entries from the update
					update.dropSmallEntries();
					
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
