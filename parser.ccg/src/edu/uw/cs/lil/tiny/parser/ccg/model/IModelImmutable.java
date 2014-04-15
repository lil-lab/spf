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

import java.io.Serializable;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;

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
	 * @return
	 */
	IHashVector computeFeatures(IParseStep<MR> parseStep, DI dataItem);
	
	/**
	 * Compute features for a given parsing step.
	 * 
	 * @param parseStep
	 *            Parsing step to compute features for.
	 * @param features
	 *            Feature vector to load with features. The features will be
	 *            added to the given vector.
	 * @return 'features' vector
	 */
	IHashVector computeFeatures(IParseStep<MR> parseStep, IHashVector features,
			DI dataItem);
	
	/**
	 * Compute features for a lexical item,
	 * 
	 * @param lexicalEntry
	 *            Lexical entry to compute features for.
	 * @return
	 */
	IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry);
	
	/**
	 * Compute feature for a lexical item.
	 * 
	 * @param lexicalEntry
	 *            Lexical entry to compute features for
	 * @param features
	 *            Feature vector to load with features. The features will be
	 *            added to the given vector.
	 * @return the 'features' vector
	 */
	IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry,
			IHashVector features);
	
	IDataItemModel<MR> createDataItemModel(DI dataItem);
	
	/** Return the lexicon of the model. The returned lexicon is immutable. */
	ILexiconImmutable<MR> getLexicon();
	
	/**
	 * @return Parameters vectors (immutable).
	 */
	IHashVectorImmutable getTheta();
	
	/**
	 * Verified that the given weight vector is valid (i.e., doesn't try to
	 * update invalid feature weights).
	 */
	boolean isValidWeightVector(IHashVectorImmutable vector);
	
	double score(IParseStep<MR> parseStep, DI dataItem);
	
	double score(LexicalEntry<MR> entry);
}
