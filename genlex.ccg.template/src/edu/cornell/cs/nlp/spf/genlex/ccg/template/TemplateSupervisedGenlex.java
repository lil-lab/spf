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
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.AbstractLexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetConstantsSet;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelListener;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Template-based GENLEX that uses the labeled logical form to get the set of
 * constants to instantiate the templates.
 *
 * @author Yoav Artzi
 */
public class TemplateSupervisedGenlex<SAMPLE extends Sentence, DI extends ILabeledDataItem<SAMPLE, LogicalExpression>>
		extends
		AbstractLexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>>
		implements IModelListener<LogicalExpression> {
	public static final ILogger			LOG					= LoggerFactory
			.create(TemplateSupervisedGenlex.class);

	private static final long			serialVersionUID	= 7983306766078896442L;

	private final int					maxTokens;

	private final GenerationRepository	repository			= new GenerationRepository();

	public TemplateSupervisedGenlex(int maxTokens, boolean mark,
			String origin) {
		super(origin, mark);
		this.maxTokens = maxTokens;
		LOG.info("Init %s: #templates=%d, maxTokens=%d",
				getClass().getSimpleName(), repository.getTemplates().size(),
				maxTokens);
	}

	@Override
	public ILexicon<LogicalExpression> generate(DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		// Get set of all constants from the logical form.
		final Set<LogicalConstant> constants = GetConstantsSet
				.of(dataItem.getLabel());

		CollectionUtils.filterInPlace(constants,
				e -> FactoringServices.isFactorable(e));

		return new FactoredLexicon(repository.setConstants(constants).generate(
				dataItem.getSample().getTokens(), maxTokens, entryProperties),
				repository.getTemplates());
	}

	@Override
	public void init(IModelImmutable<Sentence, LogicalExpression> model) {
		repository.init(model);
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
		if (repository.addTemplate(template)) {
			LOG.info(
					"Template supervised GENLEX: Added new template (-> %d): %s",
					repository.numTemplates(), template);
		}
	}

	public static class Creator<SAMPLE extends Sentence, DI extends ILabeledDataItem<SAMPLE, LogicalExpression>>
			implements
			IResourceObjectCreator<TemplateSupervisedGenlex<SAMPLE, DI>> {

		private final String type;

		public Creator() {
			this("genlex.template.supervised");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public TemplateSupervisedGenlex<SAMPLE, DI> create(Parameters params,
				IResourceRepository repo) {
			return new TemplateSupervisedGenlex<>(
					Integer.valueOf(params.get("maxTokens")),
					params.getAsBoolean("mark", false), params.get("origin",
							ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					TemplateSupervisedGenlex.class)
							.addParam("maxTokens", "int",
									"Max number of tokens to consider for new lexical entries")
							.addParam("origin", String.class,
									"Origin of generated entries (default: "
											+ ILexiconGenerator.GENLEX_LEXICAL_ORIGIN
											+ ")")
							.addParam("mark", Boolean.class,
									"Mark generated entries (default: false)")
							.build();
		}

	}

}
