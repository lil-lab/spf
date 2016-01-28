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
package edu.cornell.cs.nlp.spf.mr.lambda.mapping;

import java.util.Iterator;

import edu.cornell.cs.nlp.utils.collections.stackmap.IStackMap;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class ScopeMappingOverlay<K, V> extends ScopeMapping<K, V> {

	private final ScopeMapping<K, V>	base;

	public ScopeMappingOverlay(ScopeMapping<K, V> base, IStackMap<K, V> map,
			IStackMap<V, K> reverseMap) {
		super(map, reverseMap);
		this.base = base;
	}

	public void applyToBase() {
		for (final Pair<K, Iterator<V>> entry : map) {
			final Iterator<V> stackIterator = entry.second();
			final K key = entry.first();
			while (stackIterator.hasNext()) {
				base.push(key, stackIterator.next());
			}
		}
		map.clear();
		reverseMapping.clear();
	}

	@Override
	public boolean containsKey(K key) {
		return super.containsKey(key) || base.containsKey(key);
	}

	@Override
	public boolean containsValue(V value) {
		return super.containsValue(value) || base.containsValue(value);
	}

	@Override
	public V peek(K key) {
		final V overlayValue = super.peek(key);
		if (overlayValue == null) {
			return base.peek(key);
		} else {
			return overlayValue;
		}
	}

	@Override
	public K peekValue(V value) {
		final K overlayKey = super.peekValue(value);
		if (overlayKey == null) {
			return base.peekValue(value);
		} else {
			return overlayKey;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder("{")
				.append(ScopeMappingOverlay.class.getSimpleName())
				.append(" base=").append(base.toString()).append(" ")
				.append(super.toString()).append("}").toString();
	}

}
