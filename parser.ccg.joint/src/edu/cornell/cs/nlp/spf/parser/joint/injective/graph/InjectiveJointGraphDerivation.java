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
package edu.cornell.cs.nlp.spf.parser.joint.injective.graph;

import edu.cornell.cs.nlp.spf.parser.graph.IGraphDerivation;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParser;
import edu.cornell.cs.nlp.spf.parser.joint.IEvaluation;
import edu.cornell.cs.nlp.spf.parser.joint.graph.IJointGraphDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.injective.InjectiveJointDerivation;

/**
 * Joint parse as generated from a graph-based parser ({@link IGraphParser}).
 * Simplifies joint inference by assuming a injective semantic evaluation (i.e.,
 * a one-to-one matching between the meaning representation and its evaluation
 * result).
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 */
public class InjectiveJointGraphDerivation<MR, ERESULT> extends
		InjectiveJointDerivation<MR, ERESULT> implements
		IJointGraphDerivation<MR, ERESULT> {

	private final IGraphDerivation<MR>	baseDerivation;

	private final IEvaluation<ERESULT>	execResult;

	public InjectiveJointGraphDerivation(IGraphDerivation<MR> baseParse,
			IEvaluation<ERESULT> evalResult) {
		super(baseParse, evalResult);
		this.baseDerivation = baseParse;
		this.execResult = evalResult;
	}

	@Override
	public double getLogInsideScore() {
		// The log inside score is a addition of the log
		// inside score of the base parse and the
		// evaluation score.
		return baseDerivation.getLogInsideScore() + execResult.getScore();
	}

	protected IGraphDerivation<MR> getBaseParse() {
		return baseDerivation;
	}

}
