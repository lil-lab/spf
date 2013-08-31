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

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
import edu.uw.cs.utils.composites.Pair;

public class ModelLogger {
	private final boolean	sortLexicalEntries;
	
	public ModelLogger(boolean sortLexicalEntries) {
		this.sortLexicalEntries = sortLexicalEntries;
	}
	
	private static <DI extends IDataItem<?>, MR> String lexicalEntryToString(
			LexicalEntry<MR> entry, IModelImmutable<DI, MR> model) {
		return String.format("[%.4f] %s %s (%s)", model.score(entry), entry,
				model.getTheta().printValues(model.computeFeatures(entry)),
				entry.getOrigin());
	}
	
	public <DI extends IDataItem<?>, MR> void log(
			final IModelImmutable<DI, MR> model, PrintStream out) {
		// Lexicon
		out.println("Lexical entries:");
		if (sortLexicalEntries) {
			final Map<List<String>, List<LexicalEntry<MR>>> tokensToLexicalEntries = new HashMap<List<String>, List<LexicalEntry<MR>>>();
			for (final LexicalEntry<MR> entry : model.getLexicon()
					.toCollection()) {
				if (!tokensToLexicalEntries.containsKey(entry.getTokens())) {
					tokensToLexicalEntries.put(entry.getTokens(),
							new LinkedList<LexicalEntry<MR>>());
				}
				tokensToLexicalEntries.get(entry.getTokens()).add(entry);
			}
			
			for (final Entry<List<String>, List<LexicalEntry<MR>>> mapEntry : tokensToLexicalEntries
					.entrySet()) {
				Collections.sort(mapEntry.getValue(),
						new Comparator<LexicalEntry<MR>>() {
							
							@Override
							public int compare(LexicalEntry<MR> o1,
									LexicalEntry<MR> o2) {
								return -Double.compare(model.score(o1),
										model.score(o2));
							}
						});
				for (final LexicalEntry<MR> entry : mapEntry.getValue()) {
					out.println(lexicalEntryToString(entry, model));
				}
			}
			
		} else {
			for (final LexicalEntry<MR> entry : model.getLexicon()
					.toCollection()) {
				out.println(lexicalEntryToString(entry, model));
			}
		}
		
		// Theta
		out.println("Feature weights:");
		for (final Pair<KeyArgs, Double> feature : model.getTheta()) {
			out.println(String.format("%s=%s", feature.first(),
					feature.second()));
		}
		
	}
}
