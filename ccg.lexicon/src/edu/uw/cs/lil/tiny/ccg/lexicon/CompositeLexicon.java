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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.string.IStringFilter;
import edu.uw.cs.lil.tiny.utils.string.StubStringFilter;

/**
 * Lexicon that contains many sub lexicons. All of the additions and deletions
 * of lexical items happen in the single master lexicon passed into the
 * constructor. The other lexicons provide lexical entries during parsing but
 * can not be modified.
 * 
 * @author Luke Zettlemoyer
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
	
	public static <Y> IDecoder<CompositeLexicon<Y>> getDecoder(
			DecoderHelper<Y> decoderHelper) {
		return new Decoder<Y>(decoderHelper);
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
	
	private static class Decoder<Y> extends
			AbstractDecoderIntoFile<CompositeLexicon<Y>> {
		private static final int		VERSION	= 1;
		private final DecoderHelper<Y>	decoderHelper;
		
		public Decoder(DecoderHelper<Y> decoderHelper) {
			super(CompositeLexicon.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				CompositeLexicon<Y> object) {
			final Map<String, String> attributes = new HashMap<String, String>();
			
			// Skipping cost
			attributes.put("numSubLexicons",
					Integer.toString(object.subLexicons.size()));
			
			return attributes;
		}
		
		@Override
		protected CompositeLexicon<Y> doDecode(Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			// Get default scorer
			final ILexicon<Y> masterLexicon = DecoderServices.decode(
					dependentFiles.get("masterLexicon"), decoderHelper);
			
			// Get skipping cost
			final int numSubLexicons = Integer.valueOf(attributes
					.get("numSubLexicons"));
			final List<ILexicon<Y>> subLexicons = new ArrayList<ILexicon<Y>>(
					numSubLexicons);
			for (int i = 0; i < numSubLexicons; i++) {
				final ILexicon<Y> subLexicon = DecoderServices.decode(
						dependentFiles.get("subLexicon" + i), decoderHelper);
				subLexicons.add(subLexicon);
			}
			return new CompositeLexicon<Y>(masterLexicon, subLexicons);
		}
		
		@Override
		protected void doEncode(CompositeLexicon<Y> object,
				BufferedWriter writer) throws IOException {
			// Nothing to do here
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				CompositeLexicon<Y> object, File directory, File parentFile)
				throws IOException {
			final Map<String, File> files = new HashMap<String, File>();
			
			// Encode default scorer
			final File masterLexiconFile = new File(directory,
					parentFile.getName() + ".masterLexicon");
			DecoderServices.encode(object.masterLexicon, masterLexiconFile,
					decoderHelper);
			files.put("masterLexicon", masterLexiconFile);
			
			int i = 0;
			for (final ILexicon<Y> lexicon : object.subLexicons) {
				final File subLexiconFile = new File(directory,
						parentFile.getName() + ".subLexicon" + i);
				DecoderServices.encode(lexicon, subLexiconFile, decoderHelper);
				files.put("subLexicon" + i, subLexiconFile);
				i++;
			}
			return files;
		}
		
	}
	
}
