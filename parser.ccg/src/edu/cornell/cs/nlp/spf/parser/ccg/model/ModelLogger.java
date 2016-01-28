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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class ModelLogger {
	private final boolean	sortLexicalEntries;

	public ModelLogger(boolean sortLexicalEntries) {
		this.sortLexicalEntries = sortLexicalEntries;
	}

	private static <DI extends IDataItem<?>, MR> String lexicalEntryToString(
			LexicalEntry<MR> entry, IModelImmutable<DI, MR> model) {
		return String.format("[%.4f] %s %s %s", model.score(entry), entry,
				model.getTheta().printValues(model.computeFeatures(entry)),
				entry.getProperties());
	}

	public <DI extends IDataItem<?>, MR> void log(
			final IModelImmutable<DI, MR> model, PrintStream out) {
		// Lexicon
		final Collection<LexicalEntry<MR>> lexicalCollection = model
				.getLexicon().toCollection();
		out.println(String.format("Lexical entries [%d]:",
				lexicalCollection.size()));
		if (sortLexicalEntries) {
			final Map<TokenSeq, List<LexicalEntry<MR>>> tokensToLexicalEntries = new HashMap<TokenSeq, List<LexicalEntry<MR>>>();
			for (final LexicalEntry<MR> entry : lexicalCollection) {
				if (!tokensToLexicalEntries.containsKey(entry.getTokens())) {
					tokensToLexicalEntries.put(entry.getTokens(),
							new LinkedList<LexicalEntry<MR>>());
				}
				tokensToLexicalEntries.get(entry.getTokens()).add(entry);
			}

			for (final Entry<TokenSeq, List<LexicalEntry<MR>>> mapEntry : tokensToLexicalEntries
					.entrySet()) {
				Collections.sort(
						mapEntry.getValue(),
						(o1, o2) -> -Double.compare(model.score(o1),
								model.score(o2)));
				for (final LexicalEntry<MR> entry : mapEntry.getValue()) {
					out.println(lexicalEntryToString(entry, model));
				}
			}

		} else {
			for (final LexicalEntry<MR> entry : lexicalCollection) {
				out.println(lexicalEntryToString(entry, model));
			}
		}

		// Theta
		out.println(String.format("Feature weights [%d]:", model.getTheta()
				.size()));
		for (final Pair<KeyArgs, Double> feature : model.getTheta()) {
			out.println(String.format("%s=%s", feature.first(),
					feature.second()));
		}

	}

	public static class Creator implements IResourceObjectCreator<ModelLogger> {

		@Override
		public ModelLogger create(Parameters params, IResourceRepository repo) {
			return new ModelLogger("true".equals(params.get("cluster")));
		}

		@Override
		public String type() {
			return "logger.model";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), ModelLogger.class)
					.setDescription("Logs models in a human-readable format")
					.addParam(
							"cluster",
							"boolean",
							"Cluster lexical entries according to their phrase. Options: true, false. Default: false.")
					.build();
		}

	}
}
