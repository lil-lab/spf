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
package edu.uw.cs.lil.tiny.learn.weakp.loss;

import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.utils.composites.Pair;

/**
 * Uniform weight function. Gives an equal weight to all parses, so the sum
 * weight for all optimal parses is 1.0 and the sum weight for all non-optimal
 * parses is 1.0.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public class UniformWeightFunction<Y> implements IUpdateWeightFunction<Y> {
	private final double	valueForNonOptimalParses;
	private final double	valueForOptimalParses;
	
	public UniformWeightFunction(int numViolatingOptimalParses,
			int numViolatingNonOptimalParses) {
		this.valueForOptimalParses = 1.0 / numViolatingOptimalParses;
		this.valueForNonOptimalParses = 1.0 / numViolatingNonOptimalParses;
	}
	
	@Override
	public double evalNonOptimalParse(
			Pair<Double, ? extends IParse<Y>> nonOptimalParse) {
		return valueForNonOptimalParses;
	}
	
	@Override
	public double evalOptimalParse(
			Pair<Double, ? extends IParse<Y>> optimalParse) {
		return valueForOptimalParses;
	}
}
