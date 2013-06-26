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

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;

/**
 * Immutable parsing model.
 * 
 * @author Yoav Artzi
 * @param <X>
 *            type of sample
 * @param <Y>
 *            Type of semantics.
 */
public interface IModelImmutable<X, Y> {
	
	/**
	 * Compute features for a given parsing step
	 * 
	 * @param parseStep
	 *            Parsing step to compute features for.
	 * @return
	 */
	IHashVector computeFeatures(IParseStep<Y> parseStep, IDataItem<X> dataItem);
	
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
	IHashVector computeFeatures(IParseStep<Y> parseStep, IHashVector features,
			IDataItem<X> dataItem);
	
	/**
	 * Compute features for a lexical item,
	 * 
	 * @param lexicalEntry
	 *            Lexical entry to compute features for.
	 * @return
	 */
	IHashVector computeFeatures(LexicalEntry<Y> lexicalEntry);
	
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
	IHashVector computeFeatures(LexicalEntry<Y> lexicalEntry,
			IHashVector features);
	
	IDataItemModel<Y> createDataItemModel(IDataItem<X> dataItem);
	
	/** Return the lexicon of the model. The returned lexicon is immutable. */
	ILexiconImmutable<Y> getLexicon();
	
	/**
	 * @return Parameters vectors (immutable).
	 */
	IHashVectorImmutable getTheta();
	
	double score(IParseStep<Y> parseStep, IDataItem<X> dataItem);
	
	double score(LexicalEntry<Y> entry);
}
