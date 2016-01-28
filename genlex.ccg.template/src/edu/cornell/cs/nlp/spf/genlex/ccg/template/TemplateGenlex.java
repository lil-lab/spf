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
package edu.cornell.cs.nlp.spf.genlex.ccg.template;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.genlex.ccg.AbstractLexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelListener;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Simple template-based GENLEX. Given a sentence, creates all possible factored
 * lexical entries with available templates and all available constants.
 *
 * @author Yoav Artzi
 */
public class TemplateGenlex<DI extends Sentence> extends
		AbstractLexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>>
		implements IModelListener<LogicalExpression> {

	public static final ILogger						LOG					= LoggerFactory
			.create(TemplateGenlex.class);

	private static final long						serialVersionUID	= 8608329951801957784L;

	private final GenerationRepositoryWithConstants	generationRepository;

	private final int								maxTokens;

	protected TemplateGenlex(int maxTokens,
			GenerationRepositoryWithConstants generationRepository,
			String origin, boolean mark) {
		super(origin, mark);
		this.maxTokens = maxTokens;
		this.generationRepository = generationRepository;
		LOG.info("Init %s: maxTokens=%d #Templates=%d",
				getClass().getSimpleName(), maxTokens,
				generationRepository.getTemplates().size());
	}

	@Override
	public ILexicon<LogicalExpression> generate(DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		return new FactoredLexicon(generationRepository
				.generate(dataItem.getTokens(), maxTokens, entryProperties),
				generationRepository.getTemplates());
	}

	@Override
	public void init(IModelImmutable<Sentence, LogicalExpression> model) {
		generationRepository.init(model);
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
		if (generationRepository.addTemplate(template)) {
			LOG.info("Template GENLEX: Added new template (-> %d): %s",
					generationRepository.numTemplates(), template);
		}
	}

	public static class Builder<DI extends Sentence> {
		private final Set<LogicalConstant>	constants	= new HashSet<LogicalConstant>();
		private final boolean				mark;
		private final int					maxTokens;
		private String						origin		= ILexiconGenerator.GENLEX_LEXICAL_ORIGIN;

		public Builder(int maxTokens, boolean mark) {
			this.maxTokens = maxTokens;
			this.mark = mark;
		}

		public Builder<DI> addConstants(
				Iterable<LogicalConstant> constantCollection) {
			for (final LogicalConstant constant : constantCollection) {
				constants.add(constant);
			}
			return this;
		}

		public TemplateGenlex<DI> build() {
			return new TemplateGenlex<DI>(maxTokens,
					new GenerationRepository().setConstants(constants), origin,
					mark);
		}

		public void setOrigin(String origin) {
			this.origin = origin;
		}
	}

}
