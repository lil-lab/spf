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
package edu.uw.cs.lil.tiny.parser.ccg.model;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.IParse;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.Lexicon;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Prunes the lexicon of a given model. Retains all lexical item used in any
 * single optimal parse. If there's more than one optimal parse, no lexical
 * items are retained for this sample.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public class LexiconPruner<Y> implements IModelPostProcessor<Sentence, Y> {
	private static final ILogger						LOG	= LoggerFactory
																	.create(LexiconPruner.class
																			.getName());
	
	private final IDataCollection<IDataItem<Sentence>>	data;
	/**
	 * A set of lexical item to retain regardless of their usage in optimal
	 * parses.
	 */
	private final Lexicon<Y>							fixed;
	private final IParser<Sentence, Y>					parser;
	
	public LexiconPruner(IDataCollection<IDataItem<Sentence>> data,
			IParser<Sentence, Y> parser, Lexicon<Y> fixed) {
		this.data = data;
		this.parser = parser;
		this.fixed = fixed;
	}
	
	@Override
	public void process(Model<Sentence, Y> model) {
		LOG.info("Lexicon pruning...");
		final Set<LexicalEntry<Y>> usedEntries = new HashSet<LexicalEntry<Y>>(
				fixed.toCollection());
		final Set<LexicalEntry<Y>> seenEntries = new HashSet<LexicalEntry<Y>>();
		for (final IDataItem<Sentence> dataItem : data) {
			final IParserOutput<Y> parserOutput = parser.parse(dataItem,
					model.createDataItemModel(dataItem));
			final List<IParse<Y>> bestParses = parserOutput
					.getBestParses();
			if (!bestParses.isEmpty()) {
				for (final IParse<Y> parse : bestParses) {
					final LinkedHashSet<LexicalEntry<Y>> parseLexicalEntries = parse
							.getMaxLexicalEntries();
					for (final LexicalEntry<Y> entry : parseLexicalEntries) {
						// Only keep lexical entries that were used twice at
						// least
						if (!seenEntries.add(entry)) {
							usedEntries.add(entry);
							usedEntries.addAll(entry.getLinkedEntries());
						}
					}
				}
			}
		}
		final Set<LexicalEntry<Y>> originalLexicon = new HashSet<LexicalEntry<Y>>(
				model.getLexicon().toCollection());
		model.getLexicon().retainAll(usedEntries);
		originalLexicon.removeAll(usedEntries);
		for (final LexicalEntry<Y> removedEntry : originalLexicon) {
			LOG.info("Removed: [%.2f] %s", model.score(removedEntry),
					removedEntry);
		}
	}
}
