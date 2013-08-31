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
package edu.uw.cs.lil.tiny.parser.ccg.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.ILexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSetImmutable;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoDir;
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
import edu.uw.cs.utils.composites.Triplet;

/**
 * A complete parsing model, including features, parameters and a lexicon.
 * 
 * @author Yoav Artzi
 * @param <DI>
 * @param <MR>
 *            Type of semantics (output).
 */
public class Model<DI extends IDataItem<?>, MR> implements
		IModelImmutable<DI, MR> {
	private static final long									serialVersionUID	= -2202596624826388636L;
	
	private final List<IIndependentLexicalFeatureSet<DI, MR>>	lexicalFeatures;
	
	private final ILexicon<MR>									lexicon;
	
	private final List<IParseFeatureSet<DI, MR>>				parseFeatures;
	
	private final IHashVector									theta;
	
	protected Model(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures, ILexicon<MR> lexicon) {
		this(lexicalFeatures, parseFeatures, lexicon, HashVectorFactory
				.create());
	}
	
	protected Model(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures, ILexicon<MR> lexicon,
			IHashVector theta) {
		this.lexicalFeatures = Collections.unmodifiableList(lexicalFeatures);
		this.parseFeatures = Collections.unmodifiableList(parseFeatures);
		this.lexicon = lexicon;
		this.theta = theta;
	}
	
	public static <DI extends IDataItem<?>, MR> IDecoder<Model<DI, MR>> getDecoder(
			DecoderHelper<MR> decoderHelper) {
		return new Decoder<DI, MR>(decoderHelper);
	}
	
	public void addFixedLexicalEntries(Collection<LexicalEntry<MR>> entries) {
		for (final LexicalEntry<MR> entry : entries) {
			addFixedLexicalEntry(entry);
		}
	}
	
	public void addFixedLexicalEntries(ILexicon<MR> entries) {
		addFixedLexicalEntries(entries.toCollection());
	}
	
	/**
	 * Adds a batch of lexical items. The items are indifferent to one another
	 * when getting their initial scores.
	 * 
	 * @param entries
	 * @return 'true' iff at least one new entry was introduced to the lexicon.
	 */
	public boolean addLexEntries(Collection<LexicalEntry<MR>> entries) {
		final Set<LexicalEntry<MR>> addedEntries = lexicon.addAll(entries);
		for (final LexicalEntry<MR> entry : addedEntries) {
			for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
				lfs.addEntry(entry, theta);
			}
		}
		return !addedEntries.isEmpty();
	}
	
	/**
	 * Add a lexical entry to the lexicon and update the feature vector. Will
	 * not add already existing ones.
	 * 
	 * @param entry
	 * @return 'true' iff a new entry was introduced to the lexicon.
	 */
	public boolean addLexEntry(LexicalEntry<MR> entry) {
		final Set<LexicalEntry<MR>> addedEntries = lexicon.add(entry);
		for (final LexicalEntry<MR> addedEntry : addedEntries) {
			for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
				lfs.addEntry(addedEntry, theta);
			}
		}
		return !addedEntries.isEmpty();
	}
	
	@Override
	public IHashVector computeFeatures(IParseStep<MR> parseStep, DI dataItem) {
		return computeFeatures(parseStep, HashVectorFactory.create(), dataItem);
	}
	
	@Override
	public IHashVector computeFeatures(IParseStep<MR> parseStep,
			IHashVector features, DI dataItem) {
		for (final IParseFeatureSetImmutable<DI, MR> featureSet : parseFeatures) {
			featureSet.setFeats(parseStep, features, dataItem);
		}
		for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
			lfs.setFeats(parseStep, features, dataItem);
		}
		return features;
	}
	
	@Override
	public IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry) {
		return computeFeatures(lexicalEntry, HashVectorFactory.create());
	}
	
	@Override
	public IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry,
			IHashVector features) {
		for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
			lfs.setFeats(lexicalEntry, features);
		}
		return features;
	}
	
	@Override
	public IDataItemModel<MR> createDataItemModel(DI dataItem) {
		return new DataItemModel<DI, MR>(this, dataItem);
	}
	
	public List<IIndependentLexicalFeatureSet<DI, MR>> getLexicalFeatures() {
		return lexicalFeatures;
	}
	
	@Override
	public ILexicon<MR> getLexicon() {
		return lexicon;
	}
	
	public List<IParseFeatureSet<DI, MR>> getParseFeatures() {
		return parseFeatures;
	}
	
	@Override
	public IHashVector getTheta() {
		return theta;
	}
	
	public boolean isValidWeightVector(IHashVectorImmutable vector) {
		for (final IIndependentLexicalFeatureSet<DI, MR> set : lexicalFeatures) {
			if (!set.isValidWeightVector(vector)) {
				return false;
			}
		}
		for (final IParseFeatureSet<DI, MR> set : parseFeatures) {
			if (!set.isValidWeightVector(vector)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public double score(IParseStep<MR> parseStep, DI dataItem) {
		double score = 0.0;
		// Parse features
		for (final IParseFeatureSet<DI, MR> featureSet : parseFeatures) {
			score += featureSet.score(parseStep, theta, dataItem);
		}
		// Lexical features
		for (final IIndependentLexicalFeatureSet<DI, MR> featureSet : lexicalFeatures) {
			score += featureSet.score(parseStep, theta, dataItem);
		}
		return score;
	}
	
	@Override
	public double score(LexicalEntry<MR> entry) {
		double score = 0.0;
		for (final IIndependentLexicalFeatureSet<DI, MR> featureSet : lexicalFeatures) {
			score += featureSet.score(entry, theta);
		}
		return score;
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append("Lexical feature sets:\n");
		for (final ILexicalFeatureSet<DI, MR> featureSet : lexicalFeatures) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Parse feature sets:\n");
		for (final IParseFeatureSet<DI, MR> featureSet : parseFeatures) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Lexicon [size=").append(lexicon.size()).append("]\n");
		ret.append(lexiconToString(lexicon));
		ret.append("Feature vector\n").append(theta);
		
		return ret.toString();
	}
	
	private boolean addFixedLexicalEntry(LexicalEntry<MR> entry) {
		final Set<LexicalEntry<MR>> addedEntries = lexicon.add(entry);
		
		// Only the original entry is considered fixed
		for (final ILexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
			lfs.addFixedEntry(entry, theta);
		}
		
		// The rest are updated as regular entries
		for (final LexicalEntry<MR> addedEntry : addedEntries) {
			if (!addedEntries.equals(entry)) {
				for (final ILexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
					lfs.addEntry(addedEntry, theta);
				}
			}
		}
		
		return !addedEntries.isEmpty();
	}
	
	private String lexiconToString(ILexicon<MR> lex) {
		final StringBuffer result = new StringBuffer();
		final Iterator<LexicalEntry<MR>> i = lex.toCollection().iterator();
		while (i.hasNext()) {
			final LexicalEntry<MR> entry = i.next();
			result.append(entry);
			result.append(" [").append(score(entry)).append("]");
			
			result.append("\n");
		}
		return result.toString();
	}
	
	public static class Builder<DI extends IDataItem<?>, MR> {
		private final List<IIndependentLexicalFeatureSet<DI, MR>>	lexicalFeatures	= new LinkedList<IIndependentLexicalFeatureSet<DI, MR>>();
		private ILexicon<MR>										lexicon			= new Lexicon<MR>();
		private final List<IParseFeatureSet<DI, MR>>				parseFeatures	= new LinkedList<IParseFeatureSet<DI, MR>>();
		
		public Builder<DI, MR> addLexicalFeatureSet(
				IIndependentLexicalFeatureSet<DI, MR> featureSet) {
			lexicalFeatures.add(featureSet);
			return this;
		}
		
		public Builder<DI, MR> addParseFeatureSet(
				IParseFeatureSet<DI, MR> featureSet) {
			parseFeatures.add(featureSet);
			return this;
		}
		
		public Model<DI, MR> build() {
			return new Model<DI, MR>(
					Collections.unmodifiableList(lexicalFeatures),
					Collections.unmodifiableList(parseFeatures), lexicon);
		}
		
		public Builder<DI, MR> setLexicon(ILexicon<MR> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
	}
	
	private static class Decoder<DI extends IDataItem<?>, MR> extends
			AbstractDecoderIntoDir<Model<DI, MR>> {
		private static final String		LEXICAL_FEATURE_SET_FILE_EXTENSION	= ".lex.feat";
		private static final String		LEXICON_FILE_NAME					= "lexicon";
		
		private static final String		PARSE_FEATURE_SET_FILE_EXTENSION	= ".parse.feat";
		
		private static final int		VERSION								= 1;
		
		private static final String		WEIGHTS_FILE_NAME_EXTENSION			= ".weights";
		
		private final DecoderHelper<MR>	decoderHelper;
		
		public Decoder(DecoderHelper<MR> decoderHelper) {
			super(Model.class);
			this.decoderHelper = decoderHelper;
		}
		
		private static IHashVector readWeightVector(File file)
				throws IOException {
			final IHashVector weights = HashVectorFactory.create();
			final BufferedReader reader = new BufferedReader(new FileReader(
					file));
			try {
				String line;
				while ((line = readTextLine(reader)) != null) {
					final String[] split = line.split("\t");
					final String[] keySplit = split[0].split("#");
					switch (keySplit.length) {
						case 1:
							weights.set(keySplit[0], Double.valueOf(split[1]));
							break;
						case 2:
							weights.set(keySplit[0], keySplit[1],
									Double.valueOf(split[1]));
							break;
						case 3:
							weights.set(keySplit[0], keySplit[1], keySplit[2],
									Double.valueOf(split[1]));
							break;
						case 4:
							weights.set(keySplit[0], keySplit[1], keySplit[2],
									keySplit[3], Double.valueOf(split[1]));
							break;
						case 5:
							weights.set(keySplit[0], keySplit[1], keySplit[2],
									keySplit[3], keySplit[4],
									Double.valueOf(split[1]));
							break;
						default:
							throw new IllegalStateException(
									"Invalid number of KeyArgs arguments: "
											+ split[0]);
					}
				}
			} finally {
				reader.close();
			}
			return weights;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		/**
		 * Write the weights of a specific feature set to a file. Allows the
		 * feature set to create comments.
		 * 
		 * @param file
		 * @param weightVector
		 * @param featureSet
		 * @throws IOException
		 */
		private void writeWeightVector(File file, IHashVector weightVector,
				IFeatureSet featureSet) throws IOException {
			final List<Triplet<KeyArgs, Double, String>> featureWeights = featureSet
					.getFeatureWeights(weightVector);
			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					file));
			for (final Triplet<KeyArgs, Double, String> weightTriplet : featureWeights) {
				writer.write(String.format("%s\t%.20f", weightTriplet.first(),
						weightTriplet.second()));
				if (weightTriplet.third() != null) {
					writer.write(String.format(" // %s", weightTriplet.third()));
				}
				writer.write("\n");
			}
			
			writer.close();
		}
		
		@Override
		protected Map<String, String> createAttributesMap(Model<DI, MR> object) {
			// No attributes, return an empty map
			return new HashMap<String, String>();
		}
		
		@Override
		protected Model<DI, MR> decodeFromDir(Map<String, String> attributes,
				File dir) throws IOException {
			
			// Create lexical features from *.lex.feat files
			final String[] lexicalFeatureSetFiles = dir
					.list(new FilenameFilter() {
						
						@Override
						public boolean accept(File filedir, String name) {
							return name
									.endsWith(LEXICAL_FEATURE_SET_FILE_EXTENSION);
						}
					});
			final List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures = new ArrayList<IIndependentLexicalFeatureSet<DI, MR>>(
					lexicalFeatureSetFiles.length);
			for (final String filename : lexicalFeatureSetFiles) {
				@SuppressWarnings("unchecked")
				final IIndependentLexicalFeatureSet<DI, MR> featureSet = (IIndependentLexicalFeatureSet<DI, MR>) (DecoderServices
						.decode(getClassName(filename,
								LEXICAL_FEATURE_SET_FILE_EXTENSION), new File(
								dir, filename), decoderHelper));
				lexicalFeatures.add(featureSet);
			}
			
			// Create parse features from *.parse.feat files
			final String[] parseFeatureSetFiles = dir
					.list(new FilenameFilter() {
						
						@Override
						public boolean accept(File filedir, String name) {
							return name
									.endsWith(PARSE_FEATURE_SET_FILE_EXTENSION);
						}
					});
			final List<IParseFeatureSet<DI, MR>> parseFeatures = new ArrayList<IParseFeatureSet<DI, MR>>(
					parseFeatureSetFiles.length);
			for (final String filename : parseFeatureSetFiles) {
				@SuppressWarnings("unchecked")
				final IParseFeatureSet<DI, MR> featureSet = (IParseFeatureSet<DI, MR>) DecoderServices
						.decode(getClassName(filename,
								PARSE_FEATURE_SET_FILE_EXTENSION), new File(
								dir, filename), decoderHelper);
				parseFeatures.add(featureSet);
			}
			
			// Create the lexicon from model.lex file
			final ILexicon<MR> lexicon = DecoderServices.decode(new File(dir,
					LEXICON_FILE_NAME), decoderHelper);
			
			// Read feature weights from all *.weights files (e.g.
			// *.lex.feat.weights, *.parse.feat.weights)
			final IHashVector theta = HashVectorFactory.create();
			final String[] featureWeightsFiles = dir.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File filedir, String name) {
					return name.endsWith(".lex.feat.weights")
							|| name.endsWith(".parse.feat.weights");
				}
			});
			for (final String filename : featureWeightsFiles) {
				// Read each weight vector and update it into theta
				readWeightVector(new File(dir, filename)).addTimesInto(1.0,
						theta);
			}
			
			return new Model<DI, MR>(lexicalFeatures, parseFeatures, lexicon,
					theta);
		}
		
		@Override
		protected void writeFiles(Model<DI, MR> object, File dir)
				throws IOException {
			// For each parse feature set, write the feature set file. This file
			// is
			// used to create the feature set and doesn't contain the weights
			for (final IParseFeatureSet<DI, MR> featureSet : object.parseFeatures) {
				DecoderServices.encode(featureSet, new File(dir, featureSet
						.getClass().getName()
						+ PARSE_FEATURE_SET_FILE_EXTENSION), decoderHelper);
			}
			
			// For each lexical feature set, write the feature set file. This
			// file
			// is used to create the feature set and doesn't contain the weights
			for (final IIndependentLexicalFeatureSet<DI, MR> featureSet : object.lexicalFeatures) {
				DecoderServices.encode(featureSet, new File(dir, featureSet
						.getClass().getName()
						+ LEXICAL_FEATURE_SET_FILE_EXTENSION), decoderHelper);
			}
			
			// For each parse feature set, get all the members of
			// theta that are assigned to it, and dump them to a
			// *.parse.feat.weights
			// file
			for (final IParseFeatureSet<DI, MR> featureSet : object.parseFeatures) {
				writeWeightVector(new File(dir, featureSet.getClass().getName()
						+ PARSE_FEATURE_SET_FILE_EXTENSION
						+ WEIGHTS_FILE_NAME_EXTENSION), object.theta,
						featureSet);
			}
			
			// For each lexical feature set, get all the members
			// of theta that are assigned to it, and dump them to a
			// *.lex.feat.weights
			// file. For each lexical entry, add print the entry itself as a
			// comment
			// at the end of the line.
			for (final IFeatureSet featureSet : object.lexicalFeatures) {
				writeWeightVector(new File(dir, featureSet.getClass().getName()
						+ LEXICAL_FEATURE_SET_FILE_EXTENSION
						+ WEIGHTS_FILE_NAME_EXTENSION), object.theta,
						featureSet);
			}
			
			// Dump the lexicon to a file
			DecoderServices.encode(object.lexicon, new File(dir,
					LEXICON_FILE_NAME), decoderHelper);
		}
		
	}
	
}
