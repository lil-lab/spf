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
package edu.cornell.cs.nlp.spf.parser;

import java.io.Serializable;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;

/**
 * A parser for sentences {@link Sentence}.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Inference data item.
 * @param <MR>
 *            Meaning representation.
 */
public interface IParser<DI extends IDataItem<?>, MR> extends Serializable {

	default IParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model) {
		return parse(dataItem, model, false);
	}

	default IParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}

	default IParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexiconImmutable<MR> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, null);
	}

	default IParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexiconImmutable<MR> tempLexicon,
			Integer beamSize) {
		return parse(dataItem, null, model, allowWordSkipping, tempLexicon,
				beamSize);
	}

	default IParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> filter, IDataItemModel<MR> model) {
		return parse(dataItem, filter, model, false);
	}

	default IParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> filter, IDataItemModel<MR> model,
			boolean allowWordSkipping) {
		return parse(dataItem, filter, model, allowWordSkipping, null);
	}

	default IParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> filter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexiconImmutable<MR> tempLexicon) {
		return parse(dataItem, filter, model, allowWordSkipping, tempLexicon,
				null);
	}

	IParserOutput<MR> parse(DI dataItem, Predicate<ParsingOp<MR>> filter,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexiconImmutable<MR> tempLexicon, Integer beamSize);

}
