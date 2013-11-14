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
package edu.uw.cs.lil.tiny.mr.lambda;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A collection of constants forms and ontology. The ontology can be closed
 * (i.e., immutable) or not (i.e., constants can be added).
 * 
 * @author Yoav Artzi
 */
public class Ontology implements Iterable<LogicalConstant> {
	
	private final Map<WrappedConstant, LogicalConstant>	constants;
	private final Map<String, LogicalConstant>			constantsByName;
	private final boolean								isClosed;
	
	public Ontology(Iterable<LogicalConstant> constants, boolean isClosed) {
		this.isClosed = isClosed;
		this.constants = new HashMap<WrappedConstant, LogicalConstant>();
		this.constantsByName = new HashMap<String, LogicalConstant>();
		for (final LogicalConstant constant : constants) {
			this.constants.put(new WrappedConstant(constant), constant);
			this.constantsByName.put(constant.getName(), constant);
		}
	}
	
	/**
	 * Adds a new constant to the ontology if not already present. The equal
	 * constant present in the ontology after the operation is returned.
	 * 
	 * @param constant
	 * @return
	 */
	public LogicalConstant add(LogicalConstant constant, boolean force) {
		synchronized (constants) {
			if (!contains(constant)) {
				if (force || !isClosed) {
					constants.put(new WrappedConstant(constant), constant);
					constantsByName.put(constant.getName(), constant);
				} else {
					throw new LogicalExpressionRuntimeException(String.format(
							"Closed ontology. Failed to add: %s", constant));
				}
			}
			return get(constant);
		}
	}
	
	/**
	 * Checks if a constant is included in the ontology.
	 */
	public boolean contains(LogicalConstant constant) {
		return constants.containsKey(new WrappedConstant(constant));
	}
	
	/**
	 * Checks if a constant with the given name is included in the ontology.
	 */
	public boolean contains(String name) {
		return constantsByName.containsKey(name);
	}
	
	public LogicalConstant get(LogicalConstant constant) {
		return constants.get(new WrappedConstant(constant));
	}
	
	public LogicalConstant get(String name) {
		return constantsByName.get(name);
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
	
	public LogicalConstant getOrAdd(LogicalConstant constant, boolean force) {
		if (contains(constant)) {
			return get(constant);
		} else {
			return add(constant, force);
		}
	}
	
	/**
	 * @return 'true' iff it's not possible to add constatns to this ontology.
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
		private final LogicalConstant	constant;
		
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
