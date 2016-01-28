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
package edu.cornell.cs.nlp.spf.parser.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;

/**
 * A single parse step: holds a parent and its children, plus the rule that
 * created them and a full-parse flag.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public interface IParseStep<MR> {

	/**
	 * Get child i of the parse step (usually, i=0..1).
	 */
	Category<MR> getChild(int i);

	/**
	 * The end index of the span of this step.
	 */
	int getEnd();

	/**
	 * The category at the root of the parse step.
	 */
	Category<MR> getRoot();

	/**
	 * {@link RuleName} for the rule generating this step.
	 */
	RuleName getRuleName();

	/**
	 * The start index of the span of this step.
	 */
	int getStart();

	/**
	 * Indicates if the root category represents a complete parse.
	 *
	 * @return
	 */
	boolean isFullParse();

	/**
	 * The number of children participating in this parse step.
	 */
	int numChildren();

	String toString(boolean verbose, boolean recursive);
}
