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
package edu.cornell.cs.nlp.spf.parser.joint.injective;

import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.parser.joint.IJointDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.IJointOutput;
import edu.cornell.cs.nlp.utils.filter.FilterUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Abstract output for joint inference of parsing and semantics evaluation.
 * Simplifies joint inference by assuming a injective semantic evaluation (i.e.,
 * a one-to-one matching between the meaning representation and its evaluation
 * result).
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 * @param <DERIV>
 *            Base joint inference derivation.
 */
public abstract class AbstractInjectiveJointOutput<MR, ERESULT, DERIV extends IJointDerivation<MR, ERESULT>>
		implements IJointOutput<MR, ERESULT> {

	private final List<DERIV>	bestJointParses;
	private final List<DERIV>	bestSuccessfulJointParses;
	private final boolean		exactInference;
	private final long			inferenceTime;
	private final List<DERIV>	jointParses;
	private final List<DERIV>	successfulJointParses;

	public AbstractInjectiveJointOutput(List<DERIV> jointParses,
			long inferenceTime, boolean exactInference) {
		this.jointParses = jointParses;
		this.inferenceTime = inferenceTime;
		this.exactInference = exactInference;
		this.bestJointParses = findBestParses(jointParses);
		this.successfulJointParses = successfulOnly(jointParses);
		this.bestSuccessfulJointParses = findBestParses(successfulJointParses);
	}

	@Override
	public List<DERIV> getDerivations() {
		return getDerivations(true);
	}

	@Override
	public List<DERIV> getDerivations(boolean includeFails) {
		if (includeFails) {
			return jointParses;
		} else {
			return successfulJointParses;
		}
	}

	@Override
	public List<DERIV> getDerivations(final ERESULT result) {
		return getDerivations(new IFilter<ERESULT>() {

			@Override
			public boolean test(ERESULT e) {
				return result.equals(e);
			}
		});
	}

	@Override
	public List<DERIV> getDerivations(IFilter<ERESULT> filter) {
		final List<DERIV> parses = new LinkedList<DERIV>();
		for (final DERIV p : jointParses) {
			if (filter.test(p.getResult())) {
				parses.add(p);
			}
		}
		return parses;
	}

	@Override
	public long getInferenceTime() {
		return inferenceTime;
	}

	@Override
	public List<DERIV> getMaxDerivations() {
		return getMaxDerivations(true);
	}

	@Override
	public List<DERIV> getMaxDerivations(boolean includeFails) {
		if (includeFails) {
			return bestJointParses;
		} else {
			return bestSuccessfulJointParses;
		}
	}

	@Override
	public List<DERIV> getMaxDerivations(final ERESULT result) {
		return getMaxDerivations(new IFilter<ERESULT>() {

			@Override
			public boolean test(ERESULT e) {
				return result.equals(e);
			}
		});
	}

	@Override
	public List<DERIV> getMaxDerivations(IFilter<ERESULT> filter) {
		final List<DERIV> parses = new LinkedList<DERIV>();
		double score = -Double.MAX_VALUE;
		for (final DERIV p : jointParses) {
			if (filter.test(p.getResult())) {
				if (p.getViterbiScore() > score) {
					parses.clear();
					parses.add(p);
					score = p.getViterbiScore();
				} else if (p.getViterbiScore() == score) {
					parses.add(p);
				}
			}
		}
		return parses;
	}

	public List<LexicalEntry<MR>> getMaxLexicalEntries(final ERESULT result) {
		final List<LexicalEntry<MR>> entries = new LinkedList<LexicalEntry<MR>>();
		for (final DERIV p : findBestParses(jointParses,
				new IFilter<ERESULT>() {

					@Override
					public boolean test(ERESULT e) {
						return result == e || result != null
								&& result.equals(e);
					}
				})) {
			entries.addAll(p.getMaxLexicalEntries());
		}
		return entries;
	}

	@Override
	public boolean isExact() {
		return exactInference;
	}

	private List<DERIV> findBestParses(List<DERIV> all) {
		return findBestParses(all, FilterUtils.<ERESULT> stubTrue());
	}

	private List<DERIV> findBestParses(List<DERIV> all, IFilter<ERESULT> filter) {
		final List<DERIV> best = new LinkedList<DERIV>();
		double bestScore = -Double.MAX_VALUE;
		for (final DERIV p : all) {
			if (filter.test(p.getResult())) {
				if (p.getViterbiScore() == bestScore) {
					best.add(p);
				}
				if (p.getViterbiScore() > bestScore) {
					bestScore = p.getViterbiScore();
					best.clear();
					best.add(p);
				}
			}
		}
		return best;
	}

	private List<DERIV> successfulOnly(List<DERIV> parses) {
		final List<DERIV> successfulParses = new LinkedList<DERIV>();
		for (final DERIV parse : parses) {
			if (parse.getResult() != null) {
				successfulParses.add(parse);
			}
		}
		return successfulParses;
	}

}
