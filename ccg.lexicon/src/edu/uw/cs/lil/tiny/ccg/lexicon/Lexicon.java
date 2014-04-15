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
package edu.uw.cs.lil.tiny.ccg.lexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.string.IStringFilter;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.collections.SetUtils;

/**
 * Lexicon containing a collection of lexical entries that match textual tokens.
 * 
 * @author Yoav Artzi
 */
public class Lexicon<MR> implements ILexicon<MR> {
	public static final String			SAVED_LEXICON_ORIGIN	= "saved";
	
	private static final long			serialVersionUID		= -6246827857469875399L;
	
	private final Set<LexicalEntry<MR>>	emptySet				= Collections
																		.emptySet();
	
	private final Set<LexicalEntry<MR>>	entries					= new HashSet<LexicalEntry<MR>>();
	
	public Lexicon() {
	}
	
	public Lexicon(ILexicon<MR> lexicon) {
		this.entries.addAll(lexicon.toCollection());
	}
	
	/**
	 * Create a lexicon with a given list of lexical entries.
	 * 
	 * @param entries
	 */
	public Lexicon(Set<LexicalEntry<MR>> entries) {
		this.entries.addAll(entries);
	}
	
	@Override
	public Set<LexicalEntry<MR>> add(LexicalEntry<MR> lex) {
		return entries.add(lex) ? SetUtils.createSingleton(lex) : emptySet;
	}
	
	@Override
	public Set<LexicalEntry<MR>> addAll(Collection<LexicalEntry<MR>> newEntries) {
		final Set<LexicalEntry<MR>> added = new HashSet<LexicalEntry<MR>>();
		for (final LexicalEntry<MR> entry : newEntries) {
			added.addAll(add(entry));
		}
		return added;
	}
	
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
	public boolean contains(LexicalEntry<MR> lex) {
		return entries.contains(lex);
	}
	
	public Lexicon<MR> copy() {
		return new Lexicon<MR>(this);
	}
	
	/**
	 * Get all lexical entries that match a given sequence of words.
	 * 
	 * @param words
	 * @return
	 */
	public List<LexicalEntry<MR>> getLexEntries(List<String> words) {
		final List<LexicalEntry<MR>> matchingEntries = new LinkedList<LexicalEntry<MR>>();
		for (final LexicalEntry<MR> entry : entries) {
			if (entry.hasWords(words)) {
				matchingEntries.add(entry);
			}
		}
		return matchingEntries;
	}
	
	@Override
	public boolean retainAll(Collection<LexicalEntry<MR>> toKeepEntries) {
		return entries.retainAll(toKeepEntries);
	}
	
	public boolean retainAll(ILexicon<MR> lexicon) {
		return retainAll(lexicon.toCollection());
	}
	
	public int size() {
		return entries.size();
	}
	
	public Collection<LexicalEntry<MR>> toCollection() {
		return Collections.unmodifiableCollection(entries);
	}
	
	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		final Iterator<LexicalEntry<MR>> i = entries.iterator();
		while (i.hasNext()) {
			final LexicalEntry<MR> entry = i.next();
			result.append(entry).append("\n");
		}
		return result.toString();
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<Lexicon<MR>> {
		
		@Override
		public Lexicon<MR> create(Parameters parameters,
				IResourceRepository resourceRepo) {
			return new Lexicon<MR>();
		}
		
		@Override
		public String type() {
			return "lexicon";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), Lexicon.class)
					.setDescription("A simple collection of lexical entries")
					.build();
		}
		
	}
	
}
