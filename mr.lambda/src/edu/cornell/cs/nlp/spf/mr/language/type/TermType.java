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

/**
 * A type for a term with a possible parent.
 * 
 * @author Yoav Artzi
 */
public class TermType extends Type {
	private static final long	serialVersionUID	= -1885822138975802084L;
	private final TermType		parent;
	
	TermType(String label) {
		super(label);
		this.parent = null;
	}
	
	TermType(String label, TermType parent) {
		super(label);
		this.parent = parent;
	}
	
	@Override
	public Type getDomain() {
		return null;
	}
	
	public TermType getParent() {
		return parent;
	}
	
	@Override
	public Type getRange() {
		return this;
	}
	
	@Override
	public boolean isArray() {
		return false;
	}
	
	@Override
	public boolean isComplex() {
		return false;
	}
	
	@Override
	public boolean isExtending(Type other) {
		if (this.equals(other)) {
			return true;
		} else {
			return parent != null && parent.isExtending(other);
		}
	}
	
	@Override
	public boolean isExtendingOrExtendedBy(Type other) {
		return other != null
				&& (this.isExtending(other) || other.isExtending(this));
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
}
