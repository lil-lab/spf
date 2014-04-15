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
import java.util.LinkedHashSet;
import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;

/**
 * Abstract joint inference derivation that compactly holds all derivations that
 * lead to a specific result. Doesn't support fancy dynamic programming for
 * semantics evaluation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 * @param <PARSE>
 *            The base parse object.
 */
public abstract class AbstractJointDerivation<MR, ERESULT, PARSE extends IDerivation<MR>>
		implements IJointDerivation<MR, ERESULT> {
	
	private final List<Pair<PARSE, IEvaluation<ERESULT>>>	maxPairs;
	private final List<Pair<PARSE, IEvaluation<ERESULT>>>	pairs;
	private final ERESULT									result;
	private final double									viterbiScore;
	
	public AbstractJointDerivation(
			List<Pair<PARSE, IEvaluation<ERESULT>>> maxPairs,
			List<Pair<PARSE, IEvaluation<ERESULT>>> pairs, ERESULT result,
			double viterbiScore) {
		this.maxPairs = Collections.unmodifiableList(maxPairs);
		this.pairs = Collections.unmodifiableList(pairs);
		this.result = result;
		this.viterbiScore = viterbiScore;
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntries() {
		final LinkedHashSet<LexicalEntry<MR>> entries = new LinkedHashSet<LexicalEntry<MR>>();
		for (final Pair<PARSE, IEvaluation<ERESULT>> pair : pairs) {
			entries.addAll(pair.first().getAllLexicalEntries());
		}
		return entries;
	}
	
	/**
	 * All inference pairs, each one includes a base {@link IParse<MR>} and
	 * {@link IEvaluation<ERESULT>}.
	 */
	public List<Pair<PARSE, IEvaluation<ERESULT>>> getInferencePairs() {
		return pairs;
	}
	
	/**
	 * Max-scoring inference pairs, each one includes a base {@link IParse<MR>}
	 * and {@link IEvaluation<ERESULT>}.
	 */
	public List<Pair<PARSE, IEvaluation<ERESULT>>> getMaxInferencePairs() {
		return maxPairs;
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries() {
		final LinkedHashSet<LexicalEntry<MR>> entries = new LinkedHashSet<LexicalEntry<MR>>();
		for (final Pair<PARSE, IEvaluation<ERESULT>> pair : maxPairs) {
			entries.addAll(pair.first().getAllLexicalEntries());
		}
		return entries;
	}
	
	@Override
	public LinkedHashSet<RuleUsageTriplet> getMaxParsingRules() {
		final LinkedHashSet<RuleUsageTriplet> rules = new LinkedHashSet<RuleUsageTriplet>();
		for (final Pair<PARSE, IEvaluation<ERESULT>> pair : maxPairs) {
			rules.addAll(pair.first().getMaxRulesUsed());
		}
		return rules;
	}
	
	@Override
	public List<MR> getMaxSemantics() {
		return ListUtils.map(maxPairs,
				new ListUtils.Mapper<Pair<PARSE, IEvaluation<ERESULT>>, MR>() {
					
					@Override
					public MR process(Pair<PARSE, IEvaluation<ERESULT>> obj) {
						return obj.first().getSemantics();
					}
				});
	}
	
	@Override
	public IHashVectorImmutable getMeanMaxFeatures() {
		final IHashVector features = HashVectorFactory.create();
		int num = 0;
		for (final Pair<PARSE, IEvaluation<ERESULT>> pair : maxPairs) {
			++num;
			pair.first().getAverageMaxFeatureVector()
					.addTimesInto(1.0, features);
			pair.second().getFeatures().addTimesInto(1.0, features);
		}
		features.divideBy(num);
		return features;
	}
	
	@Override
	public ERESULT getResult() {
		return result;
	}
	
	@Override
	public double getViterbiScore() {
		return viterbiScore;
	}
	
}
