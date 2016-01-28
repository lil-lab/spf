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

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;

/**
 * A CCG Category has both a syntactic and semantic component. Each instance of
 * this class stores both.
 *
 * @author Yoav Artzi
 */
public abstract class Category<MR> implements Serializable {

	private static final long	serialVersionUID	= 2261734167449321894L;

	/**
	 * Category semantics.
	 */
	protected final MR			semantics;

	public Category(MR semantics) {
		this.semantics = semantics;
	}

	public static <MR> Category<MR> create(Syntax syntax) {
		return create(syntax, null);
	}

	public static <MR> Category<MR> create(Syntax syntax, MR semantics) {
		assert syntax != null : "Syntax can't be null";
		if (syntax instanceof SimpleSyntax) {
			return new SimpleCategory<MR>((SimpleSyntax) syntax, semantics);
		} else if (syntax instanceof ComplexSyntax) {
			return new ComplexCategory<MR>((ComplexSyntax) syntax, semantics);
		} else {
			throw new IllegalStateException("unsupported syntax type: "
					+ syntax.getClass());
		}
	}

	/**
	 * Clones the category, but replaces the semantics of current with the given
	 * one.
	 *
	 * @param newSemantics
	 * @return
	 */
	abstract public Category<MR> cloneWithNewSemantics(MR newSemantics);

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
		@SuppressWarnings("rawtypes")
		final Category other = (Category) obj;
		if (semantics == null) {
			if (other.semantics != null) {
				return false;
			}
		} else if (!semantics.equals(other.semantics)) {
			return false;
		}
		return true;
	}

	public MR getSemantics() {
		return semantics;
	}

	abstract public Syntax getSyntax();

	@Override
	public abstract int hashCode();

	abstract public int numSlashes();

	protected int calcHashCode() {
		if (semantics == null) {
			return 0;
		}
		return syntaxHash() + semantics.hashCode();
	}

	abstract protected int syntaxHash();
}
