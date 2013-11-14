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
package edu.uw.cs.lil.tiny.parser;

import edu.uw.cs.lil.tiny.data.ILossDataItem;
import edu.uw.cs.utils.filter.IFilter;

/**
 * Given a {@link ILossDataItem} wraps its loss and pruning abilities to
 * estimate candidate meaning representations during parsing. This object is
 * instantiated for a single data item and must be passed to the parser for
 * evaluating candidates meaning representations.
 * 
 * @author Yoav Artzi
 */
public class Pruner<DI extends ILossDataItem<?, MR>, MR> implements IFilter<MR> {
	
	private final DI	dataItem;
	
	private Pruner(DI dataItem) {
		this.dataItem = dataItem;
	}
	
	public static <DI extends ILossDataItem<?, MR>, MR> Pruner<DI, MR> create(
			DI dataItem) {
		return new Pruner<DI, MR>(dataItem);
	}
	
	@Override
	public boolean isValid(MR y) {
		return !dataItem.prune(y);
	}
}
