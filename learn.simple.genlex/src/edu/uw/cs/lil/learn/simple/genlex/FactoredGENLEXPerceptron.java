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
package edu.uw.cs.lil.learn.simple.genlex;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.IDataCollection;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetConstantsSet;
import edu.uw.cs.lil.tiny.parser.IParseResult;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.Pruner;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.FactoredLexicon;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.FactoredLexicon.FactoredLexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.LexicalTemplate;
import edu.uw.cs.lil.tiny.parser.ccg.genlex.TemplatedLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry.Origin;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.test.Tester;
import edu.uw.cs.lil.tiny.test.stats.ExactMatchTestingStatistics;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Margin Perceptron learner for parameter updates. Lexicon is induced with
 * GENLEX procedure that uses templates from the initial fixed lexicon to define
 * the set of GENLEX rules.
 * 
 * @author Luke Zettlemoyer
 */
public class FactoredGENLEXPerceptron
		implements
		ILearner<Sentence, LogicalExpression, Model<Sentence, LogicalExpression>> {
	private static final ILogger															LOG						= LoggerFactory
																															.create(FactoredGENLEXPerceptron.class);
	
	private final boolean																	doNonMaximalFactoring	= false;
	private final boolean																	factorETypeOnly			= true;
	private final int																		GENLEX_MAX_TOKENS		= 2;
	private final double																	MARGIN					= 5.0;
	private final int																		maxSentenceLength;
	private final int																		numIterations;
	
	private final IParser<Sentence, LogicalExpression>										parser;
	
	private final Tester<Sentence, LogicalExpression>										tester;
	
	private final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>	trainingData;
	
	public FactoredGENLEXPerceptron(
			int numIterations,
			int maxSentenceLength,
			Tester<Sentence, LogicalExpression> tester,
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> trainingData,
			IParser<Sentence, LogicalExpression> parser) {
		this.numIterations = numIterations;
		this.maxSentenceLength = maxSentenceLength;
		this.tester = tester;
		this.trainingData = trainingData;
		this.parser = parser;
	}
	
	protected static String lexToString(
			Iterable<LexicalEntry<LogicalExpression>> lexicalEntries,
			Model<Sentence, LogicalExpression> model) {
		final StringBuilder ret = new StringBuilder();
		ret.append("[LexEntries and scores:\n");
		
		for (final LexicalEntry<LogicalExpression> entry : lexicalEntries) {
			ret.append("[").append(model.score(entry)).append("] ");
			ret.append(entry).append(" [");
			ret.append(model.getTheta().printValues(
					model.computeFeatures(entry)));
			ret.append("]\n");
		}
		ret.append("]");
		return ret.toString();
	}
	
	protected static String lexToString(
			List<IParseResult<LogicalExpression>> correctParses,
			Model<Sentence, LogicalExpression> model) {
		final Set<LexicalEntry<LogicalExpression>> lexEntries = new LinkedHashSet<LexicalEntry<LogicalExpression>>();
		for (final IParseResult<LogicalExpression> parse : correctParses) {
			lexEntries.addAll(parse.getMaxLexicalEntries());
		}
		return lexToString(lexEntries, model);
	}
	
	@Override
	public void train(Model<Sentence, LogicalExpression> model) {
		
		final Set<LexicalTemplate> templates = new HashSet<LexicalTemplate>();
		// factor the fixed lexical entries
		for (final LexicalEntry<LogicalExpression> lex : model.getLexicon()
				.toCollection()) {
			if (!(lex instanceof FactoredLexicalEntry)) {
				continue;
			}
			
			// always add the maximal factoring
			templates.add(FactoredLexicon.factor(lex).getTemplate());
			if (factorETypeOnly) {
				for (final FactoredLexicalEntry factored : FactoredLexicon
						.factor(lex, true, true, 1)) {
					final List<LogicalConstant> lexemeConsts = factored
							.getLexeme().getConstants();
					if (lexemeConsts.size() == 1
							&& lexemeConsts
									.get(0)
									.getType()
									.isExtendingOrExtendedBy(
											LogicLanguageServices
													.getTypeRepository()
													.getEntityType())) {
						// if (!templates.contains(factored.getTemplate())) {
						// System.out.println(factored.getTemplate());
						// }
						templates.add(factored.getTemplate());
						model.addLexEntry(factored);
					}
				}
			} else {
				if (doNonMaximalFactoring) {
					for (final FactoredLexicalEntry factored : FactoredLexicon
							.factor(lex, true, true, 2)) {
						templates.add(factored.getTemplate());
						model.addLexEntry(factored);
					}
				}
			}
		}
		
		// for (final LexicalTemplate t : templates) {
		// System.out.println("TEMPLATES: " + t);
		// }
		
		for (int iterationNumber = 0; iterationNumber < numIterations; ++iterationNumber) {
			// Training iteration, go over all training samples
			LOG.info("=========================");
			LOG.info("Training iteration %d", iterationNumber);
			LOG.info("=========================");
			int itemCounter = -1;
			
			for (final ILabeledDataItem<Sentence, LogicalExpression> dataItem : trainingData) {
				final long startTime = System.currentTimeMillis();
				
				LOG.info("%d : ================== [%d]", ++itemCounter,
						iterationNumber);
				LOG.info("Sample type: %s", dataItem.getClass().getSimpleName());
				LOG.info("%s", dataItem);
				
				final int sentLength = dataItem.getSample().getTokens().size();
				if (sentLength >= maxSentenceLength) {
					LOG.info("Skipping sentence with length %s >= max of %s.",
							sentLength, maxSentenceLength);
					continue;
				}
				
				// first, do a parse (this should be fast)
				// if correct, continue on to the next example
				final IDataItemModel<LogicalExpression> initialDataItemModel = model
						.createDataItemModel(dataItem);
				final IParserOutput<LogicalExpression> initialParserOutput = parser
						.parse(dataItem, initialDataItemModel);
				// final List<IParseResult<LogicalExpression>> initialBestParses
				// = initialParserOutput
				// .getBestParses();
				// if (initialBestParses.size() == 1
				// && dataItem.isCorrect(initialBestParses.get(0).getY())) {
				// LOG.info("Correct! Skipping update.");
				// LOG.info(lexToString(initialBestParses.get(0)
				// .getMaxLexicalEntries(), model));
				// LOG.info("Sample processing time %.4f",
				// (System.currentTimeMillis() - startTime) / 1000.0);
				// continue;
				// }
				
				// get the best correct parses, if any
				final List<IParseResult<LogicalExpression>> correctParses = initialParserOutput
						.getMaxParses(dataItem.getLabel());
				if (correctParses.size() > 0) {
					// If correct parse is in chart
					updateParameters(model, dataItem, initialParserOutput,
							initialParserOutput);
				} else {
					// no correct parse, we need GENLEX
					// first try to expand the lexicon
					final IParserOutput<LogicalExpression> genlexParserOutput = doGENLEX(
							model, dataItem, initialDataItemModel, templates);
					
					// then, do a parameter update
					// should really do another parse, but lets reuse the one
					// from
					// before instead
					// final IDataItemModel<LogicalExpression> dataItemModel =
					// model
					// .createDataItemModel(dataItem);
					// final IParserOutput<LogicalExpression> parserOutput =
					// parser
					// .parse(dataItem, dataItemModel);
					
					// Correct parse
					updateParameters(model, dataItem, genlexParserOutput,
							initialParserOutput);
				}
				LOG.info("Sample processing time %.4f",
						(System.currentTimeMillis() - startTime) / 1000.0);
			}
			if (tester != null) {
				LOG.info("Testing:");
				final ExactMatchTestingStatistics<Sentence, LogicalExpression> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression>();
				tester.test(model, stats);
				LOG.info("%s", stats);
			}
		}
	}
	
	private IParserOutput<LogicalExpression> doGENLEX(
			Model<Sentence, LogicalExpression> model,
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			IDataItemModel<LogicalExpression> initialDataItemModel,
			Set<LexicalTemplate> templates) {
		// Generate lexical entries
		final long genStartTime = System.currentTimeMillis();
		
		final Set<LogicalConstant> constants = GetConstantsSet.of(dataItem
				.getLabel());
		final TemplatedLexiconGenerator genlex = new TemplatedLexiconGenerator.Builder(
				GENLEX_MAX_TOKENS + 1, model).addConstants(constants)
				.addTemplates(templates).build();
		final ILexicon<LogicalExpression> generatedLexicon = genlex
				.generate(dataItem.getSample());
		LOG.info("Generated lexicon size = %d", generatedLexicon.size());
		IParserOutput<LogicalExpression> genlexParserOutput = null;
		if (generatedLexicon.size() > 0) {
			
			genlexParserOutput = parser.parse(dataItem,
					Pruner.create(dataItem), initialDataItemModel, false,
					generatedLexicon);
			
			final List<IParseResult<LogicalExpression>> parses = genlexParserOutput
					.getMaxParses(dataItem.getLabel());
			int newLexicalEntries = 0;
			for (final IParseResult<LogicalExpression> parse : parses) {
				for (final LexicalEntry<LogicalExpression> entry : parse
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
				}
			}
			
			// LOG.info(((CKYParserOutput<LogicalExpression>)
			// genlexParserOutput)
			// .getChart());
			
			LOG.info("Number of new lexical entries = %d", newLexicalEntries);
		} else {
			LOG.info("Skipped GENLEX step. No generated lexical items.");
		}
		LOG.info("GENLEX processing time %.4f",
				(System.currentTimeMillis() - genStartTime) / 1000.0);
		
		return genlexParserOutput;
	}
	
	private void updateParameters(Model<Sentence, LogicalExpression> model,
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			IParserOutput<LogicalExpression> correctParseOutput,
			IParserOutput<LogicalExpression> badParseOutput) {
		
		final List<IParseResult<LogicalExpression>> badParses = badParseOutput
				.getAllParses();
		final List<IParseResult<LogicalExpression>> correctParses = correctParseOutput
				.getMaxParses(dataItem.getLabel());
		
		if (correctParses.size() == 0) {
			LOG.info("No correct parse. No update.");
			return;
		}
		
		final double bestScore = correctParses.get(0).getScore();
		
		// Violating parses
		
		final List<IParseResult<LogicalExpression>> violatingBadParses = new LinkedList<IParseResult<LogicalExpression>>();
		for (final IParseResult<LogicalExpression> parse : badParses) {
			if (!dataItem.isCorrect(parse.getY())
					&& parse.getScore() > (bestScore - MARGIN)) {
				violatingBadParses.add(parse);
				LOG.info("Bad parse: %s", parse.getY());
			}
		}
		
		if (violatingBadParses.isEmpty()) {
			// Case we have bad best parses and a correct parse, need to
			// update.
			LOG.info("Correct with margin %s. No update.", MARGIN);
			LOG.info(lexToString(correctParses, model));
			return;
		}
		
		// Create the parameter update
		final IHashVector update = HashVectorFactory.create();
		
		// Positive update
		
		for (final IParseResult<LogicalExpression> parse : correctParses) {
			model.addLexEntries(parse.getMaxLexicalEntries());
			parse.getAverageMaxFeatureVector().addTimesInto(
					(1.0 / correctParses.size()), update);
		}
		
		// LOG.info("POS: %s", correctParse.getAverageMaxFeatureVector());
		
		// Negative update
		for (final IParseResult<LogicalExpression> parse : violatingBadParses) {
			model.addLexEntries(parse.getMaxLexicalEntries());
			parse.getAverageMaxFeatureVector().addTimesInto(
					-1.0 * (1.0 / violatingBadParses.size()), update);
		}
		
		// Prune small entries from the update
		update.dropSmallEntries();
		
		// Update the parameters vector
		update.multiplyBy(0.1); // learning rate
		
		// Validate the update
		if (!model.isValidWeightVector(update)) {
			throw new IllegalStateException("invalid update: " + update);
		}
		
		update.addTimesInto(1.0, model.getTheta());
		
		LOG.info("Update: %s", update);
		LOG.info(lexToString(correctParses, model));
		
	}
}
