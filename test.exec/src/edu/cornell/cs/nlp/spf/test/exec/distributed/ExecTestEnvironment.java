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
package edu.cornell.cs.nlp.spf.test.exec.distributed;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.exec.IExec;
import edu.cornell.cs.nlp.spf.reliabledist.EnvironmentConfig;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * @author Yoav Artzi
 * @param <SAMPLE>
 *            Data sample.
 * @param <RESULT>
 *            Inference result.
 */
public class ExecTestEnvironment<SAMPLE extends IDataItem<?>, RESULT>
		extends AbstractExecTestEnvironment<SAMPLE, RESULT> {

	private static final long		serialVersionUID	= 80698096375175584L;
	private IExec<SAMPLE, RESULT>	exec				= null;
	private IFilter<SAMPLE>			skipExecutionFilter	= null;

	@SuppressWarnings("unchecked")
	@Override
	public void applyUpdate(EnvironmentConfig<?> update) {
		switch (update.getKey()) {
			case "exec":
				exec = (IExec<SAMPLE, RESULT>) update.getValue();
				break;
			case "skipExecutionFilter":
				skipExecutionFilter = (IFilter<SAMPLE>) update.getValue();
				break;
		}
	}

	@Override
	public IExec<SAMPLE, RESULT> getExec() {
		return exec;
	}

	@Override
	public IFilter<SAMPLE> getSkipExecutionFilter() {
		return skipExecutionFilter;
	}

	@Override
	public EnvironmentConfig<IExec<SAMPLE, RESULT>> updateExec(
			IExec<SAMPLE, RESULT> newExec) {
		return new EnvironmentConfig<>("exec", newExec);
	}

	@Override
	public EnvironmentConfig<IFilter<SAMPLE>> updateSkipExecutionFilter(
			IFilter<SAMPLE> filter) {
		return new EnvironmentConfig<>("skipExecutionFilter", filter);
	}

}
