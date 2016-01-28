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

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;

/**
 * Immutable parsing model.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            type of data item
 * @param <MR>
 *            Type of semantics.
 */
public interface IModelImmutable<DI extends IDataItem<?>, MR> extends
		Serializable {

	/**
	 * Compute features for a given parsing step
	 *
	 * @param parseStep
	 *            Parsing step to compute features for.
	 */
	IHashVector computeFeatures(IParseStep<MR> parseStep, DI dataItem);

	/**
	 * Compute features for a lexical item,
	 *
	 * @param lexicalEntry
	 *            Lexical entry to compute features for.
	 * @return
	 */
	IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry);

	IDataItemModel<MR> createDataItemModel(DI dataItem);

	/** Return the lexicon of the model. The returned lexicon is immutable. */
	ILexiconImmutable<MR> getLexicon();

	IHashVectorImmutable getTheta();

	/**
	 * Verified that the given weight vector is valid (i.e., doesn't try to
	 * update invalid feature weights).
	 */
	boolean isValidWeightVector(IHashVectorImmutable vector);

	/**
	 * Given a feature vector, returns its model score. Does dot-product with
	 * the current parameters.
	 */
	double score(IHashVectorImmutable features);

	double score(LexicalEntry<MR> entry);
}
