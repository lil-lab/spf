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
package edu.uw.cs.lil.tiny.parser.joint.exec;

import java.util.Collections;
import java.util.List;

import edu.uw.cs.lil.tiny.exec.IExecOutput;
import edu.uw.cs.lil.tiny.exec.IExecution;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointParse;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;

public class JointExecutionOutput<Y, Z> implements IExecOutput<Pair<Y, Z>> {
	
	private List<IExecution<Pair<Y, Z>>>	allExecutions;
	private List<IExecution<Pair<Y, Z>>>	bestExecutions;
	private final IJointDataItemModel<Y, Z>	dataItemModel;
	private final IJointOutput<Y, Z>		jointOutput;
	
	public JointExecutionOutput(IJointOutput<Y, Z> jointOutput,
			final IJointDataItemModel<Y, Z> dataItemModel, boolean pruneFails) {
		this.jointOutput = jointOutput;
		this.dataItemModel = dataItemModel;
		this.allExecutions = Collections
				.unmodifiableList(ListUtils.map(
						jointOutput.getAllParses(!pruneFails),
						new ListUtils.Mapper<IJointParse<Y, Z>, IExecution<Pair<Y, Z>>>() {
							
							@Override
							public IExecution<Pair<Y, Z>> process(
									IJointParse<Y, Z> obj) {
								return new JointExecution<Y, Z>(obj,
										dataItemModel);
							}
						}));
		this.bestExecutions = Collections
				.unmodifiableList(ListUtils.map(
						jointOutput.getBestParses(!pruneFails),
						new ListUtils.Mapper<IJointParse<Y, Z>, IExecution<Pair<Y, Z>>>() {
							
							@Override
							public IExecution<Pair<Y, Z>> process(
									IJointParse<Y, Z> obj) {
								return new JointExecution<Y, Z>(obj,
										dataItemModel);
							}
						}));
	}
	
	@Override
	public List<IExecution<Pair<Y, Z>>> getAllExecutions() {
		return allExecutions;
	}
	
	@Override
	public List<IExecution<Pair<Y, Z>>> getBestExecutions() {
		return bestExecutions;
	}
	
	@Override
	public long getExecTime() {
		return jointOutput.getInferenceTime();
	}
	
	@Override
	public List<IExecution<Pair<Y, Z>>> getExecutions(Pair<Y, Z> label) {
		final List<IJointParse<Y, Z>> bases = jointOutput.getParsesFor(label);
		return ListUtils
				.map(bases,
						new ListUtils.Mapper<IJointParse<Y, Z>, IExecution<Pair<Y, Z>>>() {
							
							@Override
							public IExecution<Pair<Y, Z>> process(
									IJointParse<Y, Z> obj) {
								return new JointExecution<Y, Z>(obj,
										dataItemModel);
							}
							
						});
	}
	
}
