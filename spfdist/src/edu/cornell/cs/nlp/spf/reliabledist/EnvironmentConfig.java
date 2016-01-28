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
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.cornell.cs.nlp.utils.buffers.chunked.ChunkedByteArrayInputStream;

/**
 * A single {@link AbstractEnvironment} configuration directive.
 *
 * @author Yoav Artzi
 */
public class EnvironmentConfig<VALUE> implements Serializable {
	private static final long serialVersionUID = 8548665049542396785L;

	private final String key;

	private final VALUE value;

	@SuppressWarnings("unchecked")
	public EnvironmentConfig(SerializedEnvironmentConfig serialized)
			throws IOException, ClassNotFoundException {
		this.key = serialized.getKey();
		final ChunkedByteArrayInputStream bis = new ChunkedByteArrayInputStream(
				serialized.getSerializedObject());
		try (ObjectInput in = new ObjectInputStream(bis)) {
			this.value = (VALUE) in.readObject();
		}
	}

	public EnvironmentConfig(String key, VALUE value) {
		assert key != null;
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public VALUE getValue() {
		return value;
	}
}
