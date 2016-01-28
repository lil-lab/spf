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
package edu.cornell.cs.nlp.spf.base.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Iterable object to iterate over all subsets of a given collection.
 *
 * @author Yoav Artzi
 *
 * @param <T>
 *            Type of stored objects.
 */
public class PowerSet<T> implements Iterable<List<T>> {
	private final T[]	elts;
	private final int	hashCode;
	private final long	maxMask;

	@SuppressWarnings("unchecked")
	public PowerSet(Collection<T> source) {
		this((T[]) source.toArray());
	}

	public PowerSet(T[] source) {
		final long n = source.length;
		this.elts = source;
		this.maxMask = 1L << n;
		this.hashCode = (1 << n - 1) * Arrays.hashCode(this.elts);
		if (n > 62) {
			throw new IllegalArgumentException(
					"PowerSet supports input of up to length 63");
		}
	}

	@Override
	public boolean equals(Object e) {
		return false;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public Iterator<List<T>> iterator() {
		return new Iterator<List<T>>() {
			long i = 0;

			@Override
			public boolean hasNext() {
				return i < maxMask;
			}

			@Override
			public List<T> next() {
				return new BitMaskList<T>(i++, elts);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public long size() {
		return maxMask;
	}

}
