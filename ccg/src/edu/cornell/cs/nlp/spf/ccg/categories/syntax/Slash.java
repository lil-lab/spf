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
package edu.cornell.cs.nlp.spf.ccg.categories.syntax;

import java.io.ObjectStreamException;
import java.io.Serializable;

public class Slash implements Serializable {
	public static final Slash	BACKWARD			= new Slash('\\');
	public static final Slash	FORWARD				= new Slash('/');
	public static final Slash	VERTICAL			= new Slash('|');
	private static final long	serialVersionUID	= -3344487277034825666L;

	private final char			c;

	private Slash(char c) {
		this.c = c;
	}

	public static Slash getSlash(char c) {
		if (c == BACKWARD.getChar()) {
			return BACKWARD;
		} else if (c == FORWARD.getChar()) {
			return FORWARD;
		} else if (c == VERTICAL.getChar()) {
			return VERTICAL;
		} else {
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	public char getChar() {
		return c;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + c;
		return result;
	}

	@Override
	public String toString() {
		return String.valueOf(c);
	}

	/**
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		return getSlash(c);
	}
}
