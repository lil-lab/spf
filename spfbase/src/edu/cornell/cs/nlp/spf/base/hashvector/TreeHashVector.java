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
package edu.cornell.cs.nlp.spf.base.hashvector;

import java.util.Iterator;
import java.util.Map.Entry;

import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.TreeMap;

/**
 * Sparse vector based on a {@link TreeMap}. This implementation is less
 * efficient and has higher memory consumption than {@link TroveHashVector}.
 * However it does maintain a predictable order of iteration, which is important
 * for stability between runs.
 *
 * @author Yoav Artzi
 */
class TreeHashVector implements IHashVector {
	public static final IHashVectorImmutable	EMPTY				= new TreeHashVector();

	private static final long					serialVersionUID	= -7116667847178978870L;

	protected final TreeMap<KeyArgs, Double>	values;

	TreeHashVector() {
		this.values = new TreeMap<KeyArgs, Double>();
	}

	@SuppressWarnings("unchecked")
	TreeHashVector(IHashVectorImmutable other) {
		if (other instanceof TreeHashVector) {
			this.values = (TreeMap<KeyArgs, Double>) ((TreeHashVector) other).values
					.clone();
		} else {
			this.values = new TreeMap<KeyArgs, Double>();
			for (final Pair<KeyArgs, Double> o : other) {
				values.put(o.first(), o.second());
			}
		}
	}

	@Override
	public void add(final double num) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() + num);
		}
	}

	@Override
	public void add(KeyArgs key, double value) {
		values.put(key, value
				+ (values.containsKey(key) ? values.get(key) : ZERO_VALUE));
	}

	@Override
	public void add(String arg1, double value) {
		final KeyArgs key = new KeyArgs(arg1);
		values.put(key, value
				+ (values.containsKey(key) ? values.get(key) : ZERO_VALUE));
	}

	@Override
	public void add(String arg1, String arg2, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2);
		values.put(key, value
				+ (values.containsKey(key) ? values.get(key) : ZERO_VALUE));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3);
		values.put(key, value
				+ (values.containsKey(key) ? values.get(key) : ZERO_VALUE));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4);
		values.put(key, value
				+ (values.containsKey(key) ? values.get(key) : ZERO_VALUE));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			String arg5, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4, arg5);
		values.put(key, value
				+ (values.containsKey(key) ? values.get(key) : ZERO_VALUE));
	}

	@Override
	public TreeHashVector addTimes(final double times,
			IHashVectorImmutable other) {
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			final TreeHashVector ret = new TreeHashVector(this);
			for (final Entry<KeyArgs, Double> entry : p.values.entrySet()) {
				ret.add(entry.getKey(), entry.getValue() * times);
			}
			return ret;
		} else {
			return addTimes(times, new TreeHashVector(other));
		}
	}

	@Override
	public void addTimesInto(final double times, IHashVector other) {
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;

			for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
				final double value = times * entry.getValue();
				final KeyArgs key = entry.getKey();
				if (p.values.containsKey(key)) {
					p.values.put(key, value + p.values.get(key));
				} else {
					p.values.put(key, value + ZERO_VALUE);
				}
			}
		} else {
			// Less efficient when we can't access the underlying map.
			for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
				other.add(entry.getKey(), times * entry.getValue());
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void applyFunction(ValueFunction function) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(function.apply(entry.getValue()));
		}
	}

	/** {@inheritDoc} */
	@Override
	public void clear() {
		values.clear();
	}

	@Override
	public boolean contains(KeyArgs key) {
		return values.containsKey(key);
	}

	@Override
	public boolean contains(String arg1) {
		return values.containsKey(new KeyArgs(arg1));
	}

	@Override
	public boolean contains(String arg1, String arg2) {
		return values.containsKey(new KeyArgs(arg1, arg2));
	}

	@Override
	public boolean contains(String arg1, String arg2, String arg3) {
		return values.containsKey(new KeyArgs(arg1, arg2, arg3));
	}

	@Override
	public boolean contains(String arg1, String arg2, String arg3, String arg4) {
		return values.containsKey(new KeyArgs(arg1, arg2, arg3, arg4));
	}

	@Override
	public boolean contains(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		return values.containsKey(new KeyArgs(arg1, arg2, arg3, arg4, arg5));
	}

	/** {@inheritDoc} */
	@Override
	public void divideBy(final double d) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() / d);
		}
	}

	@Override
	public double dotProduct(IHashVectorImmutable other) {
		if (size() <= other.size()) {
			if (other instanceof TreeHashVector) {
				final TreeHashVector thv = (TreeHashVector) other;
				double sum = 0.0;
				for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
					if (thv.values.containsKey(entry.getKey())) {
						sum += entry.getValue()
								* thv.values.get(entry.getKey());
					}
				}
				return sum;
			} else {
				return dotProduct(new TreeHashVector(other));
			}
		} else {
			return other.dotProduct(this);
		}
	}

	@Override
	public void dropNoise() {
		final Iterator<Entry<KeyArgs, Double>> iterator = values.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			if (Math.abs(iterator.next().getValue()) < NOISE) {
				iterator.remove();
			}
		}
	}

	@Override
	public void dropZeros() {
		final Iterator<Entry<KeyArgs, Double>> iterator = values.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			if (Math.abs(iterator.next().getValue()) == ZERO_VALUE) {
				iterator.remove();
			}
		}
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
		final TreeHashVector other = (TreeHashVector) obj;
		if (values == null) {
			if (other.values != null) {
				return false;
			}
		} else if (!values.equals(other.values)) {
			return false;
		}
		return true;
	}

	@Override
	public double get(KeyArgs key) {
		final Double value = values.get(key);
		return value == null ? IHashVector.ZERO_VALUE : value;
	}

	@Override
	public double get(KeyArgs key, double defaultReturn) {
		final Double value = values.get(key);
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1) {
		final Double value = values.get(new KeyArgs(arg1));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}

	@Override
	public double get(String arg1, double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2) {
		final Double value = values.get(new KeyArgs(arg1, arg2));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}

	@Override
	public double get(String arg1, String arg2, double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3,
			double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4,
				arg5));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5, double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4,
				arg5));
		return value == null ? defaultReturn : value;
	}

	@Override
	public IHashVector getAll(KeyArgs partialKey) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (partialKey.contains(key)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4, final String arg5) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)
					&& arg5.equals(key.arg5)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (values == null ? 0 : values.hashCode());
		return result;
	}

	@Override
	public boolean isBad() {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final Double value = entry.getValue();
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInit() {
		return false;
	}

	@Override
	public void iterate(EntryFunction function) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			function.apply(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Iterator<Pair<KeyArgs, Double>> iterator() {
		return new Iterator<Pair<KeyArgs, Double>>() {
			private final Iterator<Entry<KeyArgs, Double>>	innerIterator	= values.entrySet()
																					.iterator();

			@Override
			public boolean hasNext() {
				return innerIterator.hasNext();
			}

			@Override
			public Pair<KeyArgs, Double> next() {
				if (innerIterator.hasNext()) {
					final Entry<KeyArgs, Double> next = innerIterator.next();
					return Pair.of(next.getKey(), next.getValue());
				} else {
					return null;
				}
			}

			@Override
			public void remove() {
				innerIterator.remove();
			}
		};
	}

	@Override
	public double l1Norm() {
		double sum = 0.0;
		for (final double value : values.values()) {
			sum += Math.abs(value);
		}
		return sum;
	}

	@Override
	public void multiplyBy(double value) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() * value);
		}
	}

	@Override
	public TreeHashVector pairWiseProduct(IHashVectorImmutable other) {
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			if (size() <= other.size()) {
				final TreeHashVector ret = new TreeHashVector(this);
				for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
					final KeyArgs key = entry.getKey();
					if (p.values.containsKey(key)) {
						ret.values.put(key,
								entry.getValue() * p.values.get(key));
					}
				}
				return ret;
			} else {
				return p.pairWiseProduct(this);
			}
		} else {
			return pairWiseProduct(new TreeHashVector(other));
		}
	}

	@Override
	public String printValues(IHashVectorImmutable other) {
		final StringBuilder ret = new StringBuilder();
		ret.append("{");
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			for (final Iterator<Entry<KeyArgs, Double>> iterator = p.values
					.entrySet().iterator(); iterator.hasNext();) {
				final Entry<KeyArgs, Double> entry = iterator.next();
				if (values.containsKey(entry.getKey())) {
					ret.append(entry.getKey()
							+ "="
							+ String.format("%.3f", values.get(entry.getKey()))
							+ "("
							+ String.format("%.3f",
									p.values.get(entry.getKey())) + ")");
				} else {
					ret.append(entry.getKey()
							+ "="
							+ ZERO_VALUE
							+ "("
							+ String.format("%.3f",
									p.values.get(entry.getKey())) + ")");
				}
				if (iterator.hasNext()) {
					ret.append(",");
				}
			}
			ret.append("}");
			return ret.toString();
		} else {
			return printValues(new TreeHashVector(other));
		}
	}

	@Override
	public void set(KeyArgs key, double value) {
		values.put(key, value);
	}

	@Override
	public void set(String arg1, double value) {
		values.put(new KeyArgs(arg1), value);
	}

	@Override
	public void set(String arg1, String arg2, double value) {
		values.put(new KeyArgs(arg1, arg2), value);
	}

	@Override
	public void set(String arg1, String arg2, String arg3, double value) {
		values.put(new KeyArgs(arg1, arg2, arg3), value);
	}

	@Override
	public void set(String arg1, String arg2, String arg3, String arg4,
			double value) {
		values.put(new KeyArgs(arg1, arg2, arg3, arg4), value);
	}

	@Override
	public void set(String arg1, String arg2, String arg3, String arg4,
			String arg5, double value) {
		values.put(new KeyArgs(arg1, arg2, arg3, arg4, arg5), value);
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append("{");
		final Iterator<Entry<KeyArgs, Double>> iterator = values.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			final Entry<KeyArgs, Double> next = iterator.next();
			ret.append(next.getKey());
			ret.append("=");
			ret.append(String.format("%.3f", next.getValue()));
			if (iterator.hasNext()) {
				ret.append(", ");
			}
		}
		ret.append("}");
		return ret.toString();
	}

	@Override
	public boolean valuesInRange(final double min, final double max) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final Double value = entry.getValue();
			if (value < min || value > max) {
				return false;
			}
		}
		return true;
	}
}
