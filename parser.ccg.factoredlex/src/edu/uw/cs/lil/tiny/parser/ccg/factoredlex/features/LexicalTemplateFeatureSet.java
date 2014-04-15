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
package edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.AbstractLexicalFeatureSet;
import edu.uw.cs.utils.collections.ISerializableScorer;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

public class LexicalTemplateFeatureSet<DI extends IDataItem<?>> extends
		AbstractLexicalFeatureSet<DI, LogicalExpression> {
	
	/**
	 * The name of the default (protected) feature.
	 */
	private static final String							DEFAULT_FEAT		= "DEFAULT";
	
	private static final long							serialVersionUID	= -8421114405286202227L;
	
	private final String								featureTag;
	
	private final ISerializableScorer<LexicalTemplate>	initialScorer;
	
	private int											nextId				= 0;
	
	private final double								scale;
	
	private final Map<LexicalTemplate, Integer>			templateIds;
	
	private LexicalTemplateFeatureSet(String featureTag,
			ISerializableScorer<LexicalTemplate> initialScorer,
			Map<LexicalTemplate, Integer> templateIds, double scale) {
		this.featureTag = featureTag;
		this.initialScorer = initialScorer;
		this.templateIds = templateIds;
		this.scale = scale;
		for (final Map.Entry<LexicalTemplate, Integer> entry : this.templateIds
				.entrySet()) {
			if (entry.getValue() >= nextId) {
				nextId = entry.getValue() + 1;
			}
		}
	}
	
	@Override
	public boolean addEntry(LexicalEntry<LogicalExpression> entry,
			IHashVector parametersVector) {
		final LexicalTemplate template = getTemplate(entry);
		if (template == null) {
			return false;
		}
		if (templateIds.containsKey(template)) {
			return false;
		}
		final int num = getNextId();
		templateIds.put(template, new Integer(num));
		parametersVector.set(featureTag, String.valueOf(num),
				initialScorer.score(template));
		return true;
	}
	
	@Override
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(
			IHashVector theta) {
		// Get weights relevant to this feature set and attach each of them the
		// lexical entry as comment
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		
		for (final Map.Entry<LexicalTemplate, Integer> entry : templateIds
				.entrySet()) {
			final int index = entry.getValue();
			final double weight = theta.get(featureTag, String.valueOf(index));
			weights.add(Triplet.of(
					new KeyArgs(featureTag, String.valueOf(index)), weight,
					entry.getKey().toString()));
		}
		
		return weights;
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
	public double score(LexicalEntry<LogicalExpression> entry,
			IHashVector parametersVector) {
		if (entry == null) {
			return 0.0;
		}
		final LexicalTemplate template = getTemplate(entry);
		if (template == null) {
			// if the lexical item is not factored, we return 0. this is to
			// allow parsing with mixed lexical items (both factored and
			// unfactored).
			return 0.0;
		}
		final int i = indexOf(template);
		if (i >= 0) {
			return parametersVector.get(featureTag, String.valueOf(i)) * scale;
		}
		// return the weight that would be assigned if this feature were added
		return initialScorer.score(template) * scale;
	}
	
	@Override
	public void setFeats(LexicalEntry<LogicalExpression> entry,
			IHashVector features) {
		if (entry == null) {
			return;
		}
		final LexicalTemplate template = getTemplate(entry);
		if (template == null) {
			// if the lexical item is not factored, we don't set any features.
			// this is to allow parsing with mixed lexical items (both factored
			// and unfactored).
			return;
		}
		final int i = indexOf(template);
		if (i >= 0) {
			features.set(featureTag, String.valueOf(i),
					features.get(featureTag, String.valueOf(i)) + 1.0 * scale);
		} else {
			// Case no feature set for this template, set the default protected
			// feature using the initial scorer
			features.set(featureTag, DEFAULT_FEAT,
					initialScorer.score(template) * scale);
		}
	}
	
	/**
	 * Returns the next ID to use and increase the ID counter by one.
	 * 
	 * @return next free ID for the entries map.
	 */
	private int getNextId() {
		return nextId++;
	}
	
	private LexicalTemplate getTemplate(LexicalEntry<LogicalExpression> entry) {
		if (entry instanceof FactoredLexicon.FactoredLexicalEntry) {
			return ((FactoredLexicon.FactoredLexicalEntry) entry).getTemplate();
		}
		return null;
	}
	
	private int indexOf(LexicalTemplate l) {
		final Integer index = templateIds.get(l);
		if (index == null) {
			return -1;
		} else {
			return index.intValue();
		}
	}
	
	public static class Builder<DI extends IDataItem<?>> {
		
		private String									featureTag		= "XTMP";
		
		private ISerializableScorer<LexicalTemplate>	initialScorer	= new UniformScorer<LexicalTemplate>(
																				0.0);
		
		private double									scale			= 1.0;
		
		private final Map<LexicalTemplate, Integer>		templateIds		= new HashMap<LexicalTemplate, Integer>();
		
		public LexicalTemplateFeatureSet<DI> build() {
			return new LexicalTemplateFeatureSet<DI>(featureTag, initialScorer,
					templateIds, scale);
		}
		
		public Builder<DI> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}
		
		public Builder<DI> setInitialScorer(
				ISerializableScorer<LexicalTemplate> initialScorer) {
			this.initialScorer = initialScorer;
			return this;
		}
		
		public Builder<DI> setScale(double scale) {
			this.scale = scale;
			return this;
		}
		
	}
	
	public static class Creator<DI extends IDataItem<?>> implements
			IResourceObjectCreator<LexicalTemplateFeatureSet<DI>> {
		
		@SuppressWarnings("unchecked")
		@Override
		public LexicalTemplateFeatureSet<DI> create(Parameters parameters,
				IResourceRepository resourceRepo) {
			final LexicalTemplateFeatureSet.Builder<DI> builder = new LexicalTemplateFeatureSet.Builder<DI>();
			
			if (parameters.contains("scale")) {
				builder.setScale(Double.valueOf(parameters.get("scale")));
			}
			
			if (parameters.contains("tag")) {
				builder.setFeatureTag(parameters.get("tag"));
			}
			
			if (parameters.contains("init")) {
				builder.setInitialScorer((ISerializableScorer<LexicalTemplate>) resourceRepo
						.getResource(parameters.get("init")));
			}
			
			return builder.build();
		}
		
		@Override
		public String type() {
			return "feat.lextemplate";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					LexicalTemplateFeatureSet.class)
					.setDescription(
							"Feature set that generates features for using lexical templates")
					.addParam("scale", "double",
							"Scaling factor for generated features (default: 1.0)")
					.addParam("tag", "string",
							"Feature tag to use for generated features (default: XTMP)")
					.addParam(
							"init",
							"id",
							"Scorer used to score unknown templates (default: uniform scorer with value 0.0)")
					.build();
		}
		
	}
	
}
