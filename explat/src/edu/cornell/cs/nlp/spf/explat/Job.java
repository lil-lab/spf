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
package edu.cornell.cs.nlp.spf.explat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Set;

import edu.cornell.cs.nlp.utils.log.Log;
import edu.cornell.cs.nlp.utils.log.thread.LoggingRunnable;

public abstract class Job extends LoggingRunnable {

	private boolean				completed	= false;
	private final Set<String>	dependencyIds;
	private final String		id;
	private final IJobListener	jobListener;
	private final boolean		openedOutputStream;
	private final PrintStream	outputStream;

	public Job(String id, Set<String> dependencyIds, IJobListener jobListener,
			File outputFile, File logFile) throws FileNotFoundException {
		super(logFile);
		this.outputStream = new PrintStream(outputFile);
		this.openedOutputStream = true;
		this.id = id;
		this.jobListener = jobListener;
		this.dependencyIds = dependencyIds;
	}

	public Job(String id, Set<String> dependencyIds, IJobListener jobListener,
			Log log) {
		super(log);
		assert id != null;
		this.outputStream = System.out;
		this.openedOutputStream = false;
		this.id = id;
		this.dependencyIds = dependencyIds;
		this.jobListener = jobListener;
	}

	public void close() {
		if (openedOutputStream) {
			outputStream.close();
		}
	}

	public Set<String> getDependencyIds() {
		return dependencyIds;
	}

	public String getId() {
		return id;
	}

	public PrintStream getOutputStream() {
		return outputStream;
	}

	public boolean isCompleted() {
		return completed;
	}

	@Override
	public final void loggedRun() {
		// Do the actual job
		try {
			doJob();
		} catch (final Throwable e) {
			jobListener.jobException(this, e);
			return;
		}

		// Mark job as completed
		completed = true;

		// Close output and log streams, if non standard
		outputStream.close();

		// Signal job completed
		jobListener.jobCompleted(this);
	}

	protected abstract void doJob();
}
