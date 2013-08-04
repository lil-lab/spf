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

import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.ISerializableScorer;

public class LexicalSyntaxPenaltyScorer implements
		ISerializableScorer<LexicalTemplate> {
	
	private static final long	serialVersionUID	= 7816026951225878763L;
	final double				scale;
	
	public LexicalSyntaxPenaltyScorer(double scale) {
		this.scale = scale;
	}
	
	public static IDecoder<LexicalSyntaxPenaltyScorer> getDecoder() {
		return new Decoder();
	}
	
	@Override
	public double score(LexicalTemplate template) {
		return scale * template.getTemplateCategory().numSlashes();
	}
	
	private static class Decoder extends
			AbstractDecoderIntoFile<LexicalSyntaxPenaltyScorer> {
		private static final int	VERSION	= 1;
		
		public Decoder() {
			super(LexicalSyntaxPenaltyScorer.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexicalSyntaxPenaltyScorer object) {
			final Map<String, String> attributes = new HashMap<String, String>();
			attributes.put("scale", Double.toString(object.scale));
			return attributes;
		}
		
		@Override
		protected LexicalSyntaxPenaltyScorer doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final double scale = Double.valueOf(attributes.get("scale"));
			return new LexicalSyntaxPenaltyScorer(scale);
		}
		
		@Override
		protected void doEncode(LexicalSyntaxPenaltyScorer object,
				BufferedWriter writer) throws IOException {
			// Nothing to write
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexicalSyntaxPenaltyScorer object, File directory,
				File parentFile) throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
}
