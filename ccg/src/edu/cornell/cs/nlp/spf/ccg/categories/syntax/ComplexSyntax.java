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

import java.util.Set;

import edu.cornell.cs.nlp.utils.collections.SetUtils;

public class ComplexSyntax extends Syntax {

	// Syntax constants
	private static final char	CLOSE_PAREN			= ')';

	private static final String	CLOSE_PAREN_STR		= String.valueOf(CLOSE_PAREN);

	private static final char	OPEN_PAREN			= '(';

	private static final String	OPEN_PAREN_STR		= String.valueOf(OPEN_PAREN);

	private static final long	serialVersionUID	= 2647447680294080606L;
	private final int			hashCode;
	private final Syntax		left;

	private final int			numSlahes;
	private final Syntax		right;
	private final Slash			slash;

	public ComplexSyntax(Syntax left, Syntax right, Slash slash) {
		assert left != null : "Missing left component";
		assert right != null : "Missing right component";
		assert slash != null : "Missing slash";
		this.left = left;
		this.right = right;
		this.numSlahes = left.numSlashes() + right.numSlashes() + 1;
		this.slash = slash;
		this.hashCode = calcHashCode();
	}

	/**
	 * Reads {@link ComplexSyntax} from {@link String}.
	 */
	public static ComplexSyntax read(String string) {
		// Find the outermost slash assumes that one exists.
		int depth = 0;
		char c;
		String currentString = string.trim();
		if (currentString.startsWith(OPEN_PAREN_STR)
				&& currentString.endsWith(CLOSE_PAREN_STR)) {
			// check if we need to strip them
			boolean trim = true;
			depth = 0;
			for (int i = 0; i < currentString.length() - 1; i++) {
				c = currentString.charAt(i);
				if (c == OPEN_PAREN) {
					depth++;
				} else if (c == CLOSE_PAREN) {
					depth--;
				}
				if (depth == 0) {
					trim = false;
				}
			}
			if (trim) {
				currentString = currentString.substring(1,
						currentString.length() - 1);
			}

		}
		depth = 0;
		Slash latestSlash = null;
		int latestSlashPosition = -1;
		for (int i = 0; i < currentString.length(); i++) {
			c = currentString.charAt(i);
			if (c == OPEN_PAREN) {
				depth++;
			}
			if (c == CLOSE_PAREN) {
				depth--;
			}
			if (depth == 0 && Slash.getSlash(c) != null) {
				latestSlashPosition = i;
				latestSlash = Slash.getSlash(c);
			}
		}
		if (latestSlash == null) {
			throw new IllegalArgumentException("No outer slash found in "
					+ currentString);
		}

		return new ComplexSyntax(Syntax.read(currentString.substring(0,
				latestSlashPosition)), Syntax.read(currentString.substring(
				latestSlashPosition + 1, currentString.length())), latestSlash);
	}

	@Override
	public boolean containsSubSyntax(Syntax other) {
		if (equals(other)) {
			return true;
		}

		if (getLeft().containsSubSyntax(other)) {
			return true;
		}

		if (getRight().containsSubSyntax(other)) {
			return true;
		}

		return false;
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
		final ComplexSyntax other = (ComplexSyntax) obj;
		if (hashCode != other.hashCode) {
			return false;
		}
		if (numSlahes != other.numSlahes) {
			return false;
		}
		if (!slash.equals(other.slash)) {
			return false;
		}
		if (!left.equals(other.left)) {
			return false;
		}
		if (!right.equals(other.right)) {
			return false;
		}
		return true;
	}

	@Override
	public Set<String> getAttributes() {
		final Set<String> leftAttributes = left.getAttributes();
		final Set<String> rightAttributes = right.getAttributes();
		if (leftAttributes.isEmpty()) {
			return rightAttributes;
		} else if (rightAttributes.isEmpty()) {
			return leftAttributes;
		} else {
			return SetUtils.union(leftAttributes, rightAttributes);
		}
	}

	public Syntax getLeft() {
		return left;
	}

	public Syntax getRight() {
		return right;
	}

	public Slash getSlash() {
		return slash;
	}

	@Override
	public boolean hasAttributeVariable() {
		if (left.hasAttributeVariable()) {
			return true;
		}
		return right.hasAttributeVariable();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public int numArguments() {
		Syntax current = left;
		int count = 1;
		while (current instanceof ComplexSyntax) {
			++count;
			current = ((ComplexSyntax) current).left;
		}
		return count;
	}

	@Override
	public int numSlashes() {
		return numSlahes;
	}

	@Override
	public Syntax replace(Syntax current, Syntax replacement) {
		if (this.equals(current)) {
			return replacement;
		}

		final Syntax strippedLeft = left.replace(current, replacement);
		final Syntax strippedRight = right.replace(current, replacement);
		if (strippedLeft == left && strippedRight == right) {
			return this;
		} else {
			return new ComplexSyntax(strippedLeft, strippedRight, slash);
		}
	}

	@Override
	public Syntax replaceAttribute(String attribute, String replacement) {
		final Syntax setLeft = left.replaceAttribute(attribute, replacement);
		final Syntax setRight = right.replaceAttribute(attribute, replacement);
		if (setLeft == left && setRight == right) {
			return this;
		} else if (setLeft == null || setRight == null) {
			return null;
		} else {
			return new ComplexSyntax(setLeft, setRight, slash);
		}
	}

	@Override
	public Syntax setVariable(String assignment) {
		final Syntax setLeft = left.setVariable(assignment);
		final Syntax setRight = right.setVariable(assignment);
		if (setLeft == left && setRight == right) {
			return this;
		} else if (setLeft == null || setRight == null) {
			return null;
		} else {
			return new ComplexSyntax(setLeft, setRight, slash);
		}
	}

	@Override
	public Syntax stripAttributes() {
		final Syntax strippedLeft = left.stripAttributes();
		final Syntax strippedRight = right.stripAttributes();
		if (strippedLeft == left && strippedRight == right) {
			return this;
		} else {
			return new ComplexSyntax(strippedLeft, strippedRight, slash);
		}
	}

	@Override
	public Syntax stripVariables() {
		final Syntax strippedLeft = left.stripVariables();
		final Syntax strippedRight = right.stripVariables();
		if (strippedLeft == left && strippedRight == right) {
			return this;
		} else {
			return new ComplexSyntax(strippedLeft, strippedRight, slash);
		}
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append(left);
		ret.append(slash);
		if (right instanceof ComplexSyntax) {
			ret.append("(").append(right).append(")");
		} else {
			ret.append(right);
		}
		return ret.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (left == null ? 0 : left.hashCode());
		result = prime * result + (right == null ? 0 : right.hashCode());
		result = prime * result + (slash == null ? 0 : slash.hashCode());
		return result;
	}

	@Override
	protected UnificationHelper unify(Syntax other, UnificationHelper helper) {
		if (other instanceof ComplexSyntax
				&& slash.equals(((ComplexSyntax) other).slash)) {
			final UnificationHelper rightHelper = right.unify(
					((ComplexSyntax) other).right, helper);
			if (rightHelper != null) {
				final Syntax rightUnification = rightHelper.result;
				final UnificationHelper leftHelper = left.unify(
						((ComplexSyntax) other).left, rightHelper);
				if (leftHelper != null) {
					if (leftHelper.result == left && rightUnification == right) {
						leftHelper.result = this;
						return leftHelper;
					} else {
						leftHelper.result = new ComplexSyntax(
								leftHelper.result, rightUnification, slash);
						return leftHelper;
					}
				}
			}
		}
		return null;
	}

}
