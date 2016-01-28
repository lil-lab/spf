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
package edu.cornell.cs.nlp.spf.base.token;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * An immutable sequence of tokens. Doesn't provide an {@link Iterator} for
 * efficiency. Use conventional for-loop to with {@link #get(int)} to iterate
 * over words.
 *
 * @author Yoav Artzi
 */
public class TokenSeq implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -4291913429569093266L;
	private final int		hashCode;
	private final String[]	tokens;

	private TokenSeq(List<String> tokens) {
		this(tokens.toArray(new String[tokens.size()]));
	}

	private TokenSeq(String... tokens) {
		this.tokens = tokens;
		this.hashCode = calcHashCode();
	}

	/**
	 * Creates a {@link TokenSeq} of interned strings
	 * 
	 * @see String#intern()
	 */
	public static TokenSeq of(List<String> list) {
		// Intern all strings to save memory.
		final int len = list.size();
		final String[] interns = new String[len];
		for (int i = 0; i < len; ++i) {
			interns[i] = list.get(i).intern();
		}
		return new TokenSeq(interns);
	}

	/**
	 * Creates a {@link TokenSeq} of interned strings
	 * 
	 * @see String#intern()
	 */
	public static TokenSeq of(String... tokens) {
		final int len = tokens.length;
		final String[] interns = new String[len];
		for (int i = 0; i < len; ++i) {
			interns[i] = tokens[i].intern();
		}
		return new TokenSeq(interns);
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
		final TokenSeq other = (TokenSeq) obj;
		if (!Arrays.equals(tokens, other.tokens)) {
			return false;
		}
		return true;
	}

	public String get(int index) {
		return tokens[index];
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public int size() {
		return tokens.length;
	}

	public TokenSeq sub(int fromIndex, int toIndex) {
		return new TokenSeq(subArray(fromIndex, toIndex));
	}

	public String[] subArray(int fromIndex, int toIndex) {
		return Arrays.copyOfRange(tokens, fromIndex, toIndex);
	}

	public List<String> subList(int fromIndex, int toIndex) {
		return Arrays.asList(subArray(fromIndex, toIndex));
	}

	public List<String> toList() {
		return Collections.unmodifiableList(Arrays.asList(tokens));
	}

	public TokenSeq toLowerCase() {
		final String[] lowercased = new String[tokens.length];
		for (int i = 0; i < tokens.length; ++i) {
			lowercased[i] = tokens[i].toLowerCase();
		}
		return of(lowercased);
	}

	@Override
	public String toString() {
		return toString(" ");
	}

	public String toString(String separator) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.length; ++i) {
			sb.append(tokens[i]);
			if (i + 1 < tokens.length) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(tokens);
		return result;
	}

}
