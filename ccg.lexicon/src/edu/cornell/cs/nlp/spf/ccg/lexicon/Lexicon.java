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
package edu.cornell.cs.nlp.spf.ccg.lexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.SetUtils;
import edu.cornell.cs.nlp.utils.collections.iterators.ImmutableIterator;

/**
 * Lexicon containing a collection of lexical entries that match textual tokens.
 *
 * @author Yoav Artzi
 */
public class Lexicon<MR> implements ILexicon<MR> {
	public static final String							SAVED_LEXICON_ORIGIN	= "saved";

	private static final long							serialVersionUID		= -6246827857469875399L;

	private final Map<TokenSeq, Set<LexicalEntry<MR>>>	entries					= new HashMap<TokenSeq, Set<LexicalEntry<MR>>>();

	public Lexicon() {
	}

	public Lexicon(ILexicon<MR> lexicon) {
		addAll(lexicon);
	}

	/**
	 * Create a lexicon with a given list of lexical entries.
	 */
	public Lexicon(Set<LexicalEntry<MR>> entries) {
		addAll(entries);
	}

	@Override
	public Set<LexicalEntry<MR>> add(LexicalEntry<MR> lex) {
		final Set<LexicalEntry<MR>> set = entries.get(lex.getTokens());
		if (set == null) {
			final Set<LexicalEntry<MR>> newSet = new HashSet<LexicalEntry<MR>>();
			newSet.add(lex);
			entries.put(lex.getTokens(), newSet);
			return SetUtils.createSingleton(lex);
		} else {
			if (set.add(lex)) {
				return SetUtils.createSingleton(lex);
			}
		}
		return Collections.emptySet();
	}

	@Override
	public Set<LexicalEntry<MR>> addAll(Collection<LexicalEntry<MR>> newEntries) {
		final Set<LexicalEntry<MR>> added = new HashSet<LexicalEntry<MR>>();
		for (final LexicalEntry<MR> entry : newEntries) {
			added.addAll(add(entry));
		}
		return added;
	}

	@Override
	public Set<LexicalEntry<MR>> addAll(ILexicon<MR> lexicon) {
		return addAll(lexicon.toCollection());
	}

	@Override
	public Set<LexicalEntry<MR>> addEntriesFromFile(File file,
			ICategoryServices<MR> categoryServices, String origin) {
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
	public Set<LexicalEntry<MR>> addEntriesFromFile(File file,
			IStringFilter textFilter, ICategoryServices<MR> categoryServices,
			String origin) {
		try {
			final Set<LexicalEntry<MR>> added = new HashSet<LexicalEntry<MR>>();
			int lineCounter = 0;
			try (final BufferedReader in = new BufferedReader(new FileReader(
					file))) {
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
	public boolean contains(LexicalEntry<MR> lex) {
		final Set<LexicalEntry<MR>> set = entries.get(lex.getTokens());
		return set != null && set.contains(lex);
	}

	/**
	 * Get all lexical entries that match a given sequence of words.
	 */
	@Override
	public Iterator<LexicalEntry<MR>> get(TokenSeq tokens) {
		final Set<LexicalEntry<MR>> set = entries.get(tokens);
		if (set != null) {
			return ImmutableIterator.of(set.iterator());
		} else {
			return Collections.emptyIterator();
		}
	}

	@Override
	public boolean retainAll(Collection<LexicalEntry<MR>> toKeepEntries) {
		boolean changed = false;
		for (final Set<LexicalEntry<MR>> set : entries.values()) {
			changed = set.retainAll(toKeepEntries) || changed;
		}
		return changed;
	}

	@Override
	public boolean retainAll(ILexicon<MR> lexicon) {
		return retainAll(lexicon.toCollection());
	}

	@Override
	public int size() {
		int size = 0;
		for (final Set<LexicalEntry<MR>> set : entries.values()) {
			size += set.size();
		}
		return size;
	}

	@Override
	public Collection<LexicalEntry<MR>> toCollection() {
		final Set<LexicalEntry<MR>> all = new HashSet<LexicalEntry<MR>>();
		for (final Set<LexicalEntry<MR>> set : entries.values()) {
			all.addAll(set);
		}
		return Collections.unmodifiableCollection(all);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		final Iterator<Set<LexicalEntry<MR>>> i = entries.values().iterator();
		while (i.hasNext()) {
			final Iterator<LexicalEntry<MR>> j = i.next().iterator();
			while (j.hasNext()) {
				result.append(j.next());
				if (j.hasNext()) {
					result.append("\n");
				}
			}
			if (i.hasNext()) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	public static class Creator<MR> implements
			IResourceObjectCreator<Lexicon<MR>> {

		@SuppressWarnings("unchecked")
		@Override
		public Lexicon<MR> create(Parameters params, IResourceRepository repo) {
			final Lexicon<MR> lexicon = new Lexicon<MR>();
			if (params.contains("files")) {
				for (final File file : params.getAsFiles("files")) {
					lexicon.addEntriesFromFile(
							file,
							(ICategoryServices<MR>) repo
									.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
							params.get("origin"));
				}
			}
			return lexicon;
		}

		@Override
		public String type() {
			return "lexicon";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), Lexicon.class)
					.addParam("files", File.class,
							"List of files to read entries from")
					.addParam("origin", String.class,
							"Origin to assign to lexical entries read from files")
					.setDescription("A simple collection of lexical entries")
					.build();
		}

	}

}
