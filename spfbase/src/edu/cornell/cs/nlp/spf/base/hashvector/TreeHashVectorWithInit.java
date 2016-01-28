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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Sparse vector based on a {@link TreeMap} with arbitrary initialization. This
 * implementation is less efficient and has higher memory consumption than
 * {@link TroveHashVector}. However it does maintain a predictable order of
 * iteration, which is important for stability between runs.
 *
 * @author Yoav Artzi
 */
class TreeHashVectorWithInit extends TreeHashVector {

	private static final ToDoubleFunction<KeyArgs>	DEFAULT_INIT_FUNCTION	= new DefaultInitFunction();

	private static final long						serialVersionUID		= -4199983689437528656L;

	private final ToDoubleFunction<KeyArgs>			initFunction;

	public TreeHashVectorWithInit() {
		this(DEFAULT_INIT_FUNCTION);
	}

	TreeHashVectorWithInit(IHashVectorImmutable other) {
		this(other, DEFAULT_INIT_FUNCTION);
	}

	TreeHashVectorWithInit(IHashVectorImmutable other,
			ToDoubleFunction<KeyArgs> initFunction) {
		super(other);
		this.initFunction = initFunction;
	}

	TreeHashVectorWithInit(ToDoubleFunction<KeyArgs> initFunction) {
		this.initFunction = initFunction;
	}

	TreeHashVectorWithInit(TreeHashVectorWithInit other) {
		super(other);
		this.initFunction = other.initFunction;
	}

	@Override
	public void add(final double num) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() + num);
		}
	}

	@Override
	public void add(KeyArgs key, double value) {
		values.put(key,
				value
						+ (values.containsKey(key) ? values.get(key)
								: initFunction.applyAsDouble(key)));
	}

	@Override
	public void add(String arg1, double value) {
		final KeyArgs key = new KeyArgs(arg1);
		values.put(key,
				value
						+ (values.containsKey(key) ? values.get(key)
								: initFunction.applyAsDouble(key)));
	}

	@Override
	public void add(String arg1, String arg2, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2);
		values.put(key,
				value
						+ (values.containsKey(key) ? values.get(key)
								: initFunction.applyAsDouble(key)));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3);
		values.put(key,
				value
						+ (values.containsKey(key) ? values.get(key)
								: initFunction.applyAsDouble(key)));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4);
		values.put(key,
				value
						+ (values.containsKey(key) ? values.get(key)
								: initFunction.applyAsDouble(key)));
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			String arg5, double value) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4, arg5);
		values.put(key,
				value
						+ (values.containsKey(key) ? values.get(key)
								: initFunction.applyAsDouble(key)));
	}

	@Override
	public TreeHashVectorWithInit addTimes(final double times,
			IHashVectorImmutable other) {
		if (other.isInit()) {
			throw new IllegalStateException(
					"Adding is not defined when both vectors have non-zero initialization.");
		}

		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			final TreeHashVectorWithInit ret = new TreeHashVectorWithInit(this);
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
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"Adding a vector into another is not well-defined for vectors with initialization");
	}

	@Override
	public void applyFunction(ValueFunction function) {
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"Element-wise function application is not well-defined for vectors with initialization");
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
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"Element-wise division is not well-defined for vectors with initialization");
	}

	/**
	 * For vectors with initialization, dot-product is only defined when 'other'
	 * has no special initialization. Otherwise, will throw an exception.
	 */
	@Override
	public double dotProduct(IHashVectorImmutable other) {
		if (other.isInit()) {
			throw new IllegalStateException(
					"Dot-product is not defined when both vectors have non-zero initialization.");
		}

		// Since the other vector is guaranteed to have a 0.0 initialization
		// value, we only need to iterate its entries.

		if (other instanceof TreeHashVector) {
			final TreeHashVector thv = (TreeHashVector) other;
			double sum = 0.0;
			for (final Entry<KeyArgs, Double> entry : thv.values.entrySet()) {
				final KeyArgs key = entry.getKey();
				if (values.containsKey(key)) {
					sum += entry.getValue() * values.get(key);
				} else {
					sum += entry.getValue() * initFunction.applyAsDouble(key);
				}
			}
			return sum;
		} else {
			return dotProduct(new TreeHashVector(other));
		}
	}

	@Override
	public void dropNoise() {
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"Noise dropping is not well-defined for vectors with initialization");
	}

	@Override
	public void dropZeros() {
		final Iterator<Entry<KeyArgs, Double>> iterator = values.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			final Entry<KeyArgs, Double> next = iterator.next();
			if (next.getValue() == initFunction.applyAsDouble(next.getKey())) {
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
		final TreeHashVectorWithInit other = (TreeHashVectorWithInit) obj;
		if (initFunction == null) {
			if (other.initFunction != null) {
				return false;
			}
		} else if (!initFunction.equals(other.initFunction)) {
			return false;
		}
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
		return value == null ? initFunction.applyAsDouble(key) : value;
	}

	@Override
	public double get(KeyArgs key, double defaultReturn) {
		final Double value = values.get(key);
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1) {
		final KeyArgs key = new KeyArgs(arg1);
		final Double value = values.get(key);
		return value == null ? initFunction.applyAsDouble(key) : value;
	}

	@Override
	public double get(String arg1, double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2) {
		final KeyArgs key = new KeyArgs(arg1, arg2);
		final Double value = values.get(key);
		return value == null ? initFunction.applyAsDouble(key) : value;
	}

	@Override
	public double get(String arg1, String arg2, double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3);
		final Double value = values.get(key);
		return value == null ? initFunction.applyAsDouble(key) : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3,
			double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3));
		return value == null ? defaultReturn : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4);
		final Double value = values.get(key);
		return value == null ? initFunction.applyAsDouble(key) : value;
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
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4, arg5);
		final Double value = values.get(key);
		return value == null ? initFunction.applyAsDouble(key) : value;
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5, double defaultReturn) {
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4,
				arg5));
		return value == null ? defaultReturn : value;
	}

	/**
	 * Only gets explicitly set keys.
	 */
	@Override
	public IHashVector getAll(KeyArgs partialKey) {
		final TreeHashVectorWithInit result = new TreeHashVectorWithInit();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (partialKey.contains(key)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Only gets explicitly set keys.
	 */
	@Override
	public IHashVector getAll(final String arg1) {
		final TreeHashVectorWithInit result = new TreeHashVectorWithInit();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Only gets explicitly set keys.
	 */
	@Override
	public IHashVector getAll(final String arg1, final String arg2) {
		final TreeHashVectorWithInit result = new TreeHashVectorWithInit();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Only gets explicitly set keys.
	 */
	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3) {
		final TreeHashVectorWithInit result = new TreeHashVectorWithInit();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Only gets explicitly set keys.
	 */
	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4) {
		final TreeHashVectorWithInit result = new TreeHashVectorWithInit();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Only gets explicitly set keys.
	 */
	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4, final String arg5) {
		final TreeHashVectorWithInit result = new TreeHashVectorWithInit();
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
		result = prime * result
				+ (initFunction == null ? 0 : initFunction.hashCode());
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
		return true;
	}

	/**
	 * Function is applied only to explicitly present keys.
	 */
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
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"L1-norm is not well-defined for vectors with initialization");
	}

	@Override
	public void multiplyBy(double value) {
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"Element-wise multiplication is not well-defined for vectors with initialization");
	}

	/**
	 * Only applied to keys that are explicitly present in either of the
	 * vectors.
	 */
	@Override
	public TreeHashVectorWithInit pairWiseProduct(IHashVectorImmutable other) {
		if (other.isInit()) {
			throw new IllegalStateException(
					"Pairwise product is not defined when both vectors have non-zero initialization.");
		}

		// Since the other vector is guaranteed to have a 0.0 initialization
		// value, we only need to iterate its entries.

		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			final TreeHashVectorWithInit ret = new TreeHashVectorWithInit(this);
			for (final Entry<KeyArgs, Double> entry : p.values.entrySet()) {
				final KeyArgs key = entry.getKey();
				if (values.containsKey(key)) {
					ret.values.put(key, entry.getValue() * values.get(key));
				} else {
					ret.values.put(key,
							entry.getValue() * initFunction.applyAsDouble(key));
				}
			}
			return ret;
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
							+ initFunction.applyAsDouble(entry.getKey())
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

	/**
	 * Only tests explicitly present keys.
	 */
	@Override
	public boolean valuesInRange(final double min, final double max) {
		// This method is not well-defined for vectors with non-zero initial
		// values.
		throw new IllegalStateException(
				"Range checking is not well-defined for vectors with initialization");
	}

	/**
	 * Weight initialization function. Generates an arbitrary, but not random,
	 * value. This can be used to consistently generate arbitrary values across
	 * different environment without synchronization.
	 *
	 * @author Yoav Artzi
	 */
	public static class HashInitFunction implements ToDoubleFunction<KeyArgs>,
			Serializable {

		private static final long	serialVersionUID	= 7581485458139669780L;
		private final double		scalingFactor;

		public HashInitFunction(double scalingFactor) {
			this.scalingFactor = scalingFactor;
		}

		@Override
		public double applyAsDouble(KeyArgs key) {
			return ((double) key.hashCode() / (double) Integer.MAX_VALUE - 0.5)
					* scalingFactor;
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
			final HashInitFunction other = (HashInitFunction) obj;
			if (Double.doubleToLongBits(scalingFactor) != Double
					.doubleToLongBits(other.scalingFactor)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(scalingFactor);
			result = prime * result + (int) (temp ^ temp >>> 32);
			return result;
		}

	}

	private static class DefaultInitFunction implements
			ToDoubleFunction<KeyArgs>, Serializable {

		private static final long	serialVersionUID	= 6057193293490846670L;

		private DefaultInitFunction() {
			// Nothing to do.
		}

		@Override
		public double applyAsDouble(KeyArgs value) {
			return ZERO_VALUE;
		}

		/**
		 * Resolve to static instance.
		 *
		 * @throws ObjectStreamException
		 */
		protected Object readResolve() throws ObjectStreamException {
			return DEFAULT_INIT_FUNCTION;
		}

	}
}
