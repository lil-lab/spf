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

import java.util.List;

/**
 * A message with a specific ID.
 *
 * @author Yoav Artzi
 */
public class MessageWithId extends Message {

	private static final long	serialVersionUID	= 2339316063529841985L;
	private final long			messageId;

	public MessageWithId(long messageId, String command) {
		super(command);
		this.messageId = messageId;
	}

	public MessageWithId(long messageId, String command,
			AbstractEnvironment environment) {
		super(command, environment);
		this.messageId = messageId;
	}

	public MessageWithId(long messageId, String command,
			List<SerializedEnvironmentConfig> updates) {
		super(command, updates);
		this.messageId = messageId;
	}

	public MessageWithId(long messageId, String command, Task task) {
		super(command, task);
		this.messageId = messageId;
	}

	public MessageWithId(long messageId, String command, TaskResult result) {
		super(command, result);
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}

}
