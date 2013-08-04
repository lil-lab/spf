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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.Lexeme;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Lexeme scorer that takes the number of tokens into account.
 * 
 * @author Luke Zettlemoyer
 * @param <Y>
 */
public class ExpLengthLexemeScorer implements ISerializableScorer<Lexeme> {
	
	private static final long	serialVersionUID	= -5560915581878575813L;
	private final double		coef;
	private final double		exponent;
	
	public ExpLengthLexemeScorer(double baseScore, double exponent) {
		this.coef = baseScore;
		this.exponent = exponent;
	}
	
	public static IDecoder<ExpLengthLexemeScorer> getDecoder() {
		return new Decoder();
	}
	
	@Override
	public double score(Lexeme lex) {
		return coef * Math.pow(lex.getTokens().size(), exponent);
	}
	
	private static class Decoder extends
			AbstractDecoderIntoFile<ExpLengthLexemeScorer> {
		
		private static final int	VERSION	= 1;
		
		protected Decoder() {
			super(ExpLengthLexemeScorer.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				ExpLengthLexemeScorer object) {
			final Map<String, String> attrbiutes = new HashMap<String, String>();
			attrbiutes.put("baseScore", Double.toString(object.coef));
			attrbiutes.put("exponent", Double.toString(object.exponent));
			return attrbiutes;
		}
		
		@Override
		protected ExpLengthLexemeScorer doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final double baseScore = Double
					.valueOf(attributes.get("baseScore"));
			final double exponent = Double.valueOf(attributes.get("exponent"));
			
			return new ExpLengthLexemeScorer(baseScore, exponent);
		}
		
		@Override
		protected void doEncode(ExpLengthLexemeScorer object,
				BufferedWriter writer) throws IOException {
			// Nothing to write
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				ExpLengthLexemeScorer object, File directory, File parentFile)
				throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
}
