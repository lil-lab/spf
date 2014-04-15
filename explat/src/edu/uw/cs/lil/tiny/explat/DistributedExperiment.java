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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.uw.cs.lil.tiny.base.concurrency.ITinyExecutor;
import edu.uw.cs.lil.tiny.base.concurrency.TinyExecutorService;
import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;
import edu.uw.cs.utils.log.thread.LoggingThreadFactory;

/**
 * Distributed experiment, based on {@link Job}.
 * 
 * @author Yoav Artzi
 */
public abstract class DistributedExperiment extends LoggedExperiment implements
		IJobListener, ITinyExecutor {
	public static final ILogger			LOG						= LoggerFactory
																		.create(DistributedExperiment.class);
	private final Set<String>			completedIds			= new HashSet<String>();
	final private Object				completionSignalObject	= new Object();
	
	private final TinyExecutorService	executor;
	
	private final List<Job>				jobs					= new LinkedList<Job>();
	
	private final Set<String>			launchedIds				= new HashSet<String>();
	
	private boolean						running					= true;
	
	/** Run one job at a time. */
	private final boolean				serial;
	
	private final long					startingTime			= System.currentTimeMillis();
	
	public DistributedExperiment(File initFile, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo) throws IOException {
		super(initFile, envParams, creatorRepo);
		
		// //////////////////////////////////////////
		// Set the serial flag
		// //////////////////////////////////////////
		this.serial = globalParams.getAsBoolean("serial");
		
		// //////////////////////////////////////////
		// Create the executor
		// //////////////////////////////////////////
		this.executor = new TinyExecutorService(
				globalParams.contains("threads") ? Integer.valueOf(globalParams
						.get("threads")) : Runtime.getRuntime()
						.availableProcessors(), new LoggingThreadFactory(),
				globalParams.contains("threadMonitorPolling") ? Long
						.valueOf(globalParams.get("threadMonitorPolling"))
						: ITinyExecutor.DEFAULT_MONITOR_SLEEP);
	}
	
	public DistributedExperiment(File initFile,
			ResourceCreatorRepository creatorRepo) throws IOException {
		this(initFile, Collections.<String, String> emptyMap(), creatorRepo);
	}
	
	@Override
	public void end() {
		executor.shutdown();
		super.end();
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
	public <T> List<Future<T>> invokeAllWithUniqueTimeout(
			Collection<? extends Callable<T>> tasks, long timeout)
			throws InterruptedException {
		return executor.invokeAllWithUniqueTimeout(tasks, timeout);
	}
	
	@Override
	public void jobCompleted(Job job) {
		boolean allCompleted = true;
		synchronized (jobs) {
			completedIds.add(job.getId());
			if (running) {
				for (final Job queuedJob : jobs) {
					allCompleted &= queuedJob.isCompleted();
					if (!launchedIds.contains(queuedJob.getId())
							&& completedIds.containsAll(queuedJob
									.getDependencyIds())) {
						executor.execute(queuedJob);
						launchedIds.add(queuedJob.getId());
						if (serial) {
							break;
						}
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
	public void jobException(Job job, Exception e) {
		synchronized (jobs) {
			running = false;
		}
		LOG.error("Job %s threw an exception: %s", job.getId(), e.getMessage());
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		LOG.error(sw.toString()); // stack trace as a string
		jobCompleted(job);
	}
	
	public void start() {
		synchronized (jobs) {
			for (final Job job : jobs) {
				if (job.getDependencyIds().isEmpty()) {
					executor.execute(job);
					launchedIds.add(job.getId());
					if (serial) {
						break;
					}
				}
			}
		}
		
		synchronized (completionSignalObject) {
			try {
				completionSignalObject.wait();
				end();
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
	public <T> Future<T> submit(Callable<T> task, long timeout) {
		return executor.submit(task, timeout);
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
	
	protected File createJobLogFile(String jobId) {
		return new File(outputDir, String.format("%s.log", jobId));
	}
	
	protected File createJobOutputFile(String jobId) {
		return new File(outputDir, String.format("%s.out", jobId));
	}
}
