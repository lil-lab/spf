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
import edu.uw.cs.lil.tiny.data.IDataItem;
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
import edu.uw.cs.utils.collections.ListUtils;
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
	
	private final ISerializableScorer<LexicalEntry<MR>>				initialFixedScorer;
	private final ISerializableScorer<LexicalEntry<MR>>				initialScorer;
	
	private int														nextId				= 0;
	
	public LexicalFeatureSet(String featureTag,
			ISerializableScorer<LexicalEntry<MR>> initialLexicalScorer,
			ISerializableScorer<LexicalEntry<MR>> initialScorer) {
		this(initialLexicalScorer, featureTag, initialScorer,
				new HashMap<Pair<List<String>, Category<MR>>, Integer>());
	}
	
	private LexicalFeatureSet(
			ISerializableScorer<LexicalEntry<MR>> initialLexicalScorer,
			String featureTag,
			ISerializableScorer<LexicalEntry<MR>> initialScorer,
			Map<Pair<List<String>, Category<MR>>, Integer> idMapping) {
		this.initialScorer = initialScorer;
		this.initialFixedScorer = initialLexicalScorer;
		this.featureTag = featureTag;
		this.idMapping = idMapping;
		for (final Entry<Pair<List<String>, Category<MR>>, Integer> entry : this.idMapping
				.entrySet()) {
			if (entry.getValue() >= nextId) {
				nextId = entry.getValue() + 1;
			}
		}
	}
	
	public static <DI extends IDataItem<?>, MR> IDecoder<LexicalFeatureSet<DI, MR>> getDecoder(
			DecoderHelper<MR> decoderHelper) {
		return new Decoder<DI, MR>(decoderHelper);
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
	public boolean addFixedEntry(LexicalEntry<MR> entry,
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
		private String									featureTag			= "LEX";
		
		/**
		 * Scorer for fixed lexical entries.
		 */
		private ISerializableScorer<LexicalEntry<MR>>	initialFixedScorer	= new UniformScorer<LexicalEntry<MR>>(
																					0.0);
		/**
		 * Scorer for new lexical entries.
		 */
		private ISerializableScorer<LexicalEntry<MR>>	initialScorer		= new UniformScorer<LexicalEntry<MR>>(
																					0.0);
		
		public LexicalFeatureSet<DI, MR> build() {
			return new LexicalFeatureSet<DI, MR>(featureTag,
					initialFixedScorer, initialScorer);
		}
		
		public Builder<DI, MR> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}
		
		public Builder<DI, MR> setInitialFixedScorer(
				ISerializableScorer<LexicalEntry<MR>> initialFixedScorer) {
			this.initialFixedScorer = initialFixedScorer;
			return this;
		}
		
		public Builder<DI, MR> setInitialScorer(
				ISerializableScorer<LexicalEntry<MR>> initialScorer) {
			this.initialScorer = initialScorer;
			return this;
		}
	}
	
	private static class Decoder<DI extends IDataItem<?>, MR> extends
			AbstractDecoderIntoFile<LexicalFeatureSet<DI, MR>> {
		
		private static final int		VERSION	= 2;
		
		private final DecoderHelper<MR>	decoderHelper;
		
		public Decoder(DecoderHelper<MR> decoderHelper) {
			super(LexicalFeatureSet.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexicalFeatureSet<DI, MR> object) {
			final HashMap<String, String> attributes = new HashMap<String, String>();
			
			attributes.put("featureTag", object.featureTag);
			
			return attributes;
		}
		
		@Override
		protected LexicalFeatureSet<DI, MR> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final String featureTag = attributes.get("featureTag");
			
			// Read scorers from external files
			final ISerializableScorer<LexicalEntry<MR>> initialScorer = DecoderServices
					.decode(dependentFiles.get("initialScorer"), decoderHelper);
			final ISerializableScorer<LexicalEntry<MR>> initialFixedScorer = DecoderServices
					.decode(dependentFiles.get("initialFixedScorer"),
							decoderHelper);
			
			// Read IDs mapping
			final Map<Pair<List<String>, Category<MR>>, Integer> idMapping = new HashMap<Pair<List<String>, Category<MR>>, Integer>();
			// Read the header of the map
			readTextLine(reader);
			String line;
			while (!(line = readTextLine(reader)).equals("LEX_ITEMS_MAP_END")) {
				final String split[] = line.split("\t");
				if (attributes.get(VERSION_ATTRIBUTE_NAME).equals("1")) {
					// Deprecated representation --- backward compatibility
					final LexicalEntry<MR> lexicalEntry = LexicalEntry.parse(
							split[0], decoderHelper.getCategoryServices(),
							Lexicon.SAVED_LEXICON_ORIGIN);
					final int id = Integer.valueOf(split[1]);
					idMapping.put(
							Pair.of(lexicalEntry.getTokens(),
									lexicalEntry.getCategory()), id);
				} else {
					final List<String> tokens = Arrays.asList(Arrays
							.copyOfRange(split, 0, split.length - 2));
					final Category<MR> category = decoderHelper
							.getCategoryServices().parse(
									split[split.length - 2]);
					final int id = Integer.valueOf(split[split.length - 1]);
					idMapping.put(Pair.of(tokens, category), id);
				}
			}
			
			return new LexicalFeatureSet<DI, MR>(initialFixedScorer,
					featureTag, initialScorer, idMapping);
		}
		
		@Override
		protected void doEncode(LexicalFeatureSet<DI, MR> object,
				BufferedWriter writer) throws IOException {
			// Store mapping of lexical items to feature IDs
			writer.write("LEX_ITEMS_MAP_START\n");
			for (final Entry<Pair<List<String>, Category<MR>>, Integer> entry : object.idMapping
					.entrySet()) {
				writer.write(String.format("%s\t%s\t%d\n", ListUtils.join(entry
						.getKey().first(), "\t"), entry.getKey().second(),
						entry.getValue()));
			}
			writer.write("LEX_ITEMS_MAP_END\n");
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexicalFeatureSet<DI, MR> object, File directory,
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
