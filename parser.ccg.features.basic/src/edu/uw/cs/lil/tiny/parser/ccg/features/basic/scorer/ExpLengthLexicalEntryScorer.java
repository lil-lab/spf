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
package edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Lexical entry scorer that takes the number of tokens into account. coef *
 * n^exp, where n is the number of tokens in the lexical entry.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public class ExpLengthLexicalEntryScorer<Y> implements
		ISerializableScorer<LexicalEntry<Y>> {
	
	private static final long	serialVersionUID	= 3086307252253251483L;
	private final double		coef;
	private final double		exponent;
	
	public ExpLengthLexicalEntryScorer(double coef, double exponent) {
		this.coef = coef;
		this.exponent = exponent;
	}
	
	public static <Y> IDecoder<ExpLengthLexicalEntryScorer<Y>> getDecoder() {
		return new Decoder<Y>();
	}
	
	@Override
	public double score(LexicalEntry<Y> lex) {
		return coef * Math.pow(lex.getTokens().size(), exponent);
	}
	
	private static class Decoder<Y> extends
			AbstractDecoderIntoFile<ExpLengthLexicalEntryScorer<Y>> {
		
		private static final int	VERSION	= 1;
		
		protected Decoder() {
			super(ExpLengthLexicalEntryScorer.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				ExpLengthLexicalEntryScorer<Y> object) {
			final Map<String, String> attrbiutes = new HashMap<String, String>();
			attrbiutes.put("coef", Double.toString(object.coef));
			attrbiutes.put("exponent", Double.toString(object.exponent));
			return attrbiutes;
		}
		
		@Override
		protected ExpLengthLexicalEntryScorer<Y> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final double baseScore = Double.valueOf(attributes.get("coef"));
			final double exponent = Double.valueOf(attributes.get("exponent"));
			
			return new ExpLengthLexicalEntryScorer<Y>(baseScore, exponent);
		}
		
		@Override
		protected void doEncode(ExpLengthLexicalEntryScorer<Y> object,
				BufferedWriter writer) throws IOException {
			// Nothing to write
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				ExpLengthLexicalEntryScorer<Y> object, File directory,
				File parentFile) throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
}
