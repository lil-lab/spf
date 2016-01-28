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

import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Local pool of {@link Worker}s.
 *
 * @author Yoav Artzi
 * @author Mark Yatskar
 */
public class LocalWorkerPool {

	public static final ILogger	LOG	= LoggerFactory
											.create(LocalWorkerPool.class);

	private final Worker[]		pool;

	private final int			threads;

	public LocalWorkerPool(int threads, ThreadFactory threadFactory,
			Supplier<Worker> workerSupplier) {
		this.threads = threads;
		this.pool = new Worker[threads];
		for (int i = 0; i < threads; i++) {
			final Worker worker = workerSupplier.get();
			threadFactory.newThread(worker).start();
			pool[i] = worker;
		}
	}

	public boolean addWork(Task task) {
		for (int i = 0; i < pool.length; i++) {
			final Worker worker = pool[i];
			if (worker.isFree()) {
				worker.execute(task);
				LOG.info("%s :: assigned work (id = %d)", worker.getName(),
						task.getId());
				return true;
			}
		}
		LOG.info("Failed to assign task (id = %d)", task.getId());
		return false;
	}

	public boolean allFree() {
		for (int i = 0; i < threads; i++) {
			if (!pool[i].isFree()) {
				return false;
			}
		}
		return true;
	}

	public boolean existsFree() {
		for (int i = 0; i < threads; i++) {
			if (pool[i].isFree()) {
				return true;
			}
		}
		return false;
	}

	public boolean isRunning() {
		for (int i = 0; i < pool.length; ++i) {
			if (pool[i].isRunning()) {
				return true;
			}
		}
		return false;
	}

	public int numFreeWorkers() {
		int total = 0;
		for (int i = 0; i < pool.length; i++) {
			total += pool[i].isFree() ? 1 : 0;
		}
		return total;
	}

	public void terminate() {
		for (final Worker worker : pool) {
			worker.terminate();
		}
	}
}
