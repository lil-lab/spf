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
package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.Unification;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Abstract generic category services class to supply a base to LF-specific
 * category services classes.
 *
 * @author Yoav Artzi
 * @param <MR>
 */
public abstract class AbstractCategoryServices<MR> implements
		ICategoryServices<MR> {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5238221570838411477L;
	public static final ILogger	LOG	= LoggerFactory
											.create(AbstractCategoryServices.class);

	@Override
	public final Category<MR> apply(ComplexCategory<MR> function,
			Category<MR> argument) {
		if (argument == null || argument.getSemantics() == null
				|| function.getSemantics() == null) {
			return null;
		}
		if (function.getSlash() == Slash.VERTICAL) {
			return null;
		}
		final Unification syntaxArgUnification = function.getSyntax()
				.getRight().unify(argument.getSyntax());
		if (syntaxArgUnification != null) {
			final Syntax newSyntax = function.getSyntax().getLeft()
					.setVariable(syntaxArgUnification.getVariableAssignment());
			if (newSyntax != null) {
				final MR newSemantics = apply(function.getSemantics(),
						argument.getSemantics());
				if (newSemantics != null) {
					return Category.create(newSyntax, newSemantics);
				}
			}
		}
		return null;
	}

	@Override
	public final Category<MR> compose(ComplexCategory<MR> primary,
			ComplexCategory<MR> secondary, int order, boolean cross) {
		assert order > 0 : "Order must be at least 1. Zero-order composition is identical to application and should use the apply() method.";
		assert !cross || order == 1 : "Only allow cross composition for first order composition.";

		// Some basic checks.
		if (primary.getSlash() == Slash.VERTICAL) {
			return null;
		}

		if (secondary.getSemantics() == null || primary.getSemantics() == null) {
			return null;
		}

		// Grab all components from the primary.
		final Slash primarySlash = primary.getSlash();
		final Syntax primaryYieldSyntax = primary.getSyntax().getLeft();
		final Syntax primaryArgSyntax = primary.getSyntax().getRight();

		// Iterate through secondary until the requested order.
		final Syntax[] secondarySyntaxStack = new Syntax[order];
		final Slash[] secondarySlashStack = new Slash[order];
		Syntax currentSecondarySyntax = secondary.getSyntax();
		for (int i = 0; i < order; ++i) {
			if (!(currentSecondarySyntax instanceof ComplexSyntax)) {
				// The secondary is not of high enough order.
				return null;
			}

			secondarySyntaxStack[i] = ((ComplexSyntax) currentSecondarySyntax)
					.getRight();
			secondarySlashStack[i] = ((ComplexSyntax) currentSecondarySyntax)
					.getSlash();
			currentSecondarySyntax = ((ComplexSyntax) currentSecondarySyntax)
					.getLeft();
		}

		// For order=1 composition, enforce the directionality of the slash in
		// the secondary.
		if (order == 1) {
			if (cross && primarySlash.equals(secondarySlashStack[0])
					|| secondarySlashStack[0] == Slash.VERTICAL) {
				// For crossing composition, the slashes must lean in opposite
				// directions (\/ or /\).
				return null;
			} else if (!cross && !primarySlash.equals(secondarySlashStack[0])) {
				// For non-crossing composition, the slashes must be identical.
				return null;
			}
		}

		// Reached the appropriate depth in the secondary syntax.
		final Unification syntaxUnification = primaryArgSyntax
				.unify(currentSecondarySyntax);
		if (syntaxUnification != null) {
			final Syntax newYieldSyntax = primaryYieldSyntax
					.setVariable(syntaxUnification.getVariableAssignment());
			if (newYieldSyntax == null) {
				return null;
			}

			// Case the syntax matches. Try to compose the semantics.
			final MR newSemantics = compose(primary.getSemantics(),
					secondary.getSemantics(), order);

			if (newSemantics == null) {
				// Semantics composition failed.
				return null;
			} else {
				// Create the composed syntax. Pop from the stack to
				// gradually create the new syntax object.
				ComplexSyntax newSyntax = new ComplexSyntax(newYieldSyntax,
						secondarySyntaxStack[order - 1],
						secondarySlashStack[order - 1]);
				for (int i = order - 2; i >= 0; --i) {
					newSyntax = new ComplexSyntax(newSyntax,
							secondarySyntaxStack[i], secondarySlashStack[i]);
				}

				return new ComplexCategory<MR>(newSyntax, newSemantics);
			}
		}

		return null;
	}

	@Override
	public final Category<MR> read(String string) {
		String trimmed = string.trim();

		final int colon = trimmed.indexOf(':');

		// Everything after the colon is semantics.
		final MR semantics;
		if (colon != -1) {
			semantics = readSemantics(trimmed.substring(colon + 1,
					trimmed.length()).trim());
			trimmed = trimmed.substring(0, colon);
		} else {
			semantics = null;
		}

		// Read the syntactic component.
		final Syntax syntax = Syntax.read(trimmed);

		return Category.create(syntax, semantics);
	}

	/**
	 * Parse the semantics from the given string. Checks that new expression is
	 * well typed after loading.
	 */
	@Override
	public MR readSemantics(String string) {
		return readSemantics(string, true);
	}

	/**
	 * Parse the semantics from the given string.
	 */
	public abstract MR readSemantics(String string, boolean checkType);

}
