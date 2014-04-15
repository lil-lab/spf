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
package edu.uw.cs.lil.tiny.genlex.ccg.template.coarse;

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
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon.FactoredLexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.Lexeme;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.counter.Counter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lexicon generator that uses coarse ontology to prune the space of potential
 * new lexical entries. To do so, the generator first parses the current
 * sentence with the current model and a set of coarse lexical entries. The
 * coarse entries are created by combining a set of pre-defined templates with a
 * coarse ontology (see the ACL 2013 tutorial for details on coarse ontologies).
 * Coarse entries used in the generated parses are then used to initialize
 * fine-grained lexical entries using all the constants in the original
 * ontologies. These entries are then returned by the generator.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Data item for generation.
 */
public class TemplateCoarseGenlex<DI extends Sentence>
		implements
		ILexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>> {
	public static final ILogger									LOG	= LoggerFactory
																			.create(TemplateCoarseGenlex.class);
	
	private final Set<List<LogicalConstant>>					abstractConstantSeqs;
	private final int											maxTokens;
	private final IParser<Sentence, LogicalExpression>			parser;
	private final int											parsingBeam;
	private final Set<Pair<List<Type>, List<LogicalConstant>>>	potentialConstantSeqs;
	private final Set<LexicalTemplate>							templates;
	
	protected TemplateCoarseGenlex(Set<LexicalTemplate> templates,
			Set<Pair<List<Type>, List<LogicalConstant>>> pontetialConstantSeqs,
			Set<List<LogicalConstant>> abstractConstantSeqs, int maxTokens,
			IParser<Sentence, LogicalExpression> parser, int parsingBeam) {
		this.potentialConstantSeqs = pontetialConstantSeqs;
		this.abstractConstantSeqs = abstractConstantSeqs;
		this.parser = parser;
		this.parsingBeam = parsingBeam;
		this.templates = Collections.unmodifiableSet(templates);
		this.maxTokens = maxTokens;
		LOG.info(
				"Init %s :: maxTokens=%d, size(templates)=%d, parsingBeam=%d ...",
				this.getClass().getSimpleName(), maxTokens, templates.size(),
				parsingBeam);
		LOG.info(
				"Init %s :: ... size(abstractConstantsSeqs)=%d, size(potentialConstantSeqs)=%d",
				this.getClass().getSimpleName(), abstractConstantSeqs.size(),
				pontetialConstantSeqs.size());
	}
	
	@Override
	public ILexicon<LogicalExpression> generate(DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		final List<String> tokens = dataItem.getTokens();
		final int numTokens = tokens.size();
		
		// Create all possible lexemes using abstract lists for all spans of
		// tokens up to the limit
		final Set<Lexeme> abstractLexemes = new HashSet<Lexeme>();
		for (int i = 0; i < numTokens; ++i) {
			for (int j = i; j < numTokens && j - i + 1 < maxTokens; ++j) {
				for (final List<LogicalConstant> constants : abstractConstantSeqs) {
					abstractLexemes.add(new Lexeme(CollectionUtils.subList(
							tokens, i, j + 1), constants,
							ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
				}
			}
		}
		
		// Abstract factored lexicon
		final FactoredLexicon abstractLexicon = new FactoredLexicon(
				abstractLexemes, templates,
				ILexiconGenerator.GENLEX_LEXICAL_ORIGIN);
		
		// Parse with abstract constants
		final IParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem, model.createDataItemModel(dataItem), false,
				abstractLexicon, parsingBeam);
		
		LOG.debug("Abstract parse for lexicon generation completed, %.4fsec",
				parserOutput.getParsingTime() / 1000.0);
		LOG.debug("Generated %d abstract parses", parserOutput.getAllParses()
				.size());
		
		// Collect: (a) all lexical templates from GENLEX entries from all
		// complete parses and (b) all spans of words used in GENLEX entries for
		// each template
		final Map<List<Type>, Set<Pair<LexicalTemplate, List<String>>>> usedTemplatesAndTokens = new HashMap<List<Type>, Set<Pair<LexicalTemplate, List<String>>>>();
		int counter = 0;
		for (final IDerivation<LogicalExpression> parse : parserOutput
				.getAllParses()) {
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
		
		LOG.debug("Lexicon generation, %d template-token pairs:", counter);
		LOG.debug(new Runnable() {
			
			@Override
			public void run() {
				for (final Set<Pair<LexicalTemplate, List<String>>> pairs : usedTemplatesAndTokens
						.values()) {
					for (final Pair<LexicalTemplate, List<String>> pair : pairs) {
						LOG.debug("%s -> %s", pair.first(),
								ListUtils.join(pair.second(), " "));
					}
				}
			}
		});
		
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
		
		LOG.debug("%d lexical entries generated", lexicon.size());
		LOG.debug(new Runnable() {
			
			@Override
			public void run() {
				for (final LexicalEntry<LogicalExpression> entry : lexicon
						.toCollection()) {
					LOG.info(entry);
				}
			}
		});
		
		return lexicon;
	}
	
	public static class Builder<DI extends Sentence> {
		private static final String								CONST_SEED_NAME	= "absconst";
		
		protected final Set<LogicalConstant>					constants		= new HashSet<LogicalConstant>();
		protected final int										maxTokens;
		protected final IParser<Sentence, LogicalExpression>	parser;
		
		protected final int										parsingBeam;
		
		protected final Set<LexicalTemplate>					templates		= new HashSet<LexicalTemplate>();
		
		public Builder(int maxTokens,
				IParser<Sentence, LogicalExpression> parser, int parsingBeam) {
			this.maxTokens = maxTokens;
			this.parser = parser;
			this.parsingBeam = parsingBeam;
		}
		
		private static LogicalConstant createConstant(int num, Type type) {
			return LogicalConstant
					.createDynamic(
							LogicalConstant.makeName(String.format("%s_%d",
									CONST_SEED_NAME, num), type), type);
		}
		
		public Builder<DI> addConstants(
				Iterable<LogicalConstant> constantCollection) {
			for (final LogicalConstant constant : constantCollection) {
				constants.add(constant);
			}
			return this;
		}
		
		public Builder<DI> addTemplate(LexicalTemplate template) {
			templates.add(template);
			return this;
		}
		
		public Builder<DI> addTemplates(
				Iterable<LexicalTemplate> templateCollection) {
			for (final LexicalTemplate template : templateCollection) {
				addTemplate(template);
			}
			return this;
		}
		
		public Builder<DI> addTemplatesFromLexicon(
				ILexicon<LogicalExpression> lexicon) {
			final Collection<LexicalEntry<LogicalExpression>> lexicalEntries = lexicon
					.toCollection();
			for (final LexicalEntry<LogicalExpression> entry : lexicalEntries) {
				final FactoredLexicalEntry factored = FactoredLexicon
						.factor(entry);
				addTemplate(factored.getTemplate());
			}
			return this;
		}
		
		public Builder<DI> addTemplatesFromModel(
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
		
		public TemplateCoarseGenlex<DI> build() {
			return new TemplateCoarseGenlex<DI>(templates,
					createPotentialLists(), createAbstractLists(), maxTokens,
					parser, parsingBeam);
		}
		
		protected Set<List<LogicalConstant>> createAbstractLists() {
			// Collect all type signatures
			final Set<List<Type>> typeSignatures = new HashSet<List<Type>>();
			for (final LexicalTemplate template : templates) {
				typeSignatures.add(template.getTypeSignature());
			}
			
			// Abstract constants counters
			final Map<Type, Counter> typeCounters = new HashMap<Type, Counter>();
			
			// Iterate over type signatures, for each one create the list of
			// abstract constants that will satisfy it
			final Set<List<LogicalConstant>> abstractLists = new HashSet<List<LogicalConstant>>();
			for (final List<Type> typeSignature : typeSignatures) {
				final List<LogicalConstant> abstractConstants = new ArrayList<LogicalConstant>(
						typeSignature.size());
				for (final Type type : typeSignature) {
					if (!typeCounters.containsKey(type)) {
						typeCounters.put(type, new Counter());
					}
					
					// Create the abstract constant, each one has a unique name,
					// controlled by the number
					abstractConstants.add(createConstant(typeCounters.get(type)
							.value(), type));
					
					// Increase the counter
					typeCounters.get(type).inc();
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
	
	public static class Creator<DI extends Sentence> implements
			IResourceObjectCreator<TemplateCoarseGenlex<DI>> {
		
		private final String	type;
		
		public Creator() {
			this("genlex.template.coarse");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public TemplateCoarseGenlex<DI> create(Parameters params,
				IResourceRepository repo) {
			final Builder<DI> builder = new Builder<DI>(
					params.getAsInteger("maxTokens"),
					(IParser<Sentence, LogicalExpression>) repo
							.getResource(params.get("parser")),
					params.getAsInteger("beam"));
			
			builder.addConstants((Iterable<LogicalConstant>) repo
					.getResource(params.get("ontology")));
			
			if (params.contains("templatesModel")) {
				builder.addTemplatesFromModel((IModelImmutable<?, LogicalExpression>) repo
						.getResource(params.get("model")));
			} else if (params.contains("lexicon")) {
				builder.addTemplatesFromLexicon((ILexicon<LogicalExpression>) repo
						.getResource(params.get("lexicon")));
			} else {
				throw new IllegalStateException("no templates source specified");
			}
			
			return builder.build();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, TemplateCoarseGenlex.class)
					.addParam("maxTokens", Integer.class,
							"Max number of tokens to include in lexical entries.")
					.addParam("parser", IParser.class,
							"Parser to use for coarse parsing.")
					.addParam("beam", Integer.class, "Beam for parsing.")
					.addParam("ontology", Set.class,
							"Collection of logical constants to initialize templates.")
					.addParam("templatesModel", IModelImmutable.class,
							"Model to extract templates from.")
					.addParam("lexicon", ILexiconImmutable.class,
							"Lexicon to extract templates from.").build();
		}
		
	}
}
