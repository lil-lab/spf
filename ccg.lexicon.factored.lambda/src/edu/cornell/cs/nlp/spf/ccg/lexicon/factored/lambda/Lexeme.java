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
package edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.collections.MapUtils;

/**
 * A lexeme is a tuple of (1) a sequence of tokens, (2) a set of constants and
 * (3) a set of syntactic attributes. The set are represented as lists to
 * simplify the initialization of templates and make it deterministic. This
 * simplifications results in some spurious ambiguity.
 *
 * @author Yoav Artzi
 */
public class Lexeme implements Serializable {
	private static final long			serialVersionUID	= 669086599072880122L;
	/**
	 * The syntactic attributes defined by this lexeme.
	 */
	private final List<String>			attributes;
	private final List<LogicalConstant>	constants;
	/**
	 * Immutable cache for the hashing code. This field is for internal use
	 * only! It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private final int					hashCodeCache;

	private final Map<String, String>	properties;

	private final FactoringSignature	signature;

	private final TokenSeq				tokens;

	public Lexeme(TokenSeq tokens, List<LogicalConstant> constants,
			List<String> attributes) {
		this(tokens, constants, attributes, new HashMap<String, String>());
	}

	public Lexeme(TokenSeq tokens, List<LogicalConstant> constants,
			List<String> attributes, FactoringSignature signature) {
		this(tokens, constants, attributes, signature,
				new HashMap<String, String>());
	}

	public Lexeme(TokenSeq tokens, List<LogicalConstant> constants,
			List<String> attributes, FactoringSignature signature,
			Map<String, String> properties) {
		assert signature != null;
		assert tokens != null;
		assert constants != null;
		assert properties != null;
		assert signature.equals(getSignature(constants,
				signature.getNumAttributes()));
		this.properties = Collections.unmodifiableMap(properties);
		this.attributes = attributes;
		this.constants = Collections.unmodifiableList(constants);
		this.tokens = tokens;
		this.signature = signature;
		this.hashCodeCache = calcHashCode();
	}

	public Lexeme(TokenSeq tokens, List<LogicalConstant> constants,
			List<String> attributes, Map<String, String> properties) {
		this(tokens, constants, attributes, getSignature(constants,
				attributes.size()), properties);
	}

	public static FactoringSignature getSignature(
			List<LogicalConstant> constants, int numAttributes) {
		assert constants != null;
		assert numAttributes >= 0;
		final List<Type> types = new ArrayList<Type>(constants.size());
		for (final LogicalConstant constant : constants) {
			types.add(LogicLanguageServices.getTypeRepository().generalizeType(
					constant.getType()));
		}
		return new FactoringSignature(Collections.unmodifiableList(types),
				numAttributes);
	}

	/**
	 * Given a string, read a lexeme from it.
	 */
	public static Lexeme read(String line, String origin) {
		final String[] split = line.split(" :: ", 3);

		final String[] constantsSplit = split[1].split(", ");
		final List<LogicalConstant> constants = new ArrayList<LogicalConstant>(
				constantsSplit.length);
		for (final String constant : constantsSplit) {
			constants.add(LogicalConstant.read(constant));
		}

		final String[] attributesSplit = split[2].split(", ");
		final List<String> attributes = new ArrayList<String>(
				attributesSplit.length);
		for (final String attribute : attributesSplit) {
			attributes.add(attribute);
		}

		return new Lexeme(TokenSeq.of(split[0].split(" ")), constants,
				attributes, origin == null ? new HashMap<String, String>()
						: MapUtils.createSingletonMap(
								LexicalEntry.ORIGIN_PROPERTY, origin));
	}

	public Lexeme cloneWithProperties(Map<String, String> newProperties) {
		return new Lexeme(tokens, constants, attributes, signature,
				newProperties);
	}

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
		final Lexeme other = (Lexeme) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
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

	public List<String> getAttributes() {
		return attributes;
	}

	public List<LogicalConstant> getConstants() {
		return constants;
	}

	/**
	 * Note the comment on {@link LexicalEntry#ORIGIN_PROPERTY}.
	 *
	 * @see LexicalEntry#ORIGIN_PROPERTY
	 * @return
	 */
	public String getOrigin() {
		return getProperty(LexicalEntry.ORIGIN_PROPERTY);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.get(key);
	}

	public FactoringSignature getSignature() {
		return signature;
	}

	public TokenSeq getTokens() {
		return tokens;
	}

	@Override
	public int hashCode() {
		return hashCodeCache;
	}

	public int numConstants() {
		return constants.size();
	}

	@Override
	public String toString() {
		return new StringBuilder(tokens.toString()).append(" :: ")
				.append(ListUtils.join(attributes, ", ")).append(" :: ")
				.append(ListUtils.join(constants, ", ")).toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (attributes == null ? 0 : attributes.hashCode());
		result = prime * result
				+ (constants == null ? 0 : constants.hashCode());
		result = prime * result + (tokens == null ? 0 : tokens.hashCode());
		return result;
	}

}
