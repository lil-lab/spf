/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
package edu.uw.cs.lil.tiny.parser.ccg.lexicon;

import java.io.File;
import java.util.Collection;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.utils.string.IStringFilter;

/**
 * Lexicon containing a collection of lexical entries that match textual tokens.
 * 
 * @author Luke Zettlemoyer
 */
public interface ILexicon<Y> extends ILexiconImmutable<Y> {
	
	boolean add(LexicalEntry<Y> lex);
	
	boolean addAll(Collection<LexicalEntry<Y>> entries);
	
	boolean addAll(ILexicon<Y> lexicon);
	
	void addEntriesFromFile(File file, IStringFilter textFilter,
			ICategoryServices<Y> categoryServices, String origin);
	
	boolean retainAll(Collection<LexicalEntry<Y>> entries);
	
	boolean retainAll(ILexicon<Y> entries);
	
}
