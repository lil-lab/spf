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
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.Lexeme;
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
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class LexemeFeatureSet<DI extends IDataItem<?>> extends
		AbstractLexicalFeatureSet<DI, LogicalExpression> {
	
	/**
	 * The name of the default (protected) feature.
	 */
	private static final String					DEFAULT_FEAT		= "DEFAULT";
	
	private static ILogger						LOG					= LoggerFactory
																			.create(LexemeFeatureSet.class);
	
	private static final long					serialVersionUID	= 1207002303754559846L;
	
	private final String						featureTag;
	private final ISerializableScorer<Lexeme>	initialFixedScorer;
	private final ISerializableScorer<Lexeme>	initialScorer;
	private final Map<Lexeme, Integer>			lexemeIds;
	private int									nextId				= 0;
	
	private final double						scale;
	
	private LexemeFeatureSet(String featureTag,
			ISerializableScorer<Lexeme> initialFixedScorer,
			ISerializableScorer<Lexeme> initialScorer,
			Map<Lexeme, Integer> lexemeIds, double scale) {
		this.featureTag = featureTag;
		this.initialFixedScorer = initialFixedScorer;
		this.initialScorer = initialScorer;
		this.lexemeIds = lexemeIds;
		this.scale = scale;
		for (final Map.Entry<Lexeme, Integer> entry : this.lexemeIds.entrySet()) {
			if (entry.getValue() >= nextId) {
				nextId = entry.getValue() + 1;
			}
		}
	}
	
	public static <DI extends IDataItem<?>> IDecoder<LexemeFeatureSet<DI>> getDecoder(
			DecoderHelper<LogicalExpression> decoderHelper) {
		return new Decoder<DI>(decoderHelper);
	}
	
	@Override
	public boolean addEntry(LexicalEntry<LogicalExpression> entry,
			IHashVector parametersVector) {
		final Lexeme lexeme = getLexeme(entry);
		if (lexeme == null) {
			return false;
		}
		if (lexemeIds.containsKey(lexeme)) {
			return false;
		}
		final int num = getNextId();
		lexemeIds.put(lexeme, new Integer(num));
		parametersVector.set(featureTag, String.valueOf(num),
				initialScorer.score(lexeme));
		LOG.debug("Lexeme added to feature set: [%d] %s [score=%f]", num,
				lexeme, parametersVector.get(featureTag, String.valueOf(num)));
		return true;
	}
	
	@Override
	public boolean addFixedEntry(LexicalEntry<LogicalExpression> entry,
			IHashVector parametersVector) {
		final Lexeme lexeme = getLexeme(entry);
		if (lexeme == null) {
			return false;
		}
		if (lexemeIds.containsKey(lexeme)) {
			return false;
		}
		final int num = getNextId();
		lexemeIds.put(lexeme, new Integer(num));
		parametersVector.set(featureTag, String.valueOf(num),
				initialFixedScorer.score(lexeme));
		LOG.info("Lexeme added to feature set: [%d] %s [score=%f]", num,
				lexeme, parametersVector.get(featureTag, String.valueOf(num)));
		return true;
	}
	
	@Override
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(
			IHashVector theta) {
		// Get weights relevant to this feature set and attach each of them the
		// lexical entry as comment
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		
		for (final Map.Entry<Lexeme, Integer> entry : lexemeIds.entrySet()) {
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
	public double score(LexicalEntry<LogicalExpression> entry, IHashVector theta) {
		if (entry == null) {
			return 0.0;
		}
		final Lexeme lexeme = getLexeme(entry);
		if (lexeme == null) {
			// if the lexical item is not factored, we return 0. this is to
			// allow parsing with mixed lexical items (both factored and
			// unfactored).
			return 0.0;
		}
		final int i = indexOf(lexeme);
		if (i >= 0) {
			return theta.get(featureTag, String.valueOf(i)) * scale;
		}
		// return what the initial weight would be it it were added...
		return initialScorer.score(lexeme) * scale;
	}
	
	@Override
	public void setFeats(LexicalEntry<LogicalExpression> entry,
			IHashVector features) {
		if (entry == null) {
			return;
		}
		final Lexeme lexeme = getLexeme(entry);
		if (lexeme == null) {
			// if the lexical item is not factored, we don't set any features.
			// this is to allow parsing with mixed lexical items (both factored
			// and unfactored).
			return;
		}
		final int i = indexOf(lexeme);
		if (i >= 0) {
			features.set(featureTag, String.valueOf(i),
					features.get(featureTag, String.valueOf(i)) + 1.0 * scale);
		} else {
			// Case no feature set for this lexeme, set the default protected
			// feature using the initial scorer
			features.set(featureTag, DEFAULT_FEAT, initialScorer.score(lexeme)
					* scale);
		}
		
	}
	
	private Lexeme getLexeme(LexicalEntry<LogicalExpression> entry) {
		if (entry instanceof FactoredLexicon.FactoredLexicalEntry) {
			return ((FactoredLexicon.FactoredLexicalEntry) entry).getLexeme();
		}
		return null;
	}
	
	/**
	 * Returns the next ID to use and increase the ID counter by one.
	 * 
	 * @return next free ID for the entries map.
	 */
	private int getNextId() {
		return nextId++;
	}
	
	private int indexOf(Lexeme l) {
		final Integer index = lexemeIds.get(l);
		if (index == null) {
			return -1;
		} else {
			return index.intValue();
		}
	}
	
	public static class Builder<DI extends IDataItem<?>> {
		
		private String						featureTag			= "XEME";
		
		private ISerializableScorer<Lexeme>	initialFixedScorer	= new UniformScorer<Lexeme>(
																		0.0);
		
		private ISerializableScorer<Lexeme>	initialScorer		= new UniformScorer<Lexeme>(
																		0.0);
		
		private final Map<Lexeme, Integer>	lexemeIds			= new HashMap<Lexeme, Integer>();
		
		private double						scale				= 1.0;
		
		public LexemeFeatureSet<DI> build() {
			return new LexemeFeatureSet<DI>(featureTag, initialFixedScorer,
					initialScorer, lexemeIds, scale);
		}
		
		public Builder<DI> setFeatureTag(String featureTag) {
			this.featureTag = featureTag;
			return this;
		}
		
		public Builder<DI> setInitialFixedScorer(
				ISerializableScorer<Lexeme> initialFixedScorer) {
			this.initialFixedScorer = initialFixedScorer;
			return this;
		}
		
		public Builder<DI> setInitialScorer(
				ISerializableScorer<Lexeme> initialScorer) {
			this.initialScorer = initialScorer;
			return this;
		}
		
		public Builder<DI> setScale(double scale) {
			this.scale = scale;
			return this;
		}
	}
	
	private static class Decoder<DI extends IDataItem<?>> extends
			AbstractDecoderIntoFile<LexemeFeatureSet<DI>> {
		private static final int						VERSION	= 1;
		private final DecoderHelper<LogicalExpression>	decoderHelper;
		
		public Decoder(DecoderHelper<LogicalExpression> decoderHelper) {
			super(LexemeFeatureSet.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexemeFeatureSet<DI> object) {
			final HashMap<String, String> attributes = new HashMap<String, String>();
			
			attributes.put("featureTag", object.featureTag);
			attributes.put("scale", Double.toString(object.scale));
			
			return attributes;
		}
		
		@Override
		protected LexemeFeatureSet<DI> doDecode(Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final String featureTag = attributes.get("featureTag");
			final double scale = Double.parseDouble(attributes.get("scale"));
			
			// Read scorers from external files
			final ISerializableScorer<Lexeme> initialScorer = DecoderServices
					.decode(dependentFiles.get("initialScorer"), decoderHelper);
			final ISerializableScorer<Lexeme> initialFixedScorer = DecoderServices
					.decode(dependentFiles.get("initialFixedScorer"),
							decoderHelper);
			
			// Read lexItems mapping
			final Map<Lexeme, Integer> lexemeIds = new HashMap<Lexeme, Integer>();
			// Read the header of the map
			readTextLine(reader);
			String line;
			while (!(line = readTextLine(reader)).equals("LEXEME_MAP_END")) {
				final String split[] = line.split("\t");
				final Lexeme lexeme = Lexeme.parse(split[0],
						decoderHelper.getCategoryServices(),
						Lexicon.SAVED_LEXICON_ORIGIN);
				final int id = Integer.valueOf(split[1]);
				lexemeIds.put(lexeme, id);
			}
			
			return new LexemeFeatureSet<DI>(featureTag, initialFixedScorer,
					initialScorer, lexemeIds, scale);
			
		}
		
		@Override
		protected void doEncode(LexemeFeatureSet<DI> object,
				BufferedWriter writer) throws IOException {
			// Store mapping of lexemes to feature IDs
			writer.write("LEXEME_MAP_START\n");
			for (final Map.Entry<Lexeme, Integer> entry : object.lexemeIds
					.entrySet()) {
				writer.write(String.format("%s\t%d\n", entry.getKey(),
						entry.getValue()));
			}
			writer.write("LEXEME_MAP_END\n");
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexemeFeatureSet<DI> object, File directory, File parentFile)
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
