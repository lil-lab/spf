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
package edu.uw.cs.lil.tiny.utils.hashvector;

import edu.uw.cs.utils.composites.Pair;

public interface IHashVectorImmutable extends Iterable<Pair<KeyArgs, Double>> {
	/**
	 * Multiply 'other' by 'times' and adds add it to the current vector and
	 * returns the result in a new one. Doesn't modify the current vector.
	 * 
	 * @param times
	 * @param other
	 * @return
	 */
	IHashVector addTimes(final double times, final IHashVectorImmutable other);
	
	/**
	 * Add this vector 'times' times into p.
	 * 
	 * @param times
	 * @param p
	 */
	void addTimesInto(final double times, final IHashVector p);
	
	boolean equals(Object obj);
	
	double get(String arg1);
	
	double get(String arg1, String arg2);
	
	double get(String arg1, String arg2, String arg3);
	
	double get(String arg1, String arg2, String arg3, String arg4);
	
	double get(String arg1, String arg2, String arg3, String arg4, String arg5);
	
	/**
	 * Get all features with the given arg1. Any of the arguments may be null.
	 * 
	 * @param arg1
	 * @return
	 */
	IHashVector getAll(String arg1);
	
	/**
	 * Get all features with the given arg1 and arg2. Any of the arguments may
	 * be null.
	 * 
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	IHashVector getAll(String arg1, String arg2);
	
	/**
	 * Get all features with the given arg1, arg2 and arg3. Any of the arguments
	 * may be null.
	 * 
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @return
	 */
	IHashVector getAll(String arg1, String arg2, String arg3);
	
	/**
	 * Get all features with the given arg1, arg2, arg3, and arg4. Any of the
	 * arguments may be null.
	 * 
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @return
	 */
	IHashVector getAll(final String arg1, final String arg2, final String arg3,
			final String arg4);
	
	/**
	 * Get all features with the given arg1, arg2, arg3, arg4 and arg5. Any of
	 * the arguments may be null.
	 * 
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 * @return
	 */
	IHashVector getAll(final String arg1, final String arg2, final String arg3,
			final String arg4, String arg5);
	
	int hashCode();
	
	boolean isBad();
	
	double l1Norm();
	
	String printValues(final IHashVectorImmutable other);
	
	int size();
	
	String toString();
	
	boolean valuesInRange(final double min, final double max);
	
	/**
	 * Vector multiplication.
	 * 
	 * @param other
	 * @return
	 */
	double vectorMultiply(final IHashVectorImmutable other);
}
