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

import java.io.Serializable;

/**
 * Hash vector to store sparse vectors.
 * 
 * @author Yoav Artzi
 */
public interface IHashVector extends IHashVectorImmutable, Serializable {
	
	/**
	 * Threshold value to drop values.
	 */
	public static final double	NOISE		= 0.001;
	
	/** The value of zero */
	static final double			ZERO_VALUE	= 0.0;
	
	/**
	 * Add a given constant to all the values in the vector.
	 * 
	 * @param num
	 */
	void add(final double num);
	
	/**
	 * Remove all the values from the vector.
	 */
	void clear();
	
	/**
	 * Divide all the values by the given constant.
	 * 
	 * @param d
	 */
	void divideBy(final double d);
	
	/**
	 * Drop all the small entries according to the {@link #NOISE} constant.
	 */
	void dropSmallEntries();
	
	void multiplyBy(final double d);
	
	void set(String arg1, double value);
	
	void set(String arg1, String arg2, double value);
	
	void set(String arg1, String arg2, String arg3, double value);
	
	void set(String arg1, String arg2, String arg3, String arg4, double value);
	
	void set(String arg1, String arg2, String arg3, String arg4, String arg5,
			double value);
	
}
