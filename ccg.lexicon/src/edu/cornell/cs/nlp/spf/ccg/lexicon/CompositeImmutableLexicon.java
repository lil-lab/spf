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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.utils.collections.CompositeCollection;
import edu.cornell.cs.nlp.utils.collections.iterators.CompositeIterator;

/**
 * A sequential composition of {@link ILexiconImmutable}s.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class CompositeImmutableLexicon<MR> implements ILexiconImmutable<MR> {

	private static final long							serialVersionUID	= -1974172665647205685L;
	private final List<? extends ILexiconImmutable<MR>>	lexicons;

	public CompositeImmutableLexicon(
			List<? extends ILexiconImmutable<MR>> lexicons) {
		this.lexicons = lexicons;
	}

	@Override
	public boolean contains(LexicalEntry<MR> lex) {
		for (final ILexiconImmutable<MR> lexicon : lexicons) {
			if (lexicon.contains(lex)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<? extends LexicalEntry<MR>> get(TokenSeq tokens) {
		final List<Iterator<? extends LexicalEntry<MR>>> iterators = new ArrayList<Iterator<? extends LexicalEntry<MR>>>(
				lexicons.size());
		for (final ILexiconImmutable<MR> lexicon : lexicons) {
			iterators.add(lexicon.get(tokens));
		}
		return new CompositeIterator<LexicalEntry<MR>>(iterators);
	}

	@Override
	public int size() {
		int sum = 0;
		for (final ILexiconImmutable<MR> lexicon : lexicons) {
			sum += lexicon.size();
		}
		return sum;
	}

	@Override
	public Collection<LexicalEntry<MR>> toCollection() {
		final List<Collection<LexicalEntry<MR>>> collections = new ArrayList<Collection<LexicalEntry<MR>>>(
				lexicons.size());
		for (final ILexiconImmutable<MR> lexicon : lexicons) {
			collections.add(lexicon.toCollection());
		}
		return new CompositeCollection<LexicalEntry<MR>>(collections);
	}

}
