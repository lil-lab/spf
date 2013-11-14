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
package edu.uw.cs.lil.tiny.genlex.ccg.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon.FactoredLexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.Lexeme;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetConstantsSet;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Template-based GENLEX that uses the labeled logical form to get the set of
 * constants to instantiate the templates.
 * 
 * @author Yoav Artzi
 */
public class TemplateSupervisedGenlex<DI extends SingleSentence>
		implements
		ILexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>> {
	public static final ILogger					LOG			= LoggerFactory
																	.create(TemplateSupervisedGenlex.class);
	private static final List<LogicalConstant>	EMPTY_LIST	= Collections
																	.emptyList();
	
	private final int							maxTokens;
	private final Set<LexicalTemplate>			templates;
	private final Set<List<Type>>				typeSignatures;
	
	private TemplateSupervisedGenlex(int maxTokens,
			Set<LexicalTemplate> templates) {
		this.maxTokens = maxTokens;
		this.templates = templates;
		
		// Collect all type signatures
		final Set<List<Type>> signatures = new HashSet<List<Type>>();
		for (final LexicalTemplate template : templates) {
			signatures.add(template.getTypeSignature());
		}
		this.typeSignatures = Collections.unmodifiableSet(signatures);
		LOG.info("Init %s: #templates=%d", getClass().getSimpleName(),
				templates.size());
	}
	
	@Override
	public ILexicon<LogicalExpression> generate(DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		final List<String> tokens = dataItem.getSample().getTokens();
		final int numTokens = tokens.size();
		
		// Get set of all constants from the logical form
		final Set<LogicalConstant> constants = GetConstantsSet.of(dataItem
				.getLabel());
		
		// Iterate over type signatures, for each one create all possible
		// combinations of constants, pair each such combination with all
		// possible sub-sequences of tokens
		final Set<Lexeme> lexemes = new HashSet<Lexeme>();
		
		// Cache logical constants for each type
		final Map<Type, Set<LogicalConstant>> constsCache = new HashMap<Type, Set<LogicalConstant>>();
		
		boolean addEmptyLexems = false;
		
		for (final List<Type> typeSignature : typeSignatures) {
			if (typeSignature.isEmpty()) {
				addEmptyLexems = true;
				continue;
			}
			
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
				for (int i = 0; i < numTokens; ++i) {
					for (int j = i; j < numTokens && j - i + 1 < maxTokens; ++j) {
						lexemes.add(new Lexeme(CollectionUtils.subList(tokens,
								i, j + 1), constantsList,
								ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
					}
				}
			}
		}
		
		if (addEmptyLexems) {
			
			for (int i = 0; i < numTokens; ++i) {
				for (int j = i; j < numTokens && j - i + 1 < maxTokens; ++j) {
					lexemes.add(new Lexeme(CollectionUtils.subList(tokens, i,
							j + 1), EMPTY_LIST,
							ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
				}
			}
		}
		
		return new FactoredLexicon(lexemes, templates,
				ILexiconGenerator.GENLEX_LEXICAL_ORIGIN);
	}
	
	public static class Builder<DI extends SingleSentence> {
		protected final int						maxTokens;
		protected final Set<LexicalTemplate>	templates	= new HashSet<LexicalTemplate>();
		
		public Builder(int maxTokens) {
			this.maxTokens = maxTokens;
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
		
		public TemplateSupervisedGenlex<DI> build() {
			return new TemplateSupervisedGenlex<DI>(maxTokens, templates);
		}
		
	}
	
	public static class Creator<DI extends SingleSentence> implements
			IResourceObjectCreator<TemplateSupervisedGenlex<DI>> {
		
		private final String	type;
		
		public Creator() {
			this("genlex.template.supervised");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public TemplateSupervisedGenlex<DI> create(Parameters params,
				IResourceRepository repo) {
			final TemplateSupervisedGenlex.Builder<DI> builder = new TemplateSupervisedGenlex.Builder<DI>(
					Integer.valueOf(params.get("maxTokens")));
			
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
			return new ResourceUsage.Builder(type(),
					TemplateSupervisedGenlex.class)
					.addParam("model", "Model",
							"Model object to get templates from")
					.addParam("lexicon", "ILexicon",
							"Lexicon to get templates from")
					.addParam("maxTokens", "int",
							"Max number of tokens to consider for new lexical entries")
					.build();
		}
		
	}
	
}
