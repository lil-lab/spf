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
package edu.uw.cs.lil.tiny.parser.joint.injective;

import java.util.LinkedHashSet;
import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;
import edu.uw.cs.lil.tiny.parser.joint.IEvaluation;
import edu.uw.cs.lil.tiny.parser.joint.IJointDerivation;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * Single joint derivation for parsing and semantic evaluation. Simplifies joint
 * inference by assuming a injective semantic evaluation (i.e., a one-to-one
 * matching between the meaning representation and its evaluation result).
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 */
public class InjectiveJointDerivation<MR, ERESULT> implements
		IJointDerivation<MR, ERESULT> {
	
	private final IDerivation<MR>			baseParse;
	private final IEvaluation<ERESULT>	evalResult;
	
	private final double				score;
	private IHashVectorImmutable		viterbiFeatures;
	
	public InjectiveJointDerivation(IDerivation<MR> innerParse,
			IEvaluation<ERESULT> execResult) {
		this.baseParse = innerParse;
		this.evalResult = execResult;
		this.score = innerParse.getScore() + execResult.getScore();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final InjectiveJointDerivation<?, ?> other = (InjectiveJointDerivation<?, ?>) obj;
		if (evalResult == null) {
			if (other.evalResult != null) {
				return false;
			}
		} else if (!evalResult.equals(other.evalResult)) {
			return false;
		}
		if (baseParse == null) {
			if (other.baseParse != null) {
				return false;
			}
		} else if (!baseParse.equals(other.baseParse)) {
			return false;
		}
		return true;
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntries() {
		return baseParse.getAllLexicalEntries();
	}
	
	public IEvaluation<ERESULT> getExecResult() {
		return evalResult;
	}
	
	public ERESULT getFinalResult() {
		return evalResult.getResult();
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries() {
		return baseParse.getMaxLexicalEntries();
	}
	
	@Override
	public LinkedHashSet<RuleUsageTriplet> getMaxParsingRules() {
		return baseParse.getMaxRulesUsed();
	}
	
	@Override
	public List<MR> getMaxSemantics() {
		return ListUtils.createSingletonList(baseParse.getSemantics());
	}
	
	@Override
	public IHashVectorImmutable getMeanMaxFeatures() {
		if (viterbiFeatures == null) {
			final IHashVector features = HashVectorFactory.create();
			evalResult.getFeatures().addTimesInto(1.0, features);
			baseParse.getAverageMaxFeatureVector().addTimesInto(1.0, features);
			this.viterbiFeatures = features;
		}
		return viterbiFeatures;
	}
	
	@Override
	public ERESULT getResult() {
		return evalResult.getResult();
	}
	
	public MR getSemantics() {
		return baseParse.getSemantics();
	}
	
	@Override
	public double getViterbiScore() {
		return score;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((evalResult == null) ? 0 : evalResult.hashCode());
		result = prime * result
				+ ((baseParse == null) ? 0 : baseParse.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(baseParse.toString()).append(" => ")
				.append(evalResult).toString();
	}
	
}
