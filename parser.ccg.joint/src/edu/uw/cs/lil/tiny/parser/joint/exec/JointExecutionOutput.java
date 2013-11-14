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
import edu.uw.cs.utils.filter.IFilter;

public class JointExecutionOutput<MR, ERESULT> implements IExecOutput<Pair<MR, ERESULT>> {
	
	private List<IExecution<Pair<MR, ERESULT>>>	allExecutions;
	private List<IExecution<Pair<MR, ERESULT>>>	bestExecutions;
	private final IJointDataItemModel<MR, ERESULT>	dataItemModel;
	private final IJointOutput<MR, ERESULT>		jointOutput;
	
	public JointExecutionOutput(IJointOutput<MR, ERESULT> jointOutput,
			final IJointDataItemModel<MR, ERESULT> dataItemModel, boolean pruneFails) {
		this.jointOutput = jointOutput;
		this.dataItemModel = dataItemModel;
		this.allExecutions = Collections
				.unmodifiableList(ListUtils.map(
						jointOutput.getAllParses(!pruneFails),
						new ListUtils.Mapper<IJointParse<MR, ERESULT>, IExecution<Pair<MR, ERESULT>>>() {
							
							@Override
							public IExecution<Pair<MR, ERESULT>> process(
									IJointParse<MR, ERESULT> obj) {
								return new JointExecution<MR, ERESULT>(obj,
										dataItemModel);
							}
						}));
		this.bestExecutions = Collections
				.unmodifiableList(ListUtils.map(
						jointOutput.getBestParses(!pruneFails),
						new ListUtils.Mapper<IJointParse<MR, ERESULT>, IExecution<Pair<MR, ERESULT>>>() {
							
							@Override
							public IExecution<Pair<MR, ERESULT>> process(
									IJointParse<MR, ERESULT> obj) {
								return new JointExecution<MR, ERESULT>(obj,
										dataItemModel);
							}
						}));
	}
	
	@Override
	public List<IExecution<Pair<MR, ERESULT>>> getAllExecutions() {
		return allExecutions;
	}
	
	@Override
	public List<IExecution<Pair<MR, ERESULT>>> getBestExecutions() {
		return bestExecutions;
	}
	
	@Override
	public long getExecTime() {
		return jointOutput.getInferenceTime();
	}
	
	@Override
	public List<IExecution<Pair<MR, ERESULT>>> getExecutions(IFilter<Pair<MR, ERESULT>> filter) {
		final List<? extends IJointParse<MR, ERESULT>> bases = jointOutput
				.getParses(filter);
		return ListUtils
				.map(bases,
						new ListUtils.Mapper<IJointParse<MR, ERESULT>, IExecution<Pair<MR, ERESULT>>>() {
							
							@Override
							public IExecution<Pair<MR, ERESULT>> process(
									IJointParse<MR, ERESULT> obj) {
								return new JointExecution<MR, ERESULT>(obj,
										dataItemModel);
							}
							
						});
	}
	
	@Override
	public List<IExecution<Pair<MR, ERESULT>>> getExecutions(final Pair<MR, ERESULT> label) {
		return getExecutions(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return e.equals(label);
			}
		});
	}
	
}
