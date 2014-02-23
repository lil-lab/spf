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
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.AbstractScaledScorerCreator;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Returns a score for a Lexeme<MR> that is an average over the pairwise scores
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
	
	public static class Creator extends
			AbstractScaledScorerCreator<Lexeme, LexemeCooccurrenceScorer> {
		
		@Override
		public LexemeCooccurrenceScorer createScorer(Parameters parameters,
				IResourceRepository resourceRepo) {
			final File file = parameters.getAsFile("file");
			try {
				return new LexemeCooccurrenceScorer(file);
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
					.addParam("scale", "double", "Scaling factor")
					.addParam("file", "file",
							"File to initialize cooccurrence table").build();
		}
		
	}
	
}
