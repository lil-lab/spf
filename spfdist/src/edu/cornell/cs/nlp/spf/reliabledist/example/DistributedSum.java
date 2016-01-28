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
package edu.cornell.cs.nlp.spf.reliabledist.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import edu.cornell.cs.nlp.spf.reliabledist.AbstractEnvironment;
import edu.cornell.cs.nlp.spf.reliabledist.EnslavedLocalManager;
import edu.cornell.cs.nlp.spf.reliabledist.EnvironmentConfig;
import edu.cornell.cs.nlp.spf.reliabledist.JobFuture;
import edu.cornell.cs.nlp.spf.reliabledist.ReliableManager;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.log.thread.LoggingThreadFactory;

public class DistributedSum {
	public static final boolean			FAILURE_TESTING		= true;
	public static final ILogger			LOG					= LoggerFactory
			.create(DistributedSum.Slave.class);
	public static final String			MANAGER_HOST		= "localhost";
	public static final int				MANAGER_PORT		= 4444;
	public static final int				NUM_SLAVE_THREADS	= 3;
	public static final ThreadFactory	THREAD_FACTORY		= new LoggingThreadFactory(
			"worker");

	private static final int NUM_SLAVES = 3;

	public static void main(String[] args) throws Exception {

		// Logger.
		LogLevel.INFO.set();
		Logger.setSkipPrefix(true);

		// Create a master and some a few workers.
		new Manager("master").start();
		for (int i = 0; i < NUM_SLAVES; ++i) {
			new Slave(String.format("worker-%d", i)).start();
		}
	}

	public static class Slave extends Thread {

		public Slave(String threadName) {
			super(threadName);
		}

		@Override
		public void run() {
			EnslavedLocalManager manager = new EnslavedLocalManager(
					DistributedSum.MANAGER_HOST, DistributedSum.MANAGER_PORT,
					DistributedSum.NUM_SLAVE_THREADS, THREAD_FACTORY, "worker",
					null);
			THREAD_FACTORY.newThread(manager).start();

			// tests the ability to
			// make progress in the
			// face of errors.
			if (DistributedSum.FAILURE_TESTING) {
				while (manager.isRunning()) {
					if (Math.random() > .98) {
						LOG.info("Simulating worker failure");
						manager.terminate();
						manager = new EnslavedLocalManager(
								DistributedSum.MANAGER_HOST,
								DistributedSum.MANAGER_PORT,
								DistributedSum.NUM_SLAVE_THREADS,
								THREAD_FACTORY, "worker", null);
						THREAD_FACTORY.newThread(manager).start();
					}
					try {
						Thread.sleep(1000);
					} catch (final Exception e) {
						// Ignore.
					}
				}
			}

		}
	}

	public static class SummingConstants {
		public static String	ADDTOFACTOR		= "add";
		public static String	ARRAY			= "array";
		public static String	CACHING			= "caching";
		public static String	LOOPLENGTH		= "looplength";
		public static String	LOOPSUM			= "loopsum";
		public static String	MULTIPLYFACTOR	= "mulitply";
		public static String	SETFACTOR		= "set";
		public static String	TOTAL			= "total";
	}

	public static class SummingEnviroment extends AbstractEnvironment {
		private static final long	serialVersionUID	= 741536596033762532L;
		int							factor				= 0;
		int							init				= 0;
		int							total				= 0;

		public SummingEnviroment() {
		}

		public SummingEnviroment(int initialValue) {
			init = initialValue;
		}

		public synchronized void addToTotal(int value) {
			total += value;
		}

		public EnvironmentConfig<Integer> createAddToFactorUpdate(int x) {
			return new EnvironmentConfig<>(SummingConstants.ADDTOFACTOR, x);
		}

		public EnvironmentConfig<Integer> createFactorUpdate(int x) {
			return new EnvironmentConfig<Integer>(SummingConstants.SETFACTOR,
					x);
		}

		public EnvironmentConfig<Integer> createMultiplyFactorUpdate(int x) {
			return new EnvironmentConfig<Integer>(
					SummingConstants.MULTIPLYFACTOR, x);
		}

		public int getScalingFactor() {
			return factor;
		}

		public synchronized int getTotalAndReset() {
			final int rv = total;
			total = 0;
			return rv;
		}

		@Override
		protected void applyUpdate(EnvironmentConfig<?> update) {
			if (update.getKey().equals(SummingConstants.SETFACTOR)) {
				final int value = (Integer) update.getValue();
				this.factor = value;
			} else if (update.getKey().equals(SummingConstants.ADDTOFACTOR)) {
				final int value = (Integer) update.getValue();
				this.factor += value;
			} else
				if (update.getKey().equals(SummingConstants.MULTIPLYFACTOR)) {
				final int value = (Integer) update.getValue();
				this.factor *= value;
			}
		}

	}

	public static class SummingJob
			implements Function<SummingEnviroment, Integer>, Serializable {

		private static final long serialVersionUID = -6063968317541580360L;

		private final int expectedResult;

		private int looplength;

		private final List<Integer> values;

		public SummingJob(List<Integer> values, int looplength,
				int expectedResult) {
			this.values = values;
			this.looplength = looplength;
			this.expectedResult = expectedResult;
		}

		@Override
		public Integer apply(SummingEnviroment e) {
			final int factor = e.getScalingFactor();
			int total = 0;
			for (final Integer i : values) {
				try {
					total += factor * i; // this whole process is just too fast
											// as
											// of right now.
				} catch (final NumberFormatException ex) {
					continue;
				}
			}

			if (total != expectedResult) {
				throw new IllegalStateException("unexpected result");
			}

			looplength += (int) (Math.random() * 100);
			try {
				Thread.sleep(looplength);
			} catch (final Exception ex) {
				// Ignore.
			}

			LOG.info("...task slept for: " + looplength + "...");
			return total;
		}

	}

	private static class Manager extends Thread {

		public Manager(String name) {
			super(name);
		}

		@Override
		public void run() {
			try {
				final ReliableManager manager = new ReliableManager.Builder(
						new LoggingThreadFactory()).setPort(MANAGER_PORT)
								.setPingFrequency(2000).setTimeout(4000)
								.setPort(MANAGER_PORT).build();

				manager.start();

				// we are going to randomly sum for a few iterations and confirm
				// that distributed computation returns the same result

				final int rounds = 1;
				final int scalarOperations = 5;
				final int spansize = 5;
				final int totalspans = 5;
				final int looplength = 1000;

				final Random r = new Random();
				while (true) {
					final SummingEnviroment e = new SummingEnviroment();
					if (!manager.setupEnviroment(e)) {
						throw new IllegalStateException(
								"Faield to setup environemnt");
					}
					long totaltime = 0;
					long total = 0;
					int returned_total = 0;
					for (int i = 1; i <= rounds; i++) {
						manager.updateEnviroment(e.createFactorUpdate(i));
						int scalar = i;
						for (int j = 0; j < scalarOperations; j++) {
							final int op = r.nextInt(2);
							final int value = 1 + r.nextInt(5);

							if (op == 0) {
								scalar += value;
								if (!manager.updateEnviroment(
										e.createAddToFactorUpdate(value))) {
									throw new IllegalStateException(
											"Failed to update environment");
								}
							} else {
								scalar *= value;
								if (!manager.updateEnviroment(
										e.createMultiplyFactorUpdate(value))) {
									throw new IllegalStateException(
											"Failed to update environment");
								}
							}
						}
						System.err.println("current factor = " + scalar);
						final List<SummingJob> jobs = new ArrayList<SummingJob>();
						for (int j = 0; j < totalspans; j++) {
							final List<Integer> span = new ArrayList<Integer>();
							int spanTotal = 0;
							for (int k = 0; k < spansize; k++) {
								final Integer v = r.nextInt(10);
								spanTotal += scalar * v;
								total += scalar * v;
								span.add(v);
							}
							jobs.add(new SummingJob(span, looplength,
									spanTotal));
						}
						final List<JobFuture<Integer>> futures = new LinkedList<JobFuture<Integer>>();
						for (final SummingJob job : jobs) {
							final JobFuture<Integer> future = manager
									.execute(job);
							if (future == null) {
								throw new IllegalStateException(
										"Failed to submit job");
							} else {
								futures.add(future);
								LOG.debug("Submitted job %s", future);
							}
						}
						final long start = System.currentTimeMillis();
						while (true) {
							boolean complete = true;
							for (final JobFuture<Integer> future : futures) {
								if (!future.isDone()) {
									complete = false;
									break;
								}
							}
							if (complete) {
								break;
							}
							try {
								Thread.sleep(10);
							} catch (final Exception ex) {
								// Ignore.
							}
						}
						final long end = System.currentTimeMillis();
						totaltime += end - start;
						for (final JobFuture<Integer> future : futures) {
							System.out.println("LOG ---------");
							System.out.println(future.getLog());
							final Integer value = future.get();
							returned_total += value;
						}
					}
					System.out.println("Summary:");
					System.out.println(manager.getSummary());
					if (returned_total == total) {
						System.out.println("TOTAL AS EXPECTED! " + total);
					} else {
						System.out.println("TOTAL FAILED ! expected  : " + total
								+ " retrieved:" + returned_total);
						throw new IllegalStateException();
					}
					System.out.println(totaltime / 1000.0
							+ " ---------------------------------");
					if (!manager.createBoundary()) {
						throw new IllegalStateException("Can't create boundry");
					}
				}
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

}
