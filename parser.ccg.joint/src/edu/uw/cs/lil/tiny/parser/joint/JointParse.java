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

import java.util.LinkedHashSet;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.utils.composites.Pair;

/**
 * Single joint parse result. Includes a LF coming from the parser and a result
 * of its execution.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic type
 * @param <ERESULT>
 *            Execution result type
 */
public class JointParse<MR, ERESULT> implements IJointParse<MR, ERESULT> {
	
	private final IExecResultWrapper<ERESULT>	execResult;
	private final IParse<MR>					innerParse;
	
	private final Pair<MR, ERESULT>				resultPair;
	private final double						score;
	private IHashVectorImmutable				viterbiFeatures;
	
	public JointParse(IParse<MR> innerParse,
			IExecResultWrapper<ERESULT> execResult) {
		this.innerParse = innerParse;
		this.execResult = execResult;
		this.resultPair = Pair.of(innerParse.getSemantics(),
				execResult.getResult());
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
		final JointParse<?, ?> other = (JointParse<?, ?>) obj;
		if (execResult == null) {
			if (other.execResult != null) {
				return false;
			}
		} else if (!execResult.equals(other.execResult)) {
			return false;
		}
		if (innerParse == null) {
			if (other.innerParse != null) {
				return false;
			}
		} else if (!innerParse.equals(other.innerParse)) {
			return false;
		}
		return true;
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntries() {
		return innerParse.getAllLexicalEntries();
	}
	
	@Override
	public IHashVectorImmutable getAverageMaxFeatureVector() {
		if (viterbiFeatures == null) {
			final IHashVector features = HashVectorFactory.create();
			execResult.getFeatures().addTimesInto(1.0, features);
			innerParse.getAverageMaxFeatureVector().addTimesInto(1.0, features);
			this.viterbiFeatures = features;
		}
		return viterbiFeatures;
	}
	
	@Override
	public double getBaseScore() {
		return innerParse.getScore();
	}
	
	public IExecResultWrapper<ERESULT> getExecResult() {
		return execResult;
	}
	
	public ERESULT getFinalResult() {
		return execResult.getResult();
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries() {
		return innerParse.getMaxLexicalEntries();
	}
	
	@Override
	public LinkedHashSet<RuleUsageTriplet> getMaxRulesUsed() {
		return innerParse.getMaxRulesUsed();
	}
	
	@Override
	public Pair<MR, ERESULT> getResult() {
		return resultPair;
	}
	
	@Override
	public double getScore() {
		return score;
	}
	
	@Override
	public MR getSemantics() {
		return innerParse.getSemantics();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((execResult == null) ? 0 : execResult.hashCode());
		result = prime * result
				+ ((innerParse == null) ? 0 : innerParse.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(innerParse.toString()).append(" => ")
				.append(execResult).toString();
	}
	
}
