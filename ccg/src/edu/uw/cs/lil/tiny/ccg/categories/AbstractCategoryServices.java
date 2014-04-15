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
package edu.uw.cs.lil.tiny.ccg.categories;

import java.util.Stack;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Abstract generic category services class to supply a base to LF-specific
 * category services classes.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public abstract class AbstractCategoryServices<MR> implements
		ICategoryServices<MR> {
	
	public static final ILogger	LOG				= LoggerFactory
														.create(AbstractCategoryServices.class);
	// Syntax constants
	private static final char	CLOSE_PAREN		= ')';
	private static final String	CLOSE_PAREN_STR	= String.valueOf(CLOSE_PAREN);
	private static final char	OPEN_PAREN		= '(';
	
	private static final String	OPEN_PAREN_STR	= String.valueOf(OPEN_PAREN);
	
	private final boolean		restrictiveCompositionDirection;
	
	public AbstractCategoryServices() {
		this(true);
	}
	
	public AbstractCategoryServices(boolean restrictiveCompositionDirection) {
		this.restrictiveCompositionDirection = restrictiveCompositionDirection;
		LOG.info("Init :: %s:  restrictiveCompositionDirection=%s",
				AbstractCategoryServices.class.getSimpleName(),
				restrictiveCompositionDirection);
	}
	
	@Override
	public final Category<MR> apply(ComplexCategory<MR> function,
			Category<MR> argument) {
		if (argument == null || argument.getSem() == null
				|| function.getSem() == null) {
			return null;
		}
		if (function.getSlash() == Slash.VERTICAL) {
			return null;
		}
		if (function.getSyntax().getRight().equals(argument.getSyntax())) {
			final MR newSemantics = apply(function.getSem(), argument.getSem());
			if (newSemantics != null) {
				return Category.create(function.getSyntax().getLeft(),
						newSemantics);
			}
		}
		return null;
	}
	
	@Override
	public final Category<MR> compose(ComplexCategory<MR> primary,
			ComplexCategory<MR> secondary, int order) {
		// Some basic checks.
		if (primary.getSlash() == Slash.VERTICAL) {
			return null;
		}
		
		if (secondary.getSem() == null || primary.getSem() == null) {
			return null;
		}
		
		// Grab all components from the primary.
		final Slash primarySlash = primary.getSlash();
		final Syntax primaryYieldSyntax = primary.getSyntax().getLeft();
		final Syntax primaryArgSyntax = primary.getSyntax().getRight();
		
		// Iterate through secondary until the requested order.
		final Stack<Syntax> secondarySyntaxStack = new Stack<Syntax>();
		final Stack<Slash> secondarySlashStack = new Stack<Slash>();
		ComplexSyntax currentSecondarySyntax = secondary.getSyntax();
		for (int i = 0; i < order; ++i) {
			if ((restrictiveCompositionDirection && !currentSecondarySyntax
					.getSlash().equals(primarySlash))
					|| !(currentSecondarySyntax.getLeft() instanceof ComplexSyntax)) {
				// Case composition in this direction is not possible or the
				// secondary is not of high enough order.
				return null;
			}
			secondarySyntaxStack.push(currentSecondarySyntax.getRight());
			secondarySlashStack.push(currentSecondarySyntax.getSlash());
			currentSecondarySyntax = (ComplexSyntax) currentSecondarySyntax
					.getLeft();
		}
		
		// Reached the appropriate depth in the secondary syntax.
		if (currentSecondarySyntax.getLeft().equals(primaryArgSyntax)
				&& (!restrictiveCompositionDirection || currentSecondarySyntax
						.getSlash().equals(primarySlash))) {
			// Case the syntax matches. Try to compose the semantics.
			final MR newSemantics = compose(primary.getSem(),
					secondary.getSem(), order);
			
			if (newSemantics == null) {
				// Semantics composition failed.
				return null;
			} else {
				// Create the composed syntax. Pop from the stack to
				// gradually create the new syntax object.
				ComplexSyntax newSyntax = new ComplexSyntax(primaryYieldSyntax,
						currentSecondarySyntax.getRight(),
						currentSecondarySyntax.getSlash());
				while (!secondarySyntaxStack.isEmpty()) {
					newSyntax = new ComplexSyntax(newSyntax,
							secondarySyntaxStack.pop(),
							secondarySlashStack.pop());
				}
				
				return new ComplexCategory<MR>(newSyntax, newSemantics);
			}
		}
		
		return null;
	}
	
	public ComplexCategory<MR> createComplexCategory(String syntaxString,
			MR semantics) {
		// find the outermost slash
		// assumes that one exists
		int depth = 0;
		char c;
		syntaxString = syntaxString.trim();
		if (syntaxString.startsWith(OPEN_PAREN_STR)
				&& syntaxString.endsWith(CLOSE_PAREN_STR)) {
			// check if we need to strip them
			boolean trim = true;
			depth = 0;
			for (int i = 0; i < syntaxString.length() - 1; i++) {
				c = syntaxString.charAt(i);
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
				syntaxString = syntaxString.substring(1,
						syntaxString.length() - 1);
			}
			
		}
		depth = 0;
		Slash latestSlash = null;
		int latestSlashPosition = -1;
		for (int i = 0; i < syntaxString.length(); i++) {
			c = syntaxString.charAt(i);
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
					+ syntaxString);
		}
		
		return new ComplexCategory<MR>(new ComplexSyntax(parse(
				syntaxString.substring(0, latestSlashPosition)).getSyntax(),
				parse(
						syntaxString.substring(latestSlashPosition + 1,
								syntaxString.length())).getSyntax(),
				latestSlash), semantics);
	}
	
	@Override
	public final Category<MR> parse(String string) {
		String trimmed = string.trim();
		
		final int colon = trimmed.indexOf(':');
		
		// Everything after the colon is semantics
		final MR semantics;
		if (colon != -1) {
			semantics = parseSemantics(trimmed.substring(colon + 1,
					trimmed.length()).trim());
			trimmed = trimmed.substring(0, colon);
		} else {
			semantics = null;
		}
		
		// ComplexCategories have slashes, SimpleCategories don't
		if (trimmed.indexOf('\\') != -1 || trimmed.indexOf('/') != -1
				|| trimmed.indexOf('|') != -1) {
			return createComplexCategory(trimmed, semantics);
		} else {
			return new SimpleCategory<MR>(Syntax.valueOf(trimmed.trim()),
					semantics);
		}
	}
	
	/**
	 * Parse the semantics from the given string. Checks that new expression is
	 * well typed after loading.
	 */
	@Override
	public MR parseSemantics(String string) {
		return parseSemantics(string, true);
	}
	
	/**
	 * Parse the semantics from the given string.
	 */
	public abstract MR parseSemantics(String string, boolean checkType);
	
}
