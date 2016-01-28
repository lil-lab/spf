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
package edu.cornell.cs.nlp.spf.base.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

public class TinyExecutorService implements ExecutorService, ITinyExecutor {

	private static final ILogger		LOG					= LoggerFactory
																	.create(TinyExecutorService.class);
	private final ThreadPoolExecutor	executor;
	private boolean						isRunning			= true;
	private final long					monitorSleep;

	private final Map<Thread, Long>		scheduledTimeouts	= new ConcurrentHashMap<Thread, Long>();

	public TinyExecutorService(int nThreads) {
		this(nThreads, Executors.defaultThreadFactory(), DEFAULT_MONITOR_SLEEP);
	}

	public TinyExecutorService(int nThreads, ThreadFactory threadFactory,
			long monitorSleepMSec) {
		LOG.info("%s :: Creating executor with %d threads",
				TinyExecutorService.class.getSimpleName(), nThreads);
		// +1 for the monitor.
		this.executor = new ThreadPoolExecutor(nThreads + 1, nThreads + 1, 10L,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
				threadFactory);
		executor.allowCoreThreadTimeOut(false);
		this.monitorSleep = monitorSleepMSec;
		executor.submit(new Monitor());
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		final boolean result = executor.awaitTermination(timeout, unit);
		return result;
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
		// Create timed callables for all
		final List<Callable<T>> wrappers = new ArrayList<Callable<T>>(
				tasks.size());
		for (final Callable<T> task : tasks) {
			wrappers.add(new TimedCallable<T>(task, timeout));
		}

		// Invoke all wrapping callables
		return executor.invokeAll(wrappers);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public boolean isShutdown() {
		return executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	@Override
	public void shutdown() {
		isRunning = false;
		executor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		isRunning = false;
		return executor.shutdownNow();
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task, long timeout) {
		return executor.submit(new TimedCallable<T>(task, timeout));
	}

	@Override
	public Future<?> submit(Runnable task) {
		return executor.submit(task);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return executor.submit(task, result);
	}

	/**
	 * Job used to monitor timed jobs. Makes a best effort at killing them.
	 *
	 * @author Yoav Artzi
	 */
	private class Monitor implements Runnable {

		@Override
		public void run() {
			while (isRunning) {
				final long current = System.currentTimeMillis();
				final Iterator<Entry<Thread, Long>> iterator = scheduledTimeouts
						.entrySet().iterator();
				while (iterator.hasNext()) {
					final Entry<Thread, Long> entry = iterator.next();
					if (entry.getValue() < current) {
						iterator.remove();
						entry.getKey().interrupt();
					}
				}
				try {
					Thread.sleep(monitorSleep);
				} catch (final InterruptedException e) {
					if (isRunning) {
						LOG.info("Monitor thread interrupted.");
					}
				}
			}
		}

	}

	private class TimedCallable<V> implements Callable<V> {

		private final Callable<V>	task;
		private final long			timeout;

		public TimedCallable(Callable<V> task, long timeout) {
			this.task = task;
			this.timeout = timeout;
		}

		@Override
		public V call() throws Exception {

			scheduledTimeouts.put(Thread.currentThread(),
					System.currentTimeMillis() + timeout);
			try {
				return task.call();
			} catch (final InterruptedException e) {
				throw e;
			} catch (final ExecutionException e) {
				throw e;
			} finally {
				if (scheduledTimeouts.containsKey(Thread.currentThread())) {
					scheduledTimeouts.remove(Thread.currentThread());
				}
			}
		}

	}

}
