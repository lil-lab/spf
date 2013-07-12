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
package edu.uw.cs.lil.tiny.learn.ubl;

import java.util.HashSet;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

/**
 * General services that UBL requires, but are also used in other places.
 * 
 * @author Yoav Artzi
 */
public class UBLServices {
	
	public static Set<LexicalEntry<LogicalExpression>> createSentenceLexicalEntries(
			final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> data,
			ICategoryServices<LogicalExpression> categoryServices) {
		final Set<LexicalEntry<LogicalExpression>> result = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final ILabeledDataItem<Sentence, LogicalExpression> dataItem : data) {
			result.add(createSentenceLexicalEntry(dataItem, categoryServices));
		}
		return result;
	}
	
	public static LexicalEntry<LogicalExpression> createSentenceLexicalEntry(
			final ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			ICategoryServices<LogicalExpression> categoryServices) {
		final LexicalEntry<LogicalExpression> l = new LexicalEntry<LogicalExpression>(
				dataItem.getSample().getTokens(), categoryServices
						.getSentenceCategory().cloneWithNewSemantics(
								dataItem.getLabel()),
				AbstractUBL.SPLITTING_LEXICAL_ORIGIN);
		return FactoredLexicon.factor(l);
	}
	
}
