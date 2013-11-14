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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.parser.joint.GenericJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphParserOutput;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;

public class SimpleGraphJointOutput<MR, ERESULT> extends
		GenericJointOutput<MR, ERESULT, SimpleGraphJointParse<MR, ERESULT>>
		implements IJointGraphParserOutput<MR, ERESULT> {
	
	private final IGraphParserOutput<MR>	baseParserOutput;
	
	private final Map<ERESULT, ResultCell>	resultCells	= new HashMap<ERESULT, SimpleGraphJointOutput<MR, ERESULT>.ResultCell>();
	
	public SimpleGraphJointOutput(IGraphParserOutput<MR> baseParserOutput,
			List<SimpleGraphJointParse<MR, ERESULT>> jointParses,
			long parsingTime) {
		super(baseParserOutput, jointParses, parsingTime);
		this.baseParserOutput = baseParserOutput;
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
		}, false);
	}
	
	@Override
	public IHashVector expectedFeatures(IFilter<ERESULT> filter, boolean maxOnly) {
		// Init result cells outside scores.
		for (final ResultCell cell : resultCells.values()) {
			cell.initOutsideScore(maxOnly ? createMaxFitler(filter) : filter);
		}
		
		// Preparing a mapping of logical forms to their initial outside scores.
		// These scores are summation over all execution root (cells) of the
		// execution step score times the outside score of that cell. Iterate
		// over result cells. For each cell, first init result cells outside
		// scores. Then iterate over all parses and sum the outside
		// contribution.
		final IFilter<ERESULT> filterToUse;
		if (maxOnly) {
			filterToUse = createMaxFitler(filter);
		} else {
			filterToUse = filter;
		}
		final Map<MR, Double> initBaseParseOutsideScores = new HashMap<MR, Double>();
		for (final ResultCell cell : resultCells.values()) {
			cell.initOutsideScore(filterToUse);
			// Iterate over over all joint parses to create the initialization
			// score for the base parse logical form.
			for (final SimpleGraphJointParse<MR, ERESULT> parse : cell.parses) {
				final MR semantics = parse.getBaseParse().getSemantics();
				// If taking max parses only, set outside to be different than
				// zero only for viterbi parses.
				final double outsideContribution;
				if (maxOnly && parse.getScore() < cell.viterbiScore) {
					// Case we only require max parses, and this logical form
					// doesn't come from a max scoring parse, set its outside
					// score to 0.
					outsideContribution = 0.0;
				} else {
					outsideContribution = resultCells.get(
							parse.getResult().second()).getOutsideScore()
							* Math.exp(parse.getExecResult().getScore());
				}
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
		}, false);
	}
	
	@Override
	public double norm(IFilter<ERESULT> filter, boolean maxOnly) {
		final IFilter<ERESULT> filterToUse;
		if (maxOnly) {
			filterToUse = createMaxFitler(filter);
		} else {
			filterToUse = filter;
		}
		double norm = 0.0;
		// Iterate over all result cells
		for (final ResultCell cell : resultCells.values()) {
			// Test the result with the filter
			if (filterToUse.isValid(cell.getResult())) {
				// Sum inside score
				norm += (maxOnly ? cell.viterbiInsideScore : cell.insideScore);
			}
		}
		return norm;
	}
	
	private IFilter<ERESULT> createMaxFitler(final IFilter<ERESULT> filter) {
		final Set<ERESULT> maxResults = new HashSet<ERESULT>(
				ListUtils.map(
						getMaxParses(new IFilter<Pair<MR, ERESULT>>() {
							
							@Override
							public boolean isValid(Pair<MR, ERESULT> e) {
								return filter.isValid(e.second());
							}
						}),
						new ListUtils.Mapper<SimpleGraphJointParse<MR, ERESULT>, ERESULT>() {
							
							@Override
							public ERESULT process(
									SimpleGraphJointParse<MR, ERESULT> obj) {
								return obj.getResult().second();
							}
						}));
		return new IFilter<ERESULT>() {
			
			@Override
			public boolean isValid(ERESULT e) {
				return maxResults.contains(e);
			}
		};
		
	}
	
	private class ResultCell {
		private double											insideScore			= 0.0;
		private double											outsideScore		= 0.0;
		private final List<SimpleGraphJointParse<MR, ERESULT>>	parses				= new LinkedList<SimpleGraphJointParse<MR, ERESULT>>();
		private final ERESULT									result;
		private double											viterbiInsideScore	= 0.0;
		private final List<SimpleGraphJointParse<MR, ERESULT>>	viterbiParses		= new LinkedList<SimpleGraphJointParse<MR, ERESULT>>();
		private double											viterbiScore		= -Double.MAX_VALUE;
		
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
			final double parseInsideScore = Math.exp(parse.getExecResult()
					.getScore()) * parse.getBaseParse().getInsideScore();
			insideScore += parseInsideScore;
			
			// Update cell viterbi score and viterbi parses
			if (parse.getScore() == viterbiScore) {
				viterbiParses.add(parse);
				viterbiInsideScore += parseInsideScore;
			} else if (parse.getScore() > viterbiScore) {
				viterbiParses.clear();
				viterbiScore = parse.getScore();
				viterbiInsideScore = parseInsideScore;
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
