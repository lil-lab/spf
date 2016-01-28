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
package edu.cornell.cs.nlp.spf.geoquery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory.Type;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry.Origin;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.data.collection.CompositeDataCollection;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.sentence.SentenceLengthFilter;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentenceCollection;
import edu.cornell.cs.nlp.spf.data.utils.LabeledValidator;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.template.TemplateSupervisedGenlex;
import edu.cornell.cs.nlp.spf.learn.ILearner;
import edu.cornell.cs.nlp.spf.learn.validation.stocgrad.ValidationStocGrad;
import edu.cornell.cs.nlp.spf.learn.validation.stocgrad.ValidationStocGrad.Builder;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.SimpleFullParseFilter;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYBinaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYUnaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.logger.ChartLogger;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.single.CKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.BackwardSkippingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.ForwardSkippingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.SimpleWordSkippingLexicalGenerator;
import edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.FactoredLexicalFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.DynamicWordSkippingFeatures;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.LexicalFeaturesInit;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.ExpLengthLexicalEntryScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.features.lambda.LogicalExpressionCoordinationFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.lambda.pruning.SupervisedFilterFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.model.LexiconModelInit;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.spf.parser.ccg.model.ModelLogger;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.hb.HBComposedConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.PluralExistentialTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.ThatlessRelative;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.PrepositionTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.BackwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.ForwardComposition;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParser;
import edu.cornell.cs.nlp.spf.test.Tester;
import edu.cornell.cs.nlp.spf.test.stats.ExactMatchTestingStatistics;
import edu.cornell.cs.nlp.utils.collections.SetUtils;
import edu.cornell.cs.nlp.utils.function.PredicateUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.Log;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Cross validation experiment for GeoQuery using fold0 for testing. This class
 * is intended to illustrate how an experiment is structured. For complete
 * experiments see the accompanying ExPlat files.
 *
 * @author Yoav Artzi
 */
public class GeoExpSimple {
	public static final ILogger LOG = LoggerFactory.create(GeoExpSimple.class);

	private GeoExpSimple() {
		// Private ctor. Service class.
	}

	public static void main(String[] args) {

		// //////////////////////////////////////////
		// Init logging
		// //////////////////////////////////////////
		Logger.DEFAULT_LOG = new Log(System.err);
		Logger.setSkipPrefix(true);
		LogLevel.setLogLevel(LogLevel.INFO);

		// //////////////////////////////////////////
		// Set some locations to use later
		// //////////////////////////////////////////

		final File resourceDir = new File("geoquery/resources/");
		final File dataDir = new File("geoquery/experiments/data");

		// //////////////////////////////////////////
		// Use tree hash vector
		// //////////////////////////////////////////

		HashVectorFactory.DEFAULT = Type.FAST_TREE;

		// //////////////////////////////////////////
		// Init lambda calculus system.
		// //////////////////////////////////////////

		final File typesFile = new File(resourceDir, "geo.types");
		final File predOntology = new File(resourceDir, "geo.preds.ont");
		final File simpleOntology = new File(resourceDir, "geo.consts.ont");

		try {
			// Init the logical expression type system
			LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
					new TypeRepository(typesFile), new FlexibleTypeComparator())
							.addConstantsToOntology(simpleOntology)
							.addConstantsToOntology(predOntology)
							.setUseOntology(true).setNumeralTypeName("i")
							.closeOntology(true).build());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// //////////////////////////////////////////////////
		// Category services for logical expressions
		// //////////////////////////////////////////////////

		final LogicalExpressionCategoryServices categoryServices = new LogicalExpressionCategoryServices(
				true);

		// //////////////////////////////////////////////////
		// Lexical factoring services
		// //////////////////////////////////////////////////

		FactoringServices.set(new FactoringServices.Builder()
				.addConstant(LogicalConstant.read("exists:<<e,t>,t>"))
				.addConstant(LogicalConstant.read("the:<<e,t>,e>")).build());

		// //////////////////////////////////////////////////
		// Read initial lexicon
		// //////////////////////////////////////////////////

		// Create a static set of lexical entries, which are factored using
		// non-maximal factoring (each lexical entry is factored to multiple
		// entries). This static set is used to init the model with various
		// templates and lexemes.

		final File seedLexiconFile = new File(resourceDir, "seed.lex");
		final File npLexiconFile = new File(resourceDir, "np-list.lex");

		final Lexicon<LogicalExpression> readLexicon = new Lexicon<LogicalExpression>();
		readLexicon.addEntriesFromFile(seedLexiconFile, categoryServices,
				Origin.FIXED_DOMAIN);

		final Lexicon<LogicalExpression> semiFactored = new Lexicon<LogicalExpression>();
		for (final LexicalEntry<LogicalExpression> entry : readLexicon
				.toCollection()) {
			for (final FactoredLexicalEntry factoredEntry : FactoringServices
					.factor(entry, true, true, 2)) {
				semiFactored.add(FactoringServices.factor(factoredEntry));
			}
		}

		// Read NP list
		final ILexicon<LogicalExpression> npLexicon = new FactoredLexicon();
		npLexicon.addEntriesFromFile(npLexiconFile, categoryServices,
				Origin.FIXED_DOMAIN);

		// //////////////////////////////////////////////////
		// CKY parser
		// //////////////////////////////////////////////////

		// Use the Hockenmeir-Bisk normal form parsing constaints. To parse with
		// NF constraints, just set this variable to null.
		final NormalFormValidator nf = new NormalFormValidator.Builder()
				.addConstraint(
						new HBComposedConstraint(Collections.emptySet(), false))
				.build();

		// Build the parser.
		final IGraphParser<Sentence, LogicalExpression> parser = new CKYParser.Builder<Sentence, LogicalExpression>(
				categoryServices)
						.setCompleteParseFilter(new SimpleFullParseFilter(
								SetUtils.createSingleton((Syntax) Syntax.S)))
						.setPruneLexicalCells(true)
						.addSloppyLexicalGenerator(
								new SimpleWordSkippingLexicalGenerator<Sentence, LogicalExpression>(
										categoryServices))
						.setMaxNumberOfCellsInSpan(50)
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new ForwardComposition<LogicalExpression>(
												categoryServices, 1, false),
										nf))
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new BackwardComposition<LogicalExpression>(
												categoryServices, 1, false),
										nf))
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new ForwardApplication<LogicalExpression>(
												categoryServices),
										nf))
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new BackwardApplication<LogicalExpression>(
												categoryServices),
										nf))
						.addParseRule(
								new CKYUnaryParsingRule<LogicalExpression>(
										new PrepositionTypeShifting(
												categoryServices),
										nf))
						.addParseRule(
								new ForwardSkippingRule<LogicalExpression>(
										categoryServices))
						.addParseRule(
								new BackwardSkippingRule<LogicalExpression>(
										categoryServices, false))
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new ForwardTypeRaisedComposition(
												categoryServices),
										nf))
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new ThatlessRelative(categoryServices),
										nf))
						.addParseRule(
								new CKYBinaryParsingRule<LogicalExpression>(
										new PluralExistentialTypeShifting(
												categoryServices),
										nf))
						.build();

		// //////////////////////////////////////////////////
		// Model
		// //////////////////////////////////////////////////

		final Model<Sentence, LogicalExpression> model = new Model.Builder<Sentence, LogicalExpression>()
				.setLexicon(new FactoredLexicon())
				.addFeatureSet(new FactoredLexicalFeatureSet.Builder<Sentence>()
						.setTemplateScale(0.1).build())
				.addFeatureSet(new DynamicWordSkippingFeatures<>(
						categoryServices.getEmptyCategory()))
				.addFeatureSet(
						new LogicalExpressionCoordinationFeatureSet<Sentence>(
								true, true, true))
				.build();

		// Model logger
		final ModelLogger modelLogger = new ModelLogger(true);

		// //////////////////////////////////////////////////
		// Validation function
		// //////////////////////////////////////////////////

		final LabeledValidator<SingleSentence, LogicalExpression> validator = new LabeledValidator<SingleSentence, LogicalExpression>();

		// //////////////////////////////////////////////////
		// Genlex function
		// //////////////////////////////////////////////////

		final TemplateSupervisedGenlex<Sentence, SingleSentence> genlex = new TemplateSupervisedGenlex<Sentence, SingleSentence>(
				4, false, ILexiconGenerator.GENLEX_LEXICAL_ORIGIN);

		// //////////////////////////////////////////////////
		// Load training and testing data
		// //////////////////////////////////////////////////

		final List<IDataCollection<? extends SingleSentence>> folds = new ArrayList<IDataCollection<? extends SingleSentence>>(
				10);
		for (int i = 0; i < 10; ++i) {
			folds.add(SingleSentenceCollection
					.read(new File(dataDir, String.format("fold%d.ccg", i))));
		}
		final CompositeDataCollection<SingleSentence> train = new CompositeDataCollection<SingleSentence>(
				folds.subList(1, folds.size()));
		final IDataCollection<? extends SingleSentence> test = folds.get(0);

		// //////////////////////////////////////////////////
		// Learner
		// //////////////////////////////////////////////////

		// Many complex classes use the builder design pattern. For example, the
		// learner we will use in this example is created via a builder. The
		// builder constructor takes the non-optional argument the learner will
		// need. All other parameters are optional and have default values
		// inside the builder. In general, builder classes are located in the
		// classes they are used to create.
		final Builder<Sentence, SingleSentence, LogicalExpression> builder = new ValidationStocGrad.Builder<Sentence, SingleSentence, LogicalExpression>(
				train, parser, validator);

		// Add the GENLEX procedure to allow for lexical learning.
		builder.setGenlex(genlex, categoryServices);

		// We will use a larger beam for lexical generation. This will override
		// the beam set for the parser.
		builder.setLexiconGenerationBeamSize(100);

		// 4 learning iterations. If we hadn't specified 4 here, the learner
		// would have used the default value from the builder class. Usually, we
		// run experiments with a higher number of iterations, when possible.
		// However, we use 4 iterations here to make this example faster to run.
		builder.setNumIterations(4);

		// We are doing supervised learning, so we will use a supervised filter.
		// See the filter factory for an explanation.
		builder.setParsingFilterFactory(
				new SupervisedFilterFactory<>(PredicateUtils.alwaysTrue()));

		// To make learning faster we are going to ignore all sentences that are
		// longer than 50 tikens.
		builder.setProcessingFilter(new SentenceLengthFilter<>(50));

		// To speed learning further, we make the learner error driven, meaning:
		// if it can parse a sentence, it will skip lexical induction.
		builder.setErrorDriven(true);

		// Another option for speeding the learner. This time we choose not to
		// use it. If this was set to true, it would have recycled derivations
		// between the two steps (lexical induction and parameter update).
		builder.setConflateGenlexAndPrunedParses(false);

		// Optional: we have the option to create files with more verbose
		// logging. This is separate from simply increasing the log level of the
		// system (or of a specific class). The output logger we use here dumps
		// the chart of the CKY parser to a file. It assumes the directory given
		// exists. If the directory is missing, it will LOG an error message and
		// won't log the chart. Naturally, this logging slows the system.
		builder.setParserOutputLogger(new ChartLogger<>(new File("/tmp/charts"),
				"geoexpsimple", false));

		// Not that we set all the learning parameters, we call build() to
		// create the learner.
		final ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>> learner = builder
				.build();

		// //////////////////////////////////////////////////
		// Tester
		// //////////////////////////////////////////////////

		final Tester.Builder<Sentence, LogicalExpression, SingleSentence> testBuilder = new Tester.Builder<Sentence, LogicalExpression, SingleSentence>(
				test, parser);

		// Optional: we have the option to create files with more verbose
		// logging. This is separate from simply increasing the log level of the
		// system (or of a specific class). The output logger we use here dumps
		// the chart of the CKY parser to a file. It assumes the directory given
		// exists. If the directory is missing, it will LOG an error message and
		// won't log the chart. Naturally, this logging slows the system.
		testBuilder.setOutputLogger(new ChartLogger<>(new File("/tmp/charts"),
				"geoexpsimple", false));

		final Tester<Sentence, LogicalExpression, SingleSentence> tester = testBuilder
				.build();

		// //////////////////////////////////////////////////
		// Init model
		// //////////////////////////////////////////////////

		new LexiconModelInit<Sentence, LogicalExpression>(semiFactored)
				.init(model);
		new LexiconModelInit<Sentence, LogicalExpression>(npLexicon)
				.init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(semiFactored,
				KeyArgs.read("FACLEX#LEX"),
				new ExpLengthLexicalEntryScorer<LogicalExpression>(10.0, 1.1))
						.init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(npLexicon,
				KeyArgs.read("FACLEX#LEX"),
				new ExpLengthLexicalEntryScorer<LogicalExpression>(10.0, 1.1))
						.init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(semiFactored,
				KeyArgs.read("FACLEX#XEME"), 10.0).init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(npLexicon,
				KeyArgs.read("FACLEX#XEME"), 10.0).init(model);

		// Init the weight for the dynamic word skipping feature.
		model.getTheta().set("DYNSKIP", -1.0);

		// //////////////////////////////////////////////////
		// Log initial model
		// //////////////////////////////////////////////////

		LOG.info("Initial model:");
		modelLogger.log(model, System.err);

		// //////////////////////////////////////////////////
		// Training
		// //////////////////////////////////////////////////

		final long startTime = System.currentTimeMillis();

		learner.train(model);

		// Output total run time
		LOG.info("Total training time %.4f seconds",
				(System.currentTimeMillis() - startTime) / 1000.0);

		// //////////////////////////////////////////////////
		// Log final model.
		// //////////////////////////////////////////////////

		LOG.info("Final model:");
		modelLogger.log(model, System.err);

		// //////////////////////////////////////////////////
		// Testing.
		// //////////////////////////////////////////////////

		final ExactMatchTestingStatistics<Sentence, LogicalExpression, SingleSentence> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression, SingleSentence>();
		tester.test(model, stats);
		LOG.info(stats.toString());

	}
}
