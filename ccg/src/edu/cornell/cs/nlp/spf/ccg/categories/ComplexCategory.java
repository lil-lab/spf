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

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;

/**
 * A CCG category with a complex syntactic category.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class ComplexCategory<MR> extends Category<MR> {
	private static final long	serialVersionUID	= -6816584146794811796L;

	/**
	 * Immutable cache for the hashing code. This field is for internal use
	 * only! It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private final int			hashCodeCache;

	private final ComplexSyntax	syntax;

	public ComplexCategory(ComplexSyntax syntax, MR semantics) {
		super(semantics);
		this.syntax = syntax;
		this.hashCodeCache = calcHashCode();
	}

	@Override
	public Category<MR> cloneWithNewSemantics(MR newSemantics) {
		return new ComplexCategory<MR>(syntax, newSemantics);
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
		@SuppressWarnings("unchecked")
		final ComplexCategory<MR> other = (ComplexCategory<MR>) obj;
		if (!syntax.equals(other.syntax)) {
			return false;
		}
		return true;
	}

	public Slash getSlash() {
		return syntax.getSlash();
	}

	@Override
	public ComplexSyntax getSyntax() {
		return syntax;
	}

	@Override
	public int hashCode() {
		return hashCodeCache;
	}

	/**
	 * 'true' iff the slash is semantically equal to the given one.
	 */
	public boolean hasSlash(Slash s) {
		return syntax.getSlash() == Slash.VERTICAL || s == syntax.getSlash()
				|| s == Slash.VERTICAL;
	}

	@Override
	public int numSlashes() {
		return syntax.numSlashes();
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder(syntax.toString());
		if (getSemantics() != null) {
			result.append(" : ").append(getSemantics().toString());
		}
		return result.toString();
	}

	@Override
	protected int syntaxHash() {
		return syntax.hashCode();
	}
}
