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
package edu.uw.cs.lil.tiny.parser.ccg.cky.chart;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.utils.filter.IFilter;

/**
 * Factory for {@link Cell} objects.
 * 
 * @author Yoav Artzi
 */
public class CellFactory<Y> extends AbstractCellFactory<Y> {
	
	public CellFactory(IDataItemModel<Y> model, int sentenceSize,
			IFilter<Category<Y>> completeParseFilter) {
		super(model, sentenceSize, completeParseFilter);
	}
	
	@Override
	protected Cell<Y> doCreate(Category<Y> category, Cell<Y> child,
			boolean completeSpan, boolean fullParse, String ruleName) {
		return new Cell<Y>(category, ruleName, child, completeSpan, fullParse);
	}
	
	@Override
	protected Cell<Y> doCreate(Category<Y> category, Cell<Y> leftChild,
			Cell<Y> rightChild, boolean completeSpan, boolean fullParse,
			String ruleName) {
		return new Cell<Y>(category, ruleName, leftChild, rightChild,
				completeSpan, fullParse);
	}
	
	@Override
	protected Cell<Y> doCreate(LexicalEntry<Y> lexicalEntry, int begin,
			int end, boolean completeSpan, boolean fullParse) {
		return new Cell<Y>(lexicalEntry, begin, end, completeSpan, fullParse);
	}
}
