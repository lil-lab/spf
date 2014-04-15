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
package edu.uw.cs.lil.tiny.parser.ccg.model;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;

public interface IDataItemModel<MR> {
	
	IHashVector computeFeatures(IParseStep<MR> parseStep);
	
	IHashVector computeFeatures(IParseStep<MR> parseStep, IHashVector features);
	
	IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry);
	
	IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry,
			IHashVector features);
	
	ILexiconImmutable<MR> getLexicon();
	
	IHashVectorImmutable getTheta();
	
	double score(IParseStep<MR> parseStep);
	
	double score(LexicalEntry<MR> entry);
	
}
