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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.parser.graph.IGraphParser;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.tiny.parser.joint.injective.AbstractInjectiveJointOutput;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.filter.IFilter;

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
	public IHashVector expectedFeatures() {
		return expectedFeatures(new IFilter<ERESULT>() {
			
			@Override
			public boolean isValid(ERESULT e) {
				return true;
			}
		});
	}
	
	@Override
	public IHashVector expectedFeatures(IFilter<ERESULT> filter) {
		// To propagate the outside scores into the graph of the base output, we
		// create a scorer that uses the outside scores of the joint
		// derivations. Preparing a mapping of logical forms to their initial
		// outside scores. These scores are summation over all execution root
		// (cells) of the execution step score times the outside score of that
		// cell. Iterate over result cells. For each cell, first init result
		// cells outside scores. Then iterate over all parses and sum the
		// outside contribution.
		final Map<MR, Double> initBaseParseOutsideScores = new HashMap<MR, Double>();
		for (final ResultCell cell : resultCells.values()) {
			// Init result cell outside scores.
			cell.initOutsideScore(filter);
			// Iterate over over all joint parses to create the initialization
			// score for the base parse logical form.
			for (final InjectiveJointGraphDerivation<MR, ERESULT> parse : cell.parses) {
				final MR semantics = parse.getBaseParse().getSemantics();
				final double outsideContribution = cell.getOutsideScore()
						* Math.exp(parse.getExecResult().getScore());
				if (initBaseParseOutsideScores.containsKey(semantics)) {
					initBaseParseOutsideScores.put(semantics,
							initBaseParseOutsideScores.get(semantics)
									+ outsideContribution);
				} else {
					initBaseParseOutsideScores.put(semantics,
							outsideContribution);
				}
				
			}
		}
		
		// Create the scorer.
		final IScorer<MR> scorer = new IScorer<MR>() {
			@Override
			public double score(MR e) {
				// If the MR was executed and has a joint parse, it has an
				// outside score, so use it, otherwise, consider as if the score
				// of the execution is -\inf --> return 0.
				return initBaseParseOutsideScores.containsKey(e) ? initBaseParseOutsideScores
						.get(e) : 0.0;
			}
		};
		
		// Get expected features from base parser output.
		final IHashVector expectedFeatures = baseParserOutput
				.expectedFeatures(scorer);
		
		// Add expected features from the execution result cells.
		for (final ResultCell cell : resultCells.values()) {
			for (final InjectiveJointGraphDerivation<MR, ERESULT> parse : cell.parses) {
				final double weight = Math
						.exp(parse.getExecResult().getScore())
						* parse.getBaseParse().getInsideScore()
						* cell.outsideScore;
				parse.getExecResult().getFeatures()
						.addTimesInto(weight, expectedFeatures);
			}
		}
		
		return expectedFeatures;
	}
	
	@Override
	public IGraphParserOutput<MR> getBaseParserOutput() {
		return baseParserOutput;
	}
	
	@Override
	public double norm() {
		return norm(new IFilter<ERESULT>() {
			
			@Override
			public boolean isValid(ERESULT e) {
				return true;
			}
		});
	}
	
	@Override
	public double norm(IFilter<ERESULT> filter) {
		double norm = 0.0;
		// Iterate over all result cells
		for (final ResultCell cell : resultCells.values()) {
			// Test the result with the filter
			if (filter.isValid(cell.getResult())) {
				// Sum inside score
				norm += cell.insideScore;
			}
		}
		return norm;
	}
	
	private class ResultCell {
		private double													insideScore		= 0.0;
		private double													outsideScore	= 0.0;
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
			final double parseInsideScore = Math.exp(parse.getExecResult()
					.getScore()) * parse.getBaseParse().getInsideScore();
			insideScore += parseInsideScore;
		}
		
		public double getOutsideScore() {
			return outsideScore;
		}
		
		public ERESULT getResult() {
			return result;
		}
		
		public void initOutsideScore(IFilter<ERESULT> filter) {
			if (filter.isValid(result)) {
				outsideScore = 1.0;
			} else {
				outsideScore = 0.0;
			}
		}
		
	}
	
}
