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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * @author Yoav Artzi
 * @author Mark Yatskar
 */
public class EnslavedRemoteManager
		implements IManager, Runnable, ITaskExecutor {

	public static final ILogger LOG = LoggerFactory
			.create(EnslavedRemoteManager.class);

	private MessageWithId			activeMessage	= null;
	private final Map<Long, Task>	activeTasks		= new HashMap<>();

	private final Map<Long, Long> activeTaskStartTime = new HashMap<>();

	private double executionTimeDecayingAverage = 0.0;

	private int freeSpots = -1;

	private final ReliableManager globalManager;

	private final int				id;
	private final Queue<Object>		incomingObjects	= new ConcurrentLinkedQueue<Object>();
	private final ObjectInputStream	inputStream;

	private boolean isFailed = false;

	private boolean	isRunning	= true;
	private long	lastHeard	= System.currentTimeMillis();
	private long	lastPing	= System.currentTimeMillis();

	private final AtomicLong			messageIdGenerator	= new AtomicLong(0);
	private final String				name;
	private final ObjectOutputStream	outputStream;
	private final long					pingFrequency;

	private final long pingTimeout;

	private final Queue<MessageWithId> queuedMessages = new LinkedList<MessageWithId>();

	private final ObjectReadingThread readingThread;

	private final AtomicInteger taskAccepted = new AtomicInteger(0);

	private final AtomicInteger taskReturned = new AtomicInteger(0);

	public EnslavedRemoteManager(Socket client, long pingFrequency,
			long pingTimeout, ReliableManager globalManager, int id)
					throws IOException {
		this.pingFrequency = pingFrequency;
		this.pingTimeout = pingTimeout;
		this.globalManager = globalManager;
		this.id = id;
		this.name = client.getInetAddress().toString() + ":" + client.getPort();
		this.outputStream = new ObjectOutputStream(client.getOutputStream());
		this.inputStream = new ObjectInputStream(client.getInputStream());
		this.readingThread = new ObjectReadingThread();
		readingThread.start();
	}

	public boolean allFree() {
		return activeTasks.isEmpty();
	}

	@Override
	public boolean execute(Task task) {
		if (freeSpots == 0) {
			return false;
		}
		final MessageWithId message = new MessageWithId(
				messageIdGenerator.getAndIncrement(),
				DistributionConstants.WORK, task);
		synchronized (this) {
			activeTasks.put(task.getId(), task);
			activeTaskStartTime.put(task.getId(), System.currentTimeMillis());
			freeSpots--; // even if you haven't gotten back such an indicator.
			taskAccepted.incrementAndGet();
		}
		qsend(message);
		return true;
	}

	@Override
	public boolean existsFree() {
		synchronized (this) {
			return freeSpots > 0 && queuedMessages.isEmpty();
		}
	}

	@Override
	public <ENV extends AbstractEnvironment> ENV getEnviroment() {
		return null;
	}

	public double getExecutionTimeAverage() {
		return executionTimeDecayingAverage;
	}

	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public WorkerSummary getSummary() {
		return new WorkerSummary.Builder(id, getName())
				.setTasksAccepted(taskAccepted.get())
				.setTaskCompelted(taskReturned.get()).setFailed(isFailed)
				.setMeanTime(executionTimeDecayingAverage)
				.setFreeSpots(freeSpots).build();
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public boolean reportResult(ITaskExecutor worker, Task task,
			TaskResult result) {
		// Nothing much to do here. It shouldn't be called.
		throw new IllegalStateException();
	}

	@Override
	public void run() {
		try {
			serverLoop();
		} catch (final RuntimeException e) {
			isRunning = false;
			throw e;
		}

	}

	public void serverLoop() {
		// Main server loop.
		while (true) {

			if (!isRunning) {
				break;
			}

			// Used for tracking pings.
			final long currentTime = System.currentTimeMillis();

			try {

				// Send a ping if required.
				if (currentTime - lastHeard > pingFrequency) {
					if (currentTime - lastHeard > pingTimeout * 2) {
						LOG.error("Entering failed state on ping failure.");
						isRunning = false;
						isFailed = true;
						break;
					} else if (currentTime - lastPing > pingFrequency) {
						// Case we sent the ping a while ago, can send another
						// one.
						lastPing = currentTime;
						LOG.debug("Sending ping");
						if (!send(new Message(DistributionConstants.PING))) {
							isFailed = true;
							isRunning = false;
							break;
						}
					}

				}

				// Try to send a message if we have and can.

				MessageWithId toSend;
				synchronized (this) {
					if (activeMessage == null && queuedMessages.size() > 0) {
						activeMessage = queuedMessages.poll();
						toSend = activeMessage;
					} else {
						toSend = null;
					}
				}
				if (toSend != null) {
					if (!send(activeMessage)) {
						isFailed = true;
						isRunning = false;
						break;
					}
				}

				// Try to read something.
				if (!incomingObjects.isEmpty()) {
					// Record the last time we heard form the client.
					lastHeard = currentTime;
					lastPing = currentTime;
					processReply();
				}

				synchronized (this) {
					try {
						this.wait(1000);
					} catch (final InterruptedException e) {
						// Ignore.
					}
				}

			} catch (final Exception e) {
				LOG.error("Entering failed state on a local exception: %s", e);
				isRunning = false;
				isFailed = true;
				break;
			}
		}
	}

	@Override
	public boolean setupCommand(Map<String, String> initMap) {
		final MessageWithId message = new MessageWithId(
				messageIdGenerator.getAndIncrement(),
				DistributionConstants.INIT);
		for (final Entry<String, String> entry : initMap.entrySet()) {
			message.put(entry.getKey(), entry.getValue());
		}
		qsend(message);
		return true;
	}

	@Override
	public boolean setupEnviroment(AbstractEnvironment enviroment) {
		final MessageWithId message = new MessageWithId(
				messageIdGenerator.getAndIncrement(),
				DistributionConstants.ENIVROMENT, enviroment);
		qsend(message);
		return true;
	}

	public void terminate() {
		synchronized (this) {
			LOG.info("Sending shutdown message to %s", name);
			queuedMessages.clear();
			qsend(new MessageWithId(messageIdGenerator.getAndIncrement(),
					DistributionConstants.SHUTDOWN));
			readingThread.interrupt();
			isRunning = false;
		}
	}

	@Override
	public boolean updateEnviroment(List<SerializedEnvironmentConfig> updates) {
		final MessageWithId message = new MessageWithId(
				messageIdGenerator.getAndIncrement(),
				DistributionConstants.MODIFY_ENVIROMENT, updates);
		qsend(message);
		return true;
	}

	private void processReply() throws Exception {
		final Object object = incomingObjects.poll();

		if (!(object instanceof Message)) {
			LOG.error("Invalid object received: %s", object.getClass());
			return;
		}

		final Message message = (Message) object;
		LOG.debug("Received command from client: %s", message.getCommand());

		final String free = message.get(DistributionConstants._free);
		synchronized (this) {
			if (freeSpots == -1) {
				this.freeSpots = Integer.parseInt(free);
			}
		}
		final String command = message.getCommand();

		if (DistributionConstants.AK.equals(command)) {
			if (message.get(DistributionConstants._ackid) != null) {
				final long confirmedId = Long
						.parseLong(message.get(DistributionConstants._ackid));
				final long activeId = activeMessage.getMessageId();
				synchronized (this) {
					if (confirmedId == activeId) {
						// We are done with this command.
						activeMessage = null;
					} else {
						LOG.error("active command not lining up!");
					}
				}
			}
		} else if (DistributionConstants.RETURN.equals(command)) {
			processResultMessage(message);

			synchronized (this) {
				freeSpots++;
			}
			taskReturned.incrementAndGet();
		} else if (command.equals(DistributionConstants.SUMMARY)) {
			LOG.error("Not properly implemented yet");
			// currentSummary = message.get();
		} else if (command.equals(DistributionConstants.ERROR)) {
			LOG.info("Error message: %s",
					message.get(DistributionConstants._message));
			if (message.get(DistributionConstants._stacktrace) != null) {
				LOG.info(message.get(DistributionConstants._stacktrace));
			}
		}
	}

	private void processResultMessage(Message message) {
		final long taskId = message.getResult().getTaskId();
		final Task task = activeTasks.get(taskId);
		final long execTime = System.currentTimeMillis()
				- activeTaskStartTime.get(taskId);
		executionTimeDecayingAverage = (executionTimeDecayingAverage + execTime)
				/ 2.0;
		final TaskResult result = message.getResult();
		synchronized (this) {
			if (activeTasks.remove(taskId) == null
					|| activeTaskStartTime.remove(taskId) == null) {
				LOG.error("BUG: Unknown returned task ID");
			}
		}

		// Report the task back to the global manager, which manages task
		// accounting.
		globalManager.reportResult(this, task, result);
	}

	private void qsend(MessageWithId message) {
		synchronized (this) {
			queuedMessages.add(message);
			this.notifyAll();
		}
	}

	private boolean send(Message message) {
		synchronized (outputStream) {
			try {
				outputStream.writeObject(message);
				outputStream.flush();
				outputStream.reset();
				return true;
			} catch (final IOException e) {
				LOG.error("Failed to send message: %s", e);
				return false;
			} catch (final RuntimeException e) {
				LOG.error("Failed to send message: %s", message.getCommand());
				LOG.error("Failed to send message: %s", e);
				throw e;
			}
		}
	}

	private class ObjectReadingThread extends Thread {

		@Override
		public void run() {
			while (isRunning) {
				try {
					final Object object = inputStream.readObject();
					incomingObjects.offer(object);
					synchronized (EnslavedRemoteManager.this) {
						EnslavedRemoteManager.this.notifyAll();
					}
				} catch (final ClassNotFoundException e) {
					// Ignore.
					LOG.error("Failed to read object: %s", e);
				} catch (final IOException e) {
					// Treat an IOException as if this worker is dead.
					LOG.error("Reading thread exception: %s", e);
					isFailed = true;
					isRunning = false;
				}
			}
		}
	}

}
