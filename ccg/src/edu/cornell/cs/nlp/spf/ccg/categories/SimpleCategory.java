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
package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;

/**
 * A CCG category with a primitive syntactic category.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class SimpleCategory<MR> extends Category<MR> {

	private static final long	serialVersionUID	= -5684681598851371506L;
	/**
	 * Immutable cache for the hashing code. This field is for internal use
	 * only! It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private final int			hashCodeCache;

	/** The name of this atomic category */
	private final SimpleSyntax	syntax;

	public SimpleCategory(SimpleSyntax syntax, MR semantics) {
		super(semantics);
		assert syntax != null;
		this.syntax = syntax;
		this.hashCodeCache = calcHashCode();
	}

	@Override
	public Category<MR> cloneWithNewSemantics(MR newSemantics) {
		return new SimpleCategory<MR>(syntax, newSemantics);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final SimpleCategory other = (SimpleCategory) obj;
		if (hashCodeCache != other.hashCodeCache) {
			return false;
		}
		if (!syntax.equals(other.syntax)) {
			return false;
		}
		return true;
	}

	@Override
	public Syntax getSyntax() {
		return syntax;
	}

	@Override
	public int hashCode() {
		return hashCodeCache;
	}

	@Override
	public int numSlashes() {
		return 0;
	}

	@Override
	public String toString() {
		if (getSemantics() == null) {
			return syntax.toString();
		} else {
			return syntax + " : " + getSemantics();
		}
	}

	@Override
	protected int syntaxHash() {
		return syntax.hashCode();
	}

}
