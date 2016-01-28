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

import java.util.Set;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.UniformScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.model.lexical.AbstractLexicalFeatureSet;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;
import edu.cornell.cs.nlp.utils.collections.SetUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.function.PredicateUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Lexical feature sets. Creates a feature for every lexical entry. Features are
 * tagged using the ID the entry. The ID mapping is stored within the feature
 * set.
 **/
public class LexicalFeatureSet<DI extends IDataItem<?>, MR>
		extends AbstractLexicalFeatureSet<DI, MR> {

	/**
	 * The name of the default (protected) feature.
	 */
	private static final String							DEFAULT_FEAT		= "DEFAULT";

	private static ILogger								LOG					= LoggerFactory
			.create(LexicalFeatureSet.class.getName());

	private static final long							serialVersionUID	= -7541466894257788967L;

	private final Object2IntOpenHashMap<Category<MR>>	categoryIds;

	private final ISerializableScorer<LexicalEntry<MR>>	initialScorer;
	private int											nextCategoryId		= 0;
	private int											nextTokenId			= 0;

	private final Object2IntOpenHashMap<TokenSeq>		tokenIds;

	private LexicalFeatureSet(String featureTag,
			ISerializableScorer<LexicalEntry<MR>> initialScorer,
			Predicate<LexicalEntry<MR>> ignoreFilter,
			boolean computeSyntaxAttributeFeatures) {
		super(ignoreFilter, computeSyntaxAttributeFeatures, featureTag);
		this.initialScorer = initialScorer;
		this.tokenIds = new Object2IntOpenHashMap<>();
		this.categoryIds = new Object2IntOpenHashMap<>();
	}

	@Override
	public void doSetFeatures(LexicalEntry<MR> entry, IHashVector features) {
		super.doSetFeatures(entry, features);
		final Category<MR> category = entry.getCategory();
		final TokenSeq tokens = entry.getTokens();
		if (tokenIds.containsKey(tokens) && categoryIds.containsKey(category)) {
			final String tokenId = String.valueOf(tokenIds.getInt(tokens));
			final String categoryId = String
					.valueOf(categoryIds.getInt(category));
			if (features.get(featureTag, tokenId, categoryId) > 100) {
				LOG.error("Large %s feature: %s", featureTag, entry);
			}
			features.add(featureTag, tokenId, categoryId, 1.0);
		} else {
			// Case no feature set for this entry, set the default protected
			// feature using the initial scorer
			features.add(featureTag, DEFAULT_FEAT, initialScorer.score(entry));
		}
	}

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return SetUtils.createSingleton(new KeyArgs(featureTag, DEFAULT_FEAT));
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ":\n\t" + tokenIds.toString()
				+ "\n\t" + categoryIds.toString();
	}

	@Override
	protected boolean doAddEntry(LexicalEntry<MR> entry, IHashVector theta) {
		final Category<MR> category = entry.getCategory();
		final TokenSeq tokens = entry.getTokens();
		boolean exists = true;

		int tokenId;
		if (tokenIds.containsKey(tokens)) {
			tokenId = tokenIds.getInt(tokens);
		} else {
			exists = false;
			tokenId = nextTokenId++;
			tokenIds.put(tokens, tokenId);
		}

		int categoryId;
		if (categoryIds.containsKey(category)) {
			categoryId = categoryIds.getInt(category);
		} else {
			exists = false;
			categoryId = nextCategoryId++;
			categoryIds.put(category, categoryId);
		}

		if (exists) {
			return false;
		}

		// Initialize the feature weight.
		theta.set(featureTag, String.valueOf(tokenId),
				String.valueOf(categoryId), initialScorer.score(entry));

		return true;
	}

	public static class Builder<DI extends IDataItem<?>, MR> {

		private boolean									computeSyntaxAttributeFeatures	= false;

		/**
		 * The name tag for the features.
		 */
		private String									featureTag						= "LEX";

		private Predicate<LexicalEntry<MR>>				ignoreFilter					= PredicateUtils
				.alwaysTrue();

		/**
		 * Scorer for new lexical entries.
		 */
		private ISerializableScorer<LexicalEntry<MR>>	initialScorer					= new UniformScorer<LexicalEntry<MR>>(
				0.0);

		public LexicalFeatureSet<DI, MR> build() {
			return new LexicalFeatureSet<DI, MR>(featureTag, initialScorer,
					ignoreFilter, computeSyntaxAttributeFeatures);
		}

		public Builder<DI, MR> setComputeSyntaxAttributeFeatures(
				boolean computeSyntaxAttributeFeatures) {
			this.computeSyntaxAttributeFeatures = computeSyntaxAttributeFeatures;
			return this;
		}

		public Builder<DI, MR> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}

		public Builder<DI, MR> setIgnoreFilter(
				Predicate<LexicalEntry<MR>> ignoreFilter) {
			this.ignoreFilter = ignoreFilter;
			return this;
		}

		public Builder<DI, MR> setInitialScorer(
				ISerializableScorer<LexicalEntry<MR>> initialScorer) {
			this.initialScorer = initialScorer;
			return this;
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR>
			implements IResourceObjectCreator<LexicalFeatureSet<DI, MR>> {

		@Override
		public LexicalFeatureSet<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			final LexicalFeatureSet.Builder<DI, MR> builder = new LexicalFeatureSet.Builder<DI, MR>();

			if (params.contains("syntaxAttrib")) {
				builder.setComputeSyntaxAttributeFeatures(
						params.getAsBoolean("syntaxAttrib"));
			}

			if (params.contains("tag")) {
				builder.setFeatureTag(params.get("tag"));
			}

			if (params.contains("init")) {
				builder.setInitialScorer(repo.get(params.get("init")));
			}

			if (params.contains("filter")) {
				builder.setIgnoreFilter(repo.get(params.get("filter")));
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
					.addParam("syntaxAttrib", Boolean.class,
							"Compute syntax attribute features (default: false)")
					.addParam("filter", IFilter.class,
							"Filter to ignore certain lexical entries (default: ignore none)")
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
