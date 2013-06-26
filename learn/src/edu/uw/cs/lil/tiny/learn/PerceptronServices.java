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
package edu.uw.cs.lil.tiny.learn;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;

/**
 * Common services for validation-driven perceptron learners.
 * 
 * @author Yoav Artzi
 */
public class PerceptronServices {
	
	private PerceptronServices() {
	}
	
	public static <LF, P extends IParse<LF>, MODEL extends Model<?, LF>> IHashVector constructUpdate(
			List<P> violatingValidParses, List<P> violatingInvalidParses,
			MODEL model) {
		// Create the parameter update
		final IHashVector update = HashVectorFactory.create();
		
		// Get the update for valid violating samples
		for (final P parse : violatingValidParses) {
			parse.getAverageMaxFeatureVector().addTimesInto(
					1.0 / violatingValidParses.size(), update);
		}
		
		// Get the update for the invalid violating samples
		for (final P parse : violatingInvalidParses) {
			parse.getAverageMaxFeatureVector().addTimesInto(
					-1.0 * (1.0 / violatingInvalidParses.size()), update);
		}
		
		// Prune small entries from the update
		update.dropSmallEntries();
		
		// Validate the update
		if (!model.isValidWeightVector(update)) {
			throw new IllegalStateException("invalid update: " + update);
		}
		
		return update;
	}
	
	public static <LF, P extends IParse<LF>, MODEL extends Model<?, LF>> Pair<List<P>, List<P>> marginViolatingSets(
			MODEL model, double margin, List<P> validParses,
			List<P> invalidParses) {
		// Construct margin violating sets
		final List<P> violatingValidParses = new LinkedList<P>();
		final List<P> violatingInvalidParses = new LinkedList<P>();
		
		// Flags to mark that we inserted a parse into the violating
		// sets, so no need to check for its violation against others
		final boolean[] validParsesFlags = new boolean[validParses.size()];
		final boolean[] invalidParsesFlags = new boolean[invalidParses.size()];
		int validParsesCounter = 0;
		for (final P validParse : validParses) {
			int invalidParsesCounter = 0;
			for (final P invalidParse : invalidParses) {
				if (!validParsesFlags[validParsesCounter]
						|| !invalidParsesFlags[invalidParsesCounter]) {
					// Create the delta vector if needed, we do it only
					// once. This is why we check if we are going to
					// need it in the above 'if'.
					final IHashVector featureDelta = validParse
							.getAverageMaxFeatureVector().addTimes(-1.0,
									invalidParse.getAverageMaxFeatureVector());
					final double deltaScore = featureDelta.vectorMultiply(model
							.getTheta());
					
					// Test valid parse for insertion into violating
					// valid parses
					if (!validParsesFlags[validParsesCounter]) {
						// Case this valid sample is still not in the
						// violating set
						if (deltaScore < margin * featureDelta.l1Norm()) {
							// Case of violation
							// Add to the violating set
							violatingValidParses.add(validParse);
							// Mark flag, so we won't test it again
							validParsesFlags[validParsesCounter] = true;
						}
					}
					
					// Test invalid parse for insertion into
					// violating invalid parses
					if (!invalidParsesFlags[invalidParsesCounter]) {
						// Case this invalid sample is still not in
						// the violating set
						if (deltaScore < margin * featureDelta.l1Norm()) {
							// Case of violation
							// Add to the violating set
							violatingInvalidParses.add(invalidParse);
							// Mark flag, so we won't test it again
							invalidParsesFlags[invalidParsesCounter] = true;
						}
					}
				}
				
				// Increase the counter, as we move to the next sample
				++invalidParsesCounter;
			}
			// Increase the counter, as we move to the next sample
			++validParsesCounter;
		}
		
		return Pair.of(violatingValidParses, violatingInvalidParses);
		
	}
	
}
