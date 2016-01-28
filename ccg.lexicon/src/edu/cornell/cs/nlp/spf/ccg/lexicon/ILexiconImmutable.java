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
import java.util.Iterator;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;

public interface ILexiconImmutable<MR> extends Serializable {

	boolean contains(LexicalEntry<MR> lex);

	Iterator<? extends LexicalEntry<MR>> get(TokenSeq tokens);

	int size();

	Collection<LexicalEntry<MR>> toCollection();
}
