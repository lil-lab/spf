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
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.cornell.cs.nlp.utils.buffers.chunked.ChunkedByteArray;
import edu.cornell.cs.nlp.utils.buffers.chunked.ChunkedByteArrayOutputStream;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * A serialized {@link EnvironmentConfig}. Used to save on serialization
 * operations.
 *
 * @author Yoav Artzi
 */
public class SerializedEnvironmentConfig implements Serializable {

	public static final ILogger		LOG					= LoggerFactory
																.create(SerializedEnvironmentConfig.class);

	private static final long		serialVersionUID	= 3705908855890143570L;

	private final int				id;

	private final String			key;

	private final ChunkedByteArray	serializedObject;

	public SerializedEnvironmentConfig(EnvironmentConfig<?> config, int id)
			throws IOException {
		this.id = id;
		this.key = config.getKey();
		final ChunkedByteArray array = new ChunkedByteArray();
		try (final ChunkedByteArrayOutputStream baos = new ChunkedByteArrayOutputStream(
				array);
				final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(config.getValue());
		}
		array.lock();
		this.serializedObject = array;
		LOG.info("Serialized environment update [id=%d]: %s (%.2fKB)", id,
				config.getKey(), serializedObject.size() / 1024.0);
	}

	public int getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	public ChunkedByteArray getSerializedObject() {
		return serializedObject;
	}
}
