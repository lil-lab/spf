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
package edu.uw.cs.lil.tiny.parser.joint;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.utils.composites.Pair;

/**
 * Joint inference derivation that compactly holds all derivations that lead to
 * a specific result. Doesn't support fancy dynamic programming for semantics
 * evaluation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class JointDerivation<MR, ERESULT> extends
		AbstractJointDerivation<MR, ERESULT, IDerivation<MR>> {
	
	public JointDerivation(
			List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>> maxPairs,
			List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>> pairs, ERESULT result,
			double viterbiScore) {
		super(maxPairs, pairs, result, viterbiScore);
	}
	
	public static class Builder<MR, ERESULT> {
		protected final List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>	inferencePairs	= new LinkedList<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>();
		protected final ERESULT											result;
		
		public Builder(ERESULT result) {
			this.result = result;
		}
		
		public Builder<MR, ERESULT> addInferencePair(
				Pair<IDerivation<MR>, IEvaluation<ERESULT>> pair) {
			// Verify the new pair leads to the same result as the rest.
			if ((result != null || pair.second().getResult() != null)
					&& !result.equals(pair.second().getResult())) {
				throw new IllegalStateException(
						"JointDerivation can only account for a single final outcome.");
			}
			inferencePairs.add(pair);
			return this;
		}
		
		public JointDerivation<MR, ERESULT> build() {
			final Pair<List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>, Double> maxPair = createMaxPairs();
			return new JointDerivation<MR, ERESULT>(maxPair.first(),
					inferencePairs, result, maxPair.second());
		}
		
		protected Pair<List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>, Double> createMaxPairs() {
			double maxScore = -Double.MAX_VALUE;
			final List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>> maxPairs = new LinkedList<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>();
			for (final Pair<IDerivation<MR>, IEvaluation<ERESULT>> pair : inferencePairs) {
				// Viterbi score is for a linearly-weighted.
				final double score = pair.first().getScore()
						+ pair.second().getScore();
				if (score > maxScore) {
					maxScore = score;
					maxPairs.clear();
					maxPairs.add(pair);
				} else if (score == maxScore) {
					maxPairs.add(pair);
				}
			}
			return Pair.of(maxPairs, maxScore);
		}
		
	}
	
}
