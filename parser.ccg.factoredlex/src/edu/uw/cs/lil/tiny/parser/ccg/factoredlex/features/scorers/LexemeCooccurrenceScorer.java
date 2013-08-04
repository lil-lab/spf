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
package edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.scorers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.Lexeme;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Returns a score for a Lexeme<Y> that is an average over the pairwise scores
 * for each word in the phrase and constant in the logical expression. The score
 * for each (word, constant) pair is load from a file. For now, these score are
 * typically computed via IBM Alignment model 1, with the Giza++ toolkit.
 * 
 * @author Luke Zettlemoyer
 * @author Yoav Artzi
 */
public class LexemeCooccurrenceScorer implements ISerializableScorer<Lexeme> {
	
	private static final long			serialVersionUID	= 3293458533645197970L;
	protected final Map<String, Double>	pMIS;
	
	public LexemeCooccurrenceScorer(File f) throws IOException {
		this(readStatsFile(f));
	}
	
	public LexemeCooccurrenceScorer(Map<String, Double> scores) {
		this.pMIS = scores;
	}
	
	public static IDecoder<LexemeCooccurrenceScorer> getDecoder() {
		return new Decoder();
	}
	
	protected static Map<String, Double> readStats(BufferedReader reader)
			throws IOException {
		final Map<String, Double> pmis = new HashMap<String, Double>();
		
		String line = reader.readLine();
		while (line != null) { // for each line in the file
			line = line.trim();
			line = line.split("\\s*//")[0];
			if (!line.equals("")) {
				final String[] tokens = line.split("..\\:\\:..");
				final String id = tokens[0] + "  ::  " + tokens[1];
				final double score = Double.parseDouble(tokens[2]);
				pmis.put(id, new Double(score));
			}
			line = reader.readLine();
		}
		
		return pmis;
	}
	
	protected static Map<String, Double> readStatsFile(File f)
			throws IOException {
		// TODO [luke] [save2] Why do we need this method? Can't we just use the
		// storage system to load from a file? It's more consistent. Now we have
		// two formats that basically store the same structure.
		final BufferedReader reader = new BufferedReader(new FileReader(f));
		try {
			return readStats(reader);
		} finally {
			reader.close();
		}
	}
	
	public double getScore(String token, LogicalConstant exp) {
		return indexScore(token, exp.getName());
	}
	
	@Override
	public double score(Lexeme lexeme) {
		return score(lexeme.getTokens(),
				HashMultiset.create(lexeme.getConstants()));
	}
	
	private double indexScore(String token, String constantName) {
		final Double d = pMIS.get(constantName + "  ::  " + token);
		if (d == null) {
			return 0.0;
		}
		return d.doubleValue();
	}
	
	private double score(List<String> tokens,
			Multiset<LogicalConstant> constants) {
		double totalScore = 0.0;
		int numConstants = 0;
		for (final Entry<LogicalConstant> entry : constants.entrySet()) {
			if (!LogicLanguageServices.isCoordinationPredicate(entry
					.getElement())) {
				numConstants += entry.getCount();
				for (final String word : tokens) {
					totalScore += indexScore(word, entry.getElement().getName())
							* entry.getCount();
				}
			}
		}
		
		return totalScore / (tokens.size() * (numConstants + 1));
	}
	
	private static class Decoder extends
			AbstractDecoderIntoFile<LexemeCooccurrenceScorer> {
		private static final int	VERSION	= 1;
		
		public Decoder() {
			super(LexemeCooccurrenceScorer.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexemeCooccurrenceScorer object) {
			// No special attributes
			return new HashMap<String, String>();
		}
		
		@Override
		protected LexemeCooccurrenceScorer doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			return new LexemeCooccurrenceScorer(readStats(reader));
		}
		
		@Override
		protected void doEncode(LexemeCooccurrenceScorer object,
				BufferedWriter writer) throws IOException {
			// Write one pair per line, with the score
			for (final Map.Entry<String, Double> entry : object.pMIS.entrySet()) {
				writer.write(entry.getKey() + "  ::  " + entry.getValue());
				writer.write("\n");
			}
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexemeCooccurrenceScorer object, File directory, File parentFile)
				throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
	
}
