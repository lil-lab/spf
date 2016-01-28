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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.log.thread.LoggingThreadFactory;

/**
 * @author Yoav Artzi
 * @author Mark Yatskar
 */
public class EnslavedLocalManager implements IManager, Runnable, ITaskExecutor {
	public static final ILogger		LOG					= LoggerFactory
			.create(EnslavedLocalManager.class);

	private AbstractEnvironment		enviroment			= null;

	private ObjectInputStream		inputStream			= null;

	private boolean					isRunning			= true;

	private final LocalWorkerPool	localPool;

	private final String			masterAddress;
	private final int				masterPort;

	private final String			name;

	private ObjectOutputStream		outputStream		= null;

	private URLClassLoader			urlClassLoader		= new URLClassLoader(
			new URL[0]);

	private final AtomicInteger		workerIdGenerator	= new AtomicInteger(0);

	public EnslavedLocalManager(String masterAddress, int masterPort,
			int threads, ThreadFactory threadFactory, String name,
			File loggingDir) {
		this.masterAddress = masterAddress;
		this.masterPort = masterPort;
		this.name = name;
		this.localPool = new LocalWorkerPool(threads, threadFactory,
				() -> new Worker(EnslavedLocalManager.this,
						Integer.toString(workerIdGenerator.getAndIncrement()),
						loggingDir));
	}

	private static String stackToString(Exception e) {
		String output = "";
		for (final StackTraceElement _e : e.getStackTrace()) {
			output += "\t exception \t" + _e.toString() + "\n";
		}
		return output;
	}

	public void clientLoop() {

		// there are five types of messages.

		while (true) {
			// wait for information
			synchronized (this) {
				if (!isRunning) {
					return;
				}
			}
			try {
				final Object readObject = inputStream.readObject();

				if (!(readObject instanceof Message)) {
					LOG.error("Invalid object received: %s",
							readObject.getClass().toString());
				}

				final Message message = (Message) readObject;

				// If the message has an ID, ack it.
				if (message instanceof MessageWithId) {
					sendAck((MessageWithId) message);
				}

				LOG.debug("Received command: %s", message.getCommand());

				if (DistributionConstants.PING.equals(message.getCommand())) {
					// Reply with an empty AK.
					sendReply(DistributionConstants.AK);
				} else if (DistributionConstants.SUMMARY
						.equals(message.getCommand())) {
					// No real summary implemented, so just send an empty
					// summary message.
					sendReply(DistributionConstants.SUMMARY);
				} else if (DistributionConstants.ENIVROMENT
						.equals(message.getCommand())) {
					// Set the environment.
					setupEnviroment(message.getEnvironment());
				} else if (DistributionConstants.MODIFY_ENVIROMENT
						.equals(message.getCommand())) {
					updateEnviroment(message.getEnvUpdates());
				} else if (DistributionConstants.WORK
						.equals(message.getCommand())) {
					synchronized (this) {
						try {
							final Task task = message.getTask();
							LOG.info("Received task %d", task.getId());
							execute(task);
						} catch (final Exception ex) {
							throw ex;
						}
					}
				} else if (DistributionConstants.INIT
						.equals(message.getCommand())) {
					setupCommand(message.get());
				} else if (DistributionConstants.SHUTDOWN
						.equals(message.getCommand())) {
					LOG.info("Shutting down");
					// TODO handle this one better.
					System.exit(0);
				}

			} catch (final Exception e) {
				LOG.error("Exception in main loop: %s", e);
				sendErroredMessage("Fatal Exception in Main Loop", e);
				return;
			}
		}
	}

	@Override
	public boolean execute(Task task) {
		if (existsFree()) {
			return localPool.addWork(task);
		}
		LOG.error("no free space!");
		return false;
	}

	@Override
	public boolean existsFree() {
		return localPool.existsFree();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <ENV extends AbstractEnvironment> ENV getEnviroment() {
		return (ENV) enviroment;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public boolean reportResult(ITaskExecutor worker, Task task,
			TaskResult result) {
		synchronized (this) {
			return sendTaskReply(result);
		}
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {

			while (true) {
				Socket socket = null;
				try {
					socket = new Socket(masterAddress, masterPort);
					outputStream = new ObjectOutputStream(
							socket.getOutputStream());
					inputStream = new ObjectInputStream(
							socket.getInputStream());
					clientLoop();
					// Don't return here, instead just try to reconnect to the
					// master, in case it's just going down for a short while.
				} catch (final UnknownHostException e) {
					LOG.error("Don't know about host: %s", masterAddress);
				} catch (final IOException e) {
					LOG.error("Couldn't get I/O for the connection to: %s",
							masterAddress);
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (final IOException e) {
							// Ignore.
						}
					}
				}
				try {
					Thread.sleep(2000);
				} catch (final Exception e) {
					// Ignore.
				}
			}
		} catch (final RuntimeException e) {
			isRunning = false;
		}
	}

	@Override
	public synchronized boolean setupCommand(Map<String, String> initMap) {
		final String command = initMap.get(DistributionConstants._initcommand);
		if (command.equals(DistributionConstants.AWSKEY)) {
			// this.AWSACCESSKEY =
			// message.get(DistributionConstants._awsacsess);
			// this.AWSSECRETEKEY =
			// message.get(DistributionConstants._awssecret);
			throw new RuntimeException("not implemented");
		} else if (command.equals(DistributionConstants.DOWNLOADFILE)) {
			// return downloadAWS(message.get(DistributionConstants._bucket),
			// message.get(DistributionConstants._file));
			throw new RuntimeException("not implemented");
		} else if (command.equals(DistributionConstants.JARFILE)) {
			// if (!downloadAWS(message.get(DistributionConstants._bucket),
			// message.get(DistributionConstants._file))) {
			// return false;
			// }
			// return registerJar(message.get(DistributionConstants._file));
			throw new RuntimeException("not implemented");
		}

		return true;
	}

	@Override
	public synchronized boolean setupEnviroment(
			AbstractEnvironment environment) {
		if (localPool.allFree()) {
			LOG.info("Setup environment: %s", environment.getClass());
			enviroment = environment;
			return true;
		} else {
			sendErroredMessage(
					"Attempting to setup enviroment while jobs are running.");
			return false;
		}
	}

	public synchronized void terminate() {
		isRunning = false;
		localPool.terminate();
		try {
			if (outputStream != null) {
				outputStream.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (final Exception e) {
			LOG.error("Exception when closing intput stream: %s", e);
		}
	}

	@Override
	public boolean updateEnviroment(List<SerializedEnvironmentConfig> updates) {
		if (localPool.allFree()) {
			try {
				for (final SerializedEnvironmentConfig update : updates) {
					LOG.info("Applying environment update %s", update.getId());
					enviroment.update(new EnvironmentConfig<>(update));
				}
				return true;
			} catch (final IOException | ClassNotFoundException e) {
				sendErroredMessage("Failed to de-serialize environment update.",
						e);
				return false;
			}
		} else {
			sendErroredMessage(
					"Attempting to update enviroment while jobs are running.");
			return false;
		}
	}

	@SuppressWarnings("unused")
	private boolean registerJar(String file) {
		try {
			final File f = new File(file);
			final URL[] newurls = new URL[urlClassLoader.getURLs().length + 1];
			int i = 0;
			for (final URL _u : urlClassLoader.getURLs()) {
				newurls[i] = _u;
				i++;
			}
			newurls[i] = f.toURI().toURL();
			urlClassLoader = new URLClassLoader(newurls);
		} catch (final Exception e) {
			LOG.error("Exception when loading JAR: %s", e);
			return false;
		}
		return true;
	}

	private synchronized boolean send(Message message) {
		try {
			// Load the message with the status of the workers.
			message.put(DistributionConstants._free,
					Integer.toString(localPool.numFreeWorkers()));

			outputStream.writeObject(message);
			outputStream.flush();
			outputStream.reset();
			return true;
		} catch (final IOException e) {
			LOG.error("Failed to send: ", e);
			return false;
		}
	}

	private boolean sendAck(MessageWithId acked) {
		final long replyid = acked.getMessageId();
		final Message msg = new Message(DistributionConstants.AK);
		msg.put(DistributionConstants._ackid, Long.toString(replyid));
		return send(msg);
	}

	private boolean sendErroredMessage(String m) {
		LOG.info("Sending error message: %s", m);
		final Message message = new Message(DistributionConstants.ERROR);
		message.put(DistributionConstants._message, m);
		return send(message);
	}

	private boolean sendErroredMessage(String m, Exception e) {
		LOG.info("Sending error message with exception: %s", m);
		LOG.info(e);
		final Message msg = new Message(DistributionConstants.ERROR);
		msg.put(DistributionConstants._message, m + ":" + e.toString());
		msg.put(DistributionConstants._stacktrace, stackToString(e));
		return send(msg);
	}

	private boolean sendReply(String command) {
		return send(new Message(command));
	}

	private synchronized boolean sendTaskReply(TaskResult result) {
		return send(new Message(DistributionConstants.RETURN, result));
	}

	public static class Creator
			implements IResourceObjectCreator<EnslavedLocalManager> {

		private final String type;

		public Creator() {
			this("tinydist.worker");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public EnslavedLocalManager create(Parameters params,
				IResourceRepository repo) {
			return new EnslavedLocalManager(params.get("addr"),
					params.getAsInteger("port"), params.getAsInteger("threads"),
					new LoggingThreadFactory(params.get("name", "tinydist")),
					params.get("name", "tinydist"), params.contains("logDir")
							? params.getAsFile("logDir") : null);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, EnslavedLocalManager.class)
					.setDescription("Work manager")
					.addParam("addr", String.class, "Master address")
					.addParam("port", Integer.class, "Master port")
					.addParam("name", String.class,
							"Worker name (used for logging)")
					.addParam("logDir", File.class,
							"Logging directory for task execution (default: stderr)")
					.addParam("threads", Integer.class,
							"Number of worker threads")
					.build();
		}

	}

}
