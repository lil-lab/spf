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
package edu.uw.cs.lil.tiny.parser.ccg.joint.genlex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.IParseResult;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.FactoredLexicon;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.FactoredLexicon.FactoredLexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.Lexeme;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.LexicalTemplate;
import edu.uw.cs.lil.tiny.parser.ccg.genlex.ILexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointParse;
import edu.uw.cs.lil.tiny.parser.joint.IJointParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointModelImmutable;
import edu.uw.cs.lil.tiny.parser.joint.model.JointDataItemWrapper;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lexicon generator that uses a parser to do initial filtering of generated
 * lexical entries. The generation process is based on abstract lexical entries,
 * which is basically a set of all templates initialized with abstract
 * constants. Abstract constants have the most basic types only. The generation
 * process starts with generating all lexemes for the input sentence using
 * abstract constants. The set of abstract lexemes is combined into a factored
 * lexicon with all templates. Then the sentence is parsed using the current
 * model with the abstract temporary lexicon. This parse is not accurate
 * according to the model, since the model is unfamiliar the any of the abstract
 * constants and can't generate features over them. From this approximate parse,
 * all GENLEX entries that participate in complete parses that score higher that
 * the best pre-generation valid parse are collected. Using their tokens and
 * templates, a new lexicon is generated using all possible constants (from the
 * ontology). This lexicon is returned.
 * 
 * @author Yoav Artzi
 */
public class JointTemplatedAbstractLexiconGenerator<STATE, ESTEP, ERESULT>
		implements
		ILexiconGenerator<JointDataItemWrapper<Sentence, STATE>, LogicalExpression> {
	private static final ILogger														LOG	= LoggerFactory
																									.create(JointTemplatedAbstractLexiconGenerator.class);
	
	private final Set<List<LogicalConstant>>											abstractConstantSeqs;
	private final IParser<Sentence, LogicalExpression>									baseParser;
	private final int																	generationParsingBeam;
	private final IJointParser<Sentence, STATE, LogicalExpression, ESTEP, ERESULT>		jointParser;
	private final double																margin;
	private final int																	maxTokens;
	
	private final IJointModelImmutable<Sentence, STATE, LogicalExpression, ESTEP>		model;
	
	private final Set<Pair<List<Type>, List<LogicalConstant>>>							potentialConstantSeqs;
	private final Set<LexicalTemplate>													templates;
	private final IValidator<Pair<Sentence, STATE>, Pair<LogicalExpression, ERESULT>>	validator;
	
	protected JointTemplatedAbstractLexiconGenerator(
			Set<LexicalTemplate> templates,
			Set<Pair<List<Type>, List<LogicalConstant>>> pontetialConstantSeqs,
			Set<List<LogicalConstant>> abstractConstantSeqs,
			int maxTokens,
			IJointModelImmutable<Sentence, STATE, LogicalExpression, ESTEP> model,
			IParser<Sentence, LogicalExpression> baseParser,
			int generationParsingBeam,
			IJointParser<Sentence, STATE, LogicalExpression, ESTEP, ERESULT> jointParser,
			IValidator<Pair<Sentence, STATE>, Pair<LogicalExpression, ERESULT>> validator,
			double margin) {
		this.potentialConstantSeqs = pontetialConstantSeqs;
		this.abstractConstantSeqs = abstractConstantSeqs;
		this.model = model;
		this.baseParser = baseParser;
		this.generationParsingBeam = generationParsingBeam;
		this.jointParser = jointParser;
		this.validator = validator;
		this.margin = margin;
		this.templates = Collections.unmodifiableSet(templates);
		this.maxTokens = maxTokens;
		LOG.info("Init %s", this.getClass().getName());
		LOG.info("margin=%f", margin);
		LOG.info("num abstract constant seqs=%d", abstractConstantSeqs.size());
		LOG.info("num constant seqs=%d", pontetialConstantSeqs.size());
		LOG.info("num templates=%d", templates.size());
	}
	
	@Override
	public ILexicon<LogicalExpression> generate(
			JointDataItemWrapper<Sentence, STATE> dataItem) {
		final List<String> tokens = dataItem.getSample().getTokens();
		final int numTokens = tokens.size();
		
		// Pre-generation joint parse to get the score of the best valid joint
		// parse
		final IJointOutput<LogicalExpression, ERESULT> preModelParseOutput = jointParser
				.parse(dataItem.getBaseDataItem(), model
						.createJointDataItemModel(dataItem.getBaseDataItem()));
		Double bestValidScore = null;
		for (final IJointParse<LogicalExpression, ERESULT> parse : preModelParseOutput
				.getAllJointParses()) {
			if (validator
					.isValid(dataItem.getBaseDataItem(), parse.getResult())) {
				if (bestValidScore == null
						|| parse.getBaseScore() > bestValidScore) {
					bestValidScore = parse.getBaseScore();
				}
			}
		}
		LOG.debug("Best valid score: %s", bestValidScore);
		final Double thresholdScore = bestValidScore == null ? null
				: (bestValidScore + margin);
		
		// Create all possible lexemes using abstract lists for all spans of
		// tokens up to the limit
		final Set<Lexeme> abstractLexemes = new HashSet<Lexeme>();
		for (int i = 0; i < numTokens; ++i) {
			for (int j = i; j < numTokens && j - i + 1 < maxTokens; ++j) {
				for (final List<LogicalConstant> constants : abstractConstantSeqs) {
					abstractLexemes
							.add(new Lexeme(tokens.subList(i, j + 1),
									constants,
									ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
				}
			}
		}
		
		// Abstract factored lexicon
		final FactoredLexicon abstractLexicon = new FactoredLexicon(
				abstractLexemes, templates,
				ILexiconGenerator.GENLEX_LEXICAL_ORIGIN);
		
		// Parse with abstract constants
		final IParserOutput<LogicalExpression> parserOutput = baseParser.parse(
				dataItem, model.createDataItemModel(dataItem), false,
				abstractLexicon, generationParsingBeam);
		
		LOG.debug("Abstract parsing for lexicon generation completed, %.4fsec",
				parserOutput.getParsingTime() / 1000.0);
		LOG.debug("Generated %d abstract parses", parserOutput.getAllParses()
				.size());
		
		// Remove parses with a score lower or equal to the best valid
		// pre-generation parse
		final LinkedList<IParseResult<LogicalExpression>> filteredParses = new LinkedList<IParseResult<LogicalExpression>>(
				parserOutput.getAllParses());
		if (thresholdScore != null && !filteredParses.isEmpty()) {
			CollectionUtils.filterInPlace(filteredParses,
					new IFilter<IParseResult<LogicalExpression>>() {
						
						@Override
						public boolean isValid(IParseResult<LogicalExpression> e) {
							LOG.debug(
									"Abstract parse score: %f, threshold score: %f%s",
									e.getScore(),
									thresholdScore,
									e.getScore() > thresholdScore ? " --> passed"
											: "");
							return e.getScore() > thresholdScore;
						}
					});
			
			LOG.debug(
					"Filtered %d abstract parses based on margin of %f from %f, %d abstract parses left",
					parserOutput.getAllParses().size() - filteredParses.size(),
					margin, bestValidScore, filteredParses.size());
			
		}
		
		if (filteredParses.isEmpty()) {
			LOG.info("No abstract parses for lexical generation");
			return new Lexicon<LogicalExpression>();
		} else {
			LOG.info("%d abstract parses for lexical generation",
					filteredParses.size());
		}
		
		// Collect: (a) all lexical templates from GENLEX entries from all
		// complete parses and (b) all spans of words used in GENLEX entries for
		// each template
		final Map<List<Type>, Set<Pair<LexicalTemplate, List<String>>>> usedTemplatesAndTokens = new HashMap<List<Type>, Set<Pair<LexicalTemplate, List<String>>>>();
		int counter = 0;
		for (final IParseResult<LogicalExpression> parse : filteredParses) {
			for (final LexicalEntry<LogicalExpression> entry : parse
					.getMaxLexicalEntries()) {
				if (entry.getOrigin().equals(
						ILexiconGenerator.GENLEX_LEXICAL_ORIGIN)) {
					final FactoredLexicalEntry factored = FactoredLexicon
							.factor(entry);
					if (!usedTemplatesAndTokens.containsKey(factored
							.getTemplate().getTypeSignature())) {
						usedTemplatesAndTokens
								.put(factored.getTemplate().getTypeSignature(),
										new HashSet<Pair<LexicalTemplate, List<String>>>());
					}
					final Set<Pair<LexicalTemplate, List<String>>> pairs = usedTemplatesAndTokens
							.get(factored.getTemplate().getTypeSignature());
					if (pairs.add(Pair.of(factored.getTemplate(),
							entry.getTokens()))) {
						++counter;
					}
				}
			}
		}
		
		LOG.info("Lexicon generation, %d template-token pairs", counter);
		
		// Create lexemes using the tokens that were used with GENLEX entries
		// during the abstract parse
		
		// Create lexical entries from the tokens, the potential sequences and
		// used templates. Only add entries that are missing from the model.
		final Lexicon<LogicalExpression> lexicon = new Lexicon<LogicalExpression>();
		for (final Pair<List<Type>, List<LogicalConstant>> seqPair : potentialConstantSeqs) {
			if (usedTemplatesAndTokens.containsKey(seqPair.first())) {
				for (final Pair<LexicalTemplate, List<String>> pair : usedTemplatesAndTokens
						.get(seqPair.first())) {
					final Lexeme lexeme = new Lexeme(pair.second(),
							seqPair.second(),
							ILexiconGenerator.GENLEX_LEXICAL_ORIGIN);
					final Category<LogicalExpression> category = pair.first()
							.makeCategory(lexeme);
					if (category != null) {
						final LexicalEntry<LogicalExpression> entry = FactoredLexicon
								.factor(new LexicalEntry<LogicalExpression>(
										lexeme.getTokens(), category,
										ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
						if (!model.getLexicon().contains(entry)) {
							lexicon.add(entry);
						}
					}
				}
			}
		}
		
		return lexicon;
	}
	
	public static class Builder<STATE, ESTEP, ERESULT> {
		private static final String															CONST_SEED_NAME	= "absconst";
		
		protected final IParser<Sentence, LogicalExpression>								baseParser;
		protected final Set<LogicalConstant>												constants		= new HashSet<LogicalConstant>();
		protected final int																	generationParsingBeam;
		protected final IJointParser<Sentence, STATE, LogicalExpression, ESTEP, ERESULT>	jointParser;
		
		protected double																	margin			= 0.0;
		
		protected final int																	maxTokens;
		
		protected final IJointModelImmutable<Sentence, STATE, LogicalExpression, ESTEP>		model;
		
		protected final Set<LexicalTemplate>												templates		= new HashSet<LexicalTemplate>();
		
		protected final IValidator<Pair<Sentence, STATE>, Pair<LogicalExpression, ERESULT>>	validator;
		
		public Builder(
				int maxTokens,
				IJointModelImmutable<Sentence, STATE, LogicalExpression, ESTEP> model,
				IParser<Sentence, LogicalExpression> parser,
				int generationParsingBeam,
				IJointParser<Sentence, STATE, LogicalExpression, ESTEP, ERESULT> jointParser,
				IValidator<Pair<Sentence, STATE>, Pair<LogicalExpression, ERESULT>> validator) {
			this.maxTokens = maxTokens;
			this.model = model;
			this.baseParser = parser;
			this.generationParsingBeam = generationParsingBeam;
			this.jointParser = jointParser;
			this.validator = validator;
		}
		
		private static LogicalConstant createConstant(Type type) {
			return LogicalConstant.create(
					LogicalConstant.makeName(
							String.format("%s", CONST_SEED_NAME), type), type);
		}
		
		public Builder<STATE, ESTEP, ERESULT> addConstants(
				Iterable<LogicalConstant> constantCollection) {
			for (final LogicalConstant constant : constantCollection) {
				constants.add(constant);
			}
			return this;
		}
		
		public Builder<STATE, ESTEP, ERESULT> addTemplate(
				LexicalTemplate template) {
			templates.add(template);
			return this;
		}
		
		public Builder<STATE, ESTEP, ERESULT> addTemplates(
				Iterable<LexicalTemplate> templateCollection) {
			for (final LexicalTemplate template : templateCollection) {
				addTemplate(template);
			}
			return this;
		}
		
		public Builder<STATE, ESTEP, ERESULT> addTemplatesFromModel(
				IModelImmutable<?, LogicalExpression> sourceModel) {
			final Collection<LexicalEntry<LogicalExpression>> lexicalEntries = sourceModel
					.getLexicon().toCollection();
			for (final LexicalEntry<LogicalExpression> entry : lexicalEntries) {
				final FactoredLexicalEntry factored = FactoredLexicon
						.factor(entry);
				addTemplate(factored.getTemplate());
			}
			return this;
		}
		
		public JointTemplatedAbstractLexiconGenerator<STATE, ESTEP, ERESULT> build() {
			return new JointTemplatedAbstractLexiconGenerator<STATE, ESTEP, ERESULT>(
					templates, createPotentialLists(), createAbstractLists(),
					maxTokens, model, baseParser, generationParsingBeam,
					jointParser, validator, margin);
		}
		
		public Builder<STATE, ESTEP, ERESULT> setMargin(double margin) {
			this.margin = margin;
			return this;
		}
		
		protected Set<List<LogicalConstant>> createAbstractLists() {
			// Collect all type signatures
			final Set<List<Type>> typeSignatures = new HashSet<List<Type>>();
			for (final LexicalTemplate template : templates) {
				typeSignatures.add(template.getTypeSignature());
			}
			
			// Iterate over type signatures, for each one create the list of
			// abstract constants that will satisfy it
			final Set<List<LogicalConstant>> abstractLists = new HashSet<List<LogicalConstant>>();
			for (final List<Type> typeSignature : typeSignatures) {
				final List<LogicalConstant> abstractConstants = new ArrayList<LogicalConstant>(
						typeSignature.size());
				for (final Type type : typeSignature) {
					// Create the abstract constant, each one has a unique name,
					// controlled by the number
					abstractConstants.add(createConstant(type));
				}
				abstractLists.add(abstractConstants);
			}
			
			return Collections.unmodifiableSet(abstractLists);
		}
		
		protected Set<Pair<List<Type>, List<LogicalConstant>>> createPotentialLists() {
			// Collect all type signatures
			final Set<List<Type>> typeSignatures = new HashSet<List<Type>>();
			for (final LexicalTemplate template : templates) {
				typeSignatures.add(template.getTypeSignature());
			}
			
			// Iterate over type signatures, for each one create all possible
			// combinations of constants that will satisfy it
			final List<Pair<List<Type>, List<LogicalConstant>>> potentialConstantSeqs = new LinkedList<Pair<List<Type>, List<LogicalConstant>>>();
			
			// Cache logical constants for each type
			final Map<Type, Set<LogicalConstant>> constsCache = new HashMap<Type, Set<LogicalConstant>>();
			
			for (final List<Type> typeSignature : typeSignatures) {
				final List<Set<LogicalConstant>> setsOfConsts = new ArrayList<Set<LogicalConstant>>(
						typeSignature.size());
				for (final Type type : typeSignature) {
					if (!constsCache.containsKey(type)) {
						final Set<LogicalConstant> consts = new HashSet<LogicalConstant>();
						for (final LogicalConstant constant : constants) {
							if (LogicLanguageServices.getTypeRepository()
									.generalizeType(constant.getType())
									.equals(type)) {
								consts.add(constant);
							}
						}
						constsCache.put(type, consts);
					}
					setsOfConsts.add(constsCache.get(type));
				}
				for (final List<LogicalConstant> constantsList : CollectionUtils
						.cartesianProduct(setsOfConsts)) {
					potentialConstantSeqs.add(Pair.of(
							Lexeme.getSignature(constantsList),
							Collections.unmodifiableList(constantsList)));
				}
			}
			
			// Add the empty list of constants, which will be used to init
			// lexical templates that take no arguments
			potentialConstantSeqs.add(Pair.of(Collections
					.unmodifiableList(new ArrayList<Type>(0)), Collections
					.unmodifiableList(new ArrayList<LogicalConstant>(0))));
			
			return Collections
					.unmodifiableSet(new HashSet<Pair<List<Type>, List<LogicalConstant>>>(
							potentialConstantSeqs));
		}
	}
}
