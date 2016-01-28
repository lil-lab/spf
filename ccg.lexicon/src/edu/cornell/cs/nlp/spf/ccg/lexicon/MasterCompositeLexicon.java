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

import java.io.File;
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
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.iterators.CompositeIterator;

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
public class MasterCompositeLexicon<MR> implements ILexicon<MR> {
	private static final long			serialVersionUID	= -2289410658817517272L;
	private final ILexicon<MR>			masterLexicon;
	private final List<ILexicon<MR>>	subLexicons;

	public MasterCompositeLexicon(ILexicon<MR> masterLexicon,
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

	@Override
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

	/**
	 * Get all lexical entries that match a given sequence of words.
	 */
	@Override
	public Iterator<? extends LexicalEntry<MR>> get(TokenSeq tokens) {
		final List<Iterator<? extends LexicalEntry<MR>>> iterators = new ArrayList<Iterator<? extends LexicalEntry<MR>>>(
				subLexicons.size() + 1);
		iterators.add(masterLexicon.get(tokens));
		for (final ILexicon<MR> lexicon : subLexicons) {
			iterators.add(lexicon.get(tokens));
		}
		return new CompositeIterator<LexicalEntry<MR>>(iterators);
	}

	@Override
	public boolean retainAll(Collection<LexicalEntry<MR>> toKeepEntries) {
		return masterLexicon.retainAll(toKeepEntries);
	}

	@Override
	public boolean retainAll(ILexicon<MR> entries) {
		return masterLexicon.retainAll(entries);
	}

	@Override
	public int size() {
		int size = masterLexicon.size();
		for (final ILexicon<MR> lexicon : subLexicons) {
			size += lexicon.size();
		}
		return size;
	}

	@Override
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
			IResourceObjectCreator<MasterCompositeLexicon<MR>> {

		@SuppressWarnings("unchecked")
		@Override
		public MasterCompositeLexicon<MR> create(Parameters parameters,
				IResourceRepository resourceRepo) {

			final ILexicon<MR> master = (ILexicon<MR>) resourceRepo
					.get(parameters.get("masterLexicon"));

			final List<String> otherLexicons = parameters
					.getSplit("otherLexicons");
			final List<ILexicon<MR>> subLexicons = new ArrayList<ILexicon<MR>>();
			for (final String lexName : otherLexicons) {
				subLexicons.add((ILexicon<MR>) resourceRepo.get(lexName));
			}

			return new MasterCompositeLexicon<MR>(master, subLexicons);
		}

		@Override
		public String type() {
			return "lexicon.composite";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					MasterCompositeLexicon.class)
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
