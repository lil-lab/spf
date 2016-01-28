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
 * Runtime environment for task execution.
 *
 * @author Yoav Artzi
 */
public abstract class AbstractEnvironment implements Serializable {

	public static final ILogger LOG = LoggerFactory
			.create(AbstractEnvironment.class);

	private static final long	serialVersionUID	= 1L;
	private ChunkedByteArray	serializedCache		= null;

	/**
	 * Update the current configuration.
	 */
	public final void update(EnvironmentConfig<?> update) {
		synchronized (this) {
			// Reset the environment serialization cache.
			serializedCache = null;
			applyUpdate(update);
		}
	}

	/**
	 * Apply the update. Implements the application-specific logic of the
	 * update.
	 */
	protected abstract void applyUpdate(EnvironmentConfig<?> update);

	ChunkedByteArray serialize() throws IOException {
		synchronized (this) {
			if (serializedCache != null) {
				return serializedCache;
			}

			// Serialize.
			final ChunkedByteArray array = new ChunkedByteArray();
			try (final ChunkedByteArrayOutputStream baos = new ChunkedByteArrayOutputStream(
					array);
					final ObjectOutputStream oos = new ObjectOutputStream(
							baos)) {
				oos.writeObject(this);
			}
			array.lock();
			serializedCache = array;

			LOG.info("Serialized environment: %s (%.2fKB)", getClass(),
					serializedCache.size() / 1024.0);

			return serializedCache;

		}
	}
}
