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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.AbstractLexicalFeatureSet;
import edu.uw.cs.utils.collections.ISerializableScorer;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lexical feature sets. Creates a feature for every lexical entry.
 **/
public class LexicalFeatureSet<DI extends IDataItem<?>, MR> extends
		AbstractLexicalFeatureSet<DI, MR> {
	
	/**
	 * The name of the default (protected) feature.
	 */
	private static final String										DEFAULT_FEAT		= "DEFAULT";
	
	private static ILogger											LOG					= LoggerFactory
																								.create(LexicalFeatureSet.class
																										.getName());
	
	private static final long										serialVersionUID	= -6342057762767968195L;
	
	private final String											featureTag;
	
	/**
	 * Mapping is decomposed to a pair to treat lexical entries that are
	 * identical, but factored differently, the same way.
	 */
	private final Map<Pair<List<String>, Category<MR>>, Integer>	idMapping;
	
	private final ISerializableScorer<LexicalEntry<MR>>				initialScorer;
	
	private int														nextId				= 0;
	
	private LexicalFeatureSet(String featureTag,
			ISerializableScorer<LexicalEntry<MR>> initialScorer) {
		this(featureTag, initialScorer,
				new HashMap<Pair<List<String>, Category<MR>>, Integer>());
	}
	
	private LexicalFeatureSet(String featureTag,
			ISerializableScorer<LexicalEntry<MR>> initialScorer,
			Map<Pair<List<String>, Category<MR>>, Integer> idMapping) {
		this.initialScorer = initialScorer;
		this.featureTag = featureTag;
		this.idMapping = idMapping;
		for (final Entry<Pair<List<String>, Category<MR>>, Integer> entry : this.idMapping
				.entrySet()) {
			if (entry.getValue() >= nextId) {
				nextId = entry.getValue() + 1;
			}
		}
	}
	
	@Override
	public boolean addEntry(LexicalEntry<MR> entry, IHashVector theta) {
		if (indexOf(entry) < 0) {
			final int index = createIndex(entry);
			
			theta.set(featureTag, String.valueOf(index),
					initialScorer.score(entry));
			
			LOG.debug("LexicalEntry added to feature set: [%d] %s [score=%f]",
					index, entry, theta.get(featureTag, String.valueOf(index)));
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(
			IHashVector theta) {
		// Get weights relevant to this feature set and attach each of them the
		// lexical entry as comment
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		
		for (final Entry<Pair<List<String>, Category<MR>>, Integer> entry : idMapping
				.entrySet()) {
			final int index = entry.getValue();
			final double weight = theta.get(featureTag, String.valueOf(index));
			weights.add(Triplet.of(
					new KeyArgs(featureTag, String.valueOf(index)), weight,
					entry.getKey().toString()));
		}
		
		return weights;
	}
	
	public int indexOf(LexicalEntry<MR> entry) {
		final Integer i = idMapping.get(Pair.of(entry.getTokens(),
				entry.getCategory()));
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}
	
	@Override
	public boolean isValidWeightVector(IHashVectorImmutable update) {
		for (final Pair<KeyArgs, Double> keyPair : update) {
			final KeyArgs key = keyPair.first();
			if (key.getArg1().equals(featureTag)
					&& key.getArg2().equals(DEFAULT_FEAT)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public double score(LexicalEntry<MR> entry, IHashVector theta) {
		if (entry == null) {
			return 0.0;
		} else {
			final int index = indexOf(entry);
			if (index >= 0) {
				return theta.get(featureTag, String.valueOf(index));
			} else {
				// return the score that this entry would get if it were
				// to be added to the feature set
				return initialScorer.score(entry);
			}
		}
	}
	
	@Override
	public void setFeats(LexicalEntry<MR> entry, IHashVector feats) {
		final int i = indexOf(entry);
		if (i >= 0) {
			if (feats.get(featureTag, String.valueOf(i)) > 100) {
				LOG.error("Large %s feature: %s", featureTag, entry);
			}
			feats.set(featureTag, String.valueOf(i),
					feats.get(featureTag, String.valueOf(i)) + 1.0);
		} else {
			// Case no feature set for this entry, set the default protected
			// feature using the initial scorer
			feats.set(featureTag, DEFAULT_FEAT, initialScorer.score(entry));
		}
		
	}
	
	public int size() {
		return idMapping.size();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ":\n\t" + idMapping.toString();
	}
	
	private int createIndex(LexicalEntry<MR> entry) {
		final int index = getNextId();
		idMapping.put(Pair.of(entry.getTokens(), entry.getCategory()), index);
		return index;
	}
	
	/**
	 * Returns the next ID to use and increase the ID counter by one.
	 * 
	 * @return next free ID for the entries map.
	 */
	private int getNextId() {
		return nextId++;
	}
	
	public static class Builder<DI extends IDataItem<?>, MR> {
		
		/**
		 * The name tag for the features.
		 */
		private String									featureTag		= "LEX";
		
		/**
		 * Scorer for new lexical entries.
		 */
		private ISerializableScorer<LexicalEntry<MR>>	initialScorer	= new UniformScorer<LexicalEntry<MR>>(
																				0.0);
		
		public LexicalFeatureSet<DI, MR> build() {
			return new LexicalFeatureSet<DI, MR>(featureTag, initialScorer);
		}
		
		public Builder<DI, MR> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}
		
		public Builder<DI, MR> setInitialScorer(
				ISerializableScorer<LexicalEntry<MR>> initialScorer) {
			this.initialScorer = initialScorer;
			return this;
		}
	}
	
	/**
	 * Creator for {@link LexicalFeatureSet}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Creator<DI extends IDataItem<?>, MR> implements
			IResourceObjectCreator<LexicalFeatureSet<DI, MR>> {
		
		@SuppressWarnings("unchecked")
		@Override
		public LexicalFeatureSet<DI, MR> create(Parameters parameters,
				IResourceRepository resourceRepo) {
			final LexicalFeatureSet.Builder<DI, MR> builder = new LexicalFeatureSet.Builder<DI, MR>();
			
			if (parameters.contains("tag")) {
				builder.setFeatureTag(parameters.get("tag"));
			}
			
			if (parameters.contains("init")) {
				builder.setInitialScorer((ISerializableScorer<LexicalEntry<MR>>) resourceRepo
						.getResource(parameters.get("init")));
			}
			
			return builder.build();
		}
		
		@Override
		public String type() {
			return "feat.lex";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), LexicalFeatureSet.class)
					.setDescription(
							"Feature set that generates features for using lexical entries")
					.addParam("tag", "string",
							"Feature tag to be used for generated features (default: LEX)")
					.addParam("init", "id",
							"Scorer to initialize lexical entries (all non fixed entries)")
					.build();
		}
		
	}
	
}
