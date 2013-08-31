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
package edu.uw.cs.lil.tiny.parser.joint;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.utils.composites.Pair;

/**
 * @author Yoav Artzi
 * @param <LANG>
 *            Type of the natural language structure (e.g., Sentence).
 * @param <STATE>
 *            World state (e.g., task, starting position, state of the world
 *            etc.).
 * @param <MR>
 *            Type of the semantics (e.g., LogicalExpression).
 * @param <ESTEP>
 *            Execution step.
 * @param <ERESULT>
 *            Execution result.
 */
public interface IJointParser<LANG, STATE, MR, ESTEP, ERESULT> extends
		IParser<LANG, MR> {
	
	IJointOutput<MR, ERESULT> parse(IDataItem<Pair<LANG, STATE>> dataItem,
			IJointDataItemModel<MR, ESTEP> model);
	
	IJointOutput<MR, ERESULT> parse(IDataItem<Pair<LANG, STATE>> dataItem,
			IJointDataItemModel<MR, ESTEP> model, boolean allowWordSkipping);
	
	IJointOutput<MR, ERESULT> parse(IDataItem<Pair<LANG, STATE>> dataItem,
			IJointDataItemModel<MR, ESTEP> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon);
	
	IJointOutput<MR, ERESULT> parse(IDataItem<Pair<LANG, STATE>> dataItem,
			IJointDataItemModel<MR, ESTEP> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon, Integer beamSize);
	
}
