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

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;

public abstract class AbstractParser<X, Y> implements IParser<X, Y> {
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem, IDataItemModel<Y> model) {
		return parse(dataItem, model, false);
	}
	
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}
	
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping,
			ILexicon<Y> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, null);
	}
	
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping,
			ILexicon<Y> tempLexicon, Integer beamSize) {
		return parse(dataItem, null, model, allowWordSkipping, tempLexicon,
				beamSize);
	}
	
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem, Pruner<X, Y> pruner,
			IDataItemModel<Y> model) {
		return parse(dataItem, pruner, model, false);
	}
	
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem, Pruner<X, Y> pruner,
			IDataItemModel<Y> model, boolean allowWordSkipping) {
		return parse(dataItem, pruner, model, allowWordSkipping, null);
	}
	
	@Override
	public IParserOutput<Y> parse(IDataItem<X> dataItem, Pruner<X, Y> pruner,
			IDataItemModel<Y> model, boolean allowWordSkipping,
			ILexicon<Y> tempLexicon) {
		return parse(dataItem, pruner, model, allowWordSkipping, tempLexicon,
				null);
	}
}
