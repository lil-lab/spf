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
package edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features;

import java.util.Set;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.LexicalFeatureSet;
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
 * Features for factored lexical entries. If the entry is factored, will
 * optionally create features for the pairing of the template and lexeme, the
 * template and the lexeme. The feature is tagged using the IDs of the template
 * and lexeme. IDs are tracked by the feature set. If the a non-factored lexical
 * entry is given, only a binary feature for the entire lexical entry will be
 * generated. This feature will be tagged using the ID of the lexical entry,
 * which is tracked separately for non-factored entries by the feature set. This
 * feature set supersedes {@link LexicalFeatureSet} and should not be used
 * together with it.
 *
 * @author Yoav Artzi
 */
public class FactoredLexicalFeatureSet<DI extends IDataItem<?>>
		extends AbstractLexicalFeatureSet<DI, LogicalExpression> {

	public static final ILogger												LOG					= LoggerFactory
			.create(FactoredLexicalFeatureSet.class);

	/**
	 * The protected lexical entry default feature tag.
	 */
	private static final String												DEFAULT_FEAT_LEX	= "LEXDEFAULT";

	/**
	 * The protected template default feature tag.
	 */
	private static final String												DEFAULT_FEAT_TMP	= "TMPDEFAULT";

	/**
	 * The protected lexeme default feature tag.
	 */
	private static final String												DEFAULT_FEAT_XEME	= "XEMEDEFAULT";

	private static final String												DEFAULT_FEATURE_TAG	= "FACLEX";

	private static final long												serialVersionUID	= -7176601636484234288L;

	/**
	 * Secondary key for factored entries.
	 */
	protected static final String											KEY_ENTRY			= "LEX";

	/**
	 * Secondary key for templates.
	 */
	protected static final String											KEY_TMP				= "TMP";

	/**
	 * Secondary key for lexemes.
	 */
	protected static final String											KEY_XEME			= "XEME";

	private final ISerializableScorer<LexicalEntry<LogicalExpression>>		entryInitialScorer;

	/**
	 * Scaling factor for lexical entries (or pairing of lexeme and template).
	 */
	private final double													entryScale;

	/**
	 * ID mapping for lexemes.
	 */
	private final Object2IntOpenHashMap<Lexeme>								lexemeIds			= new Object2IntOpenHashMap<>();

	private final ISerializableScorer<Lexeme>								lexemeInitialScorer;
	private int																lexemeNextId		= 0;
	private final double													lexemeScale;

	/**
	 * ID mapping for non-factored lexical entries.
	 */
	private final Object2IntOpenHashMap<LexicalEntry<LogicalExpression>>	nonFactoredIds		= new Object2IntOpenHashMap<>();

	private int																nonFactoredNextId	= 0;
	/**
	 * ID mapping for lexical templates.
	 */
	private final Object2IntOpenHashMap<LexicalTemplate>					templateIds			= new Object2IntOpenHashMap<>();

	private final ISerializableScorer<LexicalTemplate>						templateInitialScorer;
	private int																templateNextId		= 0;

	private final double													templateScale;

	protected FactoredLexicalFeatureSet(
			Predicate<LexicalEntry<LogicalExpression>> ignoreFilter,
			ISerializableScorer<LexicalEntry<LogicalExpression>> entryInitialScorer,
			double entryScale, String featureTag,
			ISerializableScorer<Lexeme> lexemeInitialScorer, double lexemeScale,
			ISerializableScorer<LexicalTemplate> templateInitialScorer,
			double templateScale, boolean computeSyntaxAttributeFeatures) {
		super(ignoreFilter, computeSyntaxAttributeFeatures, featureTag);
		this.entryInitialScorer = entryInitialScorer;
		this.entryScale = entryScale;
		this.lexemeInitialScorer = lexemeInitialScorer;
		this.lexemeScale = lexemeScale;
		this.templateInitialScorer = templateInitialScorer;
		this.templateScale = templateScale;
	}

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return SetUtils.createSet(new KeyArgs(featureTag, DEFAULT_FEAT_LEX),
				new KeyArgs(featureTag, DEFAULT_FEAT_XEME),
				new KeyArgs(featureTag, DEFAULT_FEAT_TMP));
	}

	@Override
	protected boolean doAddEntry(LexicalEntry<LogicalExpression> entry,
			IHashVector parameters) {
		if (entry instanceof FactoredLexicalEntry) {
			// Case factored entry, add factored features.

			final FactoredLexicalEntry factored = (FactoredLexicalEntry) entry;
			final Lexeme lexeme = factored.getLexeme();
			final LexicalTemplate template = factored.getTemplate();
			boolean exists = true;

			// Add lexeme.
			int lexemeId;
			if (lexemeIds.containsKey(lexeme)) {
				lexemeId = lexemeIds.getInt(lexeme);
			} else {
				// Create id.
				lexemeId = lexemeNextId++;
				lexemeIds.put(lexeme, lexemeId);
				exists = false;

				// Init parameter.
				if (lexemeScale != 0.0) {
					parameters.set(featureTag, KEY_XEME,
							String.valueOf(lexemeId),
							lexemeInitialScorer.score(lexeme));
					LOG.debug("Lexeme added to feature set: [%d] %s [score=%f]",
							lexemeId, lexeme, parameters.get(featureTag,
									KEY_XEME, String.valueOf(lexemeId)));
				}
			}

			// Add template.
			int templateId;
			if (templateIds.containsKey(template)) {
				templateId = templateIds.getInt(template);
			} else {
				// Create id.
				templateId = templateNextId++;
				templateIds.put(template, templateId);
				exists = false;

				// Init parameter.
				if (templateScale != 0.0) {
					parameters.set(featureTag, KEY_TMP,
							String.valueOf(templateId),
							templateInitialScorer.score(template));
					LOG.debug(
							"Template added to feature set: [%d] %s [score=%f]",
							templateId, template, parameters.get(featureTag,
									KEY_TMP, String.valueOf(templateId)));
				}
			}

			if (exists) {
				return false;
			}

			// Initialize lexeme-template features if the pairing didn't exist
			// before.
			parameters.set(featureTag, KEY_ENTRY, String.valueOf(lexemeId),
					String.valueOf(templateId),
					entryInitialScorer.score(entry));
			LOG.debug(
					"Factored entry feautre added to feature set: [%d, %d] %s [score=%f]",
					lexemeId, templateId, entry,
					parameters.get(featureTag, KEY_ENTRY,
							String.valueOf(lexemeId),
							String.valueOf(templateId),
							entryInitialScorer.score(entry)));
		} else {
			// Case non-factored entry, add a single binary non-factored
			// feature.
			if (nonFactoredIds.containsKey(entry)) {
				return false;
			}

			final int nonFactoredId = nonFactoredNextId++;

			// Update the mapping.
			nonFactoredIds.put(entry, nonFactoredId);

			// Initialize the feature weight.
			parameters.set(featureTag, KEY_ENTRY, String.valueOf(nonFactoredId),
					entryInitialScorer.score(entry));
		}

		return true;
	}

	@Override
	protected void doSetFeatures(LexicalEntry<LogicalExpression> entry,
			IHashVector features) {
		super.doSetFeatures(entry, features);
		if (entry instanceof FactoredLexicalEntry) {

			final FactoredLexicalEntry factored = (FactoredLexicalEntry) entry;
			final Lexeme lexeme = factored.getLexeme();
			final LexicalTemplate template = factored.getTemplate();
			boolean exists = true;

			// Lexeme feature.
			int lexemeId;
			if (lexemeIds.containsKey(lexeme)) {
				lexemeId = lexemeIds.getInt(lexeme);
				if (lexemeScale != 0.0) {
					features.add(featureTag, KEY_XEME, String.valueOf(lexemeId),
							1.0 * lexemeScale);
				}
			} else {
				// Default lexeme feature.
				if (lexemeScale != 0.0) {
					features.add(featureTag, DEFAULT_FEAT_XEME,
							lexemeInitialScorer.score(lexeme) * lexemeScale);
				}
				exists = false;
				lexemeId = -1;
			}

			// Template feature.
			int templateId;
			if (templateIds.containsKey(template)) {
				templateId = templateIds.getInt(template);
				if (templateScale != 0.0) {
					features.add(featureTag, KEY_TMP,
							String.valueOf(templateId), 1.0 * templateScale);
				}
			} else {
				// Default template feature.
				if (templateScale != 0.0) {
					features.add(featureTag, DEFAULT_FEAT_TMP,
							templateInitialScorer.score(template)
									* templateScale);
				}
				exists = false;
				templateId = -1;
			}

			// Pairing feature.
			if (exists) {
				// Pairing is known, add its feature.
				features.add(featureTag, KEY_ENTRY, String.valueOf(lexemeId),
						String.valueOf(templateId), 1.0 * entryScale);
			} else {
				// Unknown pairing, add default feature.
				features.add(featureTag, DEFAULT_FEAT_LEX,
						entryInitialScorer.score(entry) * entryScale);
			}
		} else {
			// Case non-factored entry.
			if (nonFactoredIds.containsKey(entry)) {
				features.add(featureTag, KEY_ENTRY,
						String.valueOf(nonFactoredIds.getInt(entry)),
						1.0 * entryScale);
			} else {
				features.add(featureTag, DEFAULT_FEAT_LEX,
						entryInitialScorer.score(entry) * entryScale);
			}
		}
	}

	protected Integer getTemplateId(LexicalTemplate template) {
		return templateIds.get(template);
	}

	public static class Builder<DI extends IDataItem<?>> {

		private boolean													computeSyntaxAttributeFeatures	= false;
		private ISerializableScorer<LexicalEntry<LogicalExpression>>	entryInitialScorer				= new UniformScorer<>(
				0.0);
		private double													entryScale						= 1.0;
		private String													featureTag						= DEFAULT_FEATURE_TAG;
		private Predicate<LexicalEntry<LogicalExpression>>				ignoreFilter					= PredicateUtils
				.alwaysTrue();
		private ISerializableScorer<Lexeme>								lexemeInitialScorer				= new UniformScorer<>(
				0.0);
		private double													lexemeScale						= 1.0;
		private ISerializableScorer<LexicalTemplate>					templateInitialScorer			= new UniformScorer<>(
				0.0);
		private double													templateScale					= 1.0;

		public Builder() {
			// Nothing to do.
		}

		public FactoredLexicalFeatureSet<DI> build() {
			return new FactoredLexicalFeatureSet<>(ignoreFilter,
					entryInitialScorer, entryScale, featureTag,
					lexemeInitialScorer, lexemeScale, templateInitialScorer,
					templateScale, computeSyntaxAttributeFeatures);
		}

		public Builder<DI> setComputeSyntaxAttributeFeatures(
				boolean computeSyntaxAttributeFeatures) {
			this.computeSyntaxAttributeFeatures = computeSyntaxAttributeFeatures;
			return this;
		}

		public Builder<DI> setEntryInitialScorer(
				ISerializableScorer<LexicalEntry<LogicalExpression>> entryInitialScorer) {
			this.entryInitialScorer = entryInitialScorer;
			return this;
		}

		public Builder<DI> setEntryScale(double entryScale) {
			this.entryScale = entryScale;
			return this;
		}

		public Builder<DI> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}

		public Builder<DI> setIgnoreFilter(
				Predicate<LexicalEntry<LogicalExpression>> ignoreFilter) {
			this.ignoreFilter = ignoreFilter;
			return this;
		}

		public Builder<DI> setLexemeInitialScorer(
				ISerializableScorer<Lexeme> lexemeInitialScorer) {
			this.lexemeInitialScorer = lexemeInitialScorer;
			return this;
		}

		public Builder<DI> setLexemeScale(double lexemeScale) {
			this.lexemeScale = lexemeScale;
			return this;
		}

		public Builder<DI> setTemplateInitialScorer(
				ISerializableScorer<LexicalTemplate> templateInitialScorer) {
			this.templateInitialScorer = templateInitialScorer;
			return this;
		}

		public Builder<DI> setTemplateScale(double templateScale) {
			this.templateScale = templateScale;
			return this;
		}

	}

	public static class Creator<DI extends IDataItem<?>>
			implements IResourceObjectCreator<FactoredLexicalFeatureSet<DI>> {

		private String type;

		public Creator() {
			this("feat.lex.factored");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public FactoredLexicalFeatureSet<DI> create(Parameters params,
				IResourceRepository repo) {

			final Builder<DI> builder = new Builder<>();

			if (params.contains("entryScorer")) {
				builder.setEntryInitialScorer(
						repo.get(params.get("entryScorer")));
			}

			if (params.contains("entryScale")) {
				builder.setEntryScale(params.getAsDouble("entryScale"));
			}

			if (params.contains("tag")) {
				builder.setFeatureTag(params.get("tag"));
			}

			if (params.contains("ignoreFilter")) {
				builder.setIgnoreFilter(repo.get(params.get("ignoreFilter")));
			}

			if (params.contains("lexemeScorer")) {
				builder.setLexemeInitialScorer(
						repo.get(params.get("lexemeScorer")));
			}

			if (params.contains("lexemeScale")) {
				builder.setLexemeScale(params.getAsDouble("lexemeScale"));
			}

			if (params.contains("templateScorer")) {
				builder.setTemplateInitialScorer(
						repo.get(params.get("templateScorer")));
			}

			if (params.contains("templateScale")) {
				builder.setTemplateScale(params.getAsDouble("templateScale"));
			}

			if (params.contains("syntaxAttrib")) {
				builder.setComputeSyntaxAttributeFeatures(
						params.getAsBoolean("syntaxAttrib"));
			}

			return builder.build();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, FactoredLexicalFeatureSet.class)
					.setDescription(
							"Lexical features for using with a factored lexicon")
					.addParam("syntaxAttrib", Boolean.class,
							"Compute syntax attribute features (default: false)")
					.addParam("entryScorer", ISerializableScorer.class,
							"Initial scorer for binary lexical entry binary features (also for lexeme-template pairings) (default: f(e) = 0.0)")
					.addParam("entryScale", Double.class,
							"Scaling factor for lexical entry binary features (also for lexeme-template pairings) (default: 1.0)")
					.addParam("tag", String.class,
							"Feature set primary tag (default: "
									+ DEFAULT_FEATURE_TAG + ")")
					.addParam("ignoreFilter", IFilter.class,
							"Filter to ignore lexical entries (default: f(e) = true, ignore nothing)")
					.addParam("lexemeScorer", ISerializableScorer.class,
							"Initial scorer for lexeme features (default: f(l) = 0.0)")
					.addParam("lexemeScale", Double.class,
							"Scaling factor for lexeme features, may be 0.0 to disable feature  (default: 1.0)")
					.addParam("templateScorer", ISerializableScorer.class,
							"Initial scorer for template features (default: f(l) = 0.0)")
					.addParam("templateScale", Double.class,
							"Scaling factor for template features, may be 0.0 to disable feature  (default: 1.0)")
					.build();
		}

	}
}
