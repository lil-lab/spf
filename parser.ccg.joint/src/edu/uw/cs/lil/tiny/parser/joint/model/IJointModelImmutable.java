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
package edu.uw.cs.lil.tiny.parser.joint.model;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;

/**
 * Immutable model for joint inference.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Data item for inference.
 * @param <MR>
 *            Meaning representation.
 * @param <ESTEP>
 *            Execution step.
 */
public interface IJointModelImmutable<DI extends ISituatedDataItem<?, ?>, MR, ESTEP>
		extends IModelImmutable<DI, MR> {
	
	/**
	 * Compute feature over execution and logical form.
	 * 
	 * @param result
	 * @param dataItem
	 * @return
	 */
	IHashVector computeFeatures(ESTEP executionStep, DI dataItem);
	
	IJointDataItemModel<MR, ESTEP> createJointDataItemModel(DI dataItem);
	
	/**
	 * Score execution and logical form pair.
	 * 
	 * @param result
	 * @param dataItem
	 * @return
	 */
	double score(ESTEP executionStep, DI dataItem);
}
