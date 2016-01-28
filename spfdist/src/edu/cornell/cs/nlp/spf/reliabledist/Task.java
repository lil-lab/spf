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
import java.util.function.Function;

import edu.cornell.cs.nlp.utils.log.BufferingLog;
import edu.cornell.cs.nlp.utils.log.Log;
import edu.cornell.cs.nlp.utils.log.thread.LoggingThread;

/**
 * A wrapper for a job submitted to the framework. Each job is executed on a
 * single thread without any locks.
 *
 * @author Yoav Artzi
 * @param <OUTPUT>
 *            Job output.
 */
public class Task implements Serializable {

	private static final long				serialVersionUID	= -1553770414572878120L;
	private final long						id;
	private final Function<AbstractEnvironment, ?>	job;

	public Task(Function<AbstractEnvironment, ?> job, long id) {
		this.job = job;
		this.id = id;
	}

	public TaskResult execute(AbstractEnvironment environment) {
		final Log originalLog;
		final BufferingLog bufferingLog;
		if (Thread.currentThread() instanceof LoggingThread) {
			originalLog = ((LoggingThread) Thread.currentThread()).getLog();
			bufferingLog = new BufferingLog();
			((LoggingThread) Thread.currentThread()).setLog(bufferingLog);
		} else {
			originalLog = null;
			bufferingLog = null;
		}
		try {

			final Object output;
			try {
				output = job.apply(environment);
			} catch (final Throwable e) {
				return new TaskResult(null, id, e, bufferingLog == null ? null
						: bufferingLog.getBuffer());
			}

			return new TaskResult(output, id, null, bufferingLog == null ? null
					: bufferingLog.getBuffer());
		} finally {
			if (Thread.currentThread() instanceof LoggingThread) {
				((LoggingThread) Thread.currentThread()).setLog(originalLog);
			}
		}

	}

	public long getId() {
		return id;
	}

}
