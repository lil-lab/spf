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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory.Type;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry.Origin;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon.FactoredLexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexiconServices;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.explat.DistributedExperiment;
import edu.uw.cs.lil.tiny.explat.Job;
import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelInit;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.ModelLogger;
import edu.uw.cs.lil.tiny.test.Tester;
import edu.uw.cs.lil.tiny.test.stats.ExactMatchTestingStatistics;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;
import edu.uw.cs.utils.log.LoggerFactory;

public class GeoExp extends DistributedExperiment {
	public static final ILogger						LOG	= LoggerFactory
																.create(GeoExp.class);
	
	private final LogicalExpressionCategoryServices	categoryServices;
	
	public GeoExp(File initFile) throws IOException {
		this(initFile, Collections.<String, String> emptyMap(),
				new GeoResourceRepo());
	}
	
	public GeoExp(File initFile, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo) throws IOException {
		super(initFile, envParams, creatorRepo);
		
		LogLevel.DEV.set();
		Logger.setSkipPrefix(true);
		
		// //////////////////////////////////////////
		// Get parameters
		// //////////////////////////////////////////
		final File typesFile = globalParams.getAsFile("types");
		final List<File> seedLexiconFiles = globalParams.getAsFiles("seedlex");
		final List<File> npLexiconFiles = globalParams.getAsFiles("nplist");
		
		// //////////////////////////////////////////
		// Executor resource
		// //////////////////////////////////////////
		
		storeResource(EXECUTOR_RESOURCE, this);
		
		// //////////////////////////////////////////
		// Use tree hash vector
		// //////////////////////////////////////////
		
		HashVectorFactory.DEFAULT = Type.TREE;
		
		// //////////////////////////////////////////
		// Init lambda calculus system.
		// //////////////////////////////////////////
		
		try {
			// Init the logical expression type system
			LogicLanguageServices
					.setInstance(new LogicLanguageServices.Builder(
							new TypeRepository(typesFile),
							new FlexibleTypeComparator())
							.setNumeralTypeName("i")
							.addConstantsToOntology(
									globalParams.getAsFiles("ont"))
							.closeOntology(true).build());
			
			storeResource(ONTOLOGY_RESOURCE,
					LogicLanguageServices.getOntology());
			
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		// //////////////////////////////////////////////////
		// Category services for logical expressions
		// //////////////////////////////////////////////////
		
		this.categoryServices = new LogicalExpressionCategoryServices(true,
				true);
		storeResource(CATEGORY_SERVICES_RESOURCE, categoryServices);
		
		// //////////////////////////////////////////////////
		// Lexical factoring services
		// //////////////////////////////////////////////////
		
		final Set<LogicalConstant> unfactoredConstants = new HashSet<LogicalConstant>();
		unfactoredConstants.add(LogicalConstant.read("the:<<e,t>,e>"));
		unfactoredConstants.add(LogicalConstant.read("exists:<<e,t>,t>"));
		FactoredLexiconServices.set(unfactoredConstants);
		
		// //////////////////////////////////////////////////
		// Initial lexicon
		// //////////////////////////////////////////////////
		
		// Create a static set of lexical entries, which are factored using
		// non-maximal factoring (each lexical entry is factored to multiple
		// entries). This static set is used to init the model with various
		// templates and lexemes.
		
		final Lexicon<LogicalExpression> readLexicon = new Lexicon<LogicalExpression>();
		for (final File file : seedLexiconFiles) {
			readLexicon.addEntriesFromFile(file, categoryServices,
					Origin.FIXED_DOMAIN);
		}
		
		final Lexicon<LogicalExpression> semiFactored = new Lexicon<LogicalExpression>();
		for (final LexicalEntry<LogicalExpression> entry : readLexicon
				.toCollection()) {
			for (final FactoredLexicalEntry factoredEntry : FactoredLexicon
					.factor(entry, true, true, 2)) {
				semiFactored.add(FactoredLexicon.factor(factoredEntry));
			}
		}
		storeResource("seedLexicon", semiFactored);
		
		// Read NP list
		final ILexicon<LogicalExpression> npLexicon = new FactoredLexicon();
		for (final File file : npLexiconFiles) {
			npLexicon.addEntriesFromFile(file, categoryServices,
					Origin.FIXED_DOMAIN);
		}
		storeResource("npLexicon", npLexicon);
		
		// //////////////////////////////////////////////////
		// Read resources
		// //////////////////////////////////////////////////
		
		for (final Parameters params : resourceParams) {
			final String type = params.get("type");
			final String id = params.get("id");
			if (getCreator(type) == null) {
				throw new IllegalArgumentException("Invalid resource type: "
						+ type);
			} else {
				storeResource(id, getCreator(type).create(params, this));
			}
			LOG.info("Created resources %s of type %s", id, type);
		}
		
		// //////////////////////////////////////////////////
		// Create jobs
		// //////////////////////////////////////////////////
		
		for (final Parameters params : jobParams) {
			addJob(createJob(params));
		}
		
	}
	
	private Job createJob(Parameters params) throws FileNotFoundException {
		final String type = params.get("type");
		if (type.equals("train")) {
			return createTrainJob(params);
		} else if (type.equals("test")) {
			return createTestJob(params);
		} else if (type.equals("save")) {
			return createSaveJob(params);
		} else if (type.equals("log")) {
			return createModelLoggingJob(params);
		} else if ("init".equals(type)) {
			return createModelInitJob(params);
		} else {
			throw new RuntimeException("Unsupported job type: " + type);
		}
	}
	
	private Job createModelInitJob(Parameters params)
			throws FileNotFoundException {
		final Model<Sentence, LogicalExpression> model = getResource(params
				.get("model"));
		final List<IModelInit<Sentence, LogicalExpression>> modelInits = ListUtils
				.map(params.getSplit("init"),
						new ListUtils.Mapper<String, IModelInit<Sentence, LogicalExpression>>() {
							
							@Override
							public IModelInit<Sentence, LogicalExpression> process(
									String obj) {
								return getResource(obj);
							}
						});
		
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {
			
			@Override
			protected void doJob() {
				for (final IModelInit<Sentence, LogicalExpression> modelInit : modelInits) {
					modelInit.init(model);
				}
			}
		};
	}
	
	private Job createModelLoggingJob(Parameters params)
			throws FileNotFoundException {
		final IModelImmutable<?, ?> model = getResource(params.get("model"));
		final ModelLogger modelLogger = getResource(params.get("logger"));
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {
			
			@Override
			protected void doJob() {
				modelLogger.log(model, getOutputStream());
			}
		};
	}
	
	private Job createSaveJob(final Parameters params)
			throws FileNotFoundException {
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {
			
			@SuppressWarnings("unchecked")
			@Override
			protected void doJob() {
				// Save the model to file.
				try {
					LOG.info("Saving model (id=%s) to: %s",
							params.get("model"), params.getAsFile("file")
									.getAbsolutePath());
					Model.write(
							(Model<Sentence, LogicalExpression>) getResource(params
									.get("model")), params.getAsFile("file"));
				} catch (final IOException e) {
					LOG.error("Failed to save model to: %s", params.get("file"));
					throw new RuntimeException(e);
				}
				
			}
		};
	}
	
	private Job createTestJob(Parameters params) throws FileNotFoundException {
		
		// Make the stats
		final ExactMatchTestingStatistics<Sentence, LogicalExpression> stats = new ExactMatchTestingStatistics<Sentence, LogicalExpression>();
		
		// Get the tester
		final Tester<Sentence, LogicalExpression> tester = getResource(params
				.get("tester"));
		
		// The model to use
		final Model<Sentence, LogicalExpression> model = getResource(params
				.get("model"));
		
		// Create and return the job
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {
			
			@Override
			protected void doJob() {
				
				// Record start time
				final long startTime = System.currentTimeMillis();
				
				// Job started
				LOG.info("============ (Job %s started)", getId());
				
				tester.test(model, stats);
				LOG.info("%s", stats);
				
				// Output total run time
				LOG.info("Total run time %.4f seconds",
						(System.currentTimeMillis() - startTime) / 1000.0);
				
				// Output machine readable stats
				getOutputStream().println(stats.toTabDelimitedString());
				
				// Job completed
				LOG.info("============ (Job %s completed)", getId());
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	private Job createTrainJob(Parameters params) throws FileNotFoundException {
		// The model to use
		final Model<Sentence, LogicalExpression> model = (Model<Sentence, LogicalExpression>) getResource(params
				.get("model"));
		
		// The learning
		final ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>> learner = (ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>>) getResource(params
				.get("learner"));
		
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {
			
			@Override
			protected void doJob() {
				final long startTime = System.currentTimeMillis();
				
				// Start job
				LOG.info("============ (Job %s started)", getId());
				
				// Do the learning
				learner.train(model);
				
				// Log the final model
				LOG.info("Final model:\n%s", model);
				
				// Output total run time
				LOG.info("Total run time %.4f seconds",
						(System.currentTimeMillis() - startTime) / 1000.0);
				
				// Job completed
				LOG.info("============ (Job %s completed)", getId());
				
			}
		};
	}
	
}
