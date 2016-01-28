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
package edu.cornell.cs.nlp.spf.parser.joint.injective.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorUtils;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParser;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParserOutput;
import edu.cornell.cs.nlp.spf.parser.joint.graph.IJointGraphOutput;
import edu.cornell.cs.nlp.spf.parser.joint.injective.AbstractInjectiveJointOutput;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.math.LogSumExp;

/**
 * Output for joint inference of parsing and semantics evaluation using a
 * graph-base parser ({@link IGraphParser}). Simplifies joint inference by
 * assuming a injective semantic evaluation (i.e., a one-to-one matching between
 * the meaning representation and its evaluation result).
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class InjectiveJointGraphOutput<MR, ERESULT>
		extends
		AbstractInjectiveJointOutput<MR, ERESULT, InjectiveJointGraphDerivation<MR, ERESULT>>
		implements IJointGraphOutput<MR, ERESULT> {

	private final IGraphParserOutput<MR>	baseParserOutput;

	private final Map<ERESULT, ResultCell>	resultCells	= new HashMap<ERESULT, InjectiveJointGraphOutput<MR, ERESULT>.ResultCell>();

	public InjectiveJointGraphOutput(IGraphParserOutput<MR> baseParserOutput,
			List<InjectiveJointGraphDerivation<MR, ERESULT>> jointParses,
			long parsingTime, boolean exactEvaluation) {
		super(jointParses, parsingTime, exactEvaluation
				&& baseParserOutput.isExact());
		this.baseParserOutput = baseParserOutput;
		for (final InjectiveJointGraphDerivation<MR, ERESULT> parse : jointParses) {
			if (resultCells.containsKey(parse.getResult())) {
				resultCells.get(parse.getResult()).addParse(parse);
			} else {
				resultCells.put(parse.getResult(),
						new ResultCell(parse.getResult(), parse));
			}
		}
	}

	@Override
	public IGraphParserOutput<MR> getBaseParserOutput() {
		return baseParserOutput;
	}

	@Override
	public IHashVector logExpectedFeatures() {
		return logExpectedFeatures(new IFilter<ERESULT>() {

			@Override
			public boolean test(ERESULT e) {
				return true;
			}
		});
	}

	@Override
	public IHashVector logExpectedFeatures(IFilter<ERESULT> filter) {
		// To propagate the outside scores into the graph of the base output, we
		// create a scorer that uses the outside scores of the joint
		// derivations. Preparing a mapping of logical forms to their initial
		// outside scores. These scores are summation over all execution root
		// (cells) of the execution step score times the outside score of that
		// cell. Iterate over result cells. For each cell, first init result
		// cells outside scores. Then iterate over all parses and sum the
		// outside contribution.
		final Map<Category<MR>, Double> initBaseParseLogOutsideScores = new HashMap<Category<MR>, Double>();
		for (final ResultCell cell : resultCells.values()) {
			// Init result cell outside scores.
			cell.initLogOutsideScore(filter);
			// Iterate over over all joint parses to create the initialization
			// score for the base parse logical form.
			for (final InjectiveJointGraphDerivation<MR, ERESULT> parse : cell.parses) {
				final Category<MR> category = parse.getBaseParse()
						.getCategory();
				final double logOutsideContribution = cell.logOutsideScore
						+ parse.getExecResult().getScore();
				if (initBaseParseLogOutsideScores.containsKey(category)) {
					initBaseParseLogOutsideScores.put(category, LogSumExp.of(
							initBaseParseLogOutsideScores.get(category),
							logOutsideContribution));
				} else {
					initBaseParseLogOutsideScores.put(category,
							logOutsideContribution);
				}

			}
		}

		// Create the scorer.
		final IScorer<Category<MR>> scorer = new IScorer<Category<MR>>() {
			@Override
			public double score(Category<MR> e) {
				// If the MR was executed and has a joint parse, it has an
				// outside score, so use it, otherwise, consider as if the score
				// of the execution is -\inf (if exponentiated it will be 0.0).
				return initBaseParseLogOutsideScores.containsKey(e) ? initBaseParseLogOutsideScores
						.get(e) : Double.NEGATIVE_INFINITY;
			}
		};

		// Get expected features from base parser output.
		final IHashVector expectedFeatures = baseParserOutput
				.logExpectedFeatures(scorer);

		// Add expected features from the execution result cells.
		for (final ResultCell cell : resultCells.values()) {
			for (final InjectiveJointGraphDerivation<MR, ERESULT> parse : cell.parses) {
				final double logWeight = parse.getExecResult().getScore()
						+ parse.getBaseParse().getLogInsideScore()
						* cell.logOutsideScore;
				HashVectorUtils.logSumExpAdd(logWeight, parse.getExecResult()
						.getFeatures(), expectedFeatures);
			}
		}

		return expectedFeatures;
	}

	@Override
	public double logNorm() {
		return logNorm(new IFilter<ERESULT>() {

			@Override
			public boolean test(ERESULT e) {
				return true;
			}
		});
	}

	@Override
	public double logNorm(IFilter<ERESULT> filter) {
		final List<Double> logInsideScores = new ArrayList<Double>(resultCells
				.values().size());
		for (final ResultCell cell : resultCells.values()) {
			// Test the result with the filter.
			if (filter.test(cell.result)) {
				logInsideScores.add(cell.logInsideScore);
			}
		}
		// Do the log-sum-exp trick and return the log of the sum.
		return LogSumExp.of(logInsideScores);
	}

	private class ResultCell {
		private double													logInsideScore	= Double.NEGATIVE_INFINITY;
		private double													logOutsideScore	= Double.NEGATIVE_INFINITY;
		private final List<InjectiveJointGraphDerivation<MR, ERESULT>>	parses			= new LinkedList<InjectiveJointGraphDerivation<MR, ERESULT>>();
		private final ERESULT											result;

		public ResultCell(ERESULT result,
				InjectiveJointGraphDerivation<MR, ERESULT> parse) {
			this.result = result;
			addParse(parse);
		}

		public void addParse(InjectiveJointGraphDerivation<MR, ERESULT> parse) {
			// Add to parse list
			parses.add(parse);

			// Update the cell's inside score. Execution involves a single step,
			// so take a product of the inside score of the base parse and the
			// exponent of the local score of the execution step.
			final double parseLogInsideScore = parse.getExecResult().getScore()
					+ parse.getBaseParse().getLogInsideScore();
			logInsideScore = LogSumExp.of(logInsideScore, parseLogInsideScore);
		}

		public void initLogOutsideScore(IFilter<ERESULT> filter) {
			if (filter.test(result)) {
				logOutsideScore = 0.0;
			} else {
				logOutsideScore = Double.NEGATIVE_INFINITY;
			}
		}

	}

}
