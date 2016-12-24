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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorUtils;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphDerivation;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParserOutput;
import edu.cornell.cs.nlp.spf.parser.joint.AbstractJointOutput;
import edu.cornell.cs.nlp.spf.parser.joint.InferencePair;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.filter.FilterUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.math.LogSumExp;

/**
 * Joint graph-based inference output. Doesn't support fancy dynamic programming
 * for semantics evaluation.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class JointGraphOutput<MR, ERESULT> extends
		AbstractJointOutput<MR, ERESULT, JointGraphDerivation<MR, ERESULT>>
		implements IJointGraphOutput<MR, ERESULT> {

	private final IGraphParserOutput<MR> baseOutput;

	public JointGraphOutput(IGraphParserOutput<MR> baseOutput,
			long inferenceTime,
			List<JointGraphDerivation<MR, ERESULT>> derivations,
			boolean exactEvaluation) {
		super(baseOutput, inferenceTime, derivations,
				exactEvaluation && baseOutput.isExact());
		this.baseOutput = baseOutput;
	}

	@Override
	public IGraphParserOutput<MR> getBaseParserOutput() {
		return baseOutput;
	}

	@Override
	public IHashVector logExpectedFeatures() {
		return logExpectedFeatures(FilterUtils.<ERESULT> stubTrue());
	}

	@Override
	public IHashVector logExpectedFeatures(IFilter<ERESULT> filter) {
		// Init derivations outside scores. In practice, prune the joint
		// derivation using the filter and implicitly give each an outside score
		// of log(1.0).
		final List<JointGraphDerivation<MR, ERESULT>> derivationsToUse = new LinkedList<JointGraphDerivation<MR, ERESULT>>();
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivations) {
			if (filter.test(derivation.getResult())) {
				derivationsToUse.add(derivation);
			}
		}

		// To propagate the outside scores into the graph of the base
		// output, we create a scorer that uses the outside scores of the joint
		// derivations.

		// Create a mapping for the scorer. For each root category in the chart,
		// which leads to a derivation in derivationsToUse, the map gives the
		// total outside contribution.
		final Map<Category<MR>, Double> initBaseParseOutsideScores = new HashMap<Category<MR>, Double>();
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivationsToUse) {
			for (final InferencePair<MR, ERESULT, IGraphDerivation<MR>> pair : derivation
					.getInferencePairs()) {
				final Category<MR> category = pair.getBaseDerivation()
						.getCategory();
				// The log outside contribution is the current outside score of
				// the derivation (implicitly log(1.0) = 0.0) plus the log score
				// of the evaluation.
				final double logOutsideContribution = pair.getEvaluationResult()
						.getScore();
				if (initBaseParseOutsideScores.containsKey(category)) {
					initBaseParseOutsideScores.put(category,
							LogSumExp.of(
									initBaseParseOutsideScores.get(category),
									logOutsideContribution));
				} else {
					initBaseParseOutsideScores.put(category,
							logOutsideContribution);
				}
			}
		}

		// Create the scorer.
		final IScorer<Category<MR>> scorer = e -> initBaseParseOutsideScores
				.containsKey(e) ? initBaseParseOutsideScores.get(e)
						: Double.NEGATIVE_INFINITY;

		// Get expected features from base parser output.
		final IHashVector logExpectedFeatures = baseOutput
				.logExpectedFeatures(scorer);

		// Add expected features from the execution result cells.
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivationsToUse) {
			for (final InferencePair<MR, ERESULT, IGraphDerivation<MR>> pair : derivation
					.getInferencePairs()) {
				// Explicitly adding 0.0 here to account for the outside
				// score of the evaluation, which is implicitly log(1.0) = 0.0
				// (see above).
				final double logWeight = pair.getBaseDerivation().getScore()
						+ pair.getBaseDerivation().getLogInsideScore() + 0.0;
				HashVectorUtils.logSumExpAdd(logWeight,
						pair.getEvaluationResult().getFeatures(),
						logExpectedFeatures);
			}
		}

		return logExpectedFeatures;
	}

	@Override
	public double logNorm() {
		return logNorm(FilterUtils.<ERESULT> stubTrue());
	}

	@Override
	public double logNorm(IFilter<ERESULT> filter) {
		final List<Double> logInsideScores = new ArrayList<Double>(
				derivations.size());
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivations) {
			// Test the result with the filter.
			if (filter.test(derivation.getResult())) {
				logInsideScores.add(derivation.getLogInsideScore());
			}
		}
		// Do the log-sum-exp trick and return the log of the sum.
		return LogSumExp.of(logInsideScores);
	}

	public static class Builder<MR, ERESULT> {

		private final IGraphParserOutput<MR>									baseOutput;
		private boolean															exactEvaluation	= false;
		private final List<InferencePair<MR, ERESULT, IGraphDerivation<MR>>>	inferencePairs	= new LinkedList<>();
		private final long														inferenceTime;

		public Builder(IGraphParserOutput<MR> baseOutput, long inferenceTime) {
			this.baseOutput = baseOutput;
			this.inferenceTime = inferenceTime;
		}

		public Builder<MR, ERESULT> addInferencePair(
				InferencePair<MR, ERESULT, IGraphDerivation<MR>> pair) {
			inferencePairs.add(pair);
			return this;
		}

		public Builder<MR, ERESULT> addInferencePairs(
				List<InferencePair<MR, ERESULT, IGraphDerivation<MR>>> pairs) {
			inferencePairs.addAll(pairs);
			return this;
		}

		public JointGraphOutput<MR, ERESULT> build() {
			final Map<ERESULT, JointGraphDerivation.Builder<MR, ERESULT>> builders = new HashMap<ERESULT, JointGraphDerivation.Builder<MR, ERESULT>>();
			for (final InferencePair<MR, ERESULT, IGraphDerivation<MR>> pair : inferencePairs) {
				final ERESULT pairResult = pair.getEvaluationResult()
						.getResult();
				if (!builders.containsKey(pairResult)) {
					builders.put(pairResult,
							new JointGraphDerivation.Builder<MR, ERESULT>(
									pairResult));
				}
				builders.get(pairResult).addInferencePair(pair);
			}
			// Create all derivations.
			final List<JointGraphDerivation<MR, ERESULT>> derivations = Collections
					.unmodifiableList(ListUtils.map(builders.values(),
							obj -> obj.build()));
			// Get max derivations.
			return new JointGraphOutput<MR, ERESULT>(baseOutput, inferenceTime,
					derivations, exactEvaluation);
		}

		public Builder<MR, ERESULT> setExactEvaluation(
				boolean exactEvaluation) {
			this.exactEvaluation = exactEvaluation;
			return this;
		}

	}

}
