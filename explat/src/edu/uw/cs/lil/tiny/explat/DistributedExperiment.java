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
package edu.uw.cs.lil.tiny.explat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.uw.cs.lil.tiny.utils.concurrency.ITinyExecutor;
import edu.uw.cs.lil.tiny.utils.concurrency.TinyExecutorService;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.Log;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;
import edu.uw.cs.utils.log.LoggerFactory;
import edu.uw.cs.utils.log.thread.LoggingThreadFactory;

/**
 * Distributed experiment, based on {@link Job}.
 * 
 * @author Yoav Artzi
 */
public abstract class DistributedExperiment extends ParameterizedExperiment
		implements IJobListener, ITinyExecutor {
	private static final ILogger		LOG						= LoggerFactory
																		.create(DistributedExperiment.class);
	private final Set<String>			completedIds			= new HashSet<String>();
	final private Object				completionSignalObject	= new Object();
	
	private final TinyExecutorService	executor;
	
	private final List<Job>				jobs					= new LinkedList<Job>();
	private final Set<String>			launchedIds				= new HashSet<String>();
	private final File					outputDir;
	private boolean						running					= true;
	
	private final long					startingTime			= System.currentTimeMillis();
	
	public DistributedExperiment(File initFile) throws IOException {
		super(initFile);
		
		// //////////////////////////////////////////
		// Get parameters
		// //////////////////////////////////////////
		this.outputDir = globalParams.contains("outputDir") ? globalParams
				.getAsFile("outputDir") : null;
		// Create the directory, just to be on the safe side
		outputDir.mkdir();
		final File globalLogFile = globalParams.contains("globalLog")
				&& outputDir != null ? globalParams.getAsFile("globalLog")
				: null;
		
		// //////////////////////////////////////////
		// Create the executor
		// //////////////////////////////////////////
		this.executor = new TinyExecutorService(
				globalParams.contains("threads") ? Integer.valueOf(globalParams
						.get("threads")) : Runtime.getRuntime()
						.availableProcessors(), new LoggingThreadFactory());
		
		// //////////////////////////////////////////
		// Init logging and output stream
		// //////////////////////////////////////////
		Logger.DEFAULT_LOG = new Log(globalLogFile == null ? System.err
				: new PrintStream(globalLogFile));
		Logger.setSkipPrefix(true);
		LogLevel.setLogLevel(LogLevel.INFO);
		
		// //////////////////////////////////////////
		// Log global parameters
		// //////////////////////////////////////////
		LOG.info("Parameters:");
		for (final Pair<String, String> param : globalParams) {
			LOG.info("%s=%s", param.first(), param.second());
		}
	}
	
	@Override
	public void execute(Runnable command) {
		executor.execute(command);
	}
	
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return executor.invokeAll(tasks);
	}
	
	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return executor.invokeAll(tasks, timeout, unit);
	}
	
	@Override
	public void jobCompleted(String jobId) {
		boolean allCompleted = true;
		synchronized (jobs) {
			completedIds.add(jobId);
			if (running) {
				for (final Job job : jobs) {
					allCompleted &= job.isCompleted();
					if (!launchedIds.contains(job.getId())
							&& completedIds.containsAll(job.getDependencyIds())) {
						executor.execute(job);
						launchedIds.add(job.getId());
					}
				}
			}
			if (allCompleted) {
				running = false;
			}
		}
		
		if (allCompleted || !running) {
			synchronized (completionSignalObject) {
				completionSignalObject.notifyAll();
			}
		}
	}
	
	@Override
	public void jobException(String jobId, Exception e) {
		synchronized (jobs) {
			running = false;
		}
		LOG.error("Job %s threw an exception: %s", jobId, e.getMessage());
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		LOG.error(sw.toString()); // stack trace as a string
		jobCompleted(jobId);
	}
	
	public void start() {
		synchronized (jobs) {
			for (final Job job : jobs) {
				if (job.getDependencyIds().isEmpty()) {
					executor.execute(job);
					launchedIds.add(job.getId());
				}
			}
		}
		
		synchronized (completionSignalObject) {
			try {
				completionSignalObject.wait();
				executor.shutdown();
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
			LOG.info("Experiment completed");
			LOG.info("Total run time %.4f seconds",
					(System.currentTimeMillis() - startingTime) / 1000.0);
		}
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}
	
	@Override
	public void wait(Object object) throws InterruptedException {
		executor.wait(object);
	}
	
	@Override
	public void wait(Object object, long timeout) throws InterruptedException {
		executor.wait(object, timeout);
	}
	
	protected void addJob(Job job) {
		jobs.add(job);
	}
	
	protected PrintStream createJobLogStream(String jobId)
			throws FileNotFoundException {
		if (outputDir == null) {
			return System.err;
		} else {
			return new PrintStream(new File(outputDir, String.format("%s.log",
					jobId)));
		}
	}
	
	protected PrintStream createJobOutputStream(String jobId)
			throws FileNotFoundException {
		if (outputDir == null) {
			return System.out;
		}
		return new PrintStream(new File(outputDir, String.format("%s.out",
				jobId)));
	}
}
