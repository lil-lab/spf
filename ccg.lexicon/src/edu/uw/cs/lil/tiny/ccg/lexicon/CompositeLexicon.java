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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

/**
 * Lexicon that contains many sub lexicons. All of the additions and deletions
 * of lexical items happen in the single master lexicon passed into the
 * constructor. The other lexicons provide lexical entries during parsing but
 * can not be modified.
 * 
 * @author Luke Zettlemoyer
 * @param <MR>
 *            Meaning representation type.
 */
public class CompositeLexicon<MR> implements ILexicon<MR> {
	private static final long			serialVersionUID	= -2289410658817517272L;
	private final ILexicon<MR>			masterLexicon;
	private final List<ILexicon<MR>>	subLexicons;
	
	public CompositeLexicon(ILexicon<MR> masterLexicon,
			List<ILexicon<MR>> subLexicons) {
		this.masterLexicon = masterLexicon;
		this.subLexicons = subLexicons;
	}
	
	@Override
	public Set<LexicalEntry<MR>> add(LexicalEntry<MR> lex) {
		if (contains(lex)) {
			return Collections.emptySet();
		}
		return masterLexicon.add(lex);
	}
	
	@Override
	public Set<LexicalEntry<MR>> addAll(Collection<LexicalEntry<MR>> newEntries) {
		final Set<LexicalEntry<MR>> addedEntries = new HashSet<LexicalEntry<MR>>();
		for (final LexicalEntry<MR> lex : newEntries) {
			addedEntries.addAll(add(lex));
		}
		return addedEntries;
	}
	
	public Set<LexicalEntry<MR>> addAll(ILexicon<MR> lexicon) {
		return addAll(lexicon.toCollection());
	}
	
	@Override
	public Set<LexicalEntry<MR>> addEntriesFromFile(File file,
			ICategoryServices<MR> categoryServices, String origin) {
		return masterLexicon.addEntriesFromFile(file, new StubStringFilter(),
				categoryServices, origin);
	}
	
	@Override
	public Set<LexicalEntry<MR>> addEntriesFromFile(File file,
			IStringFilter textFilter, ICategoryServices<MR> categoryServices,
			String origin) {
		return masterLexicon.addEntriesFromFile(file, textFilter,
				categoryServices, origin);
	}
	
	@Override
	public boolean contains(LexicalEntry<MR> lex) {
		if (masterLexicon.contains(lex)) {
			return true;
		}
		for (final ILexicon<MR> lexicon : subLexicons) {
			if (lexicon.contains(lex)) {
				return true;
			}
		}
		return false;
	}
	
	public CompositeLexicon<MR> copy() {
		return new CompositeLexicon<MR>(masterLexicon.copy(), subLexicons);
	}
	
	/**
	 * Get all lexical entries that match a given sequence of words.
	 * 
	 * @param words
	 * @return
	 */
	public List<LexicalEntry<MR>> getLexEntries(List<String> words) {
		final List<LexicalEntry<MR>> matchingEntries = new LinkedList<LexicalEntry<MR>>();
		matchingEntries.addAll(masterLexicon.getLexEntries(words));
		for (final ILexicon<MR> lexicon : subLexicons) {
			matchingEntries.addAll(lexicon.getLexEntries(words));
		}
		return matchingEntries;
	}
	
	@Override
	public boolean retainAll(Collection<LexicalEntry<MR>> toKeepEntries) {
		return masterLexicon.retainAll(toKeepEntries);
	}
	
	@Override
	public boolean retainAll(ILexicon<MR> entries) {
		return masterLexicon.retainAll(entries);
	}
	
	public int size() {
		int size = masterLexicon.size();
		for (final ILexicon<MR> lexicon : subLexicons) {
			size += lexicon.size();
		}
		return size;
	}
	
	public Collection<LexicalEntry<MR>> toCollection() {
		final Set<LexicalEntry<MR>> result = new HashSet<LexicalEntry<MR>>();
		result.addAll(masterLexicon.toCollection());
		for (final ILexicon<MR> lexicon : subLexicons) {
			result.addAll(lexicon.toCollection());
		}
		return result;
	}
	
	@Override
	public String toString() {
		final StringBuffer output = new StringBuffer();
		output.append("MASTER LEXICON");
		output.append(masterLexicon.toString());
		int i = 0;
		for (final ILexicon<MR> lexicon : subLexicons) {
			output.append("LEXICON").append(i);
			output.append(lexicon.toString());
			i++;
		}
		return output.toString();
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<CompositeLexicon<MR>> {
		
		@SuppressWarnings("unchecked")
		@Override
		public CompositeLexicon<MR> create(Parameters parameters,
				IResourceRepository resourceRepo) {
			
			final ILexicon<MR> master = (ILexicon<MR>) resourceRepo
					.getResource(parameters.get("masterLexicon"));
			
			final List<String> otherLexicons = parameters
					.getSplit("otherLexicons");
			final List<ILexicon<MR>> subLexicons = new ArrayList<ILexicon<MR>>();
			for (final String lexName : otherLexicons) {
				subLexicons.add((ILexicon<MR>) resourceRepo
						.getResource(lexName));
			}
			
			return new CompositeLexicon<MR>(master, subLexicons);
		}
		
		@Override
		public String type() {
			return "lexicon.composite";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), CompositeLexicon.class)
					.setDescription(
							"Lexicon that is made of a union of different lexicons")
					.addParam(
							"masterLexicon",
							"id",
							"The core lexicon. All entries added to the copmosite lexicon will be added to it")
					.addParam("otherLexicons", "[id]",
							"Non-master lexicons to use (e.g., 'lexicon1,lexicon2,lexicon3')")
					.build();
		}
	}
	
}
