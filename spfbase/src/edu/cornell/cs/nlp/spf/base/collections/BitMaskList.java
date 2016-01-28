package edu.cornell.cs.nlp.spf.base.collections;

import java.util.AbstractList;
import java.util.Iterator;

/**
 * List with an underlying bitmask to an array.
 *
 * @author Yoav Artzi
 *
 * @param <T>
 *            Type of objects.
 */
public class BitMaskList<T> extends AbstractList<T> {
	private final T[]	elts;
	private final long	mask;

	BitMaskList(long mask, T[] elts) {
		this.mask = mask;
		this.elts = elts;
	}

	@Override
	public T get(int arg0) {
		int counter = arg0;
		long currentMask = mask;
		for (int i = 0; currentMask > 0; currentMask >>>= 1, ++i) {
			if ((currentMask & 1L) != 0) {
				if (counter == 0) {
					return elts[i];
				}
				--counter;
			}
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		long currentMask = mask;
		for (int i = 0; currentMask > 0; currentMask >>>= 1, ++i) {
			if ((currentMask & 1) == 1) {
				hashCode += elts[i].hashCode();
			}
		}

		return hashCode;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int		i		= 0;
			long	mask	= BitMaskList.this.mask;

			@Override
			public boolean hasNext() {
				return mask != 0;
			}

			@Override
			public T next() {
				while ((mask & 1) == 0) {
					++i;
					mask >>>= 1;
				}

				final T next = elts[i];

				++i;
				mask >>>= 1;

				return next;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size() {
		int size = 0;

		for (long mask2 = this.mask; mask2 > 0; mask2 >>>= 1) {
			size += mask2 & 1;
		}

		return size;
	}
}
