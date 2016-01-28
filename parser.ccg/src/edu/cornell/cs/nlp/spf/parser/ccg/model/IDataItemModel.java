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
package edu.cornell.cs.nlp.spf.parser.ccg.model;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;

public interface IDataItemModel<MR> {

	/**
	 * @see IModelImmutable#computeFeatures(IParseStep,
	 *      edu.cornell.cs.nlp.spf.data.IDataItem)
	 */
	IHashVector computeFeatures(IParseStep<MR> parseStep);

	/**
	 * @see IModelImmutable#computeFeatures(LexicalEntry)
	 */
	IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry);

	/**
	 * @see IModelImmutable#getLexicon()
	 */
	ILexiconImmutable<MR> getLexicon();

	/**
	 * @see IModelImmutable#getTheta()
	 */
	IHashVectorImmutable getTheta();

	/**
	 * @see IModelImmutable#score(IHashVectorImmutable)
	 */
	double score(IHashVectorImmutable features);

	/**
	 * @see IModelImmutable#score(LexicalEntry)
	 */
	double score(LexicalEntry<MR> entry);

}
