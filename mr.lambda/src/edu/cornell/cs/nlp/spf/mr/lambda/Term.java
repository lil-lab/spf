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
package edu.cornell.cs.nlp.spf.mr.lambda;

import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Logical expression term.
 *
 * @author Yoav Artzi
 */
public abstract class Term extends LogicalExpression {
	public static final String	TYPE_SEPARATOR		= ":";
	private static final long	serialVersionUID	= -5545012754214908898L;
	private final Type			type;

	public Term(Type type) {
		assert type != null;
		this.type = type;
	}

	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (type == null ? 0 : type.hashCode());
		return result;
	}

	/**
	 * For terms, there's no need for variable mapping, so override
	 * {@link LogicalExpression#equals(Object)} to avoid creating the map.
	 */
	@Override
	public boolean equals(Object obj) {
		// Short-circuit with 'instanceof' and hash code check.
		return obj instanceof Term && obj.hashCode() == hashCode()
				&& doEquals((LogicalExpression) obj);
	}

	@Override
	public Type getType() {
		return type;
	}

	protected boolean doEquals(LogicalExpression exp) {
		if (this == exp) {
			return true;
		}
		if (exp == null) {
			return false;
		}
		if (getClass() != exp.getClass()) {
			return false;
		}
		final Term other = (Term) exp;
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}
}
