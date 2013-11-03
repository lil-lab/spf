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
package edu.uw.cs.lil.tiny.data.sentence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * Representing a single sentence.
 * 
 * @author Yoav Artzi
 */
public class Sentence implements IDataItem<Sentence> {
	private static final long	serialVersionUID	= -6156378613751917459L;
	private final String		string;
	private final List<String>	tokens;
	
	public Sentence(List<String> tokens) {
		// Escpae "%" characters, to avoid problems with logging and printing.
		this.tokens = Collections.unmodifiableList(ListUtils.map(tokens,
				new ListUtils.Mapper<String, String>() {
					
					@Override
					public String process(String obj) {
						return obj.replace("%", "%%");
					}
				}));
		this.string = ListUtils.join(this.tokens, " ");
	}
	
	public Sentence(String string) {
		// Escpae "%" characters, to avoid problems with logging and printing.
		this.string = string.replace("%", "%%");
		this.tokens = Collections.unmodifiableList(tokenize(this.string));
	}
	
	private static List<String> tokenize(String input) {
		final List<String> tokens = new ArrayList<String>();
		final StringTokenizer st = new StringTokenizer(input);
		while (st.hasMoreTokens()) {
			tokens.add(st.nextToken().trim());
		}
		return tokens;
	}
	
	@Override
	public boolean equals(Object obj) {
		// Tokens are not included as they created automatically and
		// deterministically from the given text
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Sentence other = (Sentence) obj;
		if (string == null) {
			if (other.string != null) {
				return false;
			}
		} else if (!string.equals(other.string)) {
			return false;
		}
		return true;
	}
	
	@Override
	public Sentence getSample() {
		return this;
	}
	
	public String getString() {
		return string;
	}
	
	public List<String> getTokens() {
		return tokens;
	}
	
	@Override
	public int hashCode() {
		// Tokens are not included as they created automatically and
		// deterministically from the given text
		final int prime = 31;
		int result = 1;
		result = prime * result + ((string == null) ? 0 : string.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return string;
	}
}
