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
 * Type of an array for a given base type.
 * 
 * @author Yoav Artzi
 */
public class ArrayType extends Type {
	public static final String	ARRAY_SUFFIX		= "[]";
	
	private static final long	serialVersionUID	= 888739870983897365L;
	
	private final Type			baseType;
	private final Type			parent;
	
	ArrayType(String name, Type baseType, Type parent) {
		super(name);
		this.parent = parent;
		this.baseType = baseType;
	}
	
	public Type getBaseType() {
		return baseType;
	}
	
	@Override
	public Type getDomain() {
		return null;
	}
	
	@Override
	public Type getRange() {
		return this;
	}
	
	@Override
	public boolean isArray() {
		return true;
	}
	
	@Override
	public boolean isComplex() {
		return baseType.isComplex();
	}
	
	@Override
	public boolean isExtending(Type other) {
		if (other == null) {
			return false;
		}
		
		if (this.equals(other)) {
			return true;
		} else if (other.isArray()) {
			// An array A extends and array B, if the A.basetype extends
			// B.basetype
			return this.baseType.isExtending(((ArrayType) other).getBaseType());
		} else {
			return parent == null ? false : parent.isExtending(other);
		}
	}
	
	@Override
	public boolean isExtendingOrExtendedBy(Type other) {
		return other != null
				&& (this.isExtending(other) || other.isExtending(this));
	}
	
	@Override
	public String toString() {
		return baseType.toString() + ARRAY_SUFFIX;
	}
}
