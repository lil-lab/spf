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

import edu.cornell.cs.nlp.utils.string.StringUtils;

/**
 * A wrapper for the output of a single job. This is the output generated when
 * executing {@link Task}.
 *
 * @author Yoav Artzi
 */
public class TaskResult implements Serializable {

	private static final long serialVersionUID = 3752209493650358030L;

	private final Throwable exception;

	private final String log;

	private final Object output;

	private final long taskId;

	public TaskResult(Object output, long taskId, Throwable exception,
			String log) {
		this.output = output;
		this.taskId = taskId;
		this.exception = exception;
		this.log = StringUtils.escapeForPrint(log);
	}

	public Throwable getException() {
		return exception;
	}

	public String getLog() {
		return log;
	}

	@SuppressWarnings("unchecked")
	public <T> T getOutput() {
		return (T) output;
	}

	public long getTaskId() {
		return taskId;
	}

}
