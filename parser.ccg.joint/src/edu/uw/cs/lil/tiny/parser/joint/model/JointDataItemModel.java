/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.model.DataItemModel;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;

public class JointDataItemModel<LANG, STATE, LF, ESTEP> extends
		DataItemModel<LANG, LF> implements IJointDataItemModel<LF, ESTEP> {
	
	private final IDataItem<Pair<LANG, STATE>>					dataItem;
	private final IJointModelImmutable<LANG, STATE, LF, ESTEP>	model;
	
	public JointDataItemModel(
			IJointModelImmutable<LANG, STATE, LF, ESTEP> model,
			final IDataItem<Pair<LANG, STATE>> dataItem) {
		super(model, new JointDataItemWrapper<LANG, STATE>(dataItem.getSample()
				.first(), dataItem));
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
