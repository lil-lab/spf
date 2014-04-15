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
package edu.uw.cs.lil.tiny.base;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PowerSet<T> implements Iterable<List<T>> {
	private final T[]	elts;
	private final int	hashCode;
	private final long	size;
	
	@SuppressWarnings("unchecked")
	public PowerSet(Collection<T> source) {
		final long n = source.size();
		this.elts = (T[]) source.toArray();
		this.size = 1L << n;
		this.hashCode = (1 << (n - 1)) * Arrays.hashCode(this.elts);
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
			long	i	= 0;
			
			public boolean hasNext() {
				return i < size;
			}
			
			public List<T> next() {
				return new BitMaskList<T>(i++, elts);
			}
			
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public long size() {
		return size;
	}
	
	private static class BitMaskList<T> extends AbstractList<T> {
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
				
				public boolean hasNext() {
					return mask != 0;
				}
				
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
};
