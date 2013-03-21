/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.ILexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSetImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.AbstractDecoderIntoDir;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.DecoderServices;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
import edu.uw.cs.utils.composites.Triplet;

/**
 * A complete parsing model, including features, parameters and a lexicon.
 * 
 * @author Yoav Artzi
 * @param <X>
 * @param <Y>
 *            Type of semantics (output).
 */
public class Model<X, Y> implements IModelImmutable<X, Y> {
	private final List<IIndependentLexicalFeatureSet<X, Y>>	lexicalFeatures;
	
	private final ILexicon<Y>								lexicon;
	
	private final List<IParseFeatureSet<X, Y>>				parseFeatures;
	
	private final IHashVector								theta;
	
	protected Model(List<IIndependentLexicalFeatureSet<X, Y>> lexicalFeatures,
			List<IParseFeatureSet<X, Y>> parseFeatures, ILexicon<Y> lexicon) {
		this(lexicalFeatures, parseFeatures, lexicon, HashVectorFactory
				.create());
	}
	
	protected Model(List<IIndependentLexicalFeatureSet<X, Y>> lexicalFeatures,
			List<IParseFeatureSet<X, Y>> parseFeatures, ILexicon<Y> lexicon,
			IHashVector theta) {
		this.lexicalFeatures = Collections.unmodifiableList(lexicalFeatures);
		this.parseFeatures = Collections.unmodifiableList(parseFeatures);
		this.lexicon = lexicon;
		this.theta = theta;
	}
	
	public static <X, Y> IDecoder<Model<X, Y>> getDecoder(
			DecoderHelper<Y> decoderHelper) {
		return new Decoder<X, Y>(decoderHelper);
	}
	
	public void addFixedLexicalEntries(Collection<LexicalEntry<Y>> entries) {
		for (final LexicalEntry<Y> entry : entries) {
			addFixedLexicalEntry(entry);
		}
	}
	
	public void addFixedLexicalEntries(ILexicon<Y> entries) {
		addFixedLexicalEntries(entries.toCollection());
	}
	
	/**
	 * Adds a batch of lexical items. The items are indifferent to one another
	 * when getting their initial scores.
	 * 
	 * @param entries
	 */
	public void addLexEntries(Collection<LexicalEntry<Y>> entries) {
		for (final LexicalEntry<Y> entry : entries) {
			for (final IIndependentLexicalFeatureSet<X, Y> lfs : lexicalFeatures) {
				lfs.addEntry(entry, theta);
			}
		}
		for (final LexicalEntry<Y> entry : entries) {
			lexicon.add(entry);
		}
	}
	
	/**
	 * Add a lexical entry to the lexicon and update the feature vector. Will
	 * not add already existing ones.
	 * 
	 * @param entry
	 * @return 'true' iff a new entry was introduced to the lexicon. can be used
	 *         to update the features without adding anything new to the
	 *         lexicon.
	 */
	public boolean addLexEntry(LexicalEntry<Y> entry) {
		for (final IIndependentLexicalFeatureSet<X, Y> lfs : lexicalFeatures) {
			lfs.addEntry(entry, theta);
		}
		return lexicon.add(entry);
	}
	
	@Override
	public IHashVector computeFeatures(IParseStep<Y> parseStep,
			IDataItem<X> dataItem) {
		return computeFeatures(parseStep, HashVectorFactory.create(), dataItem);
	}
	
	@Override
	public IHashVector computeFeatures(IParseStep<Y> parseStep,
			IHashVector features, IDataItem<X> dataItem) {
		for (final IParseFeatureSetImmutable<X, Y> featureSet : parseFeatures) {
			featureSet.setFeats(parseStep, features, dataItem);
		}
		for (final IIndependentLexicalFeatureSet<X, Y> lfs : lexicalFeatures) {
			lfs.setFeats(parseStep, features, dataItem);
		}
		return features;
	}
	
	@Override
	public IHashVector computeFeatures(LexicalEntry<Y> lexicalEntry) {
		return computeFeatures(lexicalEntry, HashVectorFactory.create());
	}
	
	@Override
	public IHashVector computeFeatures(LexicalEntry<Y> lexicalEntry,
			IHashVector features) {
		for (final IIndependentLexicalFeatureSet<X, Y> lfs : lexicalFeatures) {
			lfs.setFeats(lexicalEntry, features);
		}
		return features;
	}
	
	@Override
	public IDataItemModel<Y> createDataItemModel(IDataItem<X> dataItem) {
		return new DataItemModel<X, Y>(this, dataItem);
	}
	
	public List<IIndependentLexicalFeatureSet<X, Y>> getLexicalFeatures() {
		return lexicalFeatures;
	}
	
	@Override
	public ILexicon<Y> getLexicon() {
		return lexicon;
	}
	
	public List<IParseFeatureSet<X, Y>> getParseFeatures() {
		return parseFeatures;
	}
	
	@Override
	public IHashVector getTheta() {
		return theta;
	}
	
	public boolean hasLexEntry(LexicalEntry<Y> entry) {
		return lexicon.contains(entry);
	}
	
	public boolean isValidWeightVector(IHashVectorImmutable vector) {
		for (final IIndependentLexicalFeatureSet<X, Y> set : lexicalFeatures) {
			if (!set.isValidWeightVector(vector)) {
				return false;
			}
		}
		for (final IParseFeatureSet<X, Y> set : parseFeatures) {
			if (set.isValidWeightVector(vector)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public double score(IParseStep<Y> parseStep, IDataItem<X> dataItem) {
		double score = 0.0;
		// Parse features
		for (final IParseFeatureSet<X, Y> featureSet : parseFeatures) {
			score += featureSet.score(parseStep, theta, dataItem);
		}
		// Lexical features
		for (final IIndependentLexicalFeatureSet<X, Y> featureSet : lexicalFeatures) {
			score += featureSet.score(parseStep, theta, dataItem);
		}
		return score;
	}
	
	@Override
	public double score(LexicalEntry<Y> entry) {
		double score = 0.0;
		for (final IIndependentLexicalFeatureSet<X, Y> featureSet : lexicalFeatures) {
			score += featureSet.score(entry, theta);
		}
		return score;
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append("Lexical feature sets:\n");
		for (final ILexicalFeatureSet<X, Y> featureSet : lexicalFeatures) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Parse feature sets:\n");
		for (final IParseFeatureSet<X, Y> featureSet : parseFeatures) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Lexicon [size=").append(lexicon.size()).append("]\n");
		ret.append(lexicon.toString(this));
		ret.append("Feature vector\n").append(theta);
		
		return ret.toString();
	}
	
	private boolean addFixedLexicalEntry(LexicalEntry<Y> entry) {
		for (final ILexicalFeatureSet<X, Y> lfs : lexicalFeatures) {
			lfs.addFixedEntry(entry, theta);
		}
		return lexicon.add(entry);
	}
	
	public static class Builder<X, Y> {
		private final List<IIndependentLexicalFeatureSet<X, Y>>	lexicalFeatures	= new LinkedList<IIndependentLexicalFeatureSet<X, Y>>();
		private ILexicon<Y>										lexicon			= new Lexicon<Y>();
		private final List<IParseFeatureSet<X, Y>>				parseFeatures	= new LinkedList<IParseFeatureSet<X, Y>>();
		
		public Builder<X, Y> addLexicalFeatureSet(
				IIndependentLexicalFeatureSet<X, Y> featureSet) {
			lexicalFeatures.add(featureSet);
			return this;
		}
		
		public Builder<X, Y> addParseFeatureSet(
				IParseFeatureSet<X, Y> featureSet) {
			parseFeatures.add(featureSet);
			return this;
		}
		
		public Model<X, Y> build() {
			return new Model<X, Y>(
					Collections.unmodifiableList(lexicalFeatures),
					Collections.unmodifiableList(parseFeatures), lexicon);
		}
		
		public Builder<X, Y> setLexicon(ILexicon<Y> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
	}
	
	private static class Decoder<X, Y> extends
			AbstractDecoderIntoDir<Model<X, Y>> {
		private static final String		LEXICAL_FEATURE_SET_FILE_EXTENSION	= ".lex.feat";
		private static final String		LEXICON_FILE_NAME					= "lexicon";
		
		private static final String		PARSE_FEATURE_SET_FILE_EXTENSION	= ".parse.feat";
		
		private static final int		VERSION								= 1;
		
		private static final String		WEIGHTS_FILE_NAME_EXTENSION			= ".weights";
		
		private final DecoderHelper<Y>	decoderHelper;
		
		public Decoder(DecoderHelper<Y> decoderHelper) {
			super(Model.class);
			this.decoderHelper = decoderHelper;
		}
		
		private static IHashVector readWeightVector(File file)
				throws IOException {
			final IHashVector weights = HashVectorFactory.create();
			final BufferedReader reader = new BufferedReader(new FileReader(
					file));
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
		protected Map<String, String> createAttributesMap(Model<X, Y> object) {
			// No attributes, return an empty map
			return new HashMap<String, String>();
		}
		
		@Override
		protected Model<X, Y> decodeFromDir(Map<String, String> attributes,
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
			final List<IIndependentLexicalFeatureSet<X, Y>> lexicalFeatures = new ArrayList<IIndependentLexicalFeatureSet<X, Y>>(
					lexicalFeatureSetFiles.length);
			for (final String filename : lexicalFeatureSetFiles) {
				@SuppressWarnings("unchecked")
				final IIndependentLexicalFeatureSet<X, Y> featureSet = (IIndependentLexicalFeatureSet<X, Y>) (DecoderServices
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
			final List<IParseFeatureSet<X, Y>> parseFeatures = new ArrayList<IParseFeatureSet<X, Y>>(
					parseFeatureSetFiles.length);
			for (final String filename : parseFeatureSetFiles) {
				@SuppressWarnings("unchecked")
				final IParseFeatureSet<X, Y> featureSet = (IParseFeatureSet<X, Y>) DecoderServices
						.decode(getClassName(filename,
								PARSE_FEATURE_SET_FILE_EXTENSION), new File(
								dir, filename), decoderHelper);
				parseFeatures.add(featureSet);
			}
			
			// Create the lexicon from model.lex file
			final ILexicon<Y> lexicon = DecoderServices.decode(new File(dir,
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
			
			return new Model<X, Y>(lexicalFeatures, parseFeatures, lexicon,
					theta);
		}
		
		@Override
		protected void writeFiles(Model<X, Y> object, File dir)
				throws IOException {
			// For each parse feature set, write the feature set file. This file
			// is
			// used to create the feature set and doesn't contain the weights
			for (final IParseFeatureSet<X, Y> featureSet : object.parseFeatures) {
				DecoderServices.encode(featureSet, new File(dir, featureSet
						.getClass().getName()
						+ PARSE_FEATURE_SET_FILE_EXTENSION), decoderHelper);
			}
			
			// For each lexical feature set, write the feature set file. This
			// file
			// is used to create the feature set and doesn't contain the weights
			for (final IIndependentLexicalFeatureSet<X, Y> featureSet : object.lexicalFeatures) {
				DecoderServices.encode(featureSet, new File(dir, featureSet
						.getClass().getName()
						+ LEXICAL_FEATURE_SET_FILE_EXTENSION), decoderHelper);
			}
			
			// For each parse feature set, get all the members of
			// theta that are assigned to it, and dump them to a
			// *.parse.feat.weights
			// file
			for (final IParseFeatureSet<X, Y> featureSet : object.parseFeatures) {
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
