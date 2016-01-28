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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.nlp.utils.collections.SetUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import jregex.Matcher;
import jregex.Pattern;

/**
 * Syntax symbol.
 *
 * @author Yoav Artzi
 */
public abstract class Syntax implements Serializable {
	public static final SimpleSyntax				ADJ					= new SimpleSyntax(
																				"ADJ");
	public static final SimpleSyntax				AP					= new SimpleSyntax(
																				"AP");
	public static final SimpleSyntax				C					= new SimpleSyntax(
																				"C");
	public static final SimpleSyntax				DEG					= new SimpleSyntax(
																				"DEG");
	public static final SimpleSyntax				EMPTY				= new SimpleSyntax(
																				"EMPTY");

	public static final ILogger						LOG					= LoggerFactory
																				.create(Syntax.class);
	public static final SimpleSyntax				N					= new SimpleSyntax(
																				"N");
	public static final SimpleSyntax				NP					= new SimpleSyntax(
																				"NP");

	public static final SimpleSyntax				PP					= new SimpleSyntax(
																				"PP");

	public static final SimpleSyntax				PUNCT				= new SimpleSyntax(
																				"PUNCT");

	public static final SimpleSyntax				S					= new SimpleSyntax(
																				"S");

	public static final String						VARIABLE_ATTRIBUTE	= "x";
	private static final String						NO_ATTRIBUTE		= "none";

	private static final long						serialVersionUID	= -3852094966016976417L;

	private static final Map<String, SimpleSyntax>	STRING_MAPPING		= new HashMap<String, SimpleSyntax>();

	static {
		register(AP);
		register(NP);
		register(EMPTY);
		register(PP);
		register(S);
		register(C);
		register(N);
		register(DEG);
		register(ADJ);
		register(PUNCT);
	}

	public static Collection<SimpleSyntax> getAllSimpleSyntax() {
		return Collections.unmodifiableCollection(STRING_MAPPING.values());
	}

	public static Syntax read(String string) {
		if (string.indexOf('\\') != -1 || string.indexOf('/') != -1
				|| string.indexOf('|') != -1) {
			return ComplexSyntax.read(string);
		} else {
			return SimpleSyntax.read(string);
		}

	}

	/**
	 * Register a new syntactic category to be read.
	 */
	public static void register(SimpleSyntax syntax) {
		if (STRING_MAPPING.containsKey(syntax.toString())) {
			throw new IllegalStateException("Can't overwrite a syntactic type");
		}
		STRING_MAPPING.put(syntax.toString(), syntax);
	}

	private static Syntax valueOf(String string) {
		return STRING_MAPPING.get(string);
	}

	/**
	 * Return 'true' if the given syntax is a sub-syntax of the current, or
	 * equals it.
	 */
	public abstract boolean containsSubSyntax(Syntax other);

	@Override
	public abstract boolean equals(Object obj);

	public abstract Set<String> getAttributes();

	public abstract boolean hasAttributeVariable();

	@Override
	public abstract int hashCode();

	public abstract int numArguments();

	public abstract int numSlashes();

	/**
	 * Replace the given current syntax with the replacement.
	 */
	public abstract Syntax replace(Syntax current, Syntax replacement);

	/**
	 * @param attribute
	 *            Attribute to replace, may be null.
	 * @param replacement
	 *            Replace attribute, may be null.
	 */
	public abstract Syntax replaceAttribute(String attribute, String replacement);

	/**
	 * Sets any attribute variable to the provided value.
	 */
	public abstract Syntax setVariable(String assignment);

	/**
	 * Set all attributes to the 'null' attribute.
	 */
	public abstract Syntax stripAttributes();

	/**
	 * Remove all variables mentions. Variable mentions are replaced with a
	 * 'null' attribute.
	 */
	public abstract Syntax stripVariables();

	@Override
	public abstract String toString();

	/**
	 * Unifies this syntax object with another.
	 *
	 * @return the unification result and a mapping of variables in this object
	 *         to attribute strings.
	 */
	public Unification unify(Syntax other) {
		if (this.numSlashes() != other.numSlashes()) {
			return null;
		}

		// Start with no helper. The helper structure will be created only when
		// needed for the first time.
		final UnificationHelper helper = unify(other, null);

		return helper == null ? null : new Unification(helper.result,
				VARIABLE_ATTRIBUTE.equals(helper.thisVariableAssignment) ? null
						: helper.thisVariableAssignment);
	}

	protected abstract UnificationHelper unify(Syntax other,
			UnificationHelper helper);

	public static class SimpleSyntax extends Syntax {
		private static final long		serialVersionUID	= -135172425101408895L;
		private final static Pattern	STRING_PATTERN		= new Pattern(
																	"({name}[A-Z]+)((\\[({attrib}.+)\\])|())");
		private final String			attribute;
		private final int				hashCode;
		private final String			label;

		public SimpleSyntax(String label) {
			this(label, NO_ATTRIBUTE);
		}

		public SimpleSyntax(String label, String attribute) {
			assert label != null;
			assert attribute != null
					&& attribute.equals(NO_ATTRIBUTE) == (attribute == NO_ATTRIBUTE);
			this.label = label;
			this.attribute = attribute;
			this.hashCode = calcHashCode();
		}

		public static SimpleSyntax read(String string) {
			final Matcher matcher = STRING_PATTERN.matcher(string.trim());
			if (matcher.matches()) {
				if (!Syntax.STRING_MAPPING.containsKey(matcher.group("name"))) {
					throw new IllegalArgumentException("Unknown syntax name: "
							+ string);
				}
				if (matcher.isCaptured("attrib")) {
					if (matcher.group("attrib").equals(NO_ATTRIBUTE)) {
						throw new IllegalStateException(
								"'none' is a reserved word: " + string);
					}
					final SimpleSyntax syntax = new SimpleSyntax(
							matcher.group("name"), matcher.group("attrib"));
					if (!SyntaxAttributeTyping.isWellTyped(syntax)) {
						LOG.error("Syntax is not well typed: %s", syntax);
						throw new IllegalStateException(
								"Invalid attributes placements: " + syntax);
					}
					return syntax;
				} else {
					return STRING_MAPPING.get(matcher.group("name"));
				}
			} else {
				throw new IllegalArgumentException("Invalid syntax: " + string);
			}
		}

		public SimpleSyntax cloneWithAttribute(String newAttribute) {
			final SimpleSyntax syntax = new SimpleSyntax(label, newAttribute);
			return SyntaxAttributeTyping.isWellTyped(syntax) ? syntax : null;
		}

		@Override
		public boolean containsSubSyntax(Syntax other) {
			return equals(other);
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
			final SimpleSyntax other = (SimpleSyntax) obj;
			if (!attribute.equals(other.attribute)) {
				return false;
			}
			if (!label.equals(other.label)) {
				return false;
			}
			return true;
		}

		public String getAttribute() {
			return attribute == NO_ATTRIBUTE ? null : attribute;
		}

		@Override
		public Set<String> getAttributes() {
			return attribute == NO_ATTRIBUTE
					|| attribute.equals(VARIABLE_ATTRIBUTE) ? Collections
					.<String> emptySet() : SetUtils.createSingleton(attribute);
		}

		public String getLabel() {
			return label;
		}

		@Override
		public boolean hasAttributeVariable() {
			return attribute.equals(VARIABLE_ATTRIBUTE);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public int numArguments() {
			return 0;
		}

		@Override
		public int numSlashes() {
			return 0;
		}

		@Override
		public Syntax replace(Syntax current, Syntax replacement) {
			if (current.equals(this)) {
				return replacement;
			} else {
				return this;
			}
		}

		@Override
		public Syntax replaceAttribute(String current, String replacement) {
			if (attribute == NO_ATTRIBUTE && current == null
					|| attribute.equals(current)) {
				return cloneWithAttribute(replacement);
			} else {
				return this;
			}
		}

		@Override
		public SimpleSyntax setVariable(String assignment) {
			if (!VARIABLE_ATTRIBUTE.equals(attribute) || assignment == null) {
				return this;
			} else {
				return cloneWithAttribute(assignment);
			}
		}

		@Override
		public Syntax stripAttributes() {
			if (attribute == NO_ATTRIBUTE) {
				return this;
			} else {
				return cloneWithAttribute(NO_ATTRIBUTE);
			}
		}

		@Override
		public Syntax stripVariables() {
			if (attribute.equals(VARIABLE_ATTRIBUTE)) {
				return cloneWithAttribute(NO_ATTRIBUTE);
			} else {
				return this;
			}
		}

		@Override
		public String toString() {
			if (attribute == NO_ATTRIBUTE) {
				return label;
			} else {
				return new StringBuilder(label).append("[").append(attribute)
						.append("]").toString();
			}
		}

		private int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (label == null ? 0 : label.hashCode());
			result = prime * result
					+ (attribute == null ? 0 : attribute.hashCode());
			return result;
		}

		/**
		 * Resolve to one of the static members.
		 *
		 * @throws ObjectStreamException
		 */
		protected Object readResolve() throws ObjectStreamException {
			if (attribute.equals(NO_ATTRIBUTE)) {
				return valueOf(getLabel());
			} else {
				return this;
			}
		}

		@Override
		protected UnificationHelper unify(Syntax other, UnificationHelper helper) {
			UnificationHelper localHelper = helper;
			if (other instanceof SimpleSyntax) {
				final SimpleSyntax otherSimple = (SimpleSyntax) other;
				if (label.equals(otherSimple.label)) {
					// Unify the attribute.
					if (attribute == NO_ATTRIBUTE) {
						if (otherSimple.attribute == null) {
							if (localHelper == null) {
								localHelper = new UnificationHelper();
							}
							localHelper.result = this;
							return localHelper;
						} else if (otherSimple.attribute
								.equals(VARIABLE_ATTRIBUTE)) {
							// Case other is variable.
							if (localHelper != null
									&& localHelper.otherVariableAssignment != null) {
								// If the assignment matches.
								if (localHelper.otherVariableAssignment == NO_ATTRIBUTE) {
									localHelper.result = this;
									return localHelper;
								} else {
									return null;
								}
							} else {
								// Create an assignment
								if (localHelper == null) {
									localHelper = new UnificationHelper();
								}
								localHelper.otherVariableAssignment = NO_ATTRIBUTE;
								localHelper.result = this;
								return localHelper;
							}
						} else {
							if (localHelper == null) {
								localHelper = new UnificationHelper();
							}
							localHelper.result = otherSimple;
							return localHelper;
						}
					} else if (otherSimple.attribute == NO_ATTRIBUTE) {
						if (attribute.equals(VARIABLE_ATTRIBUTE)) {
							// Case variable attribute and other has no
							// attribute.
							if (localHelper != null
									&& localHelper.thisVariableAssignment != null) {
								if (localHelper.thisVariableAssignment == NO_ATTRIBUTE) {
									localHelper.result = otherSimple;
									return localHelper;
								} else {
									return null;
								}
							} else {
								// Create an assignment.
								if (localHelper == null) {
									localHelper = new UnificationHelper();
								}
								localHelper.thisVariableAssignment = NO_ATTRIBUTE;
								localHelper.result = otherSimple;
								return localHelper;
							}
						} else {
							if (localHelper == null) {
								localHelper = new UnificationHelper();
							}
							localHelper.result = this;
							return localHelper;
						}
					} else if (attribute.equals(VARIABLE_ATTRIBUTE)) {
						// Case attribute is a variable. Other's attribute is
						// set
						// too.
						if (localHelper != null
								&& localHelper.thisVariableAssignment != null) {
							if (localHelper.thisVariableAssignment
									.equals(otherSimple.attribute)) {
								localHelper.result = otherSimple;
								return localHelper;
							} else {
								return null;
							}
						} else {
							if (localHelper == null) {
								localHelper = new UnificationHelper();
							}
							localHelper.thisVariableAssignment = otherSimple.attribute;
							localHelper.result = otherSimple;
							return localHelper;
						}
					} else if (otherSimple.attribute.equals(VARIABLE_ATTRIBUTE)) {
						// Case other's attribute is a variable. This attribute
						// is
						// not a variable.
						if (localHelper != null
								&& localHelper.otherVariableAssignment != null) {
							if (localHelper.otherVariableAssignment
									.equals(attribute)) {
								localHelper.result = this;
								return localHelper;
							} else {
								return null;
							}
						} else {
							if (localHelper == null) {
								localHelper = new UnificationHelper();
							}
							localHelper.otherVariableAssignment = attribute;
							localHelper.result = this;
							return localHelper;
						}
					} else if (attribute.equals(otherSimple.attribute)) {
						// Case both are not attribute and both are set.
						if (localHelper == null) {
							localHelper = new UnificationHelper();
						}
						localHelper.result = this;
						return localHelper;
					}
				}
			}
			return null;
		}
	}

	public static class Unification {
		private final Syntax	unifiedSyntax;
		private final String	variableAssignment;

		public Unification(Syntax unifiedSyntax, String variableAssignment) {
			this.unifiedSyntax = unifiedSyntax;
			this.variableAssignment = variableAssignment;
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
			final Unification other = (Unification) obj;
			if (unifiedSyntax == null) {
				if (other.unifiedSyntax != null) {
					return false;
				}
			} else if (!unifiedSyntax.equals(other.unifiedSyntax)) {
				return false;
			}
			if (variableAssignment == null) {
				if (other.variableAssignment != null) {
					return false;
				}
			} else if (!variableAssignment.equals(other.variableAssignment)) {
				return false;
			}
			return true;
		}

		public Syntax getUnifiedSyntax() {
			return unifiedSyntax;
		}

		public String getVariableAssignment() {
			return variableAssignment;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (unifiedSyntax == null ? 0 : unifiedSyntax.hashCode());
			result = prime
					* result
					+ (variableAssignment == null ? 0 : variableAssignment
							.hashCode());
			return result;
		}

		public boolean isVariableAssigned() {
			return variableAssignment != null
					&& !VARIABLE_ATTRIBUTE.equals(variableAssignment);
		}

	}

	protected static class UnificationHelper {
		String	otherVariableAssignment	= null;
		Syntax	result					= null;
		String	thisVariableAssignment	= null;
	}

	protected static class VariableAssignment {
		public String	assignment	= null;

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
			final VariableAssignment other = (VariableAssignment) obj;
			if (assignment == null) {
				if (other.assignment != null) {
					return false;
				}
			} else if (!assignment.equals(other.assignment)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (assignment == null ? 0 : assignment.hashCode());
			return result;
		}
	}

}
