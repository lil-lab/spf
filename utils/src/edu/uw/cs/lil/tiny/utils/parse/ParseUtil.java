package edu.uw.cs.lil.tiny.utils.parse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.base.concurrency.ITinyExecutor;
import edu.uw.cs.lil.tiny.base.concurrency.TinyExecutorService;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory.Type;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.lex.SingleSentenceLex;
import edu.uw.cs.lil.tiny.data.singlesentence.lex.SingleSentenceLexDataset;
import edu.uw.cs.lil.tiny.explat.LoggedExperiment;
import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.ILogicalExpressionComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.SimpleFullParseFilter;
import edu.uw.cs.lil.tiny.mr.lambda.comparators.StructureOnlyComaprator;
import edu.uw.cs.lil.tiny.mr.lambda.printers.ILogicalExpressionPrinter;
import edu.uw.cs.lil.tiny.mr.lambda.printers.LogicalExpressionToIndentedString;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.IParsingFilterFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.cky.multi.MultiCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model.Builder;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.PluralExistentialTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.ThatlessRelative;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.AdjectiveTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.AdverbialTopicalisationTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.AdverbialTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.PrepositionTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.ApplicationCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition.CompositionCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.SkippingRuleCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting.ApplicationTypeShifting;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.Log;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;

/**
 * Utility to parse a sentence given a small lexicon and compute various
 * statistics (e.g., number of parses, oracle correctness, etc.).
 * 
 * @author Yoav Artzi
 */
public class ParseUtil extends LoggedExperiment {
	
	private final ICategoryServices<LogicalExpression>	categoryServices;
	private final ILogicalExpressionComparator			comparator;
	private final ILogicalExpressionPrinter				prettyPrinter	= new LogicalExpressionToIndentedString.Printer(
																				"  ");
	private final boolean								reportBad;
	private final boolean								verbose;
	
	@SuppressWarnings("unchecked")
	private ParseUtil(Reader reader, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo, File rootDir,
			List<String> files, ITinyExecutor executor) throws IOException {
		super(reader, envParams, creatorRepo, rootDir);
		
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
		// Executor resource.
		// //////////////////////////////////////////
		
		storeResource(EXECUTOR_RESOURCE, executor);
		
		// //////////////////////////////////////////
		// Init lambda calculus system.
		// //////////////////////////////////////////
		
		try {
			// Get types files, if defined.
			final LogicLanguageServices.Builder builder = new LogicLanguageServices.Builder(
					globalParams.contains("types") ? new TypeRepository(
							globalParams.getAsFile("types"))
							: new TypeRepository(),
					new FlexibleTypeComparator());
			
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
		
		// //////////////////////////////////////////////////
		// Category services for logical expressions.
		// //////////////////////////////////////////////////
		
		this.categoryServices = new LogicalExpressionCategoryServices(true,
				true, globalParams.getAsBoolean("restrictCompositionDirection",
						true));
		storeResource(CATEGORY_SERVICES_RESOURCE, categoryServices);
		
		// //////////////////////////////////////////////////
		// Read resources.
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
		// Get comparator for parse result.
		// //////////////////////////////////////////////////
		
		if (hasResource("comparator")) {
			this.comparator = getResource("comparator");
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
					.setLexicon((ILexicon<LogicalExpression>) getResource("lexicon"));
		}
		final Model<Sentence, LogicalExpression> model = modelBuilder.build();
		final IParser<Sentence, LogicalExpression> parser = getResource(PARSER_RESOURCE);
		
		final IParsingFilterFactory<SingleSentenceLex, LogicalExpression> filterFactory;
		if (hasResource("filterFactory")) {
			filterFactory = getResource("filterFactory");
		} else {
			filterFactory = null;
		}
		
		for (final String file : files) {
			final SingleSentenceLexDataset dataset = SingleSentenceLexDataset
					.read(new File(file), new StubStringFilter(),
							categoryServices, "seed");
			for (final SingleSentenceLex dataItem : dataset) {
				LOG.info("==========================");
				processSentence(
						dataItem,
						parser,
						model,
						globalParams.getAsBoolean("sloppy"),
						filterFactory == null ? null : filterFactory
								.create(dataItem));
			}
		}
	}
	
	public static void main(String[] args) {
		main(args, new ParseUtilResourceCreatorRepository());
	}
	
	public static void main(String[] args, ResourceCreatorRepository creatorRepo) {
		if (args.length == 0) {
			usage();
			return;
		}
		
		// Executor service.
		final TinyExecutorService executor = new TinyExecutorService(Runtime
				.getRuntime().availableProcessors());
		
		try {
			final String[] files = new String[args.length - 1];
			System.arraycopy(args, 1, files, 0, files.length);
			
			final File expFile = new File(args[0]);
			
			// Set some mandatory properties.
			final HashMap<String, String> envParams = new HashMap<String, String>();
			envParams.put("outputDir", ".");
			
			// Create the experiment and run it.
			new ParseUtil(new FileReader(args[0]), envParams, creatorRepo,
					expFile.getParentFile() == null ? new File(".") : expFile
							.getParentFile(), Arrays.asList(files),
					executor).end();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			executor.shutdownNow();
		}
	}
	
	private static void usage() {
		System.out.println(String.format(
				"Usage: ... <exp_file> <data_1> <data_2> <data_3> ... ",
				ParseUtil.class.getSimpleName()));
		System.out
				.println(String
						.format("\t<exp_file>\tA explat file to define certain utility parameters and the parser to use."));
		System.out
				.println("\t<data_n>\tList of single sentence with lexicon data files to use. ");
	}
	
	private void processSentence(final SingleSentenceLex dataItem,
			IParser<Sentence, LogicalExpression> parser,
			Model<Sentence, LogicalExpression> model, boolean allowSloppy,
			IFilter<LogicalExpression> pruner) {
		LOG.info("Processing: %s", dataItem.getSample());
		LOG.info(prettyPrinter.toString(dataItem.getLabel()));
		LOG.info("Data item lexicon has %d entries", dataItem.getEntries()
				.size());
		
		// Create data item model.
		final IDataItemModel<LogicalExpression> dataItemModel = model
				.createDataItemModel(dataItem.getSample());
		
		// Parse sentence.
		final IParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem.getSample(), pruner, dataItemModel, false,
				new Lexicon<LogicalExpression>(dataItem.getEntries()));
		
		// Create filter to detect gold parse.
		final IFilter<LogicalExpression> filter = new IFilter<LogicalExpression>() {
			
			@Override
			public boolean isValid(LogicalExpression e) {
				return comparator.compare(e, dataItem.getLabel());
			}
		};
		
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
		LOG.info("Generated %d parses", parserOutput.getAllParses().size());
		
		final List<? extends IDerivation<LogicalExpression>> correctParses = parserOutput
				.getParses(filter);
		if (correctParses.isEmpty()) {
			LOG.info("No correct parses");
		} else {
			LOG.info("Generated %d correct parses", correctParses.size());
			reportCorrectParses(correctParses, dataItem.getLabel());
		}
		
		if (reportBad) {
			reportBadParse(parserOutput.getAllParses());
		}
		
		if (allowSloppy && correctParses.isEmpty()) {
			LOG.info("------------------");
			LOG.info("Sloppy parse: trying with word skipping");
			// Parse sentence.
			final IParserOutput<LogicalExpression> sloppyParserOutput = parser
					.parse(dataItem.getSample(),
							pruner,
							dataItemModel,
							true,
							new Lexicon<LogicalExpression>(dataItem
									.getEntries()));
			
			if (pruner == null) {
				if (!sloppyParserOutput.isExact()) {
					LOG.warn("WARNING: inference is not exact.");
				}
			} else {
				LOG.info("Parse is pruned.");
			}
			
			LOG.info("Sloppy parse time: %fsec",
					sloppyParserOutput.getParsingTime() / 1000.0);
			LOG.info("Generated %d sloppy parses", sloppyParserOutput
					.getAllParses().size());
			
			final List<? extends IDerivation<LogicalExpression>> sloppyCorrectParses = sloppyParserOutput
					.getParses(filter);
			if (correctParses.isEmpty()) {
				LOG.info("No sloppy correct parses");
			} else {
				LOG.info("Generated %d sloppy correct parses",
						sloppyCorrectParses.size());
				reportCorrectParses(sloppyCorrectParses, dataItem.getLabel());
			}
			
			if (reportBad) {
				reportBadParse(sloppyParserOutput.getAllParses());
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
	
	public static class ParseUtilResourceCreatorRepository extends
			ResourceCreatorRepository {
		
		public ParseUtilResourceCreatorRepository() {
			registerResourceCreator(new SingleSentenceLexDataset.Creator());
			registerResourceCreator(new ApplicationCreator<LogicalExpression>());
			registerResourceCreator(new CompositionCreator<LogicalExpression>());
			registerResourceCreator(new PrepositionTypeShifting.Creator());
			registerResourceCreator(new AdverbialTypeShifting.Creator());
			registerResourceCreator(new AdjectiveTypeShifting.Creator());
			registerResourceCreator(new AdverbialTopicalisationTypeShifting.Creator());
			registerResourceCreator(new ApplicationTypeShifting.Creator<LogicalExpression>());
			registerResourceCreator(new SkippingRuleCreator<LogicalExpression>());
			registerResourceCreator(new ForwardTypeRaisedComposition.Creator());
			registerResourceCreator(new ThatlessRelative.Creator());
			registerResourceCreator(new PluralExistentialTypeShifting.Creator());
			registerResourceCreator(new MultiCKYParser.Creator<LogicalExpression>());
			registerResourceCreator(new SimpleFullParseFilter.Creator());
			registerResourceCreator(new StructureOnlyComaprator.Creator());
		}
		
	}
}
