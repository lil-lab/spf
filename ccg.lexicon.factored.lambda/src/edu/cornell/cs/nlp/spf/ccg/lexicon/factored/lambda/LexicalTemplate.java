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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.SyntaxAttributeTyping;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceConstants;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.collections.MapUtils;

public class LexicalTemplate implements Serializable {

	private static final long					serialVersionUID	= 7466276751228011529L;

	private final List<LogicalConstant>			arguments;

	private final int							hashCode;

	private final Map<String, String>			properties;

	private final FactoringSignature			signature;

	private final Category<LogicalExpression>	template;

	LexicalTemplate(List<LogicalConstant> arguments,
			FactoringSignature signature, Category<LogicalExpression> template,
			Map<String, String> properties) {
		assert arguments != null;
		assert template != null;
		assert properties != null;
		assert signature != null;
		this.properties = Collections.unmodifiableMap(properties);
		this.arguments = Collections.unmodifiableList(arguments);
		this.template = template;
		this.signature = signature;
		this.hashCode = calcHashCode();
	}

	/**
	 * Create a template by abstracting all of the constants. NOTE: we are
	 * assuming that every constant in the list appears somewhere in the logical
	 * expression for the category.
	 */
	LexicalTemplate(List<LogicalConstant> arguments, int numAttributes,
			Category<LogicalExpression> template, Map<String, String> properties) {
		this(arguments, computeSignature(arguments, numAttributes), template,
				properties);
	}

	/**
	 * Given a string, read a lexical template from it.
	 */
	public static LexicalTemplate read(String line,
			ICategoryServices<LogicalExpression> categoryServices, String origin) {
		final int index = line.indexOf("-->");
		final String constantsString = line.substring(1, index - 1);
		final List<LogicalConstant> constants = new LinkedList<LogicalConstant>();
		if (!constantsString.equals("")) {
			for (final String constant : constantsString.split(", ")) {
				constants.add(LogicalConstant.read(constant));
			}
		}

		final String categoryString = line.substring(index + 3, line.length());

		final Category<LogicalExpression> category = categoryServices
				.read(categoryString);
		return new LexicalTemplate(constants, category.getSyntax()
				.getAttributes().size(), category, MapUtils.createSingletonMap(
				LexicalEntry.ORIGIN_PROPERTY, origin));
	}

	private static FactoringSignature computeSignature(
			List<LogicalConstant> arguments, int numAttributes) {
		final List<Type> types = new ArrayList<Type>(arguments.size());
		for (final LogicalConstant constant : arguments) {
			assert constant != null;
			types.add(constant.getType());

		}
		return new FactoringSignature(Collections.unmodifiableList(types),
				numAttributes);
	}

	/**
	 * Applies the template to a {@link Lexeme} to create a {@link Category}.
	 */
	public Category<LogicalExpression> apply(Lexeme lexeme) {
		if (!lexeme.getSignature().equals(signature)) {
			return null;
		}

		// Instantiate the syntactic template.
		final Syntax instantiatedSyntax = replacePlaceholders(
				template.getSyntax(), lexeme.getAttributes());
		if (instantiatedSyntax == null) {
			return null;
		}

		// Instantiate the semantic template.
		final int numArgs = arguments.size();
		final Map<LogicalConstant, LogicalExpression> mapping = new HashMap<LogicalConstant, LogicalExpression>(
				numArgs);
		final List<LogicalConstant> constants = lexeme.getConstants();
		for (int i = 0; i < numArgs; ++i) {
			final LogicalConstant arg = arguments.get(i);
			final LogicalConstant constant = constants.get(i);
			assert constant.getType().isExtending(arg.getType());
			mapping.put(arg, constant);
		}

		// This replacement is guaranteed to be type consistent at this point,
		// since we verified type safety above.
		return template.getSemantics() == null ? template : Category.create(
				instantiatedSyntax,
				ReplaceConstants.of(template.getSemantics(), mapping));
	}

	public LexicalTemplate cloneWithNewSyntax(Syntax syntax) {
		return new LexicalTemplate(arguments, syntax.getAttributes().size(),
				Category.create(syntax, template.getSemantics()), properties);
	}

	public LexicalTemplate cloneWithProperties(Map<String, String> newProperties) {
		return new LexicalTemplate(arguments, signature, template,
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
		if (!(obj instanceof LexicalTemplate)) {
			return false;
		}
		final LexicalTemplate other = (LexicalTemplate) obj;
		if (hashCode != other.hashCode) {
			return false;
		}
		if (!arguments.equals(other.arguments)) {
			return false;
		}
		if (!template.equals(other.template)) {
			return false;
		}
		return true;
	}

	public List<LogicalConstant> getArguments() {
		return arguments;
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

	public Category<LogicalExpression> getTemplateCategory() {
		return template;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public boolean isValid(Lexeme lexeme) {
		if (!lexeme.getSignature().equals(signature)) {
			return false;
		}

		// Instantiate the syntactic template.
		final Syntax instantiatedSyntax = replacePlaceholders(
				template.getSyntax(), lexeme.getAttributes());
		if (instantiatedSyntax == null) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return arguments + "-->" + template;
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (arguments == null ? 0 : arguments.hashCode());
		result = prime * result + (template == null ? 0 : template.hashCode());
		return result;
	}

	private Syntax replacePlaceholders(Syntax syntax, List<String> attributes) {
		if (syntax instanceof ComplexSyntax) {
			final ComplexSyntax complexSyntax = (ComplexSyntax) syntax;
			final Syntax left = replacePlaceholders(complexSyntax.getLeft(),
					attributes);
			final Syntax right = replacePlaceholders(complexSyntax.getRight(),
					attributes);
			if (left == null || right == null) {
				return null;
			} else if (left == complexSyntax.getLeft()
					&& right == complexSyntax.getRight()) {
				return syntax;
			} else {
				return new ComplexSyntax(left, right, complexSyntax.getSlash());
			}
		} else if (syntax instanceof SimpleSyntax) {
			final SimpleSyntax simpleSyntax = (SimpleSyntax) syntax;
			if (simpleSyntax.getAttribute() != null
					&& !simpleSyntax.hasAttributeVariable()) {
				final String placeholder = simpleSyntax.getAttribute();
				final int index = FactoringServices
						.attributePlacehoderToIndex(placeholder);
				final String newAttribute = attributes.get(index);
				if (SyntaxAttributeTyping.isValidSyntaxAtrributePairing(
						simpleSyntax, newAttribute)) {
					return simpleSyntax.cloneWithAttribute(newAttribute);
				} else {
					return null;
				}
			} else {
				return syntax;
			}
		} else {
			throw new RuntimeException("Unknown type of syntax");
		}
	}

}
