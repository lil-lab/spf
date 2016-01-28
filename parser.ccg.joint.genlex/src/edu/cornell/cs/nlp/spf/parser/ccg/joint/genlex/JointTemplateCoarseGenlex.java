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
package edu.cornell.cs.nlp.spf.parser.ccg.joint.genlex;

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
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.situated.ISituatedDataItem;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.genlex.ccg.AbstractLexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.template.GenerationRepository;
import edu.cornell.cs.nlp.spf.genlex.ccg.template.GenerationRepositoryWithConstants;
import edu.cornell.cs.nlp.spf.genlex.ccg.template.coarse.TemplateCoarseGenlex;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IParser;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelListener;
import edu.cornell.cs.nlp.spf.parser.joint.IJointDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.IJointOutput;
import edu.cornell.cs.nlp.spf.parser.joint.IJointParser;
import edu.cornell.cs.nlp.spf.parser.joint.model.IJointModelImmutable;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.composites.Triplet;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Identical to {@link TemplateCoarseGenlex}, but using joint inference in
 * combination with a base parser. See Artzi and Zettlemoyer (2013).
 *
 * @author Yoav Artzi
 */
public class JointTemplateCoarseGenlex<ESTEP, ERESULT, SAMPLE extends ISituatedDataItem<Sentence, ?>, DI extends ILabeledDataItem<SAMPLE, ?>>
		extends
		AbstractLexiconGenerator<DI, LogicalExpression, IJointModelImmutable<SAMPLE, LogicalExpression, ESTEP>>
		implements IModelListener<LogicalExpression> {
	public static final ILogger												LOG					= LoggerFactory
			.create(JointTemplateCoarseGenlex.class);

	private static final long												serialVersionUID	= -3322046591731362309L;

	private final IParser<Sentence, LogicalExpression>						baseParser;

	private final GenerationRepositoryWithConstants							coarseRepository;

	private final GenerationRepositoryWithConstants							fineRepository;

	private final int														generationParsingBeam;
	private final IJointParser<SAMPLE, LogicalExpression, ESTEP, ERESULT>	jointParser;
	private final double													margin;
	private final int														maxTokens;
	private final IValidator<DI, ERESULT>									validator;

	protected JointTemplateCoarseGenlex(
			GenerationRepositoryWithConstants fineRepository,
			GenerationRepositoryWithConstants coarseRepository, int maxTokens,
			IParser<Sentence, LogicalExpression> baseParser,
			int generationParsingBeam,
			IJointParser<SAMPLE, LogicalExpression, ESTEP, ERESULT> jointParser,
			double margin, IValidator<DI, ERESULT> validator, String origin,
			boolean mark) {
		super(origin, mark);
		assert fineRepository.getTemplates()
				.equals(coarseRepository.getTemplates());
		this.fineRepository = fineRepository;
		this.coarseRepository = coarseRepository;
		this.baseParser = baseParser;
		this.generationParsingBeam = generationParsingBeam;
		this.jointParser = jointParser;
		this.margin = margin;
		this.validator = validator;
		this.maxTokens = maxTokens;
		LOG.info("Init %s :: maxTokens=%d, #Templates=%d",
				this.getClass().getName(), maxTokens,
				fineRepository.getTemplates().size());
		LOG.info("... :: margin=%f", margin);
	}

	@Override
	public ILexicon<LogicalExpression> generate(DI dataItem,
			IJointModelImmutable<SAMPLE, LogicalExpression, ESTEP> model,
			ICategoryServices<LogicalExpression> categoryServices) {

		// Pre-generation joint parse to get the base parse score of the best
		// valid joint parse.
		final IJointOutput<LogicalExpression, ERESULT> preModelParseOutput = jointParser
				.parse(dataItem.getSample(),
						model.createJointDataItemModel(dataItem.getSample()));
		Double bestValidScore = null;
		for (final IJointDerivation<LogicalExpression, ERESULT> parse : preModelParseOutput
				.getDerivations()) {
			if (validator.isValid(dataItem, parse.getResult())) {
				final List<LogicalExpression> maxSemantics = parse
						.getMaxSemantics();
				for (final LogicalExpression semantics : maxSemantics) {
					final List<? extends IDerivation<LogicalExpression>> maxParses = preModelParseOutput
							.getBaseParserOutput().getMaxDerivations(
									e -> semantics.equals(e.getSemantics()));
					for (final IDerivation<LogicalExpression> baseParse : maxParses) {
						if (bestValidScore == null
								|| baseParse.getScore() > bestValidScore) {
							bestValidScore = baseParse.getScore();
						}
					}
				}
			}
		}
		LOG.debug("Best base parse valid score: %s", bestValidScore);
		final Double thresholdScore = bestValidScore == null ? null
				: bestValidScore + margin;

		// Create all possible lexemes using abstract lists for all spans of
		// tokens up to the limit and from the the abstract factored lexicon.
		final FactoredLexicon abstractLexicon = new FactoredLexicon(
				coarseRepository.generate(
						dataItem.getSample().getSample().getTokens(), maxTokens,
						entryProperties),
				coarseRepository.getTemplates());

		// Parse with abstract constants (non-joint inference, only regular
		// parsing).
		final IParserOutput<LogicalExpression> parserOutput = baseParser.parse(
				dataItem.getSample().getSample(),
				model.createDataItemModel(dataItem.getSample()), false,
				abstractLexicon, generationParsingBeam);

		LOG.debug("Abstract parsing for lexicon generation completed, %.4fsec",
				parserOutput.getParsingTime() / 1000.0);
		LOG.debug("Generated %d abstract parses",
				parserOutput.getAllDerivations().size());

		// Remove parses with a score lower or equal to the best valid
		// pre-generation parse
		final LinkedList<IDerivation<LogicalExpression>> filteredParses = new LinkedList<IDerivation<LogicalExpression>>(
				parserOutput.getAllDerivations());
		if (thresholdScore != null && !filteredParses.isEmpty()) {
			CollectionUtils.filterInPlace(filteredParses, e -> {
				LOG.debug("Abstract parse score: %f, threshold score: %f%s",
						e.getScore(), thresholdScore,
						e.getScore() > thresholdScore ? " --> passed" : "");
				return e.getScore() > thresholdScore;
			});

			LOG.debug(
					"Filtered %d abstract parses based on margin of %f from %f, %d abstract parses left",
					parserOutput.getAllDerivations().size()
							- filteredParses.size(),
					margin, bestValidScore, filteredParses.size());

		}

		if (filteredParses.isEmpty()) {
			LOG.info("No abstract parses above margin for lexical generation");
			return new Lexicon<LogicalExpression>();
		} else {
			LOG.info("%d abstract parses for lexical generation",
					filteredParses.size());
		}

		// Collect triplets of template, tokens and attributes used in generated
		// lexical entries in complete derivations.
		final List<Triplet<LexicalTemplate, TokenSeq, List<String>>> triplets = new LinkedList<Triplet<LexicalTemplate, TokenSeq, List<String>>>();
		for (final IDerivation<LogicalExpression> parse : filteredParses) {
			LOG.debug("Abstract parse: [%f] %s", parse.getScore(), parse);
			for (final LexicalEntry<LogicalExpression> entry : parse
					.getMaxLexicalEntries()) {
				if (origin.equals(entry.getOrigin())) {
					LOG.debug("Generated entry: %s", entry);
					final FactoredLexicalEntry factored = FactoringServices
							.factor(entry);
					final Triplet<LexicalTemplate, TokenSeq, List<String>> triplet = Triplet
							.of(factored.getTemplate(), entry.getTokens(),
									factored.getLexeme().getAttributes());
					triplets.add(triplet);
					LOG.debug(
							"Added a new template-token-attributes triplet: %s",
							triplet);

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
	public void init(
			IJointModelImmutable<SAMPLE, LogicalExpression, ESTEP> model) {
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
					"Coars-to-fine joint GENLEX: added a new coarse template (-> %d): %s",
					coarseRepository.numTemplates(), template);
		}
		if (fineRepository.addTemplate(template)) {
			LOG.info(
					"Coars-to-fine joint GENLEX: added a new fine template (-> %d): %s",
					fineRepository.numTemplates(), template);
		}
	}

	public static class Builder<ESTEP, ERESULT, SAMPLE extends ISituatedDataItem<Sentence, ?>, DI extends ILabeledDataItem<SAMPLE, ?>> {
		private static final String												CONST_SEED_NAME	= "absconst";

		private final boolean													mark;
		private final String													origin;
		protected final IParser<Sentence, LogicalExpression>					baseParser;
		protected final Set<LogicalConstant>									constants		= new HashSet<LogicalConstant>();
		protected final int														generationParsingBeam;
		protected final IJointParser<SAMPLE, LogicalExpression, ESTEP, ERESULT>	jointParser;
		protected double														margin			= 0.0;
		protected final int														maxTokens;
		protected final IValidator<DI, ERESULT>									validator;

		public Builder(int maxTokens,
				IParser<Sentence, LogicalExpression> parser,
				int generationParsingBeam,
				IJointParser<SAMPLE, LogicalExpression, ESTEP, ERESULT> jointParser,
				IValidator<DI, ERESULT> validator, String origin,
				boolean mark) {
			this.maxTokens = maxTokens;
			this.baseParser = parser;
			this.generationParsingBeam = generationParsingBeam;
			this.jointParser = jointParser;
			this.validator = validator;
			this.origin = origin;
			this.mark = mark;
		}

		public Builder<ESTEP, ERESULT, SAMPLE, DI> addConstants(
				Iterable<LogicalConstant> constantCollection) {
			for (final LogicalConstant constant : constantCollection) {
				constants.add(constant);
			}
			return this;
		}

		public JointTemplateCoarseGenlex<ESTEP, ERESULT, SAMPLE, DI> build() {
			final GenerationRepository repository = new GenerationRepository();

			return new JointTemplateCoarseGenlex<ESTEP, ERESULT, SAMPLE, DI>(
					repository.setConstants(constants),
					repository.setConstants(createAbstractConstants()),
					maxTokens, baseParser, generationParsingBeam, jointParser,
					margin, validator, origin, mark);
		}

		public Builder<ESTEP, ERESULT, SAMPLE, DI> setMargin(double margin) {
			this.margin = margin;
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
}
