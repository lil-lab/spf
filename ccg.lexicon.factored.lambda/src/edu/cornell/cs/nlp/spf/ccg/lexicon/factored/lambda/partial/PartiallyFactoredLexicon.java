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
package edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.partial;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.utils.collections.iterators.CompositeIterator;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * A factored lexicon that stores factored only entries with lexemes that
 * have constants (non-empty). The rest, although they might be factored, are
 * not stored in a factored way, so they won't give rise to numerous pairings of
 * unseen entries. All entries are of type {@link FactoredLexicalEntry}, even
 * these stored in the non factored lexicon. This uniform representation is
 * retained to provide a uniform feature representation.
 *
 * @author Yoav Artzi
 */
public class PartiallyFactoredLexicon implements ILexicon<LogicalExpression> {

	public static final ILogger		LOG					= LoggerFactory
			.create(PartiallyFactoredLexicon.class);
	private static final long		serialVersionUID	= -4855517842117942396L;
	private final FactoredLexicon	factored;

	private final Lexicon<LogicalExpression> nonFactored;

	public PartiallyFactoredLexicon() {
		this.factored = new FactoredLexicon();
		this.nonFactored = new Lexicon<LogicalExpression>();
	}

	public PartiallyFactoredLexicon(Collection<Lexeme> lexemes,
			Collection<LexicalTemplate> templates) {
		// Create sets without empty lexemes and empty templates.
		final Set<Lexeme> nonEmptyLexemes = new HashSet<Lexeme>();
		final Set<Lexeme> emptyLexemes = new HashSet<Lexeme>();
		for (final Lexeme lexeme : lexemes) {
			if (lexeme.getConstants().isEmpty()) {
				emptyLexemes.add(lexeme);
			} else {
				nonEmptyLexemes.add(lexeme);
			}
		}
		final Set<LexicalTemplate> nonEmptyTemplates = new HashSet<LexicalTemplate>();
		final Set<LexicalTemplate> emptyTemplates = new HashSet<LexicalTemplate>();
		for (final LexicalTemplate template : templates) {
			if (template.getArguments().isEmpty()) {
				emptyTemplates.add(template);
			} else {
				nonEmptyTemplates.add(template);
			}
		}

		this.factored = new FactoredLexicon(nonEmptyLexemes, nonEmptyTemplates);
		this.nonFactored = new Lexicon<LogicalExpression>(
				new FactoredLexicon(emptyLexemes, emptyTemplates));
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> add(
			LexicalEntry<LogicalExpression> entry) {
		final FactoredLexicalEntry factoredEntry = FactoringServices
				.factor(entry);
		if (factoredEntry.getLexeme().getConstants().isEmpty()) {
			return nonFactored.add(factoredEntry);
		} else {
			return factored.add(factoredEntry);
		}
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addAll(
			Collection<LexicalEntry<LogicalExpression>> entries) {
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final LexicalEntry<LogicalExpression> entry : entries) {
			added.addAll(add(entry));
		}
		return added;
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addAll(
			ILexicon<LogicalExpression> lexicon) {
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final LexicalEntry<LogicalExpression> entry : lexicon
				.toCollection()) {
			added.addAll(add(entry));
		}
		return added;
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addEntriesFromFile(File file,
			ICategoryServices<LogicalExpression> categoryServices,
			String origin) {
		return addEntriesFromFile(file, new StubStringFilter(),
				categoryServices, origin);
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addEntriesFromFile(File file,
			IStringFilter textFilter,
			ICategoryServices<LogicalExpression> categoryServices,
			String origin) {
		try {
			final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
			int lineCounter = 0;
			try (final BufferedReader in = new BufferedReader(
					new FileReader(file))) {
				String line;
				// For each line in the file
				while ((line = in.readLine()) != null) {
					++lineCounter;
					line = line.trim();
					// Ignore blank lines and comments
					if (!line.equals("") && !line.startsWith("//")) {
						added.addAll(add(LexicalEntry.read(line, textFilter,
								categoryServices, origin)));
					}
				}
			} catch (final RuntimeException e) {
				throw new RuntimeException(String.format(
						"Reading of input file %s failed at line %d",
						file.getName(), lineCounter), e);
			}
			return added;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean contains(LexicalEntry<LogicalExpression> lex) {
		return nonFactored.contains(lex) || factored.contains(lex);
	}

	@Override
	public Iterator<? extends LexicalEntry<LogicalExpression>> get(
			TokenSeq tokens) {
		final List<Iterator<? extends LexicalEntry<LogicalExpression>>> iterators = new ArrayList<Iterator<? extends LexicalEntry<LogicalExpression>>>(
				2);
		iterators.add(factored.get(tokens));
		iterators.add(nonFactored.get(tokens));
		return new CompositeIterator<LexicalEntry<LogicalExpression>>(
				iterators);
	}

	@Override
	public boolean retainAll(
			Collection<LexicalEntry<LogicalExpression>> entries) {
		boolean result = false;
		result = factored.retainAll(entries) || result;
		result = nonFactored.retainAll(entries) || result;
		return result;
	}

	@Override
	public boolean retainAll(ILexicon<LogicalExpression> entries) {
		boolean result = false;
		result = factored.retainAll(entries) || result;
		result = nonFactored.retainAll(entries) || result;
		return result;
	}

	@Override
	public int size() {
		return factored.size() + nonFactored.size();
	}

	@Override
	public Collection<LexicalEntry<LogicalExpression>> toCollection() {
		final Set<LexicalEntry<LogicalExpression>> set = new HashSet<LexicalEntry<LogicalExpression>>(
				factored.toCollection());
		set.addAll(nonFactored.toCollection());
		return Collections.unmodifiableSet(set);
	}

	public static class Creator
			implements IResourceObjectCreator<PartiallyFactoredLexicon> {

		private final String type;

		public Creator() {
			this("lexicon.factored.partial");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public PartiallyFactoredLexicon create(Parameters params,
				IResourceRepository repo) {
			return new PartiallyFactoredLexicon();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, PartiallyFactoredLexicon.class)
					.setDescription(
							"A factored lexicon that stores factored only entries that give lexemes that\n"
									+ " have constants (non-empty). The rest, although they might be factored, are\n"
									+ " not stored in a factored way, so they won't give rise to numerous pairings of\n"
									+ " unseen entries. All entries are of type {@link FactoredLexicalEntry}, even\n"
									+ " these stored in the non factoerd lexicon")
					.build();
		}

	}

}
