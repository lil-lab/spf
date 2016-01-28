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
package edu.cornell.cs.nlp.spf.ccg.categories.syntax;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;

public class SyntaxAttributeTyping {
	private static SyntaxAttributeTyping	INSTANCE	= new SyntaxAttributeTyping(
																new Object2ObjectOpenHashMap<String, Set<String>>());
	private final Map<String, Set<String>>	constraints;

	private SyntaxAttributeTyping(Map<String, Set<String>> constraints) {
		// Service class, so private ctor.
		this.constraints = constraints;
	}

	public static boolean isValidSyntaxAtrributePairing(SimpleSyntax syntax,
			String attribute) {
		return attribute == null
				|| attribute.equals(Syntax.VARIABLE_ATTRIBUTE)
				|| !INSTANCE.constraints.containsKey(attribute)
				|| INSTANCE.constraints.get(attribute).contains(
						syntax.getLabel());
	}

	public static boolean isWellTyped(Syntax syntax) {
		if (syntax instanceof SimpleSyntax) {
			final SimpleSyntax simple = (SimpleSyntax) syntax;
			return isValidSyntaxAtrributePairing(simple, simple.getAttribute());
		} else if (syntax instanceof ComplexSyntax) {
			final ComplexSyntax complex = (ComplexSyntax) syntax;
			return isWellTyped(complex.getLeft())
					&& isWellTyped(complex.getRight());
		} else {
			throw new IllegalStateException("unexpected syntax class");
		}
	}

	public static void setInstance(SyntaxAttributeTyping instance) {
		INSTANCE = instance;
	}

	public static class Builder {
		private final Object2ObjectOpenHashMap<String, Set<String>>	constraints	= new Object2ObjectOpenHashMap<String, Set<String>>();

		public Builder addAllowedSyntax(String attribute, SimpleSyntax syntax) {
			if (!constraints.containsKey(attribute)) {
				addAttribute(attribute);
			}
			constraints.get(attribute).add(syntax.getLabel());
			return this;
		}

		public Builder addAttribute(String attribute) {
			constraints.put(attribute, new ObjectOpenHashSet<String>());
			return this;
		}

		public SyntaxAttributeTyping build() {
			return new SyntaxAttributeTyping(
					Object2ObjectMaps.unmodifiable(constraints));
		}

	}

}
