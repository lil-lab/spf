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
package edu.uw.cs.lil.tiny.mr.lambda;

import java.util.Map;

import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;

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
		this.type = type;
	}
	
	protected static Term doParse(String string,
			Map<String, Variable> variables, TypeRepository typeRepository,
			ITypeComparator typeComparator, boolean lockOntology) {
		if (string.startsWith(Variable.PREFIX)) {
			return Variable.doParse(string, variables, typeRepository);
		} else {
			return LogicalConstant
					.doParse(string, typeRepository, lockOntology);
		}
	}
	
	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		// No need for variable mapping at this level
		return equals(obj, null);
	}
	
	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	protected boolean doEquals(Object obj,
			Map<Variable, Variable> variablesMapping) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Term other = (Term) obj;
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
