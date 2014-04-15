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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import edu.uw.cs.lil.tiny.base.string.IStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;

/**
 * A Lexical Entry associates a sequence of tokens with a category, which
 * includes both syntax and semantics.
 * 
 * @param <MR>
 *            Meaning representation type.
 **/
public class LexicalEntry<MR> implements Serializable {
	
	private static final long			serialVersionUID	= 7338579915538130443L;
	
	private final Category<MR>			category;
	
	private final int					hashCodeCache;
	/**
	 * A set of lexical entries that are linked to this one. This field is just
	 * used to carry the links through the learning and pruning processes. Since
	 * it's not part of the essential meaning of the lexical entry, it's not
	 * part of {@link #equals(Object)} and {@link #hashCode()}.
	 */
	private final Set<LexicalEntry<MR>>	linkedEntries		= new HashSet<LexicalEntry<MR>>();
	
	/**
	 * Origin label -- indicates the origin of the lexical entry, often used for
	 * various decisions (e.g., scoring). The origin is not written to files nor
	 * taken into account in equals() and hashcode().
	 */
	private final String				origin;
	
	private final List<String>			tokens;
	
	public LexicalEntry(List<String> tokens, Category<MR> category,
			String origin) {
		this.origin = origin;
		this.tokens = Collections.unmodifiableList(tokens);
		this.category = category;
		this.hashCodeCache = calcHashCode();
	}
	
	public static <MR> LexicalEntry<MR> parse(String line,
			ICategoryServices<MR> categoryServices, String origin) {
		return parse(line, new IStringFilter() {
			
			@Override
			public String filter(String str) {
				return str;
			}
		}, categoryServices, origin);
	}
	
	/**
	 * Given a string parse a lexical entry from it.
	 * 
	 * @param line
	 * @param textFilter
	 * @return
	 */
	public static <MR> LexicalEntry<MR> parse(String line,
			IStringFilter textFilter, ICategoryServices<MR> categoryServices,
			String origin) {
		// Split the tokens and category
		final int split = line.indexOf(":-");
		if (split > 0) {
			// Filter the text and split it into tokens
			final List<String> tokens = new LinkedList<String>();
			final StringTokenizer st = new StringTokenizer(
					textFilter.filter(line.substring(0, split)));
			while (st.hasMoreTokens()) {
				tokens.add(st.nextToken());
			}
			final Category<MR> category = categoryServices.parse(line.substring(
					split + 2, line.length()));
			return new LexicalEntry<MR>(tokens, category, origin);
		} else {
			throw new IllegalStateException(
					"Unrecognized format for lexical item: " + line);
		}
	}
	
	public void addLinkedEntries(Collection<LexicalEntry<MR>> entries) {
		linkedEntries.addAll(entries);
	}
	
	public void addLinkedEntry(LexicalEntry<MR> entry) {
		linkedEntries.add(entry);
	}
	
	public LexicalEntry<MR> cloneWithDifferentOrigin(String newOrigin) {
		final LexicalEntry<MR> newEntry = new LexicalEntry<MR>(tokens,
				category, newOrigin);
		newEntry.addLinkedEntries(linkedEntries);
		return newEntry;
	}
	
	/**
	 * A LexEntry's notion of equality is if both tokens and category are equal.
	 **/
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LexicalEntry)) {
			return false;
		}
		
		@SuppressWarnings("rawtypes")
		final LexicalEntry e = (LexicalEntry) o;
		if (tokens.size() != e.tokens.size()) {
			return false;
		}
		return tokens.equals(e.tokens) && category.equals(e.category);
	}
	
	public Category<MR> getCategory() {
		return category;
	}
	
	public Set<LexicalEntry<MR>> getLinkedEntries() {
		return Collections.unmodifiableSet(linkedEntries);
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public List<String> getTokens() {
		return tokens;
	}
	
	@Override
	public int hashCode() {
		return hashCodeCache;
	}
	
	/**
	 * @return true iff the words passed in are exactly my tokens.
	 **/
	public boolean hasWords(List<String> words) {
		return tokens.equals(words);
	}
	
	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		final Iterator<String> i = tokens.iterator();
		while (i.hasNext()) {
			result.append(i.next().toString()).append(" ");
		}
		result.append(":- ").append(category.toString());
		return result.toString();
	}
	
	private int calcHashCode() {
		int hc = 17;
		hc = 37 * hc + category.hashCode();
		hc = 37 * hc + tokens.hashCode();
		return hc;
	}
	
	/**
	 * Some default origins that can be used throughout the code.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Origin {
		
		/** Domain specific fixed entry */
		public static final String	FIXED_DOMAIN	= "fixed_domain";
		
		/** Domain independent fixed entry */
		public static final String	FIXED_LANG		= "fixed_lang";
		
		/** Heuristically induced lexical entry */
		public static final String	HEURISTIC		= "heuristic";
		
		/** Learned lexical entry */
		public static final String	LEARNED			= "learned";
		
		private Origin() {
		}
	}
}
