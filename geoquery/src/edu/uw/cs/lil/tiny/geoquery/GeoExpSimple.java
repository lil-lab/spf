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
package edu.uw.cs.lil.tiny.geoquery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.concurrency.ITinyExecutor;
import edu.uw.cs.lil.tiny.base.concurrency.TinyExecutorService;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory.Type;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry.Origin;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon.FactoredLexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexiconServices;
import edu.uw.cs.lil.tiny.data.collection.CompositeDataCollection;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.sentence.SentenceLengthFilter;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentenceDataset;
import edu.uw.cs.lil.tiny.data.utils.LabeledValidator;
import edu.uw.cs.lil.tiny.genlex.ccg.template.TemplateSupervisedGenlex;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.learn.validation.stocgrad.ValidationStocGrad;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.SimpleFullParseFilter;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYUnaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.multi.MultiCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.LexemeFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.LexicalTemplateFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.LexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.LexicalFeaturesInit;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.ExpLengthLexicalEntryScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.SkippingSensitiveLexicalEntryScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.lambda.LogicalExpressionCoordinationFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.LexiconModelInit;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.ModelLogger;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.PluralExistentialTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.ThatlessRelative;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.PrepositionTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition.BackwardComposition;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition.ForwardComposition;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.BackwardSkippingRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.ForwardSkippingRule;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParser;
import edu.uw.cs.lil.tiny.test.Tester;
import edu.uw.cs.lil.tiny.test.stats.ExactMatchTestingStatistics;
import edu.uw.cs.utils.collections.ISerializableScorer;
import edu.uw.cs.utils.collections.SetUtils;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.Log;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;
import edu.uw.cs.utils.log.LoggerFactory;
import edu.uw.cs.utils.log.thread.LoggingThreadFactory;

/**
 * Cross validation experiment for GeoQuery using fold0 for testing. This class
 * is intended to illustrate how an experiment is structured. For complete
 * experiments see the accompanying ExPlat files.
 * 
 * @author Yoav Artzi
 */
public class GeoExpSimple {
	public static final ILogger	LOG	= LoggerFactory.create(GeoExpSimple.class);
	
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
		
		final File resourceDir = new File("resources/");
		final File experimentsDir = new File("experiments/");
		final File dataDir = new File(experimentsDir, "data");
		
		// //////////////////////////////////////////
		// Use tree hash vector
		// //////////////////////////////////////////
		
		HashVectorFactory.DEFAULT = Type.TREE;
		
		// //////////////////////////////////////////
		// Init lambda calculus system.
		// //////////////////////////////////////////
		
		final File typesFile = new File(resourceDir, "geo.types");
		final File predOntology = new File(resourceDir, "geo.preds.ont");
		final File simpleOntology = new File(resourceDir, "geo.consts.ont");
		
		try {
			// Init the logical expression type system
			LogicLanguageServices
					.setInstance(new LogicLanguageServices.Builder(
							new TypeRepository(typesFile),
							new FlexibleTypeComparator())
							.addConstantsToOntology(simpleOntology)
							.addConstantsToOntology(predOntology)
							.setNumeralTypeName("i").closeOntology(true)
							.build());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		// //////////////////////////////////////////////////
		// Category services for logical expressions
		// //////////////////////////////////////////////////
		
		final LogicalExpressionCategoryServices categoryServices = new LogicalExpressionCategoryServices(
				true, true);
		
		// //////////////////////////////////////////////////
		// Lexical factoring services
		// //////////////////////////////////////////////////
		
		final Set<LogicalConstant> unfactoredConstants = new HashSet<LogicalConstant>();
		unfactoredConstants.add(LogicalConstant.read("the:<<e,t>,e>"));
		unfactoredConstants.add(LogicalConstant.read("exists:<<e,t>,t>"));
		FactoredLexiconServices.set(unfactoredConstants);
		
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
			for (final FactoredLexicalEntry factoredEntry : FactoredLexicon
					.factor(entry, true, true, 2)) {
				semiFactored.add(FactoredLexicon.factor(factoredEntry));
			}
		}
		
		// Read NP list
		final ILexicon<LogicalExpression> npLexicon = new FactoredLexicon();
		npLexicon.addEntriesFromFile(npLexiconFile, categoryServices,
				Origin.FIXED_DOMAIN);
		
		// //////////////////////////////////////////////////
		// Multi threaded executor
		// //////////////////////////////////////////////////
		
		final TinyExecutorService executor = new TinyExecutorService(Runtime
				.getRuntime().availableProcessors(),
				new LoggingThreadFactory(), ITinyExecutor.DEFAULT_MONITOR_SLEEP);
		
		// //////////////////////////////////////////////////
		// CKY parser
		// //////////////////////////////////////////////////
		
		final IGraphParser<Sentence, LogicalExpression> parser = new MultiCKYParser.Builder<LogicalExpression>(
				categoryServices, executor, new SimpleFullParseFilter(
						SetUtils.createSingleton((Syntax) Syntax.S)))
				.setPruneLexicalCells(true)
				.setPreChartPruning(true)
				.setMaxNumberOfCellsInSpan(50)
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new ForwardComposition<LogicalExpression>(
										categoryServices, 0)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new BackwardComposition<LogicalExpression>(
										categoryServices, 0)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new ForwardApplication<LogicalExpression>(
										categoryServices)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new BackwardApplication<LogicalExpression>(
										categoryServices)))
				.addParseRule(
						new CKYUnaryParsingRule<LogicalExpression>(
								new PrepositionTypeShifting()))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new ForwardSkippingRule<LogicalExpression>(
										categoryServices)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new BackwardSkippingRule<LogicalExpression>(
										categoryServices)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new ForwardTypeRaisedComposition(
										categoryServices)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new ThatlessRelative(categoryServices)))
				.addParseRule(
						new CKYBinaryParsingRule<LogicalExpression>(
								new PluralExistentialTypeShifting(
										categoryServices))).build();
		
		// //////////////////////////////////////////////////
		// Model
		// //////////////////////////////////////////////////
		
		final ISerializableScorer<LexicalEntry<LogicalExpression>> uniform0Scorer = new UniformScorer<LexicalEntry<LogicalExpression>>(
				0.0);
		final SkippingSensitiveLexicalEntryScorer<LogicalExpression> skippingScorer = new SkippingSensitiveLexicalEntryScorer<LogicalExpression>(
				categoryServices.getEmptyCategory(), -1.0, uniform0Scorer);
		final Model<Sentence, LogicalExpression> model = new Model.Builder<Sentence, LogicalExpression>()
				.setLexicon(new FactoredLexicon())
				.addLexicalFeatureSet(
						new LexicalFeatureSet.Builder<Sentence, LogicalExpression>()
								.setInitialScorer(skippingScorer).build())
				.addLexicalFeatureSet(
						new LexemeFeatureSet.Builder<Sentence>().build())
				.addLexicalFeatureSet(
						new LexicalTemplateFeatureSet.Builder<Sentence>()
								.setScale(0.1).build())
				.addParseFeatureSet(
						new LogicalExpressionCoordinationFeatureSet<Sentence>(
								true, true, true)).build();
		
		// Model logger
		final ModelLogger modelLogger = new ModelLogger(true);
		
		// //////////////////////////////////////////////////
		// Validation function
		// //////////////////////////////////////////////////
		
		final LabeledValidator<SingleSentence, LogicalExpression> validator = new LabeledValidator<SingleSentence, LogicalExpression>();
		
		// //////////////////////////////////////////////////
		// Genlex function
		// //////////////////////////////////////////////////
		
		final TemplateSupervisedGenlex<SingleSentence> genlex = new TemplateSupervisedGenlex.Builder<SingleSentence>(
				4).addTemplatesFromLexicon(semiFactored).build();
		
		// //////////////////////////////////////////////////
		// Load training and testing data
		// //////////////////////////////////////////////////
		
		final List<IDataCollection<? extends SingleSentence>> folds = new ArrayList<IDataCollection<? extends SingleSentence>>(
				10);
		for (int i = 0; i < 10; ++i) {
			folds.add(SingleSentenceDataset.read(
					new File(dataDir, String.format("fold%d.ccg", i)),
					new StubStringFilter()));
		}
		final CompositeDataCollection<SingleSentence> train = new CompositeDataCollection<SingleSentence>(
				folds.subList(1, folds.size()));
		final IDataCollection<? extends SingleSentence> test = folds.get(0);
		
		// //////////////////////////////////////////////////
		// Tester
		// //////////////////////////////////////////////////
		
		final Tester<Sentence, LogicalExpression> tester = new Tester.Builder<Sentence, LogicalExpression>(
				test, parser).build();
		
		// //////////////////////////////////////////////////
		// Learner
		// //////////////////////////////////////////////////
		
		final ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>> learner = new ValidationStocGrad.Builder<Sentence, SingleSentence, LogicalExpression>(
				train, parser, validator)
				.setGenlex(genlex, categoryServices)
				.setLexiconGenerationBeamSize(100)
				.setNumIterations(20)
				.setProcessingFilter(
						new SentenceLengthFilter<SingleSentence>(50))
				.setTester(tester).setErrorDriven(true)
				.setConflateGenlexAndPrunedParses(false).build();
		
		// //////////////////////////////////////////////////
		// Init model
		// //////////////////////////////////////////////////
		
		new LexiconModelInit<Sentence, LogicalExpression>(semiFactored)
				.init(model);
		new LexiconModelInit<Sentence, LogicalExpression>(npLexicon)
				.init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(semiFactored,
				"LEX", new ExpLengthLexicalEntryScorer<LogicalExpression>(10.0,
						1.1)).init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(npLexicon, "LEX",
				new ExpLengthLexicalEntryScorer<LogicalExpression>(10.0, 1.1))
				.init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(semiFactored,
				"XEME", 10.0).init(model);
		new LexicalFeaturesInit<Sentence, LogicalExpression>(npLexicon, "XEME",
				10.0).init(model);
		
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
		// Log final model
		// //////////////////////////////////////////////////
		
		LOG.info("Final model:");
		modelLogger.log(model, System.err);
		
		// //////////////////////////////////////////////////
		// Testing
		// //////////////////////////////////////////////////
		
		final ExactMatchTestingStatistics<Sentence, LogicalExpression> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression>();
		tester.test(model, stats);
		LOG.info(stats.toString());
		
		// //////////////////////////////////////////////////
		// Close executor
		// //////////////////////////////////////////////////
		
		executor.shutdownNow();
		
	}
}
