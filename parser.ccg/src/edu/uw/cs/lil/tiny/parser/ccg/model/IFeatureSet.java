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
import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.utils.composites.Triplet;

public interface IFeatureSet extends Serializable {
	/**
	 * Returns all the weights of the features represented by this feature set.
	 * 
	 * @return List of triplets of <hash vector key, weight value, optional
	 *         comment (might be null)>
	 */
	List<Triplet<KeyArgs, Double, String>> getFeatureWeights(IHashVector theta);
	
	/**
	 * Validate the weight vector. This validation process may check for
	 * reference to the default features set by this feature vector (that should
	 * never be updated into the model).
	 * 
	 * @param update
	 * @return
	 */
	boolean isValidWeightVector(IHashVectorImmutable vector);
	
}
