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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ITinyExecutor extends Executor, Shutdownable {
	public static final long	DEFAULT_MONITOR_SLEEP	= 1000l;

	<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException;

	<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * Invoke all tasks each with a unique timer set when it starts running. If
	 * the time limit is reached, the executor makes a best effort to stop the
	 * task by interrupting it.
	 *
	 * @param tasks
	 * @param timeout
	 * @return
	 * @throws InterruptedException
	 */
	<T> List<Future<T>> invokeAllWithUniqueTimeout(
			Collection<? extends Callable<T>> tasks, long timeout)
			throws InterruptedException;

	<T> Future<T> submit(Callable<T> task);

	/**
	 * Submit the task and sets a timer when it starts running, if the time
	 * limit is reached, makes a best effort to stop the working thread by
	 * interrupting it.
	 *
	 * @param task
	 * @param timeout
	 * @return
	 */
	<T> Future<T> submit(Callable<T> task, long timeout);

}
