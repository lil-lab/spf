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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.AbstractLexicalFeatureSet;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
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
	
	private final ISerializableScorer<LexicalTemplate>	initialFixedScorer;
	
	private final ISerializableScorer<LexicalTemplate>	initialScorer;
	
	private int											nextId				= 0;
	
	private final double								scale;
	
	private final Map<LexicalTemplate, Integer>			templateIds;
	
	private LexicalTemplateFeatureSet(String featureTag,
			ISerializableScorer<LexicalTemplate> initialFixedScorer,
			ISerializableScorer<LexicalTemplate> initialScorer,
			Map<LexicalTemplate, Integer> templateIds, double scale) {
		this.featureTag = featureTag;
		this.initialFixedScorer = initialFixedScorer;
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
	
	public static <DI extends IDataItem<?>> IDecoder<LexicalTemplateFeatureSet<DI>> getDecoder(
			DecoderHelper<LogicalExpression> decoderHelper) {
		return new Decoder<DI>(decoderHelper);
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
	public boolean addFixedEntry(LexicalEntry<LogicalExpression> entry,
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
				initialFixedScorer.score(template));
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
		
		private String									featureTag			= "XTMP";
		
		private ISerializableScorer<LexicalTemplate>	initialFixedScorer	= new UniformScorer<LexicalTemplate>(
																					0.0);
		
		private ISerializableScorer<LexicalTemplate>	initialScorer		= new UniformScorer<LexicalTemplate>(
																					0.0);
		
		private double									scale				= 1.0;
		
		private final Map<LexicalTemplate, Integer>		templateIds			= new HashMap<LexicalTemplate, Integer>();
		
		public LexicalTemplateFeatureSet<DI> build() {
			return new LexicalTemplateFeatureSet<DI>(featureTag,
					initialFixedScorer, initialScorer, templateIds, scale);
		}
		
		public Builder<DI> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}
		
		public Builder<DI> setInitialFixedScorer(
				ISerializableScorer<LexicalTemplate> initialFixedScorer) {
			this.initialFixedScorer = initialFixedScorer;
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
	
	private static class Decoder<DI extends IDataItem<?>> extends
			AbstractDecoderIntoFile<LexicalTemplateFeatureSet<DI>> {
		
		private static final int						VERSION	= 1;
		
		private final DecoderHelper<LogicalExpression>	decoderHelper;
		
		public Decoder(DecoderHelper<LogicalExpression> decoderHelper) {
			super(LexicalTemplateFeatureSet.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexicalTemplateFeatureSet<DI> object) {
			final HashMap<String, String> attributes = new HashMap<String, String>();
			
			attributes.put("featureTag", object.featureTag);
			attributes.put("scale", Double.toString(object.scale));
			
			return attributes;
		}
		
		@Override
		protected LexicalTemplateFeatureSet<DI> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final String featureTag = attributes.get("featureTag");
			final double scale = Double.valueOf(attributes.get("scale"));
			
			// Read scorers from external files
			final ISerializableScorer<LexicalTemplate> initialScorer = DecoderServices
					.decode(dependentFiles.get("initialScorer"), decoderHelper);
			final ISerializableScorer<LexicalTemplate> initialFixedScorer = DecoderServices
					.decode(dependentFiles.get("initialFixedScorer"),
							decoderHelper);
			
			// Read lexItems mapping
			final Map<LexicalTemplate, Integer> templateIds = new HashMap<LexicalTemplate, Integer>();
			// Read the header of the map
			readTextLine(reader);
			String line;
			while (!(line = readTextLine(reader))
					.equals("LEX_TEMPLATES_MAP_END")) {
				final String split[] = line.split("\t");
				final LexicalTemplate template = LexicalTemplate.parse(
						split[0], decoderHelper.getCategoryServices(),
						Lexicon.SAVED_LEXICON_ORIGIN);
				final int id = Integer.valueOf(split[1]);
				templateIds.put(template, id);
			}
			
			final LexicalTemplateFeatureSet<DI> lfs = new LexicalTemplateFeatureSet<DI>(
					featureTag, initialFixedScorer, initialScorer, templateIds,
					scale);
			return lfs;
		}
		
		@Override
		protected void doEncode(LexicalTemplateFeatureSet<DI> object,
				BufferedWriter writer) throws IOException {
			// Store mapping of lexical templates to feature IDs
			writer.write("LEX_TEMPLATES_MAP_START\n");
			for (final Map.Entry<LexicalTemplate, Integer> entry : object.templateIds
					.entrySet()) {
				writer.write(String.format("%s\t%d\n", entry.getKey(),
						entry.getValue()));
			}
			writer.write("LEX_TEMPLATES_MAP_END\n");
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexicalTemplateFeatureSet<DI> object, File directory,
				File parentFile) throws IOException {
			final Map<String, File> dependentFiles = new HashMap<String, File>();
			
			// Store scorers to separate files
			final File initialScorerFile = new File(directory,
					parentFile.getName() + ".initialScorer");
			DecoderServices.encode(object.initialScorer, initialScorerFile,
					decoderHelper);
			dependentFiles.put("initialScorer", initialScorerFile);
			
			final File initialFixedScorerFile = new File(directory,
					parentFile.getName() + ".initialFixedScorer");
			DecoderServices.encode(object.initialFixedScorer,
					initialFixedScorerFile, decoderHelper);
			dependentFiles.put("initialFixedScorer", initialFixedScorerFile);
			
			return dependentFiles;
		}
		
	}
	
}
