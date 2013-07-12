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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.AbstractLexicalFeatureSet;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * The lexical features, also deals with the co-occurence statistics used for
 * Initialization.
 **/
public class LexicalFeatureSet<X, Y> extends AbstractLexicalFeatureSet<X, Y> {
	
	/**
	 * The name of the default (protected) feature.
	 */
	private static final String									DEFAULT_FEAT	= "DEFAULT";
	
	private static ILogger										LOG				= LoggerFactory
																						.create(LexicalFeatureSet.class
																								.getName());
	
	private final String										featureTag;
	
	private final Map<Pair<List<String>, Category<Y>>, Integer>	idMapping;
	
	private final IScorer<LexicalEntry<Y>>						initialFixedScorer;
	private final IScorer<LexicalEntry<Y>>						initialScorer;
	
	private int													nextId			= 0;
	
	public LexicalFeatureSet(String featureTag,
			IScorer<LexicalEntry<Y>> initialLexicalScorer,
			IScorer<LexicalEntry<Y>> initialScorer) {
		this(initialLexicalScorer, featureTag, initialScorer,
				new HashMap<Pair<List<String>, Category<Y>>, Integer>());
	}
	
	private LexicalFeatureSet(IScorer<LexicalEntry<Y>> initialLexicalScorer,
			String featureTag, IScorer<LexicalEntry<Y>> initialScorer,
			Map<Pair<List<String>, Category<Y>>, Integer> idMapping) {
		this.initialScorer = initialScorer;
		this.initialFixedScorer = initialLexicalScorer;
		this.featureTag = featureTag;
		this.idMapping = idMapping;
		for (final Entry<Pair<List<String>, Category<Y>>, Integer> entry : this.idMapping
				.entrySet()) {
			if (entry.getValue() >= nextId) {
				nextId = entry.getValue() + 1;
			}
		}
	}
	
	public static <X, Y> IDecoder<LexicalFeatureSet<X, Y>> getDecoder(
			DecoderHelper<Y> decoderHelper) {
		return new Decoder<X, Y>(decoderHelper);
	}
	
	@Override
	public boolean addEntry(LexicalEntry<Y> entry, IHashVector theta) {
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
	public boolean addFixedEntry(LexicalEntry<Y> entry,
			IHashVector prametersVector) {
		if (indexOf(entry) < 0) {
			final int index = createIndex(entry);
			// initialize the parameters for the given lexical entry
			prametersVector.set(featureTag, String.valueOf(index),
					initialFixedScorer.score(entry));
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
		
		for (final Entry<Pair<List<String>, Category<Y>>, Integer> entry : idMapping
				.entrySet()) {
			final int index = entry.getValue();
			final double weight = theta.get(featureTag, String.valueOf(index));
			weights.add(Triplet.of(
					new KeyArgs(featureTag, String.valueOf(index)), weight,
					entry.getKey().toString()));
		}
		
		return weights;
	}
	
	public int indexOf(LexicalEntry<Y> entry) {
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
	public double score(LexicalEntry<Y> entry, IHashVector theta) {
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
	public void setFeats(LexicalEntry<Y> entry, IHashVector feats) {
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
	
	private int createIndex(LexicalEntry<Y> entry) {
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
	
	public static class Builder<X, Y> {
		
		/**
		 * The name tag for the features.
		 */
		private String						featureTag			= "LEX";
		
		/**
		 * Scorer for fixed lexical entries.
		 */
		private IScorer<LexicalEntry<Y>>	initialFixedScorer	= new UniformScorer<LexicalEntry<Y>>(
																		0.0);
		/**
		 * Scorer for new lexical entries.
		 */
		private IScorer<LexicalEntry<Y>>	initialScorer		= new UniformScorer<LexicalEntry<Y>>(
																		0.0);
		
		public LexicalFeatureSet<X, Y> build() {
			return new LexicalFeatureSet<X, Y>(featureTag, initialFixedScorer,
					initialScorer);
		}
		
		public Builder<X, Y> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}
		
		public Builder<X, Y> setInitialFixedScorer(
				IScorer<LexicalEntry<Y>> initialFixedScorer) {
			this.initialFixedScorer = initialFixedScorer;
			return this;
		}
		
		public Builder<X, Y> setInitialScorer(
				IScorer<LexicalEntry<Y>> initialScorer) {
			this.initialScorer = initialScorer;
			return this;
		}
	}
	
	private static class Decoder<X, Y> extends
			AbstractDecoderIntoFile<LexicalFeatureSet<X, Y>> {
		
		private static final int		VERSION	= 2;
		
		private final DecoderHelper<Y>	decoderHelper;
		
		public Decoder(DecoderHelper<Y> decoderHelper) {
			super(LexicalFeatureSet.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexicalFeatureSet<X, Y> object) {
			final HashMap<String, String> attributes = new HashMap<String, String>();
			
			attributes.put("featureTag", object.featureTag);
			
			return attributes;
		}
		
		@Override
		protected LexicalFeatureSet<X, Y> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final String featureTag = attributes.get("featureTag");
			
			// Read scorers from external files
			final IScorer<LexicalEntry<Y>> initialScorer = DecoderServices
					.decode(dependentFiles.get("initialScorer"), decoderHelper);
			final IScorer<LexicalEntry<Y>> initialFixedScorer = DecoderServices
					.decode(dependentFiles.get("initialFixedScorer"),
							decoderHelper);
			
			// Read IDs mapping
			final Map<Pair<List<String>, Category<Y>>, Integer> idMapping = new HashMap<Pair<List<String>, Category<Y>>, Integer>();
			// Read the header of the map
			readTextLine(reader);
			String line;
			while (!(line = readTextLine(reader)).equals("LEX_ITEMS_MAP_END")) {
				final String split[] = line.split("\t");
				if (attributes.get(VERSION_ATTRIBUTE_NAME).equals("1")) {
					// Deprecated representation --- backward compatibility
					final LexicalEntry<Y> lexicalEntry = LexicalEntry.parse(
							split[0], decoderHelper.getCategoryServices(),
							Lexicon.SAVED_LEXICON_ORIGIN);
					final int id = Integer.valueOf(split[1]);
					idMapping.put(
							Pair.of(lexicalEntry.getTokens(),
									lexicalEntry.getCategory()), id);
				} else {
					final List<String> tokens = Arrays.asList(Arrays
							.copyOfRange(split, 0, split.length - 2));
					final Category<Y> category = decoderHelper
							.getCategoryServices().parse(
									split[split.length - 2]);
					final int id = Integer.valueOf(split[split.length - 1]);
					idMapping.put(Pair.of(tokens, category), id);
				}
			}
			
			return new LexicalFeatureSet<X, Y>(initialFixedScorer, featureTag,
					initialScorer, idMapping);
		}
		
		@Override
		protected void doEncode(LexicalFeatureSet<X, Y> object,
				BufferedWriter writer) throws IOException {
			// Store mapping of lexical items to feature IDs
			writer.write("LEX_ITEMS_MAP_START\n");
			for (final Entry<Pair<List<String>, Category<Y>>, Integer> entry : object.idMapping
					.entrySet()) {
				writer.write(String.format("%s\t%s\t%d\n", ListUtils.join(entry
						.getKey().first(), "\t"), entry.getKey().second(),
						entry.getValue()));
			}
			writer.write("LEX_ITEMS_MAP_END\n");
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexicalFeatureSet<X, Y> object, File directory, File parentFile)
				throws IOException {
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
