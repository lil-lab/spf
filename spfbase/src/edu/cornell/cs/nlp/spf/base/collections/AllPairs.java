package edu.cornell.cs.nlp.spf.base.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Return all pairs from a given collection. Although the pairs are returned in
 * a {@link List} interface, ordering is not accounted for and either (x1, x2)
 * or (x2, x1) will be returned, but not both.
 *
 * @author Yoav Artzi
 *
 * @param <T>
 *            Type of stored objects.
 *
 */
public class AllPairs<T> implements Iterable<List<T>> {

	private final T[]	collection;
	private final long	firstMaskMax;
	private final long	secondMaskMax;

	@SuppressWarnings("unchecked")
	public AllPairs(Collection<T> source) {
		this((T[]) source.toArray());
	}

	public AllPairs(T[] collection) {
		assert collection.length <= 64 : "AllPairs supports collecitons up to size 64";
		this.collection = collection;
		final long n = collection.length;
		this.firstMaskMax = 1L << n - 2;
		this.secondMaskMax = 1L << n - 1;

	}

	@Override
	public Iterator<List<T>> iterator() {
		return new Iterator<List<T>>() {

			long	firstMask	= 1L;
			long	secondMask	= 1L << 1;

			@Override
			public boolean hasNext() {
				return secondMask <= secondMaskMax;
			}

			@Override
			public List<T> next() {
				// The current mask is a combination of the first and second
				// masks.
				final long currentMask = firstMask | secondMask;

				// Forward the two mask.
				secondMask <<= 1;
				if (secondMask > secondMaskMax && firstMask < firstMaskMax) {
					firstMask <<= 1;
					secondMask = firstMask << 1;
				}

				return new BitMaskList<T>(currentMask, collection);
			}
		};
	}

}
