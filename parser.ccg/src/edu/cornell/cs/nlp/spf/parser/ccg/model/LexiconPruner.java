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
package edu.cornell.cs.nlp.spf.parser.ccg.model;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IParser;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Prunes the lexicon of a given model. Retains all lexical item used in any
 * single optimal parse. If there's more than one optimal parse, no lexical
 * items are retained for this sample.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public class LexiconPruner<DI extends Sentence, MR> implements
		IModelProcessor<DI, MR> {
	public static final ILogger			LOG	= LoggerFactory
													.create(LexiconPruner.class
															.getName());
	
	private final IDataCollection<DI>	data;
	/**
	 * A set of lexical item to retain regardless of their usage in optimal
	 * parses.
	 */
	private final Lexicon<MR>			fixed;
	private final IParser<Sentence, MR>	parser;
	
	public LexiconPruner(IDataCollection<DI> data,
			IParser<Sentence, MR> parser, Lexicon<MR> fixed) {
		this.data = data;
		this.parser = parser;
		this.fixed = fixed;
	}
	
	@Override
	public void process(Model<DI, MR> model) {
		LOG.info("Pruning lexicon ...");
		final Set<LexicalEntry<MR>> usedEntries = new HashSet<LexicalEntry<MR>>(
				fixed.toCollection());
		final Set<LexicalEntry<MR>> seenEntries = new HashSet<LexicalEntry<MR>>();
		for (final DI dataItem : data) {
			final IParserOutput<MR> parserOutput = parser.parse(dataItem,
					model.createDataItemModel(dataItem));
			final List<? extends IDerivation<MR>> bestParses = parserOutput
					.getBestDerivations();
			if (!bestParses.isEmpty()) {
				for (final IDerivation<MR> parse : bestParses) {
					final LinkedHashSet<LexicalEntry<MR>> parseLexicalEntries = parse
							.getMaxLexicalEntries();
					for (final LexicalEntry<MR> entry : parseLexicalEntries) {
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
		final Set<LexicalEntry<MR>> originalLexicon = new HashSet<LexicalEntry<MR>>(
				model.getLexicon().toCollection());
		model.getLexicon().retainAll(usedEntries);
		originalLexicon.removeAll(usedEntries);
		for (final LexicalEntry<MR> removedEntry : originalLexicon) {
			LOG.info("Removed: [%.2f] %s", model.score(removedEntry),
					removedEntry);
		}
	}
}
