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
import edu.cornell.cs.nlp.utils.math.LogSumExp;

public class HashVectorUtils {

	public HashVectorUtils() {
		// Service class. Not instantiatable.
	}

	/**
	 * Log-sum-exp the source multiplied by the given weight into the target
	 * vector.
	 *
	 * @param logWeight
	 *            Weight in log-space.
	 * @param source
	 *            Not in log-space.
	 * @param target
	 *            Assumed to be in log-space.
	 */
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

	/**
	 * Sum-log-exp the source vector into the target vector.
	 *
	 * @param source
	 *            All values are in log-space.
	 * @param target
	 *            All values are in log-space.
	 */
	public static void sumExpLogged(IHashVectorImmutable source,
			final IHashVector target) {
		source.iterate(new EntryFunction() {

			@Override
			public void apply(KeyArgs key, double value) {
				target.set(key, LogSumExp.of(
						target.get(key, Double.NEGATIVE_INFINITY), value));
			}
		});
	}

}
