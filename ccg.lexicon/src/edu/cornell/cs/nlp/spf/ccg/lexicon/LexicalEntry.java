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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.utils.collections.MapUtils;

/**
 * A Lexical Entry associates a sequence of tokens with a category, which
 * includes both syntax and semantics.
 *
 * @param <MR>
 *            Meaning representation type.
 **/
public class LexicalEntry<MR> implements Serializable {

	/**
	 * Origin property -- indicates the origin of the lexical entry, often used
	 * for various decisions. However, notice that the origin is not written to
	 * files nor taken into account in equals() and hashcode(). Therefore,
	 * should be used extremely carefully or it might lead to weird behavior
	 * when learning lexical entries and when scoring during parsing.
	 */
	public static final String			ORIGIN_PROPERTY		= "origin";

	private static final long			serialVersionUID	= 7338579915538130443L;

	private final Category<MR>			category;

	/**
	 * Indicates that this is a dynamic lexical entry. A dynamic entry can't be
	 * added to models and is generated on-the-fly during inference.
	 */
	private final boolean				dynamic;

	private final int					hashCodeCache;

	/**
	 * A set of lexical entries that are linked to this one. This field is just
	 * used to carry the links through the learning and pruning processes. Since
	 * it's not part of the essential meaning of the lexical entry, it's not
	 * part of {@link #equals(Object)} and {@link #hashCode()}.
	 */
	private final Set<LexicalEntry<MR>>	linkedEntries		= new HashSet<LexicalEntry<MR>>();

	/**
	 * Meta data key-value pairs.
	 */
	private final Map<String, String>	properties;

	private final TokenSeq				tokens;

	public LexicalEntry(TokenSeq tokens, Category<MR> category,
			boolean dynamic, Map<String, String> properties) {
		assert tokens.size() > 0 : "Lexical entry with no tokens";
		this.dynamic = dynamic;
		this.tokens = tokens;
		this.category = category;
		this.properties = Collections.unmodifiableMap(properties);
		this.hashCodeCache = calcHashCode();
	}

	public static <MR> LexicalEntry<MR> parse(String line,
			ICategoryServices<MR> categoryServices, String origin) {
		return read(line, new IStringFilter() {

			@Override
			public String filter(String str) {
				return str;
			}
		}, categoryServices, origin);
	}

	public static <MR> LexicalEntry<MR> read(String line,
			ICategoryServices<MR> categoryServices, String origin) {
		return read(line, new StubStringFilter(), categoryServices, origin);
	}

	/**
	 * Given a string parse a lexical entry from it.
	 */
	public static <MR> LexicalEntry<MR> read(String line,
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
			final Category<MR> category = categoryServices.read(line.substring(
					split + 2, line.length()));
			return new LexicalEntry<MR>(TokenSeq.of(tokens), category, false,
					origin == null ? new HashMap<String, String>()
							: MapUtils.createSingletonMap(
									LexicalEntry.ORIGIN_PROPERTY, origin));
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

	public LexicalEntry<MR> cloneWithProperties(
			Map<String, String> newProperties) {
		return new LexicalEntry<MR>(tokens, category, dynamic, newProperties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		final LexicalEntry<MR> other = (LexicalEntry<MR>) obj;
		if (!category.equals(other.category)) {
			return false;
		}
		if (dynamic != other.dynamic) {
			return false;
		}
		if (!tokens.equals(other.tokens)) {
			return false;
		}
		return true;
	}

	public Category<MR> getCategory() {
		return category;
	}

	public Set<LexicalEntry<MR>> getLinkedEntries() {
		return Collections.unmodifiableSet(linkedEntries);
	}

	/**
	 * Note the comment on {@link #ORIGIN_PROPERTY}.
	 *
	 * @see #ORIGIN_PROPERTY
	 * @return
	 */
	public String getOrigin() {
		return getProperty(ORIGIN_PROPERTY);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.get(key);
	}

	public TokenSeq getTokens() {
		return tokens;
	}

	@Override
	public int hashCode() {
		return hashCodeCache;
	}

	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < tokens.size(); ++i) {
			result.append(tokens.get(i)).append(" ");
		}
		result.append(":- ").append(category.toString());
		return result.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + category.hashCode();
		result = prime * result + (dynamic ? 1231 : 1237);
		result = prime * result + tokens.hashCode();
		return result;
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
