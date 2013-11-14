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

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;

/**
 * Wraps a situated data item to only expose the language component.
 * 
 * @author Yoav Artzi
 * @param <DI>
 * @param <STATE>
 */
public class SituatedDataItemWrapper<DI extends ISituatedDataItem<Sentence, STATE>, STATE>
		implements IDataItem<Sentence> {
	
	private static final long			serialVersionUID	= 9125402561551010485L;
	private final IDataItem<Sentence>	sample;
	private final DI					situatedDataItem;
	
	public SituatedDataItemWrapper(DI situatedDataItem) {
		this.sample = situatedDataItem.getSample();
		this.situatedDataItem = situatedDataItem;
	}
	
	@Override
	public Sentence getSample() {
		return sample.getSample();
	}
	
	public DI getSituatedDataItem() {
		return situatedDataItem;
	}
	
}
