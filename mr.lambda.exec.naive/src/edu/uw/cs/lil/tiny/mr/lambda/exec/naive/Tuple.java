/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
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
 ******************************************************************************/
package edu.uw.cs.lil.tiny.mr.lambda.exec.naive;

import java.util.Arrays;

import edu.uw.cs.utils.collections.ListUtils;

public class Tuple {
	private final Object[]	keys;
	private final Object	value;
	
	public Tuple(Object[] keys, Object value) {
		this.keys = keys;
		this.value = value;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Tuple other = (Tuple) obj;
		if (!Arrays.equals(keys, other.keys)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}
	
	public Object get(int i) {
		return keys[i];
	}
	
	public Object getValue() {
		return value;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keys);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	public int numKeys() {
		return keys.length;
	}
	
	public Tuple subTuple(int start, int end) {
		return new Tuple(Arrays.copyOfRange(keys, start, end), value);
	}
	
	@Override
	public String toString() {
		return new StringBuilder(ListUtils.join(Arrays.asList(keys), ","))
				.append(" -> ").append(value).toString();
	}
}
