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
import java.util.LinkedList;
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
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Simple template-based GENLEX. Given a sentence, creates all possible factored
 * lexical entries with available templates and all available constants.
 * 
 * @author Yoav Artzi
 */
public class TemplateGenlex
		implements
		ILexiconGenerator<Sentence, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>> {
	private static final ILogger				LOG	= LoggerFactory
															.create(TemplateGenlex.class);
	
	private final int							maxTokens;
	private final Set<List<LogicalConstant>>	potentialConstantSeqs;
	private final Set<LexicalTemplate>			templates;
	
	protected TemplateGenlex(Set<LexicalTemplate> templates,
			Set<List<LogicalConstant>> pontetialConstantSeqs, int maxTokens) {
		this.potentialConstantSeqs = pontetialConstantSeqs;
		this.templates = Collections.unmodifiableSet(templates);
		this.maxTokens = maxTokens;
		LOG.info("Init %s: #templates=%d, #constantSeqs=%d", getClass()
				.getSimpleName(), templates.size(), pontetialConstantSeqs
				.size());
	}
	
	@Override
	public ILexicon<LogicalExpression> generate(Sentence dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		final List<String> tokens = dataItem.getTokens();
		final int numTokens = tokens.size();
		
		// Create all possible lexemes for all spans of tokens up to the limit
		final Set<Lexeme> lexemes = new HashSet<Lexeme>();
		for (int i = 0; i < numTokens; ++i) {
			for (int j = i; j < numTokens && j - i + 1 < maxTokens; ++j) {
				for (final List<LogicalConstant> constants : potentialConstantSeqs) {
					lexemes.add(new Lexeme(CollectionUtils.subList(tokens, i,
							j + 1), constants,
							ILexiconGenerator.GENLEX_LEXICAL_ORIGIN));
				}
			}
		}
		
		return new FactoredLexicon(lexemes, templates,
				ILexiconGenerator.GENLEX_LEXICAL_ORIGIN);
	}
	
	public static class Builder {
		protected final Set<LogicalConstant>	constants	= new HashSet<LogicalConstant>();
		protected final int						maxTokens;
		protected final Set<LexicalTemplate>	templates	= new HashSet<LexicalTemplate>();
		
		public Builder(int maxTokens) {
			this.maxTokens = maxTokens;
		}
		
		public Builder addConstants(Iterable<LogicalConstant> constantCollection) {
			for (final LogicalConstant constant : constantCollection) {
				constants.add(constant);
			}
			return this;
		}
		
		public Builder addTemplate(LexicalTemplate template) {
			templates.add(template);
			return this;
		}
		
		public Builder addTemplates(Iterable<LexicalTemplate> templateCollection) {
			for (final LexicalTemplate template : templateCollection) {
				addTemplate(template);
			}
			return this;
		}
		
		public Builder addTemplatesFromLexicon(
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
		
		public Builder addTemplatesFromModel(
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
		
		public TemplateGenlex build() {
			return new TemplateGenlex(templates, createPotentialLists(),
					maxTokens);
		}
		
		protected Set<List<LogicalConstant>> createPotentialLists() {
			// Collect all type signatures
			final Set<List<Type>> typeSignatures = new HashSet<List<Type>>();
			for (final LexicalTemplate template : templates) {
				typeSignatures.add(template.getTypeSignature());
			}
			
			// Iterate over type signatures, for each one create all possible
			// combinations of constants that will satisfy it
			final List<List<LogicalConstant>> potentialConstantSeqs = new LinkedList<List<LogicalConstant>>();
			
			// Cache logical constants for each type
			final Map<Type, Set<LogicalConstant>> constsCache = new HashMap<Type, Set<LogicalConstant>>();
			
			for (final List<Type> typeSignature : typeSignatures) {
				if (typeSignature.isEmpty()) {
					// Case no arguments, add the empty list and continue
					potentialConstantSeqs
							.add(Collections
									.unmodifiableList(new ArrayList<LogicalConstant>(
											0)));
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
					potentialConstantSeqs.add(Collections
							.unmodifiableList(constantsList));
				}
			}
			
			return Collections
					.unmodifiableSet(new HashSet<List<LogicalConstant>>(
							potentialConstantSeqs));
		}
	}
	
}
