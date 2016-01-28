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

import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType.Option;

public class ComplexType extends Type {
	public static final char	COMPLEX_TYPE_CLOSE_PAREN		= '>';
	public static final String	COMPLEX_TYPE_CLOSE_PAREN_STR	= String.valueOf(COMPLEX_TYPE_CLOSE_PAREN);
	public static final char	COMPLEX_TYPE_OPEN_PAREN			= '<';
	public static final String	COMPLEX_TYPE_OPEN_PAREN_STR		= String.valueOf(COMPLEX_TYPE_OPEN_PAREN);
	public static final char	COMPLEX_TYPE_SEP				= ',';
	private static final long	serialVersionUID				= -4179088110249120938L;

	private final Type			domain;
	private final Type			range;

	ComplexType(String label, Type domain, Type range) {
		super(label);
		assert domain != null;
		assert range != null;
		this.domain = domain;
		this.range = range;
	}

	public static String composeString(Type range, Type domain, Option option) {
		return new StringBuilder(20).append(COMPLEX_TYPE_OPEN_PAREN)
				.append(domain.toString())
				.append(option == null ? "" : option.toString())
				.append(COMPLEX_TYPE_SEP).append(range.toString())
				.append(COMPLEX_TYPE_CLOSE_PAREN).toString();
	}

	public static ComplexType create(String label, Type domain, Type range,
			Option option) {
		if (option == null) {
			return new ComplexType(label, domain, range);
		} else {
			return new RecursiveComplexType(label, domain, range, option);
		}
	}

	@Override
	public Type getDomain() {
		return domain;
	}

	public Option getOption() {
		return null;
	}

	@Override
	public Type getRange() {
		return range;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isComplex() {
		return true;
	}

	@Override
	public boolean isExtending(Type other) {
		return other != null
				&& (other == this || domain.isExtending(other.getDomain())
						&& range.isExtending(other.getRange()));
	}

	@Override
	public boolean isExtendingOrExtendedBy(Type other) {
		return isExtending(other) || other.isExtending(this);
	}

	public boolean isOrderSensitive() {
		return true;
	}

	@Override
	public String toString() {
		return getName();
	}

}
