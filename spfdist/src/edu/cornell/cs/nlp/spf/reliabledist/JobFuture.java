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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Yoav Artzi
 * @param <OUTPUT>
 */
public class JobFuture<OUTPUT> {

	private Throwable	exception		= null;
	private String		log				= null;
	private OUTPUT		output			= null;
	private boolean		resultReceived	= false;
	private String		workerName		= null;

	/**
	 * Waits if necessary for the computation to complete, and then retrieves
	 * its result.
	 *
	 * @return the computed result
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 */
	public OUTPUT get() throws InterruptedException, ExecutionException {
		if (resultReceived) {
			if (exception == null) {
				return output;
			} else {
				throw new ExecutionException(String.format("Exception at %s",
						workerName), exception);
			}
		} else {
			synchronized (this) {
				this.wait();
				if (exception == null) {
					return output;
				} else {
					throw new ExecutionException(String.format(
							"Exception at %s", workerName), exception);
				}
			}
		}
	}

	/**
	 * Waits if necessary for at most the given time for the computation to
	 * complete, and then retrieves its result, if available.
	 *
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return the computed result
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws TimeoutException
	 *             if the wait timed out
	 */
	public OUTPUT get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		if (resultReceived) {
			if (exception == null) {
				return output;
			} else {
				throw new ExecutionException(String.format("Exception at %s",
						workerName), exception);
			}
		} else {
			synchronized (this) {
				this.wait(unit.toMillis(timeout));
				if (resultReceived) {
					if (exception == null) {
						return output;
					} else {
						throw new ExecutionException(String.format(
								"Exception at %s", workerName), exception);
					}
				} else {
					throw new TimeoutException();
				}
			}
		}
	}

	public String getLog() throws InterruptedException {
		if (resultReceived) {
			return log;
		} else {
			synchronized (this) {
				this.wait();
				return log;
			}
		}
	}

	/**
	 * Returns {@code true} if this task completed.
	 */
	public boolean isDone() {
		return resultReceived;
	}

	void setResult(ITaskExecutor worker, TaskResult result) {
		synchronized (this) {
			log = result.getLog();
			output = result.getOutput();
			exception = result.getException();
			workerName = worker.getName();

			// Must be set last.
			resultReceived = true;
			this.notifyAll();
		}
	}

}
