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
package edu.cornell.cs.nlp.spf.parser.joint.graph;

import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.parser.graph.IGraphDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.AbstractJointDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.InferencePair;
import edu.cornell.cs.nlp.utils.math.LogSumExp;

/**
 * Joint graph-based inference derivation that compactly holds all derivations
 * that lead to a specific result. Doesn't support fancy dynamic programming for
 * semantics evaluation. Provides the exponentiated inside score.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class JointGraphDerivation<MR, ERESULT>
		extends AbstractJointDerivation<MR, ERESULT, IGraphDerivation<MR>>
		implements IJointGraphDerivation<MR, ERESULT> {

	private final double logInsideScore;

	public JointGraphDerivation(
			List<InferencePair<MR, ERESULT, IGraphDerivation<MR>>> maxPairs,
			List<InferencePair<MR, ERESULT, IGraphDerivation<MR>>> pairs,
			ERESULT result, double viterbiScore, double logInsideScore) {
		super(maxPairs, pairs, result, viterbiScore);
		assert !Double.isInfinite(logInsideScore);
		this.logInsideScore = logInsideScore;
	}

	@Override
	public double getLogInsideScore() {
		return logInsideScore;
	}

	@Override
	public double getScore() {
		return getViterbiScore();
	}

	@Override
	public String toString() {
		return String.format("[score=%.2f, v=%.2f, i=%.2f] %s", getScore(),
				getViterbiScore(), getLogInsideScore(), getResult().toString());
	}

	public static class Builder<MR, ERESULT> {
		protected final List<InferencePair<MR, ERESULT, IGraphDerivation<MR>>>	inferencePairs	= new LinkedList<>();
		protected final ERESULT													result;

		public Builder(ERESULT result) {
			this.result = result;
		}

		public Builder<MR, ERESULT> addInferencePair(
				InferencePair<MR, ERESULT, IGraphDerivation<MR>> pair) {
			// Verify the new pair leads to the same result as the rest.
			if ((result != null
					|| pair.getEvaluationResult().getResult() != null)
					&& !result.equals(pair.getEvaluationResult().getResult())) {
				throw new IllegalStateException(
						"JointDerivation can only account for a single final outcome.");
			}
			inferencePairs.add(pair);
			return this;
		}

		public JointGraphDerivation<MR, ERESULT> build() {
			double maxScore = -Double.MAX_VALUE;
			double logInsideScore = Double.NEGATIVE_INFINITY;
			final List<InferencePair<MR, ERESULT, IGraphDerivation<MR>>> maxPairs = new LinkedList<>();
			for (final InferencePair<MR, ERESULT, IGraphDerivation<MR>> pair : inferencePairs) {
				// Viterbi score is for a linearly-weighted.
				final double score = pair.getBaseDerivation().getScore()
						+ pair.getEvaluationResult().getScore();
				if (score > maxScore) {
					maxScore = score;
					maxPairs.clear();
					maxPairs.add(pair);
				} else if (score == maxScore) {
					maxPairs.add(pair);
				}
				logInsideScore = LogSumExp.of(logInsideScore,
						pair.getBaseDerivation().getLogInsideScore()
								+ pair.getEvaluationResult().getScore());
			}

			return new JointGraphDerivation<MR, ERESULT>(maxPairs,
					inferencePairs, result, maxScore, logInsideScore);
		}
	}

}
