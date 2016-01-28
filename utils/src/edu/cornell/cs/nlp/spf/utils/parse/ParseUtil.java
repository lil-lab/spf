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
package edu.cornell.cs.nlp.spf.utils.parse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory.Type;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.lex.SingleSentenceLex;
import edu.cornell.cs.nlp.spf.data.singlesentence.lex.SingleSentenceLexDataset;
import edu.cornell.cs.nlp.spf.explat.DistributedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.ResourceCreatorRepository;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.ILogicalExpressionComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.SimpleFullParseFilter;
import edu.cornell.cs.nlp.spf.mr.lambda.comparators.SkolemIdInsensitiveComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.comparators.StructureOnlyComaprator;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.ILogicalExpressionPrinter;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToIndentedString;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IParser;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.multi.MultiCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model.Builder;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.eisner.EisnerNormalFormCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.hb.HBNormalFormCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.PluralExistentialTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.ThatlessRelative;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.AdjectiveTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.AdverbialTopicalisationTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.AdverbialTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.PrepositionTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ApplicationCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.CompositionCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.typshifting.ApplicationTypeShifting;
import edu.cornell.cs.nlp.spf.parser.filter.IParsingFilterFactory;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.Log;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;

/**
 * Utility to parse a sentence given a small lexicon and compute various
 * statistics (e.g., number of parses, oracle correctness, etc.).
 *
 * @author Yoav Artzi
 */
public class ParseUtil extends DistributedExperiment {

	private final ICategoryServices<LogicalExpression>	categoryServices;
	private final ILogicalExpressionComparator			comparator;
	private int											numParsed					= 0;
	private int											numParsedSuccessfuly		= 0;
	private int											numParsedSuccessfulySloppy	= 0;

	private final ILogicalExpressionPrinter				prettyPrinter				= new LogicalExpressionToIndentedString.Printer(
			"  ");
	private final boolean								reportBad;
	private final boolean								verbose;

	@SuppressWarnings("unchecked")
	private ParseUtil(File expFile, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo, List<String> files)
					throws IOException {
		super(expFile, envParams, creatorRepo);

		// //////////////////////////////////////////
		// Init logging.
		// //////////////////////////////////////////
		Logger.DEFAULT_LOG = new Log(System.err);
		Logger.setSkipPrefix(true);
		LogLevel.setLogLevel(LogLevel.valueOf(globalParams.get("log", "INFO")));
		this.verbose = globalParams.getAsBoolean("verbose");
		this.reportBad = globalParams.getAsBoolean("reportBadParses");

		// //////////////////////////////////////////
		// Use tree hash vector.
		// //////////////////////////////////////////

		HashVectorFactory.DEFAULT = Type.TREE;

		// //////////////////////////////////////////
		// Init lambda calculus system.
		// //////////////////////////////////////////

		try {
			// Get types files, if defined.
			final LogicLanguageServices.Builder builder = new LogicLanguageServices.Builder(
					globalParams.contains("types")
							? new TypeRepository(
									globalParams.getAsFile("types"))
							: new TypeRepository(),
					new FlexibleTypeComparator()).setUseOntology(true);

			// Get constants ontology files, if defined.
			if (globalParams.contains("ont")) {
				builder.addConstantsToOntology(globalParams.getAsFiles("ont"));
			}

			// Close or open ontology.
			builder.closeOntology(globalParams.getAsBoolean("closeOnt"));

			// Get number type, if not defined, use default: i.
			if (globalParams.contains("numeral")) {
				builder.setNumeralTypeName(globalParams.get("numeral"));
			} else {
				builder.setNumeralTypeName("i");
			}

			// Build.
			LogicLanguageServices.setInstance(builder.build());

			// Store ontology.
			storeResource(ONTOLOGY_RESOURCE,
					LogicLanguageServices.getOntology());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// //////////////////////////////////////////
		// Skolem services.
		// //////////////////////////////////////////

		if (globalParams.contains("skolemType")) {
			SkolemServices
					.setInstance(
							new SkolemServices.Builder(
									LogicLanguageServices.getTypeRepository()
											.getType(globalParams
													.get("skolemType")),
							LogicalConstant
									.read(globalParams.get("idPlaceholder")))
											.build());
			LogicalExpressionReader.register(new SkolemId.Reader());
		}

		// //////////////////////////////////////////////////
		// Register custom primitive syntactic categories.
		// //////////////////////////////////////////////////

		if (globalParams.contains("syntax")) {
			for (final String syntaxLabel : globalParams.getSplit("syntax")) {
				Syntax.register(new SimpleSyntax(syntaxLabel));
			}
		}

		// //////////////////////////////////////////////////
		// Category services for logical expressions.
		// //////////////////////////////////////////////////

		this.categoryServices = new LogicalExpressionCategoryServices(true);
		storeResource(CATEGORY_SERVICES_RESOURCE, categoryServices);

		// //////////////////////////////////////////////////
		// Read resources.
		// //////////////////////////////////////////////////

		for (final Parameters params : resourceParams) {
			final String type = params.get("type");
			final String id = params.get("id");
			if (getCreator(type) == null) {
				throw new IllegalArgumentException(
						"Invalid resource type: " + type);
			} else {
				storeResource(id, getCreator(type).create(params, this));
			}
			LOG.info("Created resources %s of type %s", id, type);
		}

		// //////////////////////////////////////////////////
		// Get comparator for parse result.
		// //////////////////////////////////////////////////

		if (hasResource("comparator")) {
			this.comparator = get("comparator");
			LOG.info("Using custom comparator to select correct parses.");
		} else {
			this.comparator = LogicLanguageServices.getComparator();
		}

		// //////////////////////////////////////////////////
		// Parse each sentence in each dataset.
		// //////////////////////////////////////////////////

		final Builder<Sentence, LogicalExpression> modelBuilder = new Model.Builder<Sentence, LogicalExpression>();
		if (hasResource("lexicon")) {
			modelBuilder
					.setLexicon((ILexicon<LogicalExpression>) get("lexicon"));
		}
		final Model<Sentence, LogicalExpression> model = modelBuilder.build();
		final IParser<Sentence, LogicalExpression> parser = get(
				PARSER_RESOURCE);

		final IParsingFilterFactory<SingleSentenceLex, LogicalExpression> filterFactory;
		if (hasResource("filterFactory")) {
			filterFactory = get("filterFactory");
		} else {
			filterFactory = null;
		}

		for (final String file : files) {
			final SingleSentenceLexDataset dataset = SingleSentenceLexDataset
					.read(new File(file), categoryServices, "seed");
			for (final SingleSentenceLex dataItem : dataset) {
				LOG.info("==========================");
				processSentence(dataItem, parser, model,
						globalParams.getAsBoolean("sloppy"),
						filterFactory == null ? null
								: filterFactory.create(dataItem));
			}
		}
		LOG.info("==========================");
		LOG.info("Summary");
		LOG.info("==========================");
		LOG.info(
				"Parsed %d sentences, %d successfully, %d sucessfully with word skipping",
				numParsed, numParsedSuccessfuly, numParsedSuccessfulySloppy);
	}

	public static void main(String[] args) {
		main(args, new ParseUtilResourceCreatorRepository());
	}

	public static void main(String[] args,
			ResourceCreatorRepository creatorRepo) {
		if (args.length == 0) {
			usage();
			return;
		}

		final String[] files = new String[args.length - 1];
		System.arraycopy(args, 1, files, 0, files.length);

		final File expFile = new File(args[0]);
		try {
			// Set some mandatory properties.
			final HashMap<String, String> envParams = new HashMap<String, String>();
			envParams.put("outputDir", ".");

			// Create the experiment and run it.
			new ParseUtil(expFile, envParams, creatorRepo, Arrays.asList(files))
					.end();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void usage() {
		System.out.println(String.format(
				"Usage: ... <exp_file> <data_1> <data_2> <data_3> ... ",
				ParseUtil.class.getSimpleName()));
		System.out.println(String.format(
				"\t<exp_file>\tA explat file to define certain utility parameters and the parser to use."));
		System.out.println(
				"\t<data_n>\tList of single sentence with lexicon data files to use. ");
	}

	private void processSentence(final SingleSentenceLex dataItem,
			IParser<Sentence, LogicalExpression> parser,
			Model<Sentence, LogicalExpression> model, boolean allowSloppy,
			Predicate<ParsingOp<LogicalExpression>> pruner) {
		LOG.info("%s", dataItem.getSample());
		LOG.info(prettyPrinter.toString(dataItem.getLabel()));
		LOG.info("Data item lexicon has %d entries",
				dataItem.getEntries().size());

		// Parsing counter.
		numParsed++;

		// Create data item model.
		final IDataItemModel<LogicalExpression> dataItemModel = model
				.createDataItemModel(dataItem.getSample());

		// Parse sentence.
		final IParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem.getSample(), pruner, dataItemModel, false,
				new Lexicon<LogicalExpression>(dataItem.getEntries()));

		// Create filter to detect gold parse.
		final IFilter<Category<LogicalExpression>> filter = e -> comparator
				.compare(e.getSemantics(), dataItem.getLabel());

		if (verbose && parserOutput instanceof CKYParserOutput) {
			LOG.info("------------------");
			LOG.info("Chart:");
			LOG.info(((CKYParserOutput<LogicalExpression>) parserOutput)
					.getChart());
			LOG.info("------------------");
		}

		if (pruner == null) {
			if (!parserOutput.isExact()) {
				LOG.warn("WARNING: inference is not exact.");
			}
		} else {
			LOG.info("Parse is pruned.");
		}

		LOG.info("Parse time: %fsec", parserOutput.getParsingTime() / 1000.0);
		LOG.info("Generated %d parses",
				parserOutput.getAllDerivations().size());

		final List<? extends IDerivation<LogicalExpression>> correctParses = parserOutput
				.getDerivations(filter);
		if (correctParses.isEmpty()) {
			LOG.info("No correct parses");
		} else {
			LOG.info("Generated %d correct parses", correctParses.size());
			reportCorrectParses(correctParses, dataItem.getLabel());
			numParsedSuccessfuly++;
		}

		if (reportBad) {
			reportBadParse(parserOutput.getAllDerivations());
		}

		if (allowSloppy && correctParses.isEmpty()) {
			LOG.info("------------------");
			LOG.info("Sloppy parse: trying with word skipping");
			// Parse sentence.
			final IParserOutput<LogicalExpression> sloppyParserOutput = parser
					.parse(dataItem.getSample(), pruner, dataItemModel, true,
							new Lexicon<LogicalExpression>(
									dataItem.getEntries()));

			if (pruner == null) {
				if (!sloppyParserOutput.isExact()) {
					LOG.warn("WARNING: inference is not exact.");
				}
			} else {
				LOG.info("Parse is pruned.");
			}

			LOG.info("Sloppy parse time: %fsec",
					sloppyParserOutput.getParsingTime() / 1000.0);
			LOG.info("Generated %d sloppy parses",
					sloppyParserOutput.getAllDerivations().size());

			final List<? extends IDerivation<LogicalExpression>> sloppyCorrectParses = sloppyParserOutput
					.getDerivations(filter);
			if (correctParses.isEmpty()) {
				LOG.info("No sloppy correct parses");
			} else {
				LOG.info("Generated %d sloppy correct parses",
						sloppyCorrectParses.size());
				reportCorrectParses(sloppyCorrectParses, dataItem.getLabel());
				numParsedSuccessfulySloppy++;
			}

			if (reportBad) {
				reportBadParse(sloppyParserOutput.getAllDerivations());
			}

		}
	}

	private void reportBadParse(
			List<? extends IDerivation<LogicalExpression>> badParses) {

		final Iterator<? extends IDerivation<LogicalExpression>> iterator = badParses
				.iterator();
		while (iterator.hasNext()) {
			LOG.info("Bad parse logical form:");
			LOG.info(prettyPrinter.toString(iterator.next().getSemantics()));
			if (iterator.hasNext()) {
				LOG.info("------------------");
			}
		}
	}

	private void reportCorrectParses(
			List<? extends IDerivation<LogicalExpression>> correctParses,
			LogicalExpression label) {
		final Iterator<? extends IDerivation<LogicalExpression>> iterator = correctParses
				.iterator();
		LOG.info("------------------");
		while (iterator.hasNext()) {
			final IDerivation<LogicalExpression> parse = iterator.next();
			LOG.info("Correct parse details:");
			if (!label.equals(parse.getSemantics())) {
				LOG.info(prettyPrinter.toString(parse.getSemantics()));
			}
			LOG.info("%d parses", parse.numParses());
			LOG.info("Lexical entries used in max-scoring derivations:");
			for (final LexicalEntry<LogicalExpression> entry : parse
					.getMaxLexicalEntries()) {
				LOG.info(entry);
			}
			LOG.info("Rules used in max-scoring derivations: %s",
					ListUtils.join(parse.getMaxRulesUsed(), ", "));
			if (iterator.hasNext()) {
				LOG.info("------------------");
			}
		}
	}

	public static class ParseUtilResourceCreatorRepository
			extends ResourceCreatorRepository {

		public ParseUtilResourceCreatorRepository() {
			registerResourceCreator(new SingleSentenceLexDataset.Creator());
			registerResourceCreator(
					new ApplicationCreator<LogicalExpression>());
			registerResourceCreator(
					new CompositionCreator<LogicalExpression>());
			registerResourceCreator(new PrepositionTypeShifting.Creator());
			registerResourceCreator(new AdverbialTypeShifting.Creator());
			registerResourceCreator(new AdjectiveTypeShifting.Creator());
			registerResourceCreator(
					new AdverbialTopicalisationTypeShifting.Creator());
			registerResourceCreator(
					new ApplicationTypeShifting.Creator<LogicalExpression>());
			registerResourceCreator(new ForwardTypeRaisedComposition.Creator());
			registerResourceCreator(new ThatlessRelative.Creator());
			registerResourceCreator(
					new PluralExistentialTypeShifting.Creator());
			registerResourceCreator(
					new MultiCKYParser.Creator<Sentence, LogicalExpression>());
			registerResourceCreator(new SimpleFullParseFilter.Creator());
			registerResourceCreator(new StructureOnlyComaprator.Creator());
			registerResourceCreator(
					new SkolemIdInsensitiveComparator.Creator());
			registerResourceCreator(new EisnerNormalFormCreator());
			registerResourceCreator(new HBNormalFormCreator());
		}

	}
}
