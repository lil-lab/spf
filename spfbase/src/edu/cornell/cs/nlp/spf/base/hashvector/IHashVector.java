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

import java.io.Serializable;

/**
 * Hash vector to store sparse vectors. Any key not present in the vector is
 * mapped to {@value #ZERO_VALUE} by default. However, not all keys for which
 * the vector returns the {@value #ZERO_VALUE} are not present. To check
 * membership, use the contain methods.
 *
 * @author Yoav Artzi
 */
public interface IHashVector extends IHashVectorImmutable, Serializable {

	/**
	 * Threshold value to drop values.
	 */
	public static final double	NOISE		= 0.001;

	/**
	 * The value of zero. Quite a few implementation decisions depend on this
	 * being 0.0. Beware of changing this value.
	 */
	static final double			ZERO_VALUE	= 0.0;

	/**
	 * Add a given constant to all the values in the vector.
	 */
	void add(double num);

	void add(KeyArgs key, double value);

	/**
	 * Add the given value to the current value paired with this key.
	 */
	void add(String arg1, double value);

	void add(String arg1, String arg2, double value);

	void add(String arg1, String arg2, String arg3, double value);

	void add(String arg1, String arg2, String arg3, String arg4, double value);

	void add(String arg1, String arg2, String arg3, String arg4, String arg5,
			double value);

	/**
	 * Apply a function to modify each value in the sparse vector. The method is
	 * guaranteed to preserve {@link IHashVector#ZERO_VALUE}s. Meaning, if a
	 * value equals {@link IHashVector#ZERO_VALUE} it will remain the same.
	 */
	void applyFunction(ValueFunction function);

	/**
	 * Remove all the values from the vector.
	 */
	void clear();

	void divideBy(double d);

	/**
	 * Drop all the small entries according to the {@link #NOISE} constant.
	 */
	void dropNoise();

	/**
	 * Drop features with {@value #ZERO_VALUE} value.
	 */
	void dropZeros();

	/**
	 * Multiply each value by the given value.
	 */
	void multiplyBy(double value);

	/**
	 * Set given value for the provided given.
	 */
	void set(KeyArgs key, double value);

	/**
	 * Set given value for the key where the first portion is arg1, and the rest
	 * is null.
	 */
	void set(String arg1, double value);

	void set(String arg1, String arg2, double value);

	void set(String arg1, String arg2, String arg3, double value);

	void set(String arg1, String arg2, String arg3, String arg4, double value);

	void set(String arg1, String arg2, String arg3, String arg4, String arg5,
			double value);

	/**
	 * Function to process a key and its value. Doesn't modify the vector.
	 *
	 * @author Yoav Artzi
	 */
	public interface EntryFunction {
		void apply(KeyArgs key, double value);
	}

	/**
	 * Function from double to double.
	 *
	 * @author Yoav Artzi
	 */
	public interface ValueFunction {
		double apply(double value);
	}

}
