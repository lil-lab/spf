/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
package edu.uw.cs.lil.tiny.utils.concurrency;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TinyExecutorService implements ExecutorService, ITinyExecutor {
	private final ThreadPoolExecutor	executor;
	
	public TinyExecutorService(int nThreads) {
		this(nThreads, Executors.defaultThreadFactory());
	}
	
	public TinyExecutorService(int nThreads, ThreadFactory threadFactory) {
		this.executor = new ThreadPoolExecutor(nThreads, nThreads, 0L,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
				threadFactory);
		executor.allowCoreThreadTimeOut(false);
	}
	
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() + 1);
		}
		final boolean result = executor.awaitTermination(timeout, unit);
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() + 1);
		}
		return result;
	}
	
	@Override
	public void execute(Runnable command) {
		executor.execute(command);
	}
	
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() + 1);
		}
		final List<Future<T>> result = executor.invokeAll(tasks);
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() - 1);
		}
		return result;
	}
	
	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() + 1);
		}
		final List<Future<T>> result = executor.invokeAll(tasks, timeout, unit);
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() - 1);
		}
		return result;
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
		executor.shutdown();
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		return executor.shutdownNow();
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		return executor.submit(task);
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return executor.submit(task, result);
	}
	
	@Override
	public void wait(Object object) throws InterruptedException {
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() + 1);
		}
		try {
			object.wait();
		} finally {
			synchronized (executor) {
				executor.setCorePoolSize(executor.getCorePoolSize() - 1);
			}
		}
	}
	
	@Override
	public void wait(Object object, long timeout) throws InterruptedException {
		synchronized (executor) {
			executor.setCorePoolSize(executor.getCorePoolSize() + 1);
		}
		try {
			object.wait(timeout);
		} finally {
			synchronized (executor) {
				executor.setCorePoolSize(executor.getCorePoolSize() - 1);
			}
		}
	}
	
}
