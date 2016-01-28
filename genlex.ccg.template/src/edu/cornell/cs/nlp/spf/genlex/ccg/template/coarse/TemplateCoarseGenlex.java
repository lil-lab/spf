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
package edu.cornell.cs.nlp.spf.genlex.ccg.template.coarse;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.AbstractLexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.template.GenerationRepository;
import edu.cornell.cs.nlp.spf.genlex.ccg.template.GenerationRepositoryWithConstants;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IParser;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelListener;
import edu.cornell.cs.nlp.utils.composites.Triplet;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

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
public class TemplateCoarseGenlex<DI extends Sentence> extends
		AbstractLexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>>
		implements IModelListener<LogicalExpression> {
	public static final ILogger							LOG					= LoggerFactory
			.create(TemplateCoarseGenlex.class);

	private static final long							serialVersionUID	= 5314829683858325179L;

	private final GenerationRepositoryWithConstants		coarseRepository;

	private final GenerationRepositoryWithConstants		fineRepository;

	private final int									maxTokens;

	private final IParser<Sentence, LogicalExpression>	parser;
	private final int									parsingBeam;

	protected TemplateCoarseGenlex(
			GenerationRepositoryWithConstants fineRepository,
			GenerationRepositoryWithConstants coarseRepository, int maxTokens,
			IParser<Sentence, LogicalExpression> parser, int parsingBeam,
			String origin, boolean mark) {
		super(origin, mark);
		assert fineRepository.getTemplates()
				.equals(coarseRepository.getTemplates());
		this.fineRepository = fineRepository;
		this.coarseRepository = coarseRepository;
		this.parser = parser;
		this.parsingBeam = parsingBeam;
		this.maxTokens = maxTokens;
		LOG.info("Init %s :: maxTokens=%d, #Templates=%d, parsingBeam=%d ...",
				this.getClass().getSimpleName(), maxTokens,
				fineRepository.getTemplates().size(), parsingBeam);
	}

	@Override
	public ILexicon<LogicalExpression> generate(DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		// Create all possible lexemes using abstract lists for all spans of
		// tokens up to the limit and the abstract factored lexicon.
		final FactoredLexicon abstractLexicon = new FactoredLexicon(
				coarseRepository.generate(dataItem.getTokens(), maxTokens,
						entryProperties),
				coarseRepository.getTemplates());

		// Parse with abstract constants.
		final IParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem, model.createDataItemModel(dataItem), false,
				abstractLexicon, parsingBeam);

		LOG.debug("Abstract parse for lexicon generation completed, %.4fsec",
				parserOutput.getParsingTime() / 1000.0);
		LOG.debug("Generated %d abstract parses",
				parserOutput.getAllDerivations().size());

		// Collect triplets of template, tokens and attributes used in generated
		// lexical entries in complete derivations.
		final List<Triplet<LexicalTemplate, TokenSeq, List<String>>> triplets = new LinkedList<Triplet<LexicalTemplate, TokenSeq, List<String>>>();
		for (final IDerivation<LogicalExpression> parse : parserOutput
				.getAllDerivations()) {
			for (final LexicalEntry<LogicalExpression> entry : parse
					.getMaxLexicalEntries()) {
				if (origin.equals(entry.getOrigin())) {
					final FactoredLexicalEntry factored = FactoringServices
							.factor(entry);
					triplets.add(Triplet.of(factored.getTemplate(),
							entry.getTokens(),
							factored.getLexeme().getAttributes()));
				}
			}
		}

		LOG.debug("Lexicon generation, %d template-token pairs:",
				triplets.size());
		LOG.debug(() -> {
			for (final Triplet<LexicalTemplate, TokenSeq, List<String>> triplet : triplets) {
				LOG.debug(triplet);
			}
		});

		// Create lexemes using the tokens that were used with GENLEX entries
		// during the abstract parse. Create lexical entries from the tokens,
		// the potential sequences and used templates. Only add entries that are
		// missing from the model.
		final Lexicon<LogicalExpression> lexicon = new Lexicon<LogicalExpression>();

		for (final Triplet<LexicalTemplate, TokenSeq, List<String>> triplet : triplets) {
			final LexicalTemplate template = triplet.first();
			final List<String> attributes = triplet.third();
			final TokenSeq tokens = triplet.second();
			for (final List<LogicalConstant> seq : fineRepository
					.getConstantSeqs(template)) {
				final Lexeme lexeme = new Lexeme(tokens, seq, attributes,
						entryProperties);
				final Category<LogicalExpression> category = template
						.apply(lexeme);
				if (category != null) {
					final LexicalEntry<LogicalExpression> entry = FactoringServices
							.factor(new LexicalEntry<LogicalExpression>(
									lexeme.getTokens(), category, false,
									entryProperties));
					if (!model.getLexicon().contains(entry)) {
						lexicon.add(entry);
					}
				}
			}
		}

		LOG.debug("%d lexical entries generated", lexicon.size());
		LOG.debug(() -> {
			for (final LexicalEntry<LogicalExpression> entry : lexicon
					.toCollection()) {
				LOG.info(entry);
			}
		});

		return lexicon;
	}

	@Override
	public void init(IModelImmutable<Sentence, LogicalExpression> model) {
		coarseRepository.init(model);
		fineRepository.init(model);
	}

	@Override
	public boolean isGenerated(LexicalEntry<LogicalExpression> entry) {
		return origin.equals(entry.getOrigin());
	}

	@Override
	public void lexicalEntriesAdded(
			Collection<LexicalEntry<LogicalExpression>> entries) {
		for (final LexicalEntry<LogicalExpression> entry : entries) {
			lexicalEntryAdded(entry);
		}
	}

	@Override
	public void lexicalEntriesAdded(ILexicon<LogicalExpression> entries) {
		lexicalEntriesAdded(entries.toCollection());
	}

	@Override
	public void lexicalEntryAdded(LexicalEntry<LogicalExpression> entry) {
		final LexicalTemplate template = FactoringServices.factor(entry)
				.getTemplate();
		if (coarseRepository.addTemplate(template)) {
			LOG.info(
					"Coars-to-fine GENLEX: added a new coarse template (-> %d): %s",
					coarseRepository.numTemplates(), template);
		}
		if (fineRepository.addTemplate(template)) {
			LOG.info(
					"Coars-to-fine GENLEX: added a new fine template (-> %d): %s",
					fineRepository.numTemplates(), template);
		}
	}

	public static class Builder<DI extends Sentence> {
		private static final String								CONST_SEED_NAME	= "absconst";

		private final boolean									mark;
		private String											origin			= ILexiconGenerator.GENLEX_LEXICAL_ORIGIN;
		protected final Set<LogicalConstant>					constants		= new HashSet<LogicalConstant>();
		protected final int										maxTokens;
		protected final IParser<Sentence, LogicalExpression>	parser;
		protected final int										parsingBeam;

		public Builder(int maxTokens,
				IParser<Sentence, LogicalExpression> parser, int parsingBeam,
				boolean mark) {
			this.maxTokens = maxTokens;
			this.parser = parser;
			this.parsingBeam = parsingBeam;
			this.mark = mark;
		}

		public Builder<DI> addConstants(
				Iterable<LogicalConstant> constantCollection) {
			for (final LogicalConstant constant : constantCollection) {
				constants.add(constant);
			}
			return this;
		}

		public TemplateCoarseGenlex<DI> build() {
			final GenerationRepository repository = new GenerationRepository();

			return new TemplateCoarseGenlex<DI>(
					repository.setConstants(constants),
					repository.setConstants(createAbstractConstants()),
					maxTokens, parser, parsingBeam, origin, mark);
		}

		public Builder<DI> setOrigin(String origin) {
			this.origin = origin;
			return this;
		}

		private Set<LogicalConstant> createAbstractConstants() {
			final Set<LogicalConstant> abstractConstants = new HashSet<LogicalConstant>();
			for (final LogicalConstant constant : constants) {
				final Type type = LogicLanguageServices.getTypeRepository()
						.generalizeType(constant.getType());
				abstractConstants.add(LogicalConstant
						.createDynamic(CONST_SEED_NAME, type, true));
			}
			return abstractConstants;
		}

	}

	public static class Creator<DI extends Sentence>
			implements IResourceObjectCreator<TemplateCoarseGenlex<DI>> {

		private final String type;

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
							.get(params.get("parser")),
					params.getAsInteger("beam"),
					params.getAsBoolean("mark", false));

			builder.addConstants((Iterable<LogicalConstant>) repo
					.get(params.get("ontology")));

			if (params.contains("origin")) {
				builder.setOrigin(params.get("origin"));
			}

			return builder.build();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, TemplateCoarseGenlex.class)
					.addParam("maxTokens", Integer.class,
							"Max number of tokens to include in lexical entries.")
					.addParam("parser", IParser.class,
							"Parser to use for coarse parsing.")
					.addParam("origin", String.class,
							"Origin of generated entries (default: "
									+ ILexiconGenerator.GENLEX_LEXICAL_ORIGIN
									+ ")")
					.addParam("mark", Boolean.class,
							"Mark generated entries (default: false)")
					.addParam("beam", Integer.class, "Beam for parsing.")
					.addParam("ontology", Set.class,
							"Collection of logical constants to initialize templates.")
					.build();
		}

	}
}
