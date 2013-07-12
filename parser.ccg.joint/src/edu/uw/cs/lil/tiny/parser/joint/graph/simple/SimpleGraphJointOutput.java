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
package edu.uw.cs.lil.tiny.parser.joint.graph.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointParse;
import edu.uw.cs.lil.tiny.parser.joint.JointOutput;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphParserOutput;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.filter.IFilter;

public class SimpleGraphJointOutput<MR, ERESULT> extends
		JointOutput<MR, ERESULT> implements
		IJointGraphParserOutput<MR, ERESULT> {
	
	private final IGraphParserOutput<MR>					baseParserOutput;
	
	private final List<SimpleGraphJointParse<MR, ERESULT>>	jointParses;
	
	private final Map<ERESULT, ResultCell>					resultCells	= new HashMap<ERESULT, SimpleGraphJointOutput<MR, ERESULT>.ResultCell>();
	
	public SimpleGraphJointOutput(IGraphParserOutput<MR> baseParserOutput,
			List<SimpleGraphJointParse<MR, ERESULT>> jointParses,
			long parsingTime) {
		super(baseParserOutput, new ArrayList<IJointParse<MR, ERESULT>>(
				jointParses), parsingTime);
		this.baseParserOutput = baseParserOutput;
		this.jointParses = jointParses;
		for (final SimpleGraphJointParse<MR, ERESULT> parse : jointParses) {
			if (resultCells.containsKey(parse.getResult().second())) {
				resultCells.get(parse.getResult().second()).addParse(parse);
			} else {
				resultCells.put(parse.getResult().second(), new ResultCell(
						parse.getResult().second(), parse));
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
		// Init result cells outside scores
		for (final ResultCell cell : resultCells.values()) {
			cell.initOutsideScore(filter);
		}
		
		// Preparing a mapping of logical forms to their initial outside scores.
		// These scores are summation over all execution root (cells) of the
		// execution step score times the outside score of that cell.
		final Map<MR, Double> initBaseParseOutsideScores = new HashMap<MR, Double>();
		for (final SimpleGraphJointParse<MR, ERESULT> parse : jointParses) {
			final MR semantics = parse.getBaseParse().getSemantics();
			final double outsideContribution = resultCells.get(
					parse.getResult().second()).getOutsideScore()
					* Math.exp(parse.getExecResult().getScore());
			if (initBaseParseOutsideScores.containsKey(semantics)) {
				initBaseParseOutsideScores.put(semantics,
						initBaseParseOutsideScores.get(semantics)
								+ outsideContribution);
			} else {
				initBaseParseOutsideScores.put(semantics, outsideContribution);
			}
		}
		// Create the scorer
		final IScorer<MR> scorer = new IScorer<MR>() {
			@Override
			public double score(MR e) {
				return initBaseParseOutsideScores.get(e);
			}
		};
		
		// Get expected features from base parser output
		final IHashVector expectedFeatures = baseParserOutput
				.expectedFeatures(scorer);
		
		// Add expected features from the execution result cells
		for (final ResultCell cell : resultCells.values()) {
			for (final SimpleGraphJointParse<MR, ERESULT> parse : cell.parses) {
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
		private double											insideScore		= 0.0;
		private double											outsideScore	= 0.0;
		private final List<SimpleGraphJointParse<MR, ERESULT>>	parses			= new LinkedList<SimpleGraphJointParse<MR, ERESULT>>();
		private final ERESULT									result;
		private final List<SimpleGraphJointParse<MR, ERESULT>>	viterbiParses	= new LinkedList<SimpleGraphJointParse<MR, ERESULT>>();
		private double											viterbiScore	= -Double.MAX_VALUE;
		
		public ResultCell(ERESULT result,
				SimpleGraphJointParse<MR, ERESULT> parse) {
			this.result = result;
			addParse(parse);
		}
		
		public void addParse(SimpleGraphJointParse<MR, ERESULT> parse) {
			// Add to parse list
			parses.add(parse);
			
			// Update the cell's inside score. Execution involves a single step,
			// so take a product of the inside score of the base parse and the
			// exponent of the local score of the execution step.
			insideScore += Math.exp(parse.getExecResult().getScore())
					* parse.getBaseParse().getInsideScore();
			
			// Update cell viterbi score and viterbi parses
			if (parse.getScore() == viterbiScore) {
				viterbiParses.add(parse);
			} else if (parse.getScore() > viterbiScore) {
				viterbiParses.clear();
				viterbiScore = parse.getScore();
				viterbiParses.add(parse);
			}
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
