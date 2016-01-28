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
 * Iterable object to iterate over all subsets of a given size of a given
 * collection. All returned subsets will have the same size. The maximum value
 * for the subset size is 63.
 *
 * Bit tricks from:
 * https://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetNaive
 *
 * @author Yoav Artzi
 *
 * @param <T>
 *            Type of stored objects.
 */
public class PowerSetWithFixedSize<T> implements Iterable<List<T>> {
	private final T[]	elts;
	private final int	hashCode;
	private final long	maskUpperBound;
	private final int	subsetSize;

	@SuppressWarnings("unchecked")
	public PowerSetWithFixedSize(Collection<T> source, int subsetSize) {
		this((T[]) source.toArray(), subsetSize);
	}

	public PowerSetWithFixedSize(T[] source, int subsetSize) {
		assert subsetSize < 64 : "Subset max size must be below 64";
		final long n = source.length;
		this.elts = source;
		this.maskUpperBound = 1L << n;
		this.subsetSize = subsetSize;
		this.hashCode = subsetSize * (1 << n - 1) * Arrays.hashCode(this.elts);
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
			long i = (1 << subsetSize) - 1;

			@Override
			public boolean hasNext() {
				return i < maskUpperBound;
			}

			@Override
			public List<T> next() {
				final long currentMask = i++;

				// Count the number of bits set. It must equal the subsetSize,
				// otherwise, increment to the next mask.
				while (i <= maskUpperBound) {
					// Temporary long to manipulate when counting bits.
					long bits = i;
					// Accumulates the total number of bits set.
					int count = 0;
					for (; count <= subsetSize && bits != 0; count++) {
						// Clear the least significant bit set.
						bits &= bits - 1;
					}
					if (count == subsetSize) {
						break;
					}
					i++;
				}

				return new BitMaskList<T>(currentMask, elts);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
