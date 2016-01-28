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
package edu.cornell.cs.nlp.spf.reliabledist;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Yoav Artzi
 */
public class ManagerSummary {

	private final int					completedTasks;
	private final int					failedWorkers;
	private final int					redoneTasks;
	private final List<WorkerSummary>	workers;

	public ManagerSummary(int completedTasks, int failedWorkers,
			int redoneTasks, List<WorkerSummary> workers) {
		this.completedTasks = completedTasks;
		this.failedWorkers = failedWorkers;
		this.redoneTasks = redoneTasks;
		this.workers = workers;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append(String.format("completed=%d, redone=%d\n", completedTasks,
				redoneTasks));
		sb.append("Workers [total=").append(workers.size()).append(", failed=")
				.append(failedWorkers).append("]:\n");
		for (final WorkerSummary worker : workers) {
			sb.append(worker.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public static class Builder {

		private int							completedTasks;
		private int							failedWorkers;
		private int							redoneTasks;
		private final List<WorkerSummary>	workers	= new LinkedList<WorkerSummary>();

		public Builder addWorker(WorkerSummary summary) {
			workers.add(summary);
			return this;
		}

		public ManagerSummary build() {
			return new ManagerSummary(completedTasks, failedWorkers,
					redoneTasks, workers);
		}

		public void setCompletedTasks(int completedTasks) {
			this.completedTasks = completedTasks;
		}

		public void setFailedWorkers(int failedWorkers) {
			this.failedWorkers = failedWorkers;
		}

		public void setRedoneTasks(int redoneTasks) {
			this.redoneTasks = redoneTasks;
		}

	}

}
