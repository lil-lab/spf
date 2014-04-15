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

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;

/**
 * A CCG category with a complex syntactic category.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class ComplexCategory<MR> extends Category<MR> {
	private static final long	serialVersionUID	= -6816584146794811796L;
	
	private final ComplexSyntax	syntax;
	
	public ComplexCategory(ComplexSyntax syntax, MR semantics) {
		super(semantics);
		this.syntax = syntax;
	}
	
	@Override
	public Category<MR> cloneWithNewSemantics(MR newSemantics) {
		return new ComplexCategory<MR>(syntax, newSemantics);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ComplexCategory)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final ComplexCategory cc = (ComplexCategory) other;
		if (!equalsNoSem(other)) {
			return false;
		}
		if (getSem() != null && cc.getSem() != null
				&& !getSem().equals(cc.getSem())) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean equalsNoSem(Object other) {
		if (!(other instanceof ComplexCategory)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final ComplexCategory cc = (ComplexCategory) other;
		if (!syntax.equals(cc.syntax)) {
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
	
	/**
	 * 'true' iff the slash is semantically equal to the given one.
	 */
	public boolean hasSlash(Slash s) {
		return syntax.getSlash() == Slash.VERTICAL || s == syntax.getSlash()
				|| s == Slash.VERTICAL;
	}
	
	@Override
	public boolean matches(Category<MR> other) {
		if (!(other instanceof ComplexCategory)) {
			return false;
		}
		final ComplexCategory<MR> cc = (ComplexCategory<MR>) other;
		if (cc.syntax.getSlash() != syntax.getSlash()
				&& syntax.getSlash() != Slash.VERTICAL
				&& cc.syntax.getSlash() != Slash.VERTICAL) {
			return false;
		}
		if (!matchesNoSem(other)) {
			return false;
		}
		if (getSem() != null && other.getSem() != null
				&& !getSem().equals(other.getSem())) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean matchesNoSem(Category<MR> other) {
		if (!(other instanceof ComplexCategory)) {
			return false;
		}
		return syntax.equals(other.getSyntax());
	}
	
	@Override
	public int numSlashes() {
		return syntax.numSlashes();
	}
	
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder(syntax.toString());
		if (getSem() != null) {
			result.append(" : ").append(getSem().toString());
		}
		return result.toString();
	}
	
	@Override
	protected int syntaxHash() {
		return syntax.hashCode();
	}
}
