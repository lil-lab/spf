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
package edu.cornell.cs.nlp.spf.base.hashvector;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector.EntryFunction;
import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * The immutable portion of {@link IHashVector}.
 *
 * @author Yoav Artzi
 */
public interface IHashVectorImmutable extends Iterable<Pair<KeyArgs, Double>> {

	/**
	 * Multiply 'other' by 'times' and adds add it to the current vector and
	 * returns the result in a new one. Doesn't modify the current vector.
	 */
	IHashVector addTimes(double times, IHashVectorImmutable other);

	/**
	 * Add this vector 'times' times into 'other'.
	 */
	void addTimesInto(double times, IHashVector other);

	boolean contains(KeyArgs key);

	boolean contains(String arg1);

	boolean contains(String arg1, String arg2);

	boolean contains(String arg1, String arg2, String arg3);

	boolean contains(String arg1, String arg2, String arg3, String arg4);

	boolean contains(String arg1, String arg2, String arg3, String arg4,
			String arg5);

	/**
	 * Vector dot-product.
	 */
	double dotProduct(IHashVectorImmutable other);

	@Override
	boolean equals(Object obj);

	double get(KeyArgs key);

	/**
	 * Get the value for the given key. If the key is not contained in the
	 * vector, return defaultValue.
	 */
	double get(KeyArgs key, double defaultReturn);

	double get(String arg1);

	/**
	 * Get the value for the given key. If the key is not contained in the
	 * vector, return defaultValue. The key is composed of the given arg1 with
	 * all positions being null.
	 */
	double get(String arg1, double defaultReturn);

	double get(String arg1, String arg2);

	/**
	 * Get the value for the given key. If the key is not contained in the
	 * vector, return defaultValue. The key is composed of the given arg1 and
	 * arg2 with all positions being null.
	 */
	double get(String arg1, String arg2, double defaultReturn);

	double get(String arg1, String arg2, String arg3);

	/**
	 * Get the value for the given key. If the key is not contained in the
	 * vector, return defaultValue. The key is composed of the given arg1, arg2
	 * and arg3 with all positions being null.
	 */
	double get(String arg1, String arg2, String arg3, double defaultReturn);

	double get(String arg1, String arg2, String arg3, String arg4);

	/**
	 * Get the value for the given key. If the key is not contained in the
	 * vector, return defaultValue. The key is composed of the given arg1, arg2,
	 * arg3 and arg4 with all positions being null.
	 */
	double get(String arg1, String arg2, String arg3, String arg4,
			double defaultReturn);

	double get(String arg1, String arg2, String arg3, String arg4, String arg5);

	/**
	 * Get the value for the given key. If the key is not contained in the
	 * vector, return defaultValue. The key is composed of the given arg1, arg2,
	 * arg3, arg4 and arg5.
	 */
	double get(String arg1, String arg2, String arg3, String arg4, String arg5,
			double defaultReturn);

	/**
	 * Get all features for which the given partialKey contains their key.
	 *
	 * @see KeyArgs#contains(KeyArgs)
	 */
	IHashVector getAll(KeyArgs partialKey);

	/**
	 * Get all features with the given arg1. Any of the arguments may be null.
	 */
	IHashVector getAll(String arg1);

	/**
	 * Get all features with the given arg1 and arg2. Any of the arguments may
	 * be null.
	 */
	IHashVector getAll(String arg1, String arg2);

	/**
	 * Get all features with the given arg1, arg2 and arg3. Any of the arguments
	 * may be null.
	 */
	IHashVector getAll(String arg1, String arg2, String arg3);

	/**
	 * Get all features with the given arg1, arg2, arg3, and arg4. Any of the
	 * arguments may be null.
	 */
	IHashVector getAll(String arg1, String arg2, String arg3, String arg4);

	/**
	 * Get all features with the given arg1, arg2, arg3, arg4 and arg5. Any of
	 * the arguments may be null.
	 */
	IHashVector getAll(String arg1, String arg2, String arg3, String arg4,
			String arg5);

	@Override
	int hashCode();

	boolean isBad();

	/**
	 * Indicates if the vector has a special initialization value.
	 */
	boolean isInit();

	/**
	 * Iterate over all members in the vector. For each execute the given
	 * function. Doesn't modify the vector.
	 */
	void iterate(EntryFunction function);

	double l1Norm();

	/**
	 * Do pair-wise product. Each value in the current vector is multiplied by
	 * the respective value in 'other'. The result vector is returned as a new
	 * vector.
	 *
	 * @return New hash vector.
	 */
	IHashVector pairWiseProduct(IHashVectorImmutable other);

	/**
	 * Print the (key, value) pairs of 'other' along with the values from this
	 * vector.
	 */
	String printValues(IHashVectorImmutable other);

	int size();

	@Override
	String toString();

	boolean valuesInRange(double min, double max);
}
