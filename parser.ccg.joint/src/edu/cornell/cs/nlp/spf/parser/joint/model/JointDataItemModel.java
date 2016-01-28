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
package edu.cornell.cs.nlp.spf.parser.joint.model;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.data.situated.ISituatedDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.model.DataItemModel;

/**
 * Model for inference with a specific data item.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Inference data item.
 * @param <MR>
 *            Meaning representation.
 * @param <ESTEP>
 *            Execution step.
 */
public class JointDataItemModel<DI extends ISituatedDataItem<?, ?>, MR, ESTEP>
		extends DataItemModel<DI, MR> implements IJointDataItemModel<MR, ESTEP> {
	
	private final DI									dataItem;
	private final IJointModelImmutable<DI, MR, ESTEP>	model;
	
	public JointDataItemModel(IJointModelImmutable<DI, MR, ESTEP> model,
			final DI dataItem) {
		super(model, dataItem);
		this.model = model;
		this.dataItem = dataItem;
	}
	
	@Override
	public IHashVector computeFeatures(ESTEP executionStep) {
		return model.computeFeatures(executionStep, dataItem);
	}
	
	@Override
	public double score(ESTEP executionStep) {
		return model.score(executionStep, dataItem);
	}
}
