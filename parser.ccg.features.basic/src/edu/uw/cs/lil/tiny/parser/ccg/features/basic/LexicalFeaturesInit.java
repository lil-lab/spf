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
package edu.uw.cs.lil.tiny.parser.ccg.features.basic;

import java.util.Collection;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelInit;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

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
	private final IScorer<LexicalEntry<MR>>		scorer;
	private final String						tag;
	
	public LexicalFeaturesInit(Collection<LexicalEntry<MR>> entries,
			String tag, double value) {
		this(entries, tag, new UniformScorer<LexicalEntry<MR>>(value));
	}
	
	public LexicalFeaturesInit(Collection<LexicalEntry<MR>> entries,
			String tag, IScorer<LexicalEntry<MR>> scorer) {
		this.entries = entries;
		this.tag = tag;
		this.scorer = scorer;
	}
	
	public LexicalFeaturesInit(ILexicon<MR> lexicon, String tag, double value) {
		this(lexicon.toCollection(), tag, value);
	}
	
	public LexicalFeaturesInit(ILexicon<MR> lexicon, String tag,
			IScorer<LexicalEntry<MR>> scorer) {
		this(lexicon.toCollection(), tag, scorer);
	}
	
	@Override
	public void init(Model<DI, MR> model) {
		for (final LexicalEntry<MR> entry : entries) {
			final IHashVector features = model.computeFeatures(entry).getAll(
					tag);
			for (final Pair<KeyArgs, Double> pair : features) {
				model.getTheta().set(pair.first(), scorer.score(entry));
			}
			LOG.info("Init %s: %s -> %s", tag, entry, model.getTheta()
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
						(ILexicon<MR>) repo.getResource(params.get("lexicon")),
						params.get("tag"), params.getAsDouble("value"));
			} else {
				return new LexicalFeaturesInit<DI, MR>(
						(ILexicon<MR>) repo.getResource(params.get("lexicon")),
						params.get("tag"),
						(IScorer<LexicalEntry<MR>>) repo.getResource(params
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
					.addParam("tag", String.class, "Feature tag to init.")
					.addParam("value", Double.class,
							"Value to use for init. Can't be used together with a scorer.")
					.addParam("scorer", IScorer.class,
							"Scorer to score lexical entries.").build();
		}
		
	}
}
