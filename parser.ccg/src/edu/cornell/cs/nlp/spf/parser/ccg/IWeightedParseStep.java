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

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;

public interface IWeightedParseStep<MR> extends IParseStep<MR> {

	/**
	 * Local step features.
	 */
	public IHashVector getStepFeatures();

	/**
	 * The linear score of this step.
	 */
	public double getStepScore();

	/**
	 * Returns the un-weighted parse step that underlies this weighted step.
	 */
	public IParseStep<MR> getUnderlyingParseStep();

	/**
	 * Create a string representation of the step and its features.
	 *
	 * @param verbose
	 *            Append child strings (more than just spans).
	 * @param recursive
	 *            Create string representation of child cells (rather than
	 *            hashcodes)
	 * @param theta
	 *            Model parameters, to include with active features.
	 */
	public String toString(boolean verbose, boolean recursive,
			IHashVectorImmutable theta);

}
