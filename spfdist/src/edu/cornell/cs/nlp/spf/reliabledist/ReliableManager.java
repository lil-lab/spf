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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.cornell.cs.nlp.spf.base.concurrency.Shutdownable;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.log.thread.LoggingThreadFactory;

/**
 * @author Yoav Artzi
 * @author Mark Yatskar
 */
public class ReliableManager implements Runnable, Shutdownable {

	public static final ILogger								LOG								= LoggerFactory
			.create(ReliableManager.class);

	private final List<Task>								completed						= new LinkedList<Task>();

	private final List<Map<String, String>>					connectionCommands;

	private final long										connectionTimeout;

	private AbstractEnvironment								currentEnviroment				= null;

	/**
	 * All updates to the environment should happen under this lock,
	 * including: setting the current environment to null, updating the local
	 * environment, sending updates to workers, sending the current environment
	 * to a new worker.
	 */
	private final Object									environmentLock					= new Object();

	/**
	 * Update IDs are used solely for debug and logging. This is atomic integer,
	 * so there's no need to take a lock before using it.
	 */
	private final AtomicInteger								environmentUpdateIdGenerator	= new AtomicInteger(
			0);

	/**
	 * Maps task IDs to JobFuture. This is used to update the {@link JobFuture},
	 * which is held by the user, with the result.
	 */
	private final Map<Long, JobFuture<?>>					futures							= new HashMap<Long, JobFuture<?>>();

	private boolean											isRunning						= true;
	private final AtomicInteger								managerIdGenerator				= new AtomicInteger(
			0);

	private final List<EnslavedRemoteManager>				managers						= new LinkedList<EnslavedRemoteManager>();

	private final Thread									mythread;

	private final List<IManager>							nonworkingManager				= new LinkedList<IManager>();

	private final long										pingFrequency;
	private final Queue<Task>								queuedTasks						= new LinkedList<Task>();

	private final Thread									register;
	private final int										registerPort;

	private final Map<EnslavedRemoteManager, List<Task>>	runningTasks					= new HashMap<EnslavedRemoteManager, List<Task>>();

	/**
	 * An optional file to dump a summary of the manager's state. The frequency
	 * of writing the state is controlled by {@link #summaryFrequency}.
	 */
	private final File										summaryFile;

	/**
	 * The frequency of writing the summary to the {@link #summaryFile}.
	 */
	private final long										summaryFrequency;

	private final AtomicLong								taskIdGenerator					= new AtomicLong(
			0);

	private final Map<Task, ITaskExecutor>					taskWorker						= new HashMap<Task, ITaskExecutor>();

	private final Object									terminationLock					= new Object();

	private final ThreadFactory								threadFactory;

	private final AtomicInteger								totalCompletedTask				= new AtomicInteger(
			0);

	private final AtomicInteger								totalRedone						= new AtomicInteger(
			0);

	public ReliableManager(int registerPort,
			List<Map<String, String>> connectionCommands, long pingFrequency,
			long pingTimeout, ThreadFactory threadFactory, File summaryFile,
			long summaryFrequency) {
		this.registerPort = registerPort;
		this.connectionCommands = connectionCommands;
		this.pingFrequency = pingFrequency;
		this.connectionTimeout = pingTimeout;
		this.threadFactory = threadFactory;
		this.summaryFile = summaryFile;
		this.summaryFrequency = summaryFrequency;
		this.register = threadFactory.newThread(new RegisterThread());
		this.mythread = threadFactory.newThread(this);
		LOG.info("Init %s: summaryFile=%s ...", getClass(), summaryFile);
		LOG.info("Init %s: summaryFrequency=%d ...", getClass(),
				summaryFrequency);
		LOG.info("Init %s: registerPort=%d ...", getClass(), registerPort);
		LOG.info("Init %s: pingFrequency=%d ...", getClass(), pingFrequency);
		LOG.info("Init %s: pingTimeout=%d", getClass(), pingTimeout);
	}

	private static List<Pair<String, String>> readConfigurationCommands(
			File file) throws FileNotFoundException, IOException {
		final List<Pair<String, String>> configurationCommands = new LinkedList<Pair<String, String>>();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(new FileInputStream(file)))) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] split = line.split("\t", 2);
				configurationCommands.add(Pair.of(split[0], split[1]));
			}
		}
		return configurationCommands;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		terminationLock.wait(unit.toMillis(timeout));
		return isTerminated();
	}

	public boolean canCreateBoundary() {
		return getRemainingOutstandingTasks() == 0;
	}

	public synchronized boolean createBoundary() {
		synchronized (this) {
			if (!canCreateBoundary()) {
				return false;
			}
			synchronized (environmentLock) {
				currentEnviroment = null;
				environmentUpdateIdGenerator.set(0);
			}
			taskWorker.clear();
			return true;
		}
	}

	public <ENV extends AbstractEnvironment, OUTPUT> JobFuture<OUTPUT> execute(
			Function<ENV, OUTPUT> job) {
		if (!(job instanceof Serializable)) {
			throw new IllegalArgumentException(
					"Class not serializable: " + job.getClass().getName());
		}

		@SuppressWarnings("unchecked")
		final Task task = new Task((Function<AbstractEnvironment, ?>) job,
				taskIdGenerator.getAndIncrement());
		final JobFuture<OUTPUT> future = new JobFuture<OUTPUT>();
		synchronized (futures) {
			futures.put(task.getId(), future);
		}
		execute(task);
		return future;
	}

	public boolean existsFree() {
		for (final IManager m : managers) {
			if (m.existsFree()) {
				return true;
			}
		}
		return false;
	}

	public List<Task> getDoneTasks() {
		return Collections.unmodifiableList(completed);
	}

	@SuppressWarnings("unchecked")
	public <ENV extends AbstractEnvironment> ENV getEnviroment() {
		return (ENV) currentEnviroment;
	}

	public int getRemainingOutstandingTasks() {
		synchronized (this) {
			return queuedTasks.size() + numRunning();
		}
	}

	public synchronized ManagerSummary getSummary() {
		final ManagerSummary.Builder builder = new ManagerSummary.Builder();

		for (final EnslavedRemoteManager manager : managers) {
			builder.addWorker(manager.getSummary());
		}

		builder.setFailedWorkers(nonworkingManager.size());
		builder.setCompletedTasks(totalCompletedTask.get());
		builder.setRedoneTasks(totalRedone.get());

		return builder.build();
	}

	public boolean isRunning() {
		return mythread.isAlive();
	}

	@Override
	public boolean isShutdown() {
		return isRunning();
	}

	@Override
	public boolean isTerminated() {
		return !isRunning;
	}

	public int numRunning() {
		synchronized (this) {
			int rv = 0;
			for (final List<Task> l : runningTasks.values()) {
				rv += l.size();
			}
			return rv;
		}
	}

	public boolean reportResult(ITaskExecutor worker, Task task,
			TaskResult result) {
		synchronized (this) {
			if (!managers.contains(worker)) {
				return false;
			}

			if (!runningTasks.get(worker).remove(task)) {
				return false;
			}
		}

		synchronized (futures) {
			final JobFuture<?> future = futures.get(task.getId());
			if (future == null) {
				throw new IllegalStateException("Future is missing");
			}
			futures.remove(task.getId());
			future.setResult(worker, result);
		}

		totalCompletedTask.getAndIncrement();
		return true;
	}

	@Override
	public void run() {
		long lastSummaryDump = 0;
		while (isRunning) {
			// three things need to done here.
			// first, see if anyone has failed, and restart their running and
			// volatile tasks.
			try {
				synchronized (this) {
					final Iterator<EnslavedRemoteManager> iterator = managers
							.iterator();

					// Check for failed workers.
					while (iterator.hasNext()) {
						final EnslavedRemoteManager manager = iterator.next();
						if (!manager.isRunning()) {
							LOG.info("Manager failed: [%d] %s", manager.getId(),
									manager.getName());
							LOG.info("Resubmitting %d tasks...",
									runningTasks.get(manager).size());
							for (final Task task : runningTasks.get(manager)) {
								LOG.info("Resubmitted %s, %d",
										task.getClass().getName(),
										task.getId());
								execute(task);
								totalRedone.getAndIncrement();
							}
							nonworkingManager.add(manager);
							iterator.remove();
							runningTasks.remove(manager);
						}
					}

					// Sort the managers according to the performance. Each
					// manager stores the the time of the last ten tasks
					// executed. We prioritize managers that have a lower mean
					// time for task execution. The times might be updated
					// during sorting, so we copy them aside.
					final List<EnslavedRemoteManager> sortedManagers = managers
							.stream()
							.map(m -> Pair.of(m, m.getExecutionTimeAverage()))
							.sorted((p1, p2) -> Double.compare(p1.second(),
									p2.second()))
							.map(p -> p.first()).collect(Collectors.toList());

					// Do a single round of distributing work to the managers.
					// Each manager gets a single new job assigned to them. If
					// more works are queued, they will be distributed in the
					// next round. This is mean to distribute work evenly, which
					// works better for memory and CPU intensive jobs,
					// especially when the pool is not saturated.
					for (final EnslavedRemoteManager manager : sortedManagers) {
						if (manager.existsFree() && queuedTasks.size() > 0) {
							final Task t = queuedTasks.peek();
							if (!manager.execute(t)) {
								// The manager didn't accept the work. Not sure
								// why, but move on.
								LOG.info("Manager %d refused task %d",
										manager.getId(), t.getId());
								break;
							}
							queuedTasks.poll();
							runningTasks.get(manager).add(t);
							taskWorker.put(t, manager);
						}
					}

					// Dump a summary of the worker state into a file.
					if (summaryFile != null) {
						if (System.currentTimeMillis()
								- lastSummaryDump > summaryFrequency) {
							try (final OutputStream os = new FileOutputStream(
									summaryFile);
									final PrintStream stream = new PrintStream(
											os);) {
								stream.println(new SimpleDateFormat(
										"yyyy-MM-dd HH:mm:ss.SSS")
												.format(new Date()));
								stream.print(getSummary());
							}
							lastSummaryDump = System.currentTimeMillis();
						}
					}

					// Sleep until something happens. Only if there are not
					// tasks to distribute. TODO

				}
			} catch (final Exception e) {
				LOG.error("Exception from main loop: %s", e);
			}
		}
	}

	public boolean setupCommand(Map<String, String> v) {
		connectionCommands.add(v);
		return true;
	}

	public boolean setupEnviroment(AbstractEnvironment e) {
		assert e != null;
		synchronized (this) {
			if (!canCreateBoundary()) {
				return false;
			}
			boolean allok = true;
			synchronized (environmentLock) {
				currentEnviroment = e;
				environmentUpdateIdGenerator.set(0);
				for (final EnslavedRemoteManager m : managers) {
					if (!m.setupEnviroment(currentEnviroment)) {
						allok = false;
					}
				}
			}
			return allok;
		}
	}

	@Override
	public void shutdown() {
		synchronized (this) {
			isRunning = false;
			for (final EnslavedRemoteManager manager : managers) {
				manager.terminate();
			}
			this.notifyAll();
		}
	}

	@Override
	public List<Runnable> shutdownNow() {
		shutdown();
		synchronized (queuedTasks) {
			final List<Runnable> runnables = new LinkedList<Runnable>();
			for (final Task task : queuedTasks) {
				runnables.add(() -> task.execute(getEnviroment()));
			}
			return runnables;
		}
	}

	public void start() {
		register.start();
		mythread.start();
	}

	public boolean updateEnviroment(EnvironmentConfig<?> update) {
		return updateEnviroment(ListUtils.createSingletonList(update));
	}

	public boolean updateEnviroment(List<EnvironmentConfig<?>> updates) {
		synchronized (this) {
			if (!canCreateBoundary()) {
				LOG.error(
						"Trying to modify enviroment when boundary cannot be made. ");
				return false;
			}

			// Transform the objects in the update into a byte array. This is
			// intended to save the cost of serializing for each manager.
			final List<SerializedEnvironmentConfig> serializedUpdates = new ArrayList<>(
					updates.size());
			for (final EnvironmentConfig<?> update : updates) {
				try {
					final SerializedEnvironmentConfig serialized = new SerializedEnvironmentConfig(
							update,
							environmentUpdateIdGenerator.getAndIncrement());
					serializedUpdates.add(serialized);
				} catch (final IOException e) {
					LOG.error("Failed to serialize environment update: %s", e);
					throw new RuntimeException(e);
				}
			}

			// Send the updates to all the managers and apply to the local
			// environment.
			boolean allok = true;
			synchronized (environmentLock) {
				for (final EnslavedRemoteManager m : managers) {
					if (!m.updateEnviroment(
							Collections.unmodifiableList(serializedUpdates))) {
						allok = false;
					}
				}
				for (final EnvironmentConfig<?> update : updates) {
					currentEnviroment.update(update);
				}
			}
			return allok;
		}
	}

	private void execute(Task task) {
		synchronized (this) {
			this.queuedTasks.add(task);
			this.notifyAll();
		}
	}

	private void registerManager(EnslavedRemoteManager manager) {
		synchronized (this) {
			// there is way way more to do here.
			// we need to catch up the manager.
			// volatileTasks.put(m, new ArrayList<ITask>());
			runningTasks.put(manager, new ArrayList<Task>());
			for (final Map<String, String> init : connectionCommands) {
				manager.setupCommand(init);
			}

			synchronized (environmentLock) {
				if (currentEnviroment != null) {
					manager.setupEnviroment(currentEnviroment);
				}
			}

			managers.add(manager);
			LOG.info("Added new manager: %s -> %s", manager.getId(),
					manager.getName());
		}
	}

	public static class Builder {

		private final List<Map<String, String>>	connectionCommands	= new ArrayList<Map<String, String>>();

		private long							pingFrequency		= 20000;
		private int								port				= -1;
		private File							summaryFile			= null;
		/**
		 * Default: 20sec.
		 */
		private long							summaryFrequency	= 20000;
		private final ThreadFactory				threadFactory;

		private long							timeout				= 200000;

		public Builder(ThreadFactory threadFactory) {
			this.threadFactory = threadFactory;
		}

		public ReliableManager build() {
			if (port < 0) {
				throw new IllegalStateException("Port not set");
			}
			return new ReliableManager(port, connectionCommands, pingFrequency,
					timeout, threadFactory, summaryFile, summaryFrequency);
		}

		public Builder configureFromFile(File configFile)
				throws FileNotFoundException, IOException {
			final List<Pair<String, String>> configurationCommands = readConfigurationCommands(
					configFile);
			for (final Pair<String, String> pair : configurationCommands) {
				final String command = pair.first();
				final String content = pair.second();
				final String[] splitContent = content.split("\t");

				if (command.equals(DistributionConstants.PORT)) {
					this.port = Integer.parseInt(splitContent[0]);
				} else if (command.equals(DistributionConstants.AWSKEY)) {
					final String awsAccessKey = splitContent[0];
					final String awsSecretKey = splitContent[1];
					final HashMap<String, String> message = new HashMap<String, String>();
					message.put(DistributionConstants._initcommand,
							DistributionConstants.AWSKEY);
					message.put(DistributionConstants._awsacsess, awsAccessKey);
					message.put(DistributionConstants._awssecret, awsSecretKey);
					setupCommand(message);
				} else if (command.equals(DistributionConstants.DOWNLOADFILE)) {
					final String bucket = splitContent[0];
					final String file = splitContent[1];
					final HashMap<String, String> message = new HashMap<String, String>();
					message.put(DistributionConstants._initcommand,
							DistributionConstants.DOWNLOADFILE);
					message.put(DistributionConstants._bucket, bucket);
					message.put(DistributionConstants._file, file);
					setupCommand(message);
				} else if (command.equals(DistributionConstants.JARFILE)) {
					final String bucket = splitContent[0];
					final String file = splitContent[1];
					final HashMap<String, String> message = new HashMap<String, String>();
					message.put(DistributionConstants._initcommand,
							DistributionConstants.JARFILE);
					message.put(DistributionConstants._bucket, bucket);
					message.put(DistributionConstants._file, file);
					setupCommand(message);
				}
			}
			return this;
		}

		public Builder setPingFrequency(long pingFrequency) {
			this.pingFrequency = pingFrequency;
			return this;
		}

		public Builder setPort(int registerPort) {
			this.port = registerPort;
			return this;
		}

		public Builder setSummaryFile(File summaryFile) {
			this.summaryFile = summaryFile;
			return this;
		}

		public Builder setSummaryFrequency(long summaryFrequency) {
			this.summaryFrequency = summaryFrequency;
			return this;
		}

		public Builder setTimeout(long pingTimeout) {
			this.timeout = pingTimeout;
			return this;
		}

		private boolean setupCommand(HashMap<String, String> v) {
			connectionCommands.add(v);
			return true;
		}

	}

	public static class Creator
			implements IResourceObjectCreator<ReliableManager> {

		private final String type;

		public Creator() {
			this("tinydist.reliable");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ReliableManager create(Parameters params,
				IResourceRepository repo) {
			final Builder builder = new Builder(
					new LoggingThreadFactory("tinydist"));

			if (params.contains("summary")) {
				builder.setSummaryFile(params.getAsFile("summary"));
			}

			if (params.contains("summaryFreq")) {
				builder.setSummaryFrequency(params.getAsLong("summaryFreq"));
			}

			if (params.contains("port")) {
				builder.setPort(params.getAsInteger("port"));
			}

			if (params.contains("pingFreq")) {
				builder.setPingFrequency(params.getAsInteger("pingFreq"));
			}

			if (params.contains("pingTimeout")) {
				builder.setTimeout(params.getAsInteger("pingTimeout"));
			}

			if (params.contains("config")) {
				try {
					builder.configureFromFile(params.getAsFile("config"));
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}

			return builder.build();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, ReliableManager.class)
					.setDescription("Reliable manager for TinyDist")
					.addParam("summary", File.class,
							"File to dump the summary (default: none)")
					.addParam("summaryFreq", Long.class,
							"Summary dump frequency (default: 20sec)")
					.addParam("port", Integer.class, "Incoming connection port")
					.addParam("pingFreq", Integer.class,
							"Ping fequency (default: 20000)")
					.addParam("timeout", Integer.class,
							"Connection timeout (default: 200000)")
					.addParam("config", File.class, "Configuration file")
					.build();
		}

	}

	private class RegisterThread implements Runnable {

		@Override
		public void run() {
			while (true) {
				try (ServerSocket serverSocket = new ServerSocket(
						registerPort)) {
					final EnslavedRemoteManager manager = new EnslavedRemoteManager(
							serverSocket.accept(), pingFrequency,
							connectionTimeout, ReliableManager.this,
							managerIdGenerator.getAndIncrement());
					threadFactory.newThread(manager).start();
					LOG.info("Starting new manager: %s -> %s", manager.getId(),
							manager.getName());
					registerManager(manager);
				} catch (final Exception e) {
					LOG.error(
							"Failed to create connection and start worker thread: %s",
							e);
					LOG.error("Restarting listening thread");
				}
			}
		}
	}

}
