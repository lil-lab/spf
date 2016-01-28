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

import java.io.Serializable;

/**
 * @author Yoav Artzi
 */
public class WorkerSummary implements Serializable {

	private static final long	serialVersionUID	= -3195052665694164119L;
	private final int			accepted;
	private final int			completed;
	private final boolean		failed;
	private final int			freeSpots;
	private final int			id;
	private final double		meanTime;
	private final String		name;

	public WorkerSummary(int id, String name, int accepted, int completed,
			int freeSpots, boolean failed, double meanTime) {
		this.id = id;
		this.name = name;
		this.accepted = accepted;
		this.completed = completed;
		this.freeSpots = freeSpots;
		this.failed = failed;
		this.meanTime = meanTime;
	}

	public int getAccepted() {
		return accepted;
	}

	public int getCompleted() {
		return completed;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isFailed() {
		return failed;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder("[").append(id).append("] ")
				.append(name).append(": ").append("freeSpots=")
				.append(freeSpots).append(", meanTime=")
				.append(String.format("%.3fsec", meanTime / 1000.0))
				.append(", failed=").append(failed).append(", accepted=")
				.append(accepted).append(", ");

		if (completed == accepted) {
			sb.append("IDLE");
		} else {
			sb.append("completed=").append(completed);
		}

		return sb.toString();
	}

	public static class Builder {

		private int				accepted;
		private int				completed;
		private boolean			failed;
		private int				freeSpots;
		private final int		id;
		private double			meanTime;
		private final String	name;

		public Builder(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public WorkerSummary build() {
			return new WorkerSummary(id, name, accepted, completed, freeSpots,
					failed, meanTime);
		}

		public Builder setFailed(boolean isFailed) {
			this.failed = isFailed;
			return this;
		}

		public Builder setFreeSpots(int freeSpots) {
			this.freeSpots = freeSpots;
			return this;
		}

		public Builder setMeanTime(double meanTime) {
			this.meanTime = meanTime;
			return this;
		}

		public Builder setTaskCompelted(int completed) {
			this.completed = completed;
			return this;
		}

		public Builder setTasksAccepted(int accepted) {
			this.accepted = accepted;
			return this;
		}

	}

}
