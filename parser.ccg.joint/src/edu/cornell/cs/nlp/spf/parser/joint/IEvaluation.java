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
package edu.cornell.cs.nlp.spf.parser.joint;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.parser.IDerivation;

/**
 * Wraps an evaluation result to abstract the model signature from the joint
 * output. This interface takes a similar role to {@link IDerivation}.
 * 
 * @author Yoav Artzi
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public interface IEvaluation<ERESULT> {
	
	/**
	 * The features for this evaluation, including all steps.
	 */
	IHashVectorImmutable getFeatures();
	
	/**
	 * The final result of this evaluation.
	 */
	ERESULT getResult();
	
	/**
	 * The score of this evaluation, according to the model at time of
	 * construction.
	 */
	double getScore();
	
}
