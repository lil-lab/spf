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
package edu.cornell.cs.nlp.spf.mr.lambda;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A collection of constants forms and ontology. The ontology can be closed
 * (i.e., immutable) or not (i.e., constants can be added).
 *
 * @author Yoav Artzi
 */
public class Ontology implements Iterable<LogicalConstant> {

	private final ConcurrentHashMap<String, LogicalConstant>	constants;
	private final boolean										isClosed;

	public Ontology(Iterable<LogicalConstant> constants, boolean isClosed) {
		this.isClosed = isClosed;
		this.constants = new ConcurrentHashMap<String, LogicalConstant>();
		for (final LogicalConstant constant : constants) {
			this.constants.put(constant.getName(), constant);
		}
	}

	/**
	 * Checks if a constant is included in the ontology.
	 */
	public boolean contains(LogicalConstant constant) {
		return constants.containsKey(new WrappedConstant(constant));
	}

	public Set<LogicalConstant> getAllConstants() {
		return new HashSet<LogicalConstant>(constants.values());
	}

	public Set<LogicalConstant> getAllPredicates() {
		final Set<LogicalConstant> predicates = new HashSet<LogicalConstant>();

		for (final LogicalConstant constant : constants.values()) {
			if (constant.getType().isComplex()) {
				predicates.add(constant);
			}
		}

		return predicates;
	}

	/**
	 * Gets or adds a new {@link LogicalConstant} from the Ontology. The return
	 * value of this method is being casted to the requested class, which
	 * extends {@link LogicalConstant}. This behavior can lead to exceptions if
	 * constant creation is not consistent. Meaning, if two inhering classes are
	 * created with identical names, a casting runtime exception might be raised
	 * due to conflict.
	 */
	@SuppressWarnings("unchecked")
	public <T extends LogicalConstant> T getOrAdd(String fullName,
			boolean force, Supplier<T> supplier) {
		if (constants.containsKey(fullName)) {
			return (T) constants.get(fullName);
		} else if (!isClosed || force) {
			// Create the constant, add it to the ontology and return it.
			final LogicalConstant existing = constants.putIfAbsent(fullName,
					supplier.get());
			return (T) (existing == null ? constants.get(fullName) : existing);
		} else {
			throw new LogicalExpressionRuntimeException(String
					.format("Closed ontology. Failed to add: %s", fullName));
		}
	}

	/**
	 * @return 'true' iff it's not possible to add constants to this ontology.
	 */
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public Iterator<LogicalConstant> iterator() {
		return constants.values().iterator();
	}

	/**
	 * Wrapper used to force using content comparison (over instance comparison)
	 * when indexing constants in the ontology.
	 *
	 * @author Yoav Artzi
	 */
	private static class WrappedConstant {
		private final LogicalConstant constant;

		public WrappedConstant(LogicalConstant constant) {
			this.constant = constant;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof WrappedConstant
					&& constant.doEquals(((WrappedConstant) obj).constant);
		}

		@Override
		public int hashCode() {
			return constant.hashCode();
		}
	}
}
