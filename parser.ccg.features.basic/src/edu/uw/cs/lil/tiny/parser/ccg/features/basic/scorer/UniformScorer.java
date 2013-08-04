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

import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Returns a constant value for every lexical item.
 * 
 * @author Luke Zettlemoyer
 * @param <E>
 */
public class UniformScorer<Y> implements ISerializableScorer<Y> {
	
	private static final long	serialVersionUID	= 5896129775849488211L;
	private final double		score;
	
	public UniformScorer(double value) {
		score = value;
	}
	
	public static <Y> IDecoder<UniformScorer<Y>> getDecoder() {
		return new Decoder<Y>();
	}
	
	@Override
	public double score(Y lex) {
		return score;
	}
	
	private static class Decoder<Y> extends
			AbstractDecoderIntoFile<UniformScorer<Y>> {
		
		private static final int	VERSION	= 1;
		
		protected Decoder() {
			super(UniformScorer.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				UniformScorer<Y> object) {
			final Map<String, String> attrbiutes = new HashMap<String, String>();
			attrbiutes.put("score", Double.toString(object.score));
			return attrbiutes;
		}
		
		@Override
		protected UniformScorer<Y> doDecode(Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final double score = Double.valueOf(attributes.get("score"));
			
			return new UniformScorer<Y>(score);
		}
		
		@Override
		protected void doEncode(UniformScorer<Y> object, BufferedWriter writer)
				throws IOException {
			// Nothing to write
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				UniformScorer<Y> object, File directory, File parentFile)
				throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
	
}
