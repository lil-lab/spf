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

import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.Iterator;
import java.util.Map.Entry;

import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.TreeMap;

/**
 * Sparse vector based on a {@link TreeMap}. This implementation is less(?)
 * efficient and has higher memory consumption than {@link TroveHashVector}.
 * However it does maintain a predictable order of iteration, which is important
 * for stability between runs.
 *
 * @author Yoav Artzi
 */
class FastTreeHashVector implements IHashVector {
	public static final IHashVectorImmutable	EMPTY				= new FastTreeHashVector();
	private static final long					serialVersionUID	= 4294341073950816236L;

	private final Object2DoubleMap<KeyArgs>		values;

	FastTreeHashVector() {
		this.values = new Object2DoubleAVLTreeMap<KeyArgs>();
		this.values.defaultReturnValue(ZERO_VALUE);
	}

	FastTreeHashVector(IHashVectorImmutable other) {
		if (other instanceof FastTreeHashVector) {
			this.values = new Object2DoubleAVLTreeMap<KeyArgs>(
					((FastTreeHashVector) other).values);
		} else {
			this.values = new Object2DoubleAVLTreeMap<KeyArgs>();
			for (final Pair<KeyArgs, Double> o : other) {
				values.put(o.first(), o.second());
			}
		}
		this.values.defaultReturnValue(ZERO_VALUE);
	}

	@Override
	public void add(final double num) {
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			entry.setValue(entry.getDoubleValue() + num);
		}
	}

	@Override
	public void add(KeyArgs key, double value) {
		values.put(key, value + values.getDouble(key));
	}

	@Override
	public void add(String arg1, double value) {
		final KeyArgs key = new KeyArgs(arg1);
		values.put(key, value + values.getDouble(key));
	}

	@Override
	public void add(String arg1, String arg2, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2);
		values.put(key, value + values.getDouble(key));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3);
		values.put(key, value + values.getDouble(key));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4);
		values.put(key, value + values.getDouble(key));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			String arg5, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4, arg5);
		values.put(key, value + values.getDouble(key));
	}

	@Override
	public FastTreeHashVector addTimes(final double times,
			IHashVectorImmutable other) {
		if (other instanceof FastTreeHashVector) {
			final FastTreeHashVector p = (FastTreeHashVector) other;
			final FastTreeHashVector ret = new FastTreeHashVector(this);
			for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : p.values
					.object2DoubleEntrySet()) {
				final KeyArgs key = entry.getKey();
				Double value = entry.getDoubleValue() * times;
				if (values.containsKey(key)) {
					value += values.getDouble(key);
				}
				ret.values.put(key, value);
			}
			return ret;
		} else {
			return addTimes(times, new FastTreeHashVector(other));
		}
	}

	@Override
	public void addTimesInto(final double times, IHashVector other) {
		if (other instanceof FastTreeHashVector) {
			final FastTreeHashVector p = (FastTreeHashVector) other;

			for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
					.object2DoubleEntrySet()) {
				final double value = times * entry.getDoubleValue();
				final KeyArgs key = entry.getKey();
				if (p.values.containsKey(key)) {
					p.values.put(key, value + p.values.getDouble(key));
				} else {
					p.values.put(key, value + ZERO_VALUE);
				}
			}
		} else {
			// Less efficient when we can't access the underlying map.
			for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
				final double value = times * entry.getValue();
				final KeyArgs key = entry.getKey();
				other.set(key, value + other.get(key));
			}
		}
	}

	@Override
	public void applyFunction(ValueFunction function) {
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			entry.setValue(function.apply(entry.getDoubleValue()));
		}
	}

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

	@Override
	public void divideBy(final double d) {
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			entry.setValue(entry.getDoubleValue() / d);
		}
	}

	@Override
	public double dotProduct(IHashVectorImmutable other) {
		if (size() <= other.size()) {
			if (other instanceof FastTreeHashVector) {
				final FastTreeHashVector thv = (FastTreeHashVector) other;
				double sum = 0.0;
				for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
						.object2DoubleEntrySet()) {
					if (thv.values.containsKey(entry.getKey())) {
						sum += entry.getDoubleValue()
								* thv.values.get(entry.getKey());
					}
				}
				return sum;
			} else {
				return dotProduct(new FastTreeHashVector(other));
			}
		} else {
			return other.dotProduct(this);
		}
	}

	@Override
	public void dropNoise() {
		final Iterator<it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs>> iterator = values
				.object2DoubleEntrySet().iterator();
		while (iterator.hasNext()) {
			if (Math.abs(iterator.next().getDoubleValue()) < NOISE) {
				iterator.remove();
			}
		}
	}

	@Override
	public void dropZeros() {
		final Iterator<it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs>> iterator = values
				.object2DoubleEntrySet().iterator();
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
		final FastTreeHashVector other = (FastTreeHashVector) obj;
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
		return values.getDouble(key);
	}

	@Override
	public double get(KeyArgs key, double defaultReturn) {
		if (values.containsKey(key)) {
			return values.getDouble(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1) {
		return values.getDouble(new KeyArgs(arg1));
	}

	@Override
	public double get(String arg1, double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1);
		if (values.containsKey(key)) {
			return values.getDouble(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2) {
		return values.getDouble(new KeyArgs(arg1, arg2));
	}

	@Override
	public double get(String arg1, String arg2, double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2);
		if (values.containsKey(key)) {
			return values.getDouble(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2, String arg3) {
		return values.getDouble(new KeyArgs(arg1, arg2, arg3));
	}

	@Override
	public double get(String arg1, String arg2, String arg3,
			double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3);
		if (values.containsKey(key)) {
			return values.getDouble(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4) {
		return values.getDouble(new KeyArgs(arg1, arg2, arg3, arg4));
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4);
		if (values.containsKey(key)) {
			return values.getDouble(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		return values.getDouble(new KeyArgs(arg1, arg2, arg3, arg4, arg5));
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5, double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4, arg5);
		if (values.containsKey(key)) {
			return values.getDouble(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public IHashVector getAll(KeyArgs partialKey) {
		final FastTreeHashVector result = new FastTreeHashVector();
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final KeyArgs key = entry.getKey();
			if (partialKey.contains(key)) {
				result.values.put(key, entry.getDoubleValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1) {
		final FastTreeHashVector result = new FastTreeHashVector();
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1)) {
				result.values.put(key, entry.getDoubleValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2) {
		final FastTreeHashVector result = new FastTreeHashVector();
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)) {
				result.values.put(key, entry.getDoubleValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3) {
		final FastTreeHashVector result = new FastTreeHashVector();
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3)) {
				result.values.put(key, entry.getDoubleValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4) {
		final FastTreeHashVector result = new FastTreeHashVector();
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)) {
				result.values.put(key, entry.getDoubleValue());
			}
		}
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4, final String arg5) {
		final FastTreeHashVector result = new FastTreeHashVector();
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)
					&& arg5.equals(key.arg5)) {
				result.values.put(key, entry.getDoubleValue());
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
		final DoubleIterator iterator = values.values().iterator();
		while (iterator.hasNext()) {
			final double value = iterator.nextDouble();
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
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			function.apply(entry.getKey(), entry.getDoubleValue());
		}
	}

	@Override
	public Iterator<Pair<KeyArgs, Double>> iterator() {
		return new Iterator<Pair<KeyArgs, Double>>() {
			private final Iterator<it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs>>	innerIterator	= values.object2DoubleEntrySet()
																															.iterator();

			@Override
			public boolean hasNext() {
				return innerIterator.hasNext();
			}

			@Override
			public Pair<KeyArgs, Double> next() {
				if (innerIterator.hasNext()) {
					final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> next = innerIterator
							.next();
					return Pair.of(next.getKey(), next.getDoubleValue());
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
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			entry.setValue(entry.getDoubleValue() * value);
		}
	}

	@Override
	public FastTreeHashVector pairWiseProduct(IHashVectorImmutable other) {
		if (other instanceof FastTreeHashVector) {
			final FastTreeHashVector p = (FastTreeHashVector) other;
			if (size() <= other.size()) {
				final FastTreeHashVector ret = new FastTreeHashVector(this);
				for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
						.object2DoubleEntrySet()) {
					final KeyArgs key = entry.getKey();
					if (p.values.containsKey(key)) {
						ret.values.put(key,
								entry.getDoubleValue() * p.values.get(key));
					}
				}
				return ret;
			} else {
				return p.pairWiseProduct(this);
			}
		} else {
			return pairWiseProduct(new FastTreeHashVector(other));
		}
	}

	@Override
	public String printValues(IHashVectorImmutable other) {
		final StringBuilder ret = new StringBuilder();
		ret.append("{");
		if (other instanceof FastTreeHashVector) {
			final FastTreeHashVector p = (FastTreeHashVector) other;
			for (final Iterator<it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs>> iterator = p.values
					.object2DoubleEntrySet().iterator(); iterator.hasNext();) {
				final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry = iterator
						.next();
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
			return printValues(new FastTreeHashVector(other));
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
		final Iterator<it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs>> iterator = values
				.object2DoubleEntrySet().iterator();
		while (iterator.hasNext()) {
			final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> next = iterator
					.next();
			ret.append(next.getKey());
			ret.append("=");
			ret.append(String.format("%.3f", next.getDoubleValue()));
			if (iterator.hasNext()) {
				ret.append(", ");
			}
		}
		ret.append("}");
		return ret.toString();
	}

	@Override
	public boolean valuesInRange(final double min, final double max) {
		for (final it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<KeyArgs> entry : values
				.object2DoubleEntrySet()) {
			final Double value = entry.getDoubleValue();
			if (value < min || value > max) {
				return false;
			}
		}
		return true;
	}
}
