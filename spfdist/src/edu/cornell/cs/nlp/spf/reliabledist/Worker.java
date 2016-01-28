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

import java.io.File;

import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.log.thread.LoggingRunnable;

/**
 * Worker to run {@link Task}s.
 *
 * @author Yoav Artzi
 */
public class Worker implements Runnable, ITaskExecutor {

	public static final ILogger	LOG			= LoggerFactory
													.create(Worker.class);
	private Task				currentTask	= null;
	private boolean				isRunning	= true;
	private final File			loggingDir;

	private final IManager		manager;
	private final String		name;

	public Worker(IManager manager, String name, File loggingDir) {
		this.manager = manager;
		this.name = name;
		this.loggingDir = loggingDir;
	}

	@Override
	public boolean execute(Task task) {
		assert task != null;
		if (currentTask == null) {
			currentTask = task;
			synchronized (this) {
				notifyAll();
			}
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return name;
	}

	public synchronized boolean isFree() {
		return currentTask == null;
	}

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (this) {
				if (!isRunning) {
					return;
				}
				if (currentTask == null) {
					try {
						wait();
					} catch (final InterruptedException e) {
						// Ignore.
					}
				}
			}
			if (currentTask != null) {
				doExecute();
			}
		}
	}

	public void terminate() {
		isRunning = false;
	}

	private void doExecute() {
		final TaskResult result;
		final Task runningTask = currentTask;
		if (loggingDir == null) {
			result = runningTask.execute(manager.getEnviroment());
		} else {
			final LoggingTask loggingTask = new LoggingTask(new File(
					loggingDir, String.format("task-%d.log",
							runningTask.getId())), runningTask);
			loggingTask.run();
			result = loggingTask.result;
		}
		synchronized (this) {
			currentTask = null;
		}
		manager.reportResult(this, runningTask, result);

		LOG.info("%s :: executed task (id=%d)", name, runningTask.getId());
	}

	private class LoggingTask extends LoggingRunnable {

		private TaskResult	result;
		private final Task	task;

		public LoggingTask(File loggingFile, Task task) {
			super(loggingFile);
			this.task = task;
		}

		@Override
		public void loggedRun() {
			result = task.execute(manager.getEnviroment());
		}
	}

}
