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
package edu.cornell.cs.nlp.spf.parser.joint;

import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Abstract joint inference output. Doesn't support fancy dynamic programming
 * for semantics evaluation.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 * @param <DERIV>
 *            Joint derivation object.
 */
public abstract class AbstractJointOutput<MR, ERESULT, DERIV extends IJointDerivation<MR, ERESULT>>
		implements IJointOutput<MR, ERESULT> {

	private final boolean				exactInference;
	private final long					inferenceTime;
	protected final IParserOutput<MR>	baseOutput;
	protected final List<DERIV>			derivations;

	public AbstractJointOutput(IParserOutput<MR> baseOutput, long inferenceTime,
			List<DERIV> derivations, boolean exactInference) {
		this.baseOutput = baseOutput;
		this.inferenceTime = inferenceTime;
		this.derivations = derivations;
		this.exactInference = exactInference;
	}

	protected static <MR, ERESULT, DERIV extends IJointDerivation<MR, ERESULT>> List<DERIV> filterDerivations(
			List<DERIV> all, IFilter<ERESULT> filter, boolean maxOnly) {
		final List<DERIV> filtered = new LinkedList<DERIV>();
		double maxScore = -Double.MAX_VALUE;
		for (final DERIV derivation : all) {
			if (filter.test(derivation.getResult())) {
				if (!maxOnly || derivation.getScore() == maxScore) {
					filtered.add(derivation);
				} else if (derivation.getScore() > maxScore) {
					filtered.clear();
					filtered.add(derivation);
					maxScore = derivation.getScore();
				}
			}
		}
		return filtered;
	}

	@Override
	public List<DERIV> getDerivations() {
		return derivations;
	}

	@Override
	public List<DERIV> getDerivations(boolean includeFails) {
		return filterDerivations(derivations, e -> e != null, false);
	}

	@Override
	public List<DERIV> getDerivations(final ERESULT result) {
		return getDerivations(e -> result.equals(e));
	}

	@Override
	public List<DERIV> getDerivations(IFilter<ERESULT> filter) {
		return filterDerivations(derivations, filter, false);
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
		return filterDerivations(derivations, e -> e != null, true);
	}

	@Override
	public List<DERIV> getMaxDerivations(final ERESULT result) {
		return getMaxDerivations(e -> result.equals(e));
	}

	@Override
	public List<DERIV> getMaxDerivations(IFilter<ERESULT> filter) {
		return filterDerivations(derivations, filter, true);
	}

	@Override
	public boolean isExact() {
		return exactInference;
	}

}
