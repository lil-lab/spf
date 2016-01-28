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

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.parser.joint.IEvaluation;
import edu.cornell.cs.nlp.spf.parser.joint.model.IJointDataItemModel;

/**
 * Very simple wrapper for an evaluation with a single step (i.e.,
 * deterministic). This means that all the information is present in the logical
 * form and the final result, so the actual steps taken can be abstracted away.
 * The evaluation step is simply the final evaluation result.
 * 
 * @author Yoav Artzi
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class DeterministicEvalResultWrapper<ERESULT> implements
		IEvaluation<ERESULT> {
	
	private IHashVectorImmutable					features	= null;
	private final IJointDataItemModel<?, ERESULT>	model;
	private final ERESULT							result;
	private Double									score		= null;
	
	public DeterministicEvalResultWrapper(
			IJointDataItemModel<?, ERESULT> model, ERESULT result) {
		this.model = model;
		this.result = result;
	}
	
	@Override
	public IHashVectorImmutable getFeatures() {
		if (features == null) {
			features = result == null ? HashVectorFactory.create() : model
					.computeFeatures(result);
		}
		return features;
	}
	
	@Override
	public ERESULT getResult() {
		return result;
	}
	
	@Override
	public double getScore() {
		if (score == null) {
			score = result == null ? 0.0 : model.score(result);
		}
		return score;
	}
	
	@Override
	public String toString() {
		return result == null ? "null" : result.toString();
	}
	
}
