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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.FilterUtils;

/**
 * Joint inference output. Doesn't support fancy dynamic programming for
 * semantics evaluation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class JointOutput<MR, ERESULT> extends
		AbstractJointOutput<MR, ERESULT, JointDerivation<MR, ERESULT>> {
	
	public JointOutput(IParserOutput<MR> baseOutput, long inferenceTime,
			List<JointDerivation<MR, ERESULT>> derivations,
			List<JointDerivation<MR, ERESULT>> maxDerivations,
			boolean exactEvaluation) {
		super(baseOutput, inferenceTime, derivations, maxDerivations,
				exactEvaluation && baseOutput.isExact());
	}
	
	@Override
	public IParserOutput<MR> getBaseParserOutput() {
		return baseOutput;
	}
	
	public static class Builder<MR, ERESULT> {
		
		private final IParserOutput<MR>								baseOutput;
		private boolean												exactEvaluation	= false;
		private final List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>	inferencePairs	= new LinkedList<Pair<IDerivation<MR>, IEvaluation<ERESULT>>>();
		private final long											inferenceTime;
		
		public Builder(IParserOutput<MR> baseOutput, long inferenceTime) {
			this.baseOutput = baseOutput;
			this.inferenceTime = inferenceTime;
		}
		
		public Builder<MR, ERESULT> addInferencePair(
				Pair<IDerivation<MR>, IEvaluation<ERESULT>> pair) {
			inferencePairs.add(pair);
			return this;
		}
		
		public Builder<MR, ERESULT> addInferencePairs(
				List<Pair<IDerivation<MR>, IEvaluation<ERESULT>>> pairs) {
			inferencePairs.addAll(pairs);
			return this;
		}
		
		public JointOutput<MR, ERESULT> build() {
			final Map<ERESULT, JointDerivation.Builder<MR, ERESULT>> builders = new HashMap<ERESULT, JointDerivation.Builder<MR, ERESULT>>();
			for (final Pair<IDerivation<MR>, IEvaluation<ERESULT>> pair : inferencePairs) {
				final ERESULT pairResult = pair.second().getResult();
				if (!builders.containsKey(pairResult)) {
					builders.put(
							pairResult,
							new JointDerivation.Builder<MR, ERESULT>(pairResult));
				}
				builders.get(pairResult).addInferencePair(pair);
			}
			// Create all derivations.
			final List<JointDerivation<MR, ERESULT>> derivations = Collections
					.unmodifiableList(ListUtils.map(
							builders.values(),
							new ListUtils.Mapper<JointDerivation.Builder<MR, ERESULT>, JointDerivation<MR, ERESULT>>() {
								@Override
								public JointDerivation<MR, ERESULT> process(
										JointDerivation.Builder<MR, ERESULT> obj) {
									return obj.build();
								}
							}));
			// Get max derivations.
			final List<JointDerivation<MR, ERESULT>> maxDerivations = Collections
					.unmodifiableList(filterDerivations(derivations,
							FilterUtils.<ERESULT> stubTrue(), true));
			return new JointOutput<MR, ERESULT>(baseOutput, inferenceTime,
					derivations, maxDerivations, exactEvaluation);
		}
		
		public Builder<MR, ERESULT> setExactEvaluation(boolean exactEvaluation) {
			this.exactEvaluation = exactEvaluation;
			return this;
		}
		
	}
	
}
