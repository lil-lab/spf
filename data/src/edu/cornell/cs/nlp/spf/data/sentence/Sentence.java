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
package edu.cornell.cs.nlp.spf.data.sentence;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.utils.collections.ListUtils;

/**
 * Representing a single sentence.
 *
 * @author Yoav Artzi
 */
public class Sentence implements IDataItem<Sentence> {
	private static ITokenizer	DEFAULT_TOKENIZER	= new ITokenizer() {

														@Override
														public TokenSeq tokenize(
																String sentence) {
															final List<String> tokens = new ArrayList<String>();
															final StringTokenizer st = new StringTokenizer(
																	sentence);
															while (st
																	.hasMoreTokens()) {
																tokens.add(st
																		.nextToken()
																		.trim());
															}
															return TokenSeq
																	.of(tokens);
														}
													};

	private static final long	serialVersionUID	= -6156378613751917459L;
	private final String		string;

	private final TokenSeq		tokens;

	public Sentence(Sentence other) {
		this.tokens = other.tokens;
		this.string = other.string;
	}

	public Sentence(String string) {
		this(string, DEFAULT_TOKENIZER);
	}

	public Sentence(String string, ITokenizer tokenizer) {
		// Escpae "%" characters, to avoid problems with logging and printing.
		final TokenSeq rawTokens = tokenizer.tokenize(string);
		this.string = string.replace("%", "%%");
		this.tokens = TokenSeq.of(ListUtils.map(rawTokens.toList(),
				new ListUtils.Mapper<String, String>() {

					@Override
					public String process(String obj) {
						return obj.replace("%", "%%");
					}
				}));
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
		final Sentence other = (Sentence) obj;
		if (string == null) {
			if (other.string != null) {
				return false;
			}
		} else if (!string.equals(other.string)) {
			return false;
		}
		if (tokens == null) {
			if (other.tokens != null) {
				return false;
			}
		} else if (!tokens.equals(other.tokens)) {
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

	public TokenSeq getTokens() {
		return tokens;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (string == null ? 0 : string.hashCode());
		result = prime * result + (tokens == null ? 0 : tokens.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return string;
	}
}
