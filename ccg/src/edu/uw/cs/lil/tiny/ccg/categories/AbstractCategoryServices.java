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

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;

/**
 * Abstract generic category services class to supply a base to LF-specific
 * category services classes.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public abstract class AbstractCategoryServices<Y> implements
		ICategoryServices<Y> {
	
	// Syntax constants
	private static final char	CLOSE_PAREN		= ')';
	private static final String	CLOSE_PAREN_STR	= String.valueOf(CLOSE_PAREN);
	private static final char	OPEN_PAREN		= '(';
	private static final String	OPEN_PAREN_STR	= String.valueOf(OPEN_PAREN);
	
	@Override
	public final Category<Y> apply(ComplexCategory<Y> function,
			Category<Y> argument) {
		if (argument == null || argument.getSem() == null
				|| function.getSem() == null) {
			return null;
		}
		if (function.getSlash() == Slash.VERTICAL) {
			return null;
		}
		if (function.getSyntax().getRight().equals(argument.getSyntax())) {
			final Y newSemantics = doSemanticApplication(function.getSem(),
					argument.getSem());
			if (newSemantics != null) {
				return Category.create(function.getSyntax().getLeft(),
						newSemantics);
			}
		}
		return null;
	}
	
	@Override
	public final Category<Y> compose(ComplexCategory<Y> fCategory,
			ComplexCategory<Y> gCategory) {
		if (gCategory == null) {
			return null;
		}
		
		if (fCategory.getSlash() == Slash.VERTICAL
				|| gCategory.getSlash() == Slash.VERTICAL) {
			return null;
		}
		
		if (gCategory.getSem() == null || fCategory.getSem() == null) {
			return null;
		}
		
		if (fCategory.getSyntax().getRight()
				.equals(gCategory.getSyntax().getLeft())) {
			final Y newSemantics = doSemanticComposition(fCategory.getSem(),
					gCategory.getSem());
			if (newSemantics != null) {
				
				// Put composition direction on cell to enable Eisner normal
				// form later on
				final boolean newCategoryFromRightComp;
				final boolean newCategoryFromLeftComp;
				if (fCategory.getSlash() == Slash.FORWARD) {
					newCategoryFromRightComp = true;
					newCategoryFromLeftComp = false;
				} else {
					newCategoryFromLeftComp = true;
					newCategoryFromRightComp = false;
				}
				
				final ComplexCategory<Y> newcat = new ComplexCategory<Y>(
						new ComplexSyntax(fCategory.getSyntax().getLeft(),
								gCategory.getSyntax().getRight(),
								fCategory.getSlash()), newSemantics,
						newCategoryFromLeftComp, newCategoryFromRightComp);
				return newcat;
			}
		}
		return null;
	}
	
	public ComplexCategory<Y> createComplexCategory(String syntaxString,
			Y semantics) {
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
		
		return new ComplexCategory<Y>(new ComplexSyntax(parse(
				syntaxString.substring(0, latestSlashPosition)).getSyntax(),
				parse(
						syntaxString.substring(latestSlashPosition + 1,
								syntaxString.length())).getSyntax(),
				latestSlash), semantics, false, false);
	}
	
	@Override
	public final Category<Y> parse(String string) {
		String trimmed = string.trim();
		
		final int colon = trimmed.indexOf(':');
		
		// Everything after the colon is semantics
		final Y semantics;
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
			return new SimpleCategory<Y>(Syntax.valueOf(trimmed.trim()),
					semantics);
		}
	}
	
	/**
	 * Parse the semantics from the given string. Checks that new expression is
	 * well typed after loading.
	 * 
	 * @param string
	 * @return
	 */
	@Override
	public Y parseSemantics(String string) {
		return parseSemantics(string, true);
	}
	
	/**
	 * Parse the semantics from the given string.
	 * 
	 * @param string
	 * @return
	 */
	public abstract Y parseSemantics(String string, boolean checkType);
	
	/**
	 * Do the application of the semantics.
	 * 
	 * @param function
	 * @param argument
	 * @return
	 */
	protected abstract Y doSemanticApplication(Y function, Y argument);
	
	/**
	 * Do the composition of the semantics.
	 * 
	 * @param f
	 * @param g
	 * @return
	 */
	protected abstract Y doSemanticComposition(Y f, Y g);
	
}
