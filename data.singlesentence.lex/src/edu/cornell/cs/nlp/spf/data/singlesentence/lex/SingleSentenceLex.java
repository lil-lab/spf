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
package edu.cornell.cs.nlp.spf.data.singlesentence.lex;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

/**
 * {@link SingleSentence} with a set of lexical entries.
 *
 * @author Yoav Artzi
 */
public class SingleSentenceLex extends SingleSentence {

	private static final long							serialVersionUID	= -5718824480690161304L;
	private final Set<LexicalEntry<LogicalExpression>>	entries;

	public SingleSentenceLex(Sentence sentence, LogicalExpression semantics) {
		this(sentence, semantics, Collections
				.<LexicalEntry<LogicalExpression>> emptySet());
	}

	public SingleSentenceLex(Sentence sentence, LogicalExpression semantics,
			Set<LexicalEntry<LogicalExpression>> entries) {
		this(sentence, semantics, entries, Collections
				.<String, String> emptyMap());
	}

	public SingleSentenceLex(Sentence sentence, LogicalExpression semantics,
			Set<LexicalEntry<LogicalExpression>> entries,
			Map<String, String> properties) {
		super(sentence, semantics, properties);
		this.entries = Collections.unmodifiableSet(entries);

	}

	public Set<LexicalEntry<LogicalExpression>> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		for (final LexicalEntry<LogicalExpression> entry : entries) {
			sb.append("\n").append(entry);
		}
		return sb.toString();
	}

}
