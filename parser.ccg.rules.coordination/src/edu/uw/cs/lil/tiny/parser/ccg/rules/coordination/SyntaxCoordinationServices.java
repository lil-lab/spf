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
package edu.uw.cs.lil.tiny.parser.ccg.rules.coordination;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;

class SyntaxCoordinationServices {
	
	private SyntaxCoordinationServices() {
		// Not instantiation
	}
	
	public static Syntax getCoordinationType(Syntax syntax) {
		if (syntax instanceof ComplexSyntax) {
			final ComplexSyntax complexSyntax = (ComplexSyntax) syntax;
			if (complexSyntax.getLeft().equals(Syntax.C)
					&& complexSyntax.getSlash().equals(Slash.VERTICAL)) {
				return complexSyntax.getRight();
			}
		}
		return null;
	}
	
	/**
	 * 'true' iff the input syntax is a coordination of the type argument. If
	 * type == null, return true/false without typing constraint.
	 * 
	 * @param syntax
	 * @param type
	 * @return
	 */
	public static boolean isCoordinationOfType(Syntax syntax, Syntax type) {
		final Syntax coordinationType = getCoordinationType(syntax);
		if (coordinationType == null) {
			return false;
		} else {
			return type == null || type.equals(coordinationType);
		}
	}
}
