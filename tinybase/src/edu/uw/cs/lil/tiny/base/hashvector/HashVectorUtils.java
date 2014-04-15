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
package edu.uw.cs.lil.tiny.base.hashvector;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector.EntryFunction;
import edu.uw.cs.utils.math.LogSumExp;

public class HashVectorUtils {
	
	public HashVectorUtils() {
		// Service class. Not instantiatable.
	}
	
	public static void logSumExpAdd(final double logWeight,
			IHashVectorImmutable source, final IHashVector target) {
		source.iterate(new EntryFunction() {
			@Override
			public void apply(KeyArgs key, double value) {
				// Compute the log of the weight time each feature,
				// and aggregate it into the target vector.
				target.set(key, LogSumExp.of(
						target.get(key, Double.NEGATIVE_INFINITY),
						Math.log(value) + logWeight));
			}
		});
		
	}
	
}
