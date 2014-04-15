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

import edu.uw.cs.lil.tiny.parser.graph.IGraphDerivation;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParser;
import edu.uw.cs.lil.tiny.parser.joint.IEvaluation;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphDerivation;
import edu.uw.cs.lil.tiny.parser.joint.injective.InjectiveJointDerivation;

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
	
	private final IGraphDerivation<MR>		baseParse;
	private final IEvaluation<ERESULT>	execResult;
	
	public InjectiveJointGraphDerivation(IGraphDerivation<MR> baseParse,
			IEvaluation<ERESULT> evalResult) {
		super(baseParse, evalResult);
		this.baseParse = baseParse;
		this.execResult = evalResult;
	}
	
	@Override
	public double getLogInsideScore() {
		// The log inside score is a addition of the log
		// inside score of the base parse and the
		// evaluation score.
		return baseParse.getLogInsideScore() + execResult.getScore();
	}
	
	protected IGraphDerivation<MR> getBaseParse() {
		return baseParse;
	}
	
}
