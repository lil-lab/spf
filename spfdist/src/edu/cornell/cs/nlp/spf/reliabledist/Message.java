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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Message without an ID, but with all the actual content.
 *
 * @author Yoav Artzi
 */
public class Message implements Serializable {
	private static final long						serialVersionUID	= -4152336158407694126L;
	private final String							command;
	private final AbstractEnvironment				environment;
	private final List<SerializedEnvironmentConfig>	environmentUpdates;
	private final TaskResult						result;
	private final Task								task;
	private final Map<String, String>				values				= new HashMap<String, String>();

	public Message(String command) {
		this.command = command;
		this.task = null;
		this.result = null;
		this.environment = null;
		this.environmentUpdates = Collections.emptyList();
	}

	public Message(String command, AbstractEnvironment environment) {
		this.command = command;
		this.task = null;
		this.environment = environment;
		this.result = null;
		this.environmentUpdates = Collections.emptyList();
	}

	public Message(String command, List<SerializedEnvironmentConfig> updates) {
		this.command = command;
		this.environmentUpdates = updates;
		this.result = null;
		this.environment = null;
		this.task = null;
	}

	public Message(String command, Task task) {
		this.command = command;
		this.task = task;
		this.environment = null;
		this.result = null;
		this.environmentUpdates = Collections.emptyList();
	}

	public Message(String command, TaskResult result) {
		this.command = command;
		this.task = null;
		this.environment = null;
		this.result = result;
		this.environmentUpdates = Collections.emptyList();
	}

	public Map<String, String> get() {
		return Collections.unmodifiableMap(values);
	}

	public String get(String key) {
		return values.get(key);
	}

	public String getCommand() {
		return command;
	}

	public AbstractEnvironment getEnvironment() {
		return environment;
	}

	public List<SerializedEnvironmentConfig> getEnvUpdates() {
		return environmentUpdates;
	}

	public TaskResult getResult() {
		return result;
	}

	public Task getTask() {
		return task;
	}

	public void put(String key, String value) {
		values.put(key, value);
	}

}
