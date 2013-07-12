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

import edu.uw.cs.lil.tiny.parser.joint.IExecResultWrapper;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;

/**
 * Very simple wrapper for an execution with a single step (i.e.,
 * deterministic).
 * 
 * @author Yoav Artzi
 * @param <ESTEP>
 * @param <ERESULT>
 */
public class DeterministicExecResultWrapper<ESTEP, ERESULT> implements
		IExecResultWrapper<ERESULT> {
	
	private final ESTEP							executionStep;
	private IHashVectorImmutable				features	= null;
	private final IJointDataItemModel<?, ESTEP>	model;
	private final ERESULT						result;
	private Double								score		= null;
	
	public DeterministicExecResultWrapper(ESTEP executionStep,
			IJointDataItemModel<?, ESTEP> model, ERESULT result) {
		this.executionStep = executionStep;
		this.model = model;
		this.result = result;
	}
	
	@Override
	public IHashVectorImmutable getFeatures() {
		if (features == null) {
			features = result == null ? HashVectorFactory.create() : model
					.computeFeatures(executionStep);
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
			score = result == null ? 0.0 : model.score(executionStep);
		}
		return score;
	}
	
	@Override
	public String toString() {
		return result == null ? "null" : result.toString();
	}
	
}
