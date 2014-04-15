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
package edu.uw.cs.lil.tiny.parser.joint.injective.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorUtils;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParser;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.tiny.parser.joint.injective.AbstractInjectiveJointOutput;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.math.LogSumExp;

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
			public boolean isValid(ERESULT e) {
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
		final Map<MR, Double> initBaseParseLogOutsideScores = new HashMap<MR, Double>();
		for (final ResultCell cell : resultCells.values()) {
			// Init result cell outside scores.
			cell.initLogOutsideScore(filter);
			// Iterate over over all joint parses to create the initialization
			// score for the base parse logical form.
			for (final InjectiveJointGraphDerivation<MR, ERESULT> parse : cell.parses) {
				final MR semantics = parse.getBaseParse().getSemantics();
				final double logOutsideContribution = cell.logOutsideScore
						+ parse.getExecResult().getScore();
				if (initBaseParseLogOutsideScores.containsKey(semantics)) {
					initBaseParseLogOutsideScores.put(semantics, LogSumExp.of(
							initBaseParseLogOutsideScores.get(semantics),
							logOutsideContribution));
				} else {
					initBaseParseLogOutsideScores.put(semantics,
							logOutsideContribution);
				}
				
			}
		}
		
		// Create the scorer.
		final IScorer<MR> scorer = new IScorer<MR>() {
			@Override
			public double score(MR e) {
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
			public boolean isValid(ERESULT e) {
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
			if (filter.isValid(cell.result)) {
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
			if (filter.isValid(result)) {
				logOutsideScore = 0.0;
			} else {
				logOutsideScore = Double.NEGATIVE_INFINITY;
			}
		}
		
	}
	
}
