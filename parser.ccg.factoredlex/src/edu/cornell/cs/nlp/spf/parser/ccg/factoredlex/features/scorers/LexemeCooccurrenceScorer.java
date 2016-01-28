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
package edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.scorers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.AbstractScaledScorerCreator;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * Returns a score for a {@link Lexeme} that is an average over the pairwise
 * scores for each word in the phrase and constant in the logical expression.
 * The score for each (word, constant) pair is load from a file. For now, these
 * score are typically computed via IBM Alignment model 1, with the Giza++
 * toolkit.
 *
 * @author Yoav Artzi
 */
public class LexemeCooccurrenceScorer implements ISerializableScorer<Lexeme> {

	private static final long						serialVersionUID	= 3293458533645197970L;
	private final boolean							lowercase;
	protected final TObjectDoubleHashMap<String>	pMIS;

	public LexemeCooccurrenceScorer(File f, boolean lowercase)
			throws IOException {
		this(readStatsFile(f), lowercase);
	}

	public LexemeCooccurrenceScorer(TObjectDoubleHashMap<String> scores,
			boolean lowercase) {
		this.pMIS = scores;
		this.lowercase = lowercase;
	}

	protected static TObjectDoubleHashMap<String> readStats(
			BufferedReader reader) throws IOException {
		final TObjectDoubleHashMap<String> pmis = new TObjectDoubleHashMap<String>();

		String line = reader.readLine();
		while (line != null) { // for each line in the file
			line = line.trim();
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

	protected static TObjectDoubleHashMap<String> readStatsFile(File f)
			throws IOException {
		try (final BufferedReader reader = new BufferedReader(
				new FileReader(f))) {
			return readStats(reader);
		}
	}

	public double getScore(String token, LogicalConstant exp) {
		return indexScore(token, exp.getName());
	}

	@Override
	public double score(Lexeme lexeme) {
		return score(lexeme.getTokens(), lexeme.getConstants());
	}

	private double indexScore(String token, String constantName) {
		return pMIS.get(constantName + "  ::  "
				+ (lowercase ? token.toLowerCase() : token));
	}

	private double score(TokenSeq tokens, List<LogicalConstant> constants) {
		double totalScore = 0.0;
		int numConstants = 0;
		for (final LogicalConstant constant : constants) {
			++numConstants;
			for (int i = 0; i < tokens.size(); ++i) {
				totalScore += indexScore(tokens.get(i), constant.getName());
			}
		}
		return totalScore / (tokens.size() * (numConstants + 1));
	}

	public static class Creator extends
			AbstractScaledScorerCreator<Lexeme, LexemeCooccurrenceScorer> {

		@Override
		public LexemeCooccurrenceScorer createScorer(Parameters params,
				IResourceRepository repo) {
			final File file = params.getAsFile("file");
			try {
				return new LexemeCooccurrenceScorer(file,
						params.getAsBoolean("lower", false));
			} catch (final IOException e) {
				throw new IllegalStateException(
						"Failed to load lexical cooccurrence scorer from: "
								+ file);
			}
		}

		@Override
		public String type() {
			return "scorer.lexeme.cooc";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					LexemeCooccurrenceScorer.class)
							.setDescription(
									"Scorer to score a lexical entry based on its lexeme and a cooccurrence table of constants and tokens")
							.addParam("lower", Boolean.class,
									"Lower case tokens (default: false)")
							.addParam("scale", Double.class,
									"Scaling factor (default: 1.0)")
							.addParam("file", "file",
									"File to initialize cooccurrence table")
							.build();
		}

	}

}
