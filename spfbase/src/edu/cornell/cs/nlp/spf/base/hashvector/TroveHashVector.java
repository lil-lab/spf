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

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectDoubleProcedure;

import java.util.Iterator;

import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Sparse vector based on the Trove scientific calculation hash map. This
 * implementation puts efficiency and memory consumption first.
 *
 * @author Yoav Artzi
 */
class TroveHashVector implements IHashVector {
	public static final IHashVectorImmutable	EMPTY				= new TroveHashVector();
	private static final long					serialVersionUID	= -8168101355823745381L;
	private final TObjectDoubleHashMap<KeyArgs>	values;

	TroveHashVector() {
		this.values = new TObjectDoubleHashMap<KeyArgs>(10, 0.5f, ZERO_VALUE);
	}

	TroveHashVector(IHashVectorImmutable other) {
		if (other instanceof TroveHashVector) {
			this.values = new TObjectDoubleHashMap<KeyArgs>(
					((TroveHashVector) other).values);
		} else {
			this.values = new TObjectDoubleHashMap<KeyArgs>(10, 0.5f,
					ZERO_VALUE);
			for (final Pair<KeyArgs, Double> o : other) {
				values.put(o.first(), o.second());
			}
		}
	}

	@Override
	public void add(final double num) {
		values.transformValues(value -> value + num);
	}

	@Override
	public void add(KeyArgs key, double value) {
		values.adjustOrPutValue(key, value, value);
	}

	@Override
	public void add(String arg1, double value) {
		values.adjustOrPutValue(new KeyArgs(arg1), value, value);
	}

	@Override
	public void add(String arg1, String arg2, double value) {
		values.adjustOrPutValue(new KeyArgs(arg1, arg2), value, value);
	}

	@Override
	public void add(String arg1, String arg2, String arg3, double value) {
		values.adjustOrPutValue(new KeyArgs(arg1, arg2, arg3), value, value);
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			double value) {
		values.adjustOrPutValue(new KeyArgs(arg1, arg2, arg3, arg4), value,
				value);
	}

	@Override
	public void add(String arg1, String arg2, String arg3, String arg4,
			String arg5, double value) {
		values.adjustOrPutValue(new KeyArgs(arg1, arg2, arg3, arg4, arg5),
				value, value);
	}

	/** {@inheritDoc} */
	@Override
	public TroveHashVector addTimes(final double times,
			IHashVectorImmutable other) {
		if (other instanceof TroveHashVector) {
			final TroveHashVector p = (TroveHashVector) other;
			final TroveHashVector ret = new TroveHashVector(this);
			p.values.forEachEntry((a, b) -> {
				final double val = times * b;
				ret.values.adjustOrPutValue(a, val, val);
				return true;
			});
			return ret;
		} else {
			return addTimes(times, new TroveHashVector(other));
		}
	}

	@Override
	public void addTimesInto(final double times, final IHashVector other) {
		if (other instanceof TroveHashVector) {
			final TroveHashVector p = (TroveHashVector) other;
			values.forEachEntry((a, b) -> {
				final double val = times * b;
				p.values.adjustOrPutValue(a, val, val + ZERO_VALUE);
				return true;
			});
		} else {
			// Less efficient when we can't access the underlying map.
			values.forEachEntry((a, b) -> {
				other.add(a, times * b);
				return true;
			});
		}
	}

	@Override
	public void applyFunction(final ValueFunction function) {
		values.transformValues(value -> function.apply(value));
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
		values.transformValues(value -> value / d);
	}

	@Override
	public double dotProduct(IHashVectorImmutable other) {
		if (size() <= other.size()) {
			if (other instanceof TroveHashVector) {
				final TroveHashVector lhtv = (TroveHashVector) other;
				final VectorMultiplyProcedure procedure = new VectorMultiplyProcedure(
						lhtv);
				values.forEachEntry(procedure);
				return procedure.sum;
			} else {
				return dotProduct(new TroveHashVector(other));
			}
		} else {
			return other.dotProduct(this);
		}
	}

	@Override
	public void dropNoise() {
		values.retainEntries((a, b) -> Math.abs(b) >= NOISE);
	}

	@Override
	public void dropZeros() {
		values.retainEntries((a, b) -> Math.abs(b) != ZERO_VALUE);
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
		final TroveHashVector other = (TroveHashVector) obj;
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
		return values.get(key);
	}

	@Override
	public double get(KeyArgs key, double defaultReturn) {
		if (values.contains(key)) {
			return values.get(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1) {
		return values.get(new KeyArgs(arg1));
	}

	@Override
	public double get(String arg1, double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1);
		if (values.contains(key)) {
			return values.get(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2) {
		return values.get(new KeyArgs(arg1, arg2));
	}

	@Override
	public double get(String arg1, String arg2, double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2);
		if (values.contains(key)) {
			return values.get(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2, String arg3) {
		return values.get(new KeyArgs(arg1, arg2, arg3));
	}

	@Override
	public double get(String arg1, String arg2, String arg3,
			double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3);
		if (values.contains(key)) {
			return values.get(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4) {
		return values.get(new KeyArgs(arg1, arg2, arg3, arg4));
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4);
		if (values.contains(key)) {
			return values.get(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		return values.get(new KeyArgs(arg1, arg2, arg3, arg4, arg5));
	}

	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5, double defaultReturn) {
		final KeyArgs key = new KeyArgs(arg1, arg2, arg3, arg4, arg5);
		if (values.contains(key)) {
			return values.get(key);
		} else {
			return defaultReturn;
		}
	}

	@Override
	public IHashVector getAll(KeyArgs partialKey) {
		final TroveHashVector result = new TroveHashVector();
		values.forEachEntry((key, value) -> {
			if (partialKey.contains(key)) {
				result.values.put(key, value);
			}
			return true;
		});
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1) {
		final TroveHashVector result = new TroveHashVector();
		values.forEachEntry((key, value) -> {
			if (arg1.equals(key.arg1)) {
				result.values.put(key, value);
			}
			return true;
		});
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2) {
		final TroveHashVector result = new TroveHashVector();
		values.forEachEntry((key, value) -> {
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)) {
				result.values.put(key, value);
			}
			return true;
		});
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3) {
		final TroveHashVector result = new TroveHashVector();
		values.forEachEntry((key, value) -> {
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3)) {
				result.values.put(key, value);
			}
			return true;
		});
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4) {
		final TroveHashVector result = new TroveHashVector();
		values.forEachEntry((key, value) -> {
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)) {
				result.values.put(key, value);
			}
			return true;
		});
		return result;
	}

	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4, final String arg5) {
		final TroveHashVector result = new TroveHashVector();
		values.forEachEntry((key, value) -> {
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)
					&& arg5.equals(key.arg5)) {
				result.values.put(key, value);
			}
			return true;
		});
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
		return !values.forEachValue(value -> {
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return false;
			} else {
				return true;
			}
		});
	}

	@Override
	public boolean isInit() {
		return false;
	}

	@Override
	public void iterate(final EntryFunction function) {
		values.forEachEntry((a, b) -> {
			function.apply(a, b);
			return true;
		});
	}

	@Override
	public Iterator<Pair<KeyArgs, Double>> iterator() {
		return new Iterator<Pair<KeyArgs, Double>>() {
			private final TObjectDoubleIterator<KeyArgs>	innerIterator	= values.iterator();

			@Override
			public boolean hasNext() {
				return innerIterator.hasNext();
			}

			@Override
			public Pair<KeyArgs, Double> next() {
				if (innerIterator.hasNext()) {
					innerIterator.advance();
					return Pair.of(innerIterator.key(), innerIterator.value());
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
		final L1NormProcedure proc = new L1NormProcedure();
		values.forEachEntry(proc);
		return proc.sum;
	}

	@Override
	public void multiplyBy(final double d) {
		values.transformValues(value -> value * d);
	}

	@Override
	public TroveHashVector pairWiseProduct(final IHashVectorImmutable other) {
		if (other instanceof TroveHashVector) {
			final TroveHashVector p = (TroveHashVector) other;
			if (size() <= other.size()) {
				final TroveHashVector ret = new TroveHashVector(this);
				p.values.forEachEntry((a, b) -> {
					if (p.values.contains(a)) {
						ret.values.put(a, b * values.get(a));
					}
					return true;
				});
				return ret;
			} else {
				return p.pairWiseProduct(this);
			}
		} else {
			return pairWiseProduct(new TroveHashVector(other));
		}
	}

	@Override
	public String printValues(IHashVectorImmutable other) {
		if (other instanceof TroveHashVector) {
			final TroveHashVector p = (TroveHashVector) other;
			final StringBuilder ret = new StringBuilder();
			ret.append("{");
			p.values.forEachEntry(new TObjectDoubleProcedure<KeyArgs>() {
				boolean	first	= true;

				@Override
				public boolean execute(KeyArgs a, double b) {
					if (!first) {
						ret.append(",");
					}
					first = false;
					ret.append(a).append("=");
					if (values.containsKey(a)) {
						ret.append(String.format("%.3f", values.get(a)));
					} else {
						ret.append(ZERO_VALUE);
					}
					ret.append("(").append(String.format("%.3f", b))
							.append(")");
					return true;
				}
			});
			ret.append("}");
			return ret.toString();
		} else {
			return printValues(new TroveHashVector(other));
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
		values.forEachEntry(new TObjectDoubleProcedure<KeyArgs>() {
			boolean	notFirst	= false;

			@Override
			public boolean execute(KeyArgs a, double b) {
				if (notFirst) {
					ret.append(", ");
				}
				ret.append(a);
				ret.append("=");
				ret.append(String.format("%.3f", b));
				notFirst = true;
				return true;
			}
		});
		ret.append("}");
		return ret.toString();
	}

	@Override
	public boolean valuesInRange(final double min, final double max) {
		return values.forEachValue(value -> {
			if (value < min || value > max) {
				return false;
			} else {
				return true;
			}
		});
	}

	private static class L1NormProcedure implements
			TObjectDoubleProcedure<KeyArgs> {
		private double	sum	= 0.0;

		@Override
		public boolean execute(KeyArgs a, double b) {
			sum += Math.abs(b);
			return true;
		}

	}

	private static class VectorMultiplyProcedure implements
			TObjectDoubleProcedure<KeyArgs> {
		private final TroveHashVector	other;
		private double					sum	= 0.0;

		public VectorMultiplyProcedure(TroveHashVector other) {
			this.other = other;
		}

		@Override
		public boolean execute(KeyArgs a, double b) {
			// If 'a' is not in other.values, Trove returns (double)0
			sum += b * other.values.get(a);
			return true;
		}
	}
}
