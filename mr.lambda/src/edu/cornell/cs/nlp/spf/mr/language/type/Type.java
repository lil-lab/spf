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
package edu.cornell.cs.nlp.spf.mr.language.type;

import java.io.ObjectStreamException;
import java.io.Serializable;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;

/**
 * A language entity type.
 *
 * @author Yoav Artzi
 */
public abstract class Type implements Serializable {
	private static final long	serialVersionUID	= 1758388007880855246L;
	/**
	 * Immutable cache for the hashing code. This field is for internal use
	 * only! It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	final private int			hashCodeCache;
	/**
	 * The name of the type. This name must be unique. Meaning, we don't allow
	 * function over-loading, for example.
	 */
	final private String		name;

	Type(String name) {
		this.name = name;
		this.hashCodeCache = calcHashCode();
	}

	@Override
	final public boolean equals(Object obj) {
		// There is one object for each type, so if they are not the same, they
		// are different types
		return this == obj;
	}

	public abstract Type getDomain();

	public String getName() {
		return name;
	}

	public abstract Type getRange();

	@Override
	final public int hashCode() {
		return hashCodeCache;
	}

	/**
	 * Returns true iff the type is an array.
	 *
	 * @return
	 */
	public abstract boolean isArray();

	/**
	 * Return true iff the object is a complex function type.
	 *
	 * @return
	 */
	public abstract boolean isComplex();

	/**
	 * Is current type a child of another.
	 *
	 * @param other
	 */
	public abstract boolean isExtending(Type other);

	/**
	 * Return 'true' iff the given type and this type share a path on the
	 * hierarchical tree.
	 *
	 * @param other
	 */
	public abstract boolean isExtendingOrExtendedBy(Type other);

	@Override
	public abstract String toString();

	private int calcHashCode() {
		return this.name.hashCode();
	}

	/**
	 * Used to resolve read serialized objects to the equivalent one in the
	 * repository.
	 *
	 * @return
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		return LogicLanguageServices.getTypeRepository().getTypeCreateIfNeeded(
				name);
	}

}
