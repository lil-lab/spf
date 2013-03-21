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
package edu.uw.cs.lil.tiny.parser;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;

/**
 * A parser for sentences {@link Sentence}.
 * 
 * @author Yoav Artzi
 * @param <LANG>
 *            The representation of the input
 * @param <LF>
 *            The representation of the output of the parse.
 */
public interface IParser<LANG, LF> {
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, IDataItemModel<LF> model);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, IDataItemModel<LF> model,
			boolean allowWordSkipping);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, IDataItemModel<LF> model,
			boolean allowWordSkipping, ILexicon<LF> tempLexicon);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, IDataItemModel<LF> model,
			boolean allowWordSkipping, ILexicon<LF> tempLexicon, Integer beamSize);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, Pruner<LANG, LF> pruner,
			IDataItemModel<LF> model);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, Pruner<LANG, LF> pruner,
			IDataItemModel<LF> model, boolean allowWordSkipping);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, Pruner<LANG, LF> pruner,
			IDataItemModel<LF> model, boolean allowWordSkipping,
			ILexicon<LF> tempLexicon);
	
	IParserOutput<LF> parse(IDataItem<LANG> dataItem, Pruner<LANG, LF> pruner,
			IDataItemModel<LF> model, boolean allowWordSkipping,
			ILexicon<LF> tempLexicon, Integer beamSize);
	
}
