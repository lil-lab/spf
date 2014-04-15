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

import java.io.File;
import java.util.Collection;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.string.IStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;

/**
 * Lexicon containing a collection of lexical entries that match textual tokens.
 * 
 * @author Luke Zettlemoyer
 */
public interface ILexicon<MR> extends ILexiconImmutable<MR> {
	
	Set<LexicalEntry<MR>> add(LexicalEntry<MR> lex);
	
	Set<LexicalEntry<MR>> addAll(Collection<LexicalEntry<MR>> entries);
	
	Set<LexicalEntry<MR>> addAll(ILexicon<MR> lexicon);
	
	Set<LexicalEntry<MR>> addEntriesFromFile(File file,
			ICategoryServices<MR> categoryServices, String origin);
	
	Set<LexicalEntry<MR>> addEntriesFromFile(File file,
			IStringFilter textFilter, ICategoryServices<MR> categoryServices,
			String origin);
	
	boolean retainAll(Collection<LexicalEntry<MR>> entries);
	
	boolean retainAll(ILexicon<MR> entries);
	
}
