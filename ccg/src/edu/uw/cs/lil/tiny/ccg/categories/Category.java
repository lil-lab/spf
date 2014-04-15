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
package edu.uw.cs.lil.tiny.ccg.categories;

import java.io.Serializable;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax.SimpleSyntax;

/**
 * A CCG Category has both a syntactic and semantic component. Each instance of
 * this class stores both.
 * 
 * @author Yoav Artzi
 */
public abstract class Category<MR> implements Serializable {
	
	private static final long	serialVersionUID	= 2261734167449321894L;
	
	/**
	 * Mutable cache for the hashing code. This field is for internal use only!
	 * It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private int					hashCodeCache;
	
	/**
	 * Mutable flag to indicate if the hash code cache is populated. This field
	 * is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object.
	 */
	private boolean				hashCodeCalculated	= false;
	
	/**
	 * Category semantics
	 */
	private final MR			semantics;
	
	public Category(MR semantics) {
		this.semantics = semantics;
	}
	
	public static <MR> Category<MR> create(Syntax syntax) {
		return create(syntax, null);
	}
	
	public static <MR> Category<MR> create(Syntax syntax, MR semantics) {
		if (syntax instanceof SimpleSyntax) {
			return new SimpleCategory<MR>((SimpleSyntax) syntax, semantics);
		} else if (syntax instanceof ComplexSyntax) {
			return new ComplexCategory<MR>((ComplexSyntax) syntax, semantics);
		} else {
			throw new IllegalStateException("unsupported syntax type");
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
	
	/**
	 * tests for equality of syntax without checking if the lambda expressions
	 * are equals used primarily during parsing
	 */
	abstract public boolean equalsNoSem(Object o);
	
	public MR getSem() {
		return semantics;
	}
	
	abstract public Syntax getSyntax();
	
	@Override
	final public int hashCode() {
		if (!hashCodeCalculated) {
			hashCodeCache = calcHashCode();
			hashCodeCalculated = true;
		}
		return hashCodeCache;
	}
	
	// does the full thing match?
	abstract public boolean matches(Category<MR> c);
	
	// does just the syntactic component match?
	abstract public boolean matchesNoSem(Category<MR> c);
	
	abstract public int numSlashes();
	
	private int calcHashCode() {
		if (semantics == null) {
			return 0;
		}
		return syntaxHash() + semantics.hashCode();
	}
	
	abstract protected int syntaxHash();
}
