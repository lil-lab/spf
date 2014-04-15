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
package edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.language.type.Type;

public class Lexeme implements Serializable {
	private static final long			serialVersionUID	= 669086599072880122L;
	private final List<LogicalConstant>	constants;
	private final String				origin;
	private final List<String>			tokens;
	private final List<Type>			typeSignature;
	
	public Lexeme(List<String> tokens, List<LogicalConstant> constants,
			String origin) {
		this.origin = origin;
		this.constants = Collections.unmodifiableList(constants);
		this.tokens = Collections.unmodifiableList(tokens);
		this.typeSignature = Collections
				.unmodifiableList(getSignature(constants));
	}
	
	public static List<Type> getSignature(List<LogicalConstant> constants) {
		final List<Type> types = new ArrayList<Type>(constants.size());
		for (final LogicalConstant constant : constants) {
			types.add(LogicLanguageServices.getTypeRepository().generalizeType(
					constant.getType()));
		}
		return types;
	}
	
	/**
	 * Given a string, read a lexeme from it.
	 */
	public static Lexeme read(String line, String origin) {
		
		final int equalsIndex = line.indexOf("=");
		final String tokensString = line.substring(1, equalsIndex - 1);
		final String constantsString = line.substring(equalsIndex + 2,
				line.length() - 1);
		
		final List<String> tokens = new LinkedList<String>();
		for (final String token : tokensString.split(", ")) {
			tokens.add(token);
		}
		
		final List<LogicalConstant> constants = new LinkedList<LogicalConstant>();
		if (!constantsString.equals("")) {
			for (final String constant : constantsString.split(", ")) {
				constants.add(LogicalConstant.read(constant));
			}
		}
		return new Lexeme(tokens, constants, origin);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Lexeme)) {
			return false;
		}
		final Lexeme other = (Lexeme) obj;
		if (constants == null) {
			if (other.constants != null) {
				return false;
			}
		} else if (!constants.equals(other.constants)) {
			return false;
		}
		if (tokens == null) {
			if (other.tokens != null) {
				return false;
			}
		} else if (!tokens.equals(other.tokens)) {
			return false;
		}
		return true;
	}
	
	public List<LogicalConstant> getConstants() {
		return constants;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public List<String> getTokens() {
		return tokens;
	}
	
	public List<Type> getTypeSignature() {
		return typeSignature;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((constants == null) ? 0 : constants.hashCode());
		result = prime * result + ((tokens == null) ? 0 : tokens.hashCode());
		return result;
	}
	
	public boolean matches(List<String> inputTokens) {
		return tokens.equals(inputTokens);
	}
	
	public int numConstants() {
		return constants.size();
	}
	
	@Override
	public String toString() {
		return tokens + "=" + constants;
	}
	
}
