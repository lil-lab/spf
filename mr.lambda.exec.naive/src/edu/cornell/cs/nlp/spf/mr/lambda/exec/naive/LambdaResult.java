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
package edu.cornell.cs.nlp.spf.mr.lambda.exec.naive;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LambdaResult implements ILambdaResult {
	
	private final int			numKeys;
	final private Set<Tuple>	tuples	= new HashSet<Tuple>();
	
	public LambdaResult(int numKeys) {
		this.numKeys = numKeys;
	}
	
	public boolean addTuple(Tuple tuple) {
		if (tuple.numKeys() != numKeys) {
			throw new IllegalArgumentException("Invalid number of keys "
					+ tuple.numKeys() + ", expected " + numKeys);
		}
		return tuples.add(tuple);
	}
	
	public int getNumKeys() {
		return numKeys;
	}
	
	@Override
	public boolean isEmpty() {
		return tuples.isEmpty();
	}
	
	@Override
	public Iterator<Tuple> iterator() {
		return Collections.unmodifiableSet(tuples).iterator();
	}
	
	@Override
	public int size() {
		return tuples.size();
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		final Iterator<Tuple> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			sb.append(iterator.next().toString());
			if (iterator.hasNext()) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}
}
