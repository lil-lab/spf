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
package edu.uw.cs.lil.tiny.parser.ccg.model.lexical;

import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;

/**
 * Lexical feature set that is independent of the data item.
 * 
 * @author Yoav Artzi
 */
public interface IIndependentLexicalFeatureSet<X, Y> extends
		ILexicalFeatureSet<X, Y> {
	double score(LexicalEntry<Y> lexicalEntry, IHashVector theta);
	
	void setFeats(LexicalEntry<Y> lexicalEntry, IHashVector features);
	
}
