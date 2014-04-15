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
package edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.string.IStringFilter;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Factored lexicon. Added lexical entries are factored and stored decomposed to
 * lexemes and lexical templates. See Kwiatkowski et al. 2011 for details.
 * 
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 */
public class FactoredLexicon implements ILexicon<LogicalExpression> {
	public static final String							FACTORING_LEXICAL_ORIGIN	= "factoring";
	
	public static final ILogger							LOG							= LoggerFactory
																							.create(FactoredLexicon.class);
	
	private static final long							serialVersionUID			= -9133601778066386561L;
	
	private final String								entriesOrigin;
	
	/** Lexemes are grouped by their strings, for quick indexing */
	private final Map<List<String>, Set<Lexeme>>		lexemes						= new HashMap<List<String>, Set<Lexeme>>();
	
	/**
	 * Maintain all lexemes indexed by type for quick access for a given
	 * template
	 */
	private final Map<List<Type>, Set<Lexeme>>			lexemesByType				= new HashMap<List<Type>, Set<Lexeme>>();
	
	/**
	 * Templates are group by the types of their input arguments, for quick
	 * indexing
	 */
	private final Map<List<Type>, Set<LexicalTemplate>>	templates					= new HashMap<List<Type>, Set<LexicalTemplate>>();
	
	public FactoredLexicon() {
		this.entriesOrigin = FACTORING_LEXICAL_ORIGIN;
	}
	
	public FactoredLexicon(Set<Lexeme> inputLexemes,
			Set<LexicalTemplate> inputTemplates) {
		this(inputLexemes, inputTemplates, FACTORING_LEXICAL_ORIGIN);
	}
	
	public FactoredLexicon(Set<Lexeme> inputLexemes,
			Set<LexicalTemplate> inputTemplates, String entriesOrigin) {
		this.entriesOrigin = entriesOrigin;
		for (final Lexeme lexeme : inputLexemes) {
			addLexeme(lexeme);
		}
		for (final LexicalTemplate template : inputTemplates) {
			addTemplate(template);
		}
		
	}
	
	public static FactoredLexicalEntry factor(
			LexicalEntry<LogicalExpression> entry) {
		if (entry instanceof FactoredLexicalEntry) {
			// Case already a factored lexical entry, cast and return
			return (FactoredLexicalEntry) entry;
		} else {
			// we need to compute the maximal factoring and return it
			return factor(entry, true, false, 0).get(0);
		}
	}
	
	public static List<FactoredLexicalEntry> factor(
			final LexicalEntry<LogicalExpression> entry, boolean doMaximal,
			boolean doPartial, int maxConstantsInPartial) {
		
		final List<Pair<List<LogicalConstant>, LexicalTemplate>> factoring = LexicalTemplate
				.doFactoring(entry.getCategory(), doMaximal, doPartial,
						maxConstantsInPartial, entry.getOrigin());
		
		return ListUtils
				.map(factoring,
						new ListUtils.Mapper<Pair<List<LogicalConstant>, LexicalTemplate>, FactoredLexicalEntry>() {
							
							@Override
							public FactoredLexicalEntry process(
									Pair<List<LogicalConstant>, LexicalTemplate> obj) {
								return new FactoredLexicalEntry(entry
										.getTokens(), entry.getCategory(),
										new Lexeme(entry.getTokens(), obj
												.first(), entry.getOrigin()),
										obj.second(), entry.getOrigin());
							}
						});
	}
	
	@Override
	public Set<LexicalEntry<LogicalExpression>> add(
			LexicalEntry<LogicalExpression> entry) {
		final FactoredLexicalEntry factoredEntry = factor(entry);
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		added.addAll(addLexeme(factoredEntry.getLexeme()));
		added.addAll(addTemplate(factoredEntry.getTemplate()));
		return added;
	}
	
	@Override
	public Set<LexicalEntry<LogicalExpression>> addAll(
			Collection<LexicalEntry<LogicalExpression>> entries) {
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final LexicalEntry<LogicalExpression> lex : entries) {
			added.addAll(add(lex));
		}
		return added;
	}
	
	@Override
	public Set<LexicalEntry<LogicalExpression>> addAll(
			ILexicon<LogicalExpression> lexicon) {
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		if (lexicon instanceof FactoredLexicon) {
			final FactoredLexicon flex = (FactoredLexicon) lexicon;
			
			// Add lexemes
			for (final Set<Lexeme> set : flex.lexemes.values()) {
				for (final Lexeme lexeme : set) {
					added.addAll(addLexeme(lexeme));
				}
			}
			
			// Add templates
			for (final Set<LexicalTemplate> set : flex.templates.values()) {
				for (final LexicalTemplate template : set) {
					added.addAll(addTemplate(template));
				}
			}
			return added;
		} else {
			return addAll(lexicon.toCollection());
		}
	}
	
	@Override
	public Set<LexicalEntry<LogicalExpression>> addEntriesFromFile(File file,
			ICategoryServices<LogicalExpression> categoryServices, String origin) {
		return addEntriesFromFile(file, new StubStringFilter(),
				categoryServices, origin);
	}
	
	/**
	 * Read entries from a file, one per line, of the form
	 * 
	 * <pre>
	 *  Tokens  :-  Cat
	 * </pre>
	 */
	@Override
	public Set<LexicalEntry<LogicalExpression>> addEntriesFromFile(File file,
			IStringFilter textFilter,
			ICategoryServices<LogicalExpression> categoryServices, String origin) {
		try {
			final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
			final BufferedReader in = new BufferedReader(new FileReader(file));
			int lineCounter = 0;
			try {
				String line;
				// For each line in the file
				while ((line = in.readLine()) != null) {
					++lineCounter;
					line = line.trim();
					// Ignore blank lines and comments
					if (!line.equals("") && !line.startsWith("//")) {
						added.addAll(add(LexicalEntry.parse(line, textFilter,
								categoryServices, origin)));
					}
				}
			} catch (final RuntimeException e) {
				throw new RuntimeException(String.format(
						"Reading of input file %s failed at line %d",
						file.getName(), lineCounter), e);
			} finally {
				in.close();
			}
			return added;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean contains(LexicalEntry<LogicalExpression> entry) {
		final FactoredLexicalEntry factoring = factor(entry);
		final Set<Lexeme> lexemeSet = lexemes.get(factoring.getLexeme()
				.getTokens());
		final Set<LexicalTemplate> templateSet = templates.get(factoring
				.getTemplate().getTypeSignature());
		
		return lexemeSet != null && templateSet != null
				&& lexemeSet.contains(factoring.getLexeme())
				&& templateSet.contains(factoring.getTemplate());
	}
	
	@Override
	public FactoredLexicon copy() {
		final FactoredLexicon newLexicon = new FactoredLexicon();
		for (final Map.Entry<List<String>, Set<Lexeme>> lexemeIndex : lexemes
				.entrySet()) {
			newLexicon.lexemes.put(lexemeIndex.getKey(), new HashSet<Lexeme>(
					lexemeIndex.getValue()));
		}
		for (final Map.Entry<List<Type>, Set<LexicalTemplate>> templateIndex : templates
				.entrySet()) {
			newLexicon.templates.put(templateIndex.getKey(),
					new HashSet<LexicalTemplate>(templateIndex.getValue()));
		}
		
		return newLexicon;
	}
	
	@Override
	public List<FactoredLexicalEntry> getLexEntries(List<String> tokens) {
		final List<FactoredLexicalEntry> newLexicalEntries = new LinkedList<FactoredLexicalEntry>();
		final Set<Lexeme> lexemeSet = lexemes.get(tokens);
		if (lexemeSet == null) {
			return Collections.emptyList();
		}
		for (final Lexeme lexeme : lexemeSet) {
			final Set<LexicalTemplate> temps = templates.get(lexeme
					.getTypeSignature());
			for (final LexicalTemplate template : temps) {
				final FactoredLexicalEntry lex = applyTemplate(template, lexeme);
				if (lex != null) {
					newLexicalEntries.add(lex);
				}
			}
		}
		return newLexicalEntries;
	}
	
	@Override
	public boolean retainAll(
			Collection<LexicalEntry<LogicalExpression>> toKeepEntries) {
		final FactoredLexicon factoredLexicon = new FactoredLexicon();
		factoredLexicon.addAll(toKeepEntries);
		return retainAll(factoredLexicon);
	}
	
	@Override
	public boolean retainAll(ILexicon<LogicalExpression> lexicon) {
		if (lexicon instanceof FactoredLexicon) {
			// Case factored lexicon, so should remove all lexemes and templates
			// it doesn't include
			final FactoredLexicon factoredLexicon = (FactoredLexicon) lexicon;
			boolean somethingRemoved = false;
			
			// Remove lexemes
			final Iterator<Entry<List<String>, Set<Lexeme>>> lexemeIterator = lexemes
					.entrySet().iterator();
			while (lexemeIterator.hasNext()) {
				final Entry<List<String>, Set<Lexeme>> lexemeEntry = lexemeIterator
						.next();
				if (factoredLexicon.lexemes.containsKey(lexemeEntry.getKey())) {
					// Case string seq. known, remove lexemes not present
					LOG.debug(new Runnable() {
						
						@Override
						public void run() {
							final Set<Lexeme> retainSet = factoredLexicon.lexemes
									.get(lexemeEntry.getKey());
							for (final Lexeme lexeme : lexemeEntry.getValue()) {
								if (!retainSet.contains(lexeme)) {
									LOG.debug("Removing lexeme: %s", lexeme);
								}
							}
						}
					});
					somethingRemoved |= lexemeEntry.getValue().retainAll(
							factoredLexicon.lexemes.get(lexemeEntry.getKey()));
				} else {
					// Case this string sequence is not present, remove all its
					// lexemes
					LOG.debug(new Runnable() {
						
						@Override
						public void run() {
							for (final Lexeme lexeme : lexemeEntry.getValue()) {
								LOG.debug("Removing lexeme: %s", lexeme);
							}
						}
					});
					lexemeIterator.remove();
					somethingRemoved = true;
				}
			}
			
			// Remove templates
			final Iterator<Entry<List<Type>, Set<LexicalTemplate>>> templateIterator = templates
					.entrySet().iterator();
			while (templateIterator.hasNext()) {
				final Entry<List<Type>, Set<LexicalTemplate>> templateEntry = templateIterator
						.next();
				if (factoredLexicon.templates.containsKey(templateEntry
						.getKey())) {
					// Case type signature present, remove all templates not
					// present
					LOG.debug(new Runnable() {
						
						@Override
						public void run() {
							final Set<LexicalTemplate> retainSet = factoredLexicon.templates
									.get(templateEntry.getKey());
							for (final LexicalTemplate template : templateEntry
									.getValue()) {
								if (!retainSet.contains(template)) {
									LOG.debug("Removing template: %s", template);
								}
							}
						}
					});
					somethingRemoved |= templateEntry.getValue().retainAll(
							factoredLexicon.templates.get(templateEntry
									.getKey()));
				} else {
					// Case type signature not present, remove all templates
					LOG.debug(new Runnable() {
						
						@Override
						public void run() {
							for (final LexicalTemplate template : templateEntry
									.getValue()) {
								LOG.debug("Removing template: %s", template);
							}
						}
					});
					templateIterator.remove();
					somethingRemoved = true;
				}
			}
			
			return somethingRemoved;
		} else {
			return retainAll(lexicon.toCollection());
		}
	}
	
	@Override
	public int size() {
		int size = 0;
		for (final Set<Lexeme> lexemeSet : lexemes.values()) {
			for (final Lexeme lexeme : lexemeSet) {
				size += templates.get(lexeme.getTypeSignature()).size();
			}
		}
		return size;
	}
	
	/**
	 * WARNING: really inefficient to call this... should avoid at all costs if
	 * lexicon is large
	 * 
	 * @return
	 */
	@Override
	public Collection<LexicalEntry<LogicalExpression>> toCollection() {
		final Set<LexicalEntry<LogicalExpression>> result = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final Set<Lexeme> lexemeSet : lexemes.values()) {
			for (final Lexeme lexeme : lexemeSet) {
				if (templates.containsKey(lexeme.getTypeSignature())) {
					for (final LexicalTemplate template : templates.get(lexeme
							.getTypeSignature())) {
						final LexicalEntry<LogicalExpression> newLex = applyTemplate(
								template, lexeme);
						if (newLex != null) {
							result.add(newLex);
						}
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
		
		final StringBuilder ret = new StringBuilder();
		ret.append("Lexemes:\n");
		for (final Entry<List<String>, Set<Lexeme>> entry : lexemes.entrySet()) {
			ret.append(entry.getKey());
			ret.append("=");
			ret.append(entry.getValue());
			ret.append("\n");
		}
		ret.append("Templates:\n");
		for (final Entry<List<Type>, Set<LexicalTemplate>> entry : templates
				.entrySet()) {
			ret.append(entry.getValue());
			ret.append("\n");
		}
		return ret.toString();
	}
	
	private Set<LexicalEntry<LogicalExpression>> addLexeme(Lexeme lexeme) {
		Set<Lexeme> lexemeSet = lexemes.get(lexeme.getTokens());
		final boolean addedLexeme;
		if (lexemeSet != null) {
			addedLexeme = lexemeSet.add(lexeme);
		} else {
			lexemeSet = new HashSet<Lexeme>();
			lexemeSet.add(lexeme);
			lexemes.put(lexeme.getTokens(), lexemeSet);
			addedLexeme = true;
		}
		
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		if (addedLexeme) {
			// Update lexeme indexing by type signature
			final List<Type> typeSignature = lexeme.getTypeSignature();
			if (!lexemesByType.containsKey(typeSignature)) {
				lexemesByType.put(typeSignature, new HashSet<Lexeme>());
			}
			lexemesByType.get(typeSignature).add(lexeme);
			
			// Get all new lexical entries
			if (templates.containsKey(typeSignature)) {
				for (final LexicalTemplate template : templates
						.get(typeSignature)) {
					final FactoredLexicalEntry entry = applyTemplate(template,
							lexeme);
					if (entry != null) {
						added.add(entry);
					}
				}
			}
		}
		return added;
	}
	
	private Set<LexicalEntry<LogicalExpression>> addTemplate(
			LexicalTemplate template) {
		Set<LexicalTemplate> templateSet = templates.get(template
				.getTypeSignature());
		final boolean addedTemplate;
		if (templateSet != null) {
			addedTemplate = templateSet.add(template);
		} else {
			templateSet = new HashSet<LexicalTemplate>();
			templateSet.add(template);
			templates.put(template.getTypeSignature(), templateSet);
			addedTemplate = true;
		}
		
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		if (addedTemplate) {
			// Get all new lexical entries
			if (lexemesByType.containsKey(template.getTypeSignature())) {
				for (final Lexeme lexeme : lexemesByType.get(template
						.getTypeSignature())) {
					final FactoredLexicalEntry entry = applyTemplate(template,
							lexeme);
					if (entry != null) {
						added.add(entry);
					}
				}
			}
		}
		return added;
	}
	
	private FactoredLexicalEntry applyTemplate(LexicalTemplate template,
			Lexeme lexeme) {
		final Category<LogicalExpression> newCategory = template
				.makeCategory(lexeme);
		if (newCategory == null) {
			return null;
		}
		return new FactoredLexicalEntry(lexeme.getTokens(), newCategory,
				lexeme, template, entriesOrigin);
	}
	
	public static class Creator implements
			IResourceObjectCreator<FactoredLexicon> {
		
		@Override
		public FactoredLexicon create(Parameters parameters,
				IResourceRepository resourceRepo) {
			return new FactoredLexicon();
		}
		
		@Override
		public String type() {
			return "lexicon.factored";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), FactoredLexicon.class)
					.setDescription(
							"Lexicon that contains factored entries. Entries are factored as they are added. The lexicon contains all entries that can be generated by its templates and lexeme")
					.build();
		}
		
	}
	
	public static class FactoredLexicalEntry extends
			LexicalEntry<LogicalExpression> {
		
		private static final long		serialVersionUID	= -2547759640580678562L;
		private final Lexeme			lexeme;
		private final LexicalTemplate	template;
		
		private FactoredLexicalEntry(List<String> tokens,
				Category<LogicalExpression> category, Lexeme lexeme,
				LexicalTemplate template, String origin) {
			super(tokens, category, origin);
			this.lexeme = lexeme;
			this.template = template;
		}
		
		@Override
		public LexicalEntry<LogicalExpression> cloneWithDifferentOrigin(
				String newOrigin) {
			return new FactoredLexicalEntry(super.getTokens(),
					super.getCategory(), lexeme, template, newOrigin);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final FactoredLexicalEntry other = (FactoredLexicalEntry) obj;
			if (lexeme == null) {
				if (other.lexeme != null) {
					return false;
				}
			} else if (!lexeme.equals(other.lexeme)) {
				return false;
			}
			if (template == null) {
				if (other.template != null) {
					return false;
				}
			} else if (!template.equals(other.template)) {
				return false;
			}
			return true;
		}
		
		public Lexeme getLexeme() {
			return lexeme;
		}
		
		public LexicalTemplate getTemplate() {
			return template;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((lexeme == null) ? 0 : lexeme.hashCode());
			result = prime * result
					+ ((template == null) ? 0 : template.hashCode());
			return result;
		}
		
	}
	
}
