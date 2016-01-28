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
package edu.cornell.cs.nlp.spf.parser.ccg.features.basic;

import java.util.Collection;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.UniformScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelInit;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Model initializer to set weight for indicator features over lexical entries.
 * This initializer can be used to set weights for any features that are
 * computed by {@link IModelImmutable#computeFeatures(LexicalEntry)}. To vary
 * the features set, set the tag to the different values (e.g., lexical entry,
 * lexeme or lexical template indicator features).
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item used for inference.
 * @param <MR>
 *            Meaning representation.
 */
public class LexicalFeaturesInit<DI extends IDataItem<?>, MR> implements
		IModelInit<DI, MR> {

	public static final ILogger					LOG	= LoggerFactory
															.create(LexicalFeaturesInit.class);

	private final Collection<LexicalEntry<MR>>	entries;
	private final KeyArgs						partialKey;
	private final IScorer<LexicalEntry<MR>>		scorer;

	public LexicalFeaturesInit(Collection<LexicalEntry<MR>> entries,
			IScorer<LexicalEntry<MR>> scorer, KeyArgs partialKey) {
		this.entries = entries;
		this.scorer = scorer;
		this.partialKey = partialKey;
	}

	public LexicalFeaturesInit(Collection<LexicalEntry<MR>> entries,
			KeyArgs partialKey, double value) {
		this(entries, new UniformScorer<LexicalEntry<MR>>(value), partialKey);
	}

	public LexicalFeaturesInit(ILexicon<MR> lexicon, KeyArgs partialKey,
			double value) {
		this(lexicon.toCollection(), partialKey, value);
	}

	public LexicalFeaturesInit(ILexicon<MR> lexicon, KeyArgs partialKey,
			IScorer<LexicalEntry<MR>> scorer) {
		this(lexicon.toCollection(), scorer, partialKey);
	}

	@Override
	public void init(Model<DI, MR> model) {
		for (final LexicalEntry<MR> entry : entries) {
			final IHashVector features = model.computeFeatures(entry).getAll(
					partialKey);
			for (final Pair<KeyArgs, Double> pair : features) {
				model.getTheta().set(pair.first(), scorer.score(entry));
			}
			LOG.info("Init %s: %s -> %s", partialKey, entry, model.getTheta()
					.printValues(model.computeFeatures(entry)));
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR> implements
			IResourceObjectCreator<LexicalFeaturesInit<DI, MR>> {

		private String	type;

		public Creator() {
			this("init.lex.weights");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public LexicalFeaturesInit<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			if (params.contains("value")) {
				return new LexicalFeaturesInit<DI, MR>(
						(ILexicon<MR>) repo.get(params.get("lexicon")),
						KeyArgs.read(params.get("key")),
						params.getAsDouble("value"));
			} else {
				return new LexicalFeaturesInit<DI, MR>(
						(ILexicon<MR>) repo.get(params.get("lexicon")),
						KeyArgs.read(params.get("key")),
						(IScorer<LexicalEntry<MR>>) repo.get(params
								.get("scorer")));
			}
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, LexicalFeaturesInit.class)
					.addParam("lexicon", ILexicon.class,
							"Lexical entries to init.")
					.addParam("key", KeyArgs.class,
							"Partial key specifying the set of features to be initialized.")
					.addParam("value", Double.class,
							"Value to use for init. Can't be used together with a scorer.")
					.addParam("scorer", IScorer.class,
							"Scorer to score lexical entries.").build();
		}

	}
}
