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
package edu.uw.cs.lil.tiny.parser.ccg.model.init;

import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelInit;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;

/**
 * Init a lexicon with a set lexical entries.
 * 
 * @author Yoav Artzi
 * @param <X>
 * @param <Y>
 */
public class LexiconModelInit<X, Y> implements IModelInit<X, Y> {
	
	private final boolean		fixed;
	private final ILexicon<Y>	lexicon;
	
	public LexiconModelInit(ILexicon<Y> lexicon, boolean fixed) {
		this.lexicon = lexicon;
		this.fixed = fixed;
	}
	
	@Override
	public void init(Model<X, Y> model) {
		if (fixed) {
			model.addFixedLexicalEntries(lexicon.toCollection());
		} else {
			model.addLexEntries(lexicon.toCollection());
		}
	}
	
}
