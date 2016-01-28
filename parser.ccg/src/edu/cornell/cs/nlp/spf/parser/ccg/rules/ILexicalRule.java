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
package edu.cornell.cs.nlp.spf.parser.ccg.rules;

import java.io.Serializable;
import java.util.Iterator;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;

/**
 * Lexical parsing rule. Given a {@link ILexiconImmutable}, a {@link TokenSeq}
 * and a {@link SentenceSpan}, generates a set of {@link Category}s. The set is
 * never enumerated. Instead an iterator is returned.
 *
 * @author Yoav Artzi
 */
public interface ILexicalRule<MR> extends Serializable {

	Iterator<LexicalResult<MR>> apply(TokenSeq tokens, SentenceSpan span,
			ILexiconImmutable<MR> lexicon);

	@Override
	boolean equals(Object obj);

	UnaryRuleName getName();

	@Override
	int hashCode();

}
