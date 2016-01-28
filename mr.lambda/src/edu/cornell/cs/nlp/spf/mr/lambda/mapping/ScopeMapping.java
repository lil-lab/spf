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

import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.collections.stackmap.HashStackMap;
import edu.cornell.cs.nlp.utils.collections.stackmap.IStackMap;
import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Doesn't support <code>null</code> values.
 *
 * @author Yoav Artzi
 * @param <K>
 *            Key.
 * @param <V>
 *            Value.
 */
public class ScopeMapping<K, V> {

	protected final IStackMap<K, V>	map;
	protected final IStackMap<V, K>	reverseMapping;

	public ScopeMapping() {
		this(new HashStackMap<K, V>(), new HashStackMap<V, K>());
	}

	public ScopeMapping(IStackMap<K, V> map, IStackMap<V, K> reverseMap) {
		assert map.isEmpty() : "Map must be empty";
		assert reverseMap.isEmpty() : "Reverse map must be empty";
		this.map = map;
		this.reverseMapping = reverseMap;
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public boolean containsValue(V value) {
		return reverseMapping.containsKey(value);
	}

	public V peek(K key) {
		return map.peek(key);
	}

	public K peekValue(V value) {
		return reverseMapping.peek(value);
	}

	public V pop(K key) {
		final V value = map.pop(key);
		reverseMapping.pop(value);
		return value;
	}

	public void push(K key, V value) {
		map.push(key, value);
		reverseMapping.push(value, key);
	}

	@Override
	public String toString() {
		return new StringBuilder("{")
				.append(ScopeMapping.class.getSimpleName())
				.append(" ")
				.append(ListUtils.join(ListUtils.map(map,
						new ListUtils.Mapper<Pair<K, Iterator<V>>, String>() {

							@Override
							public String process(Pair<K, Iterator<V>> obj) {
								final StringBuilder entryString = new StringBuilder();
								entryString
										.append(obj.first())
										.append("(")
										.append(System.identityHashCode(obj
												.first())).append(")")
										.append("=>[");
								final Iterator<V> valueIterator = obj.second();
								while (valueIterator.hasNext()) {
									final V value = valueIterator.next();
									entryString.append(new StringBuilder(value
											.toString())
											.append("(")
											.append(System
													.identityHashCode(value))
											.append(")").toString());
									if (valueIterator.hasNext()) {
										entryString.append(", ");
									}
								}
								return entryString.append("]").toString();
							}
						}), ", ")).append("}").toString();
	}

}
