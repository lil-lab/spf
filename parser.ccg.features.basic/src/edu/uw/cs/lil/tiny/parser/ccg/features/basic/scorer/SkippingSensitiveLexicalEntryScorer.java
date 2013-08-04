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

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ISerializableScorer;

public class SkippingSensitiveLexicalEntryScorer<Y> implements
		ISerializableScorer<LexicalEntry<Y>> {
	
	private static final long				serialVersionUID	= 1517659515042456049L;
	private final IScorer<LexicalEntry<Y>>	defaultScorer;
	private final Category<Y>				emptyCategory;
	private final double					skippingCost;
	
	public SkippingSensitiveLexicalEntryScorer(Category<Y> emptyCategory,
			double skippingCost, IScorer<LexicalEntry<Y>> defaultScorer) {
		this.emptyCategory = emptyCategory;
		this.skippingCost = skippingCost;
		this.defaultScorer = defaultScorer;
	}
	
	public static <Y> IDecoder<SkippingSensitiveLexicalEntryScorer<Y>> getDecoder(
			DecoderHelper<Y> decoderHelper) {
		return new Decoder<Y>(decoderHelper);
	}
	
	@Override
	public double score(LexicalEntry<Y> lex) {
		if (emptyCategory.equals(lex.getCategory())) {
			return skippingCost;
		} else {
			return defaultScorer.score(lex);
		}
	}
	
	private static class Decoder<Y> extends
			AbstractDecoderIntoFile<SkippingSensitiveLexicalEntryScorer<Y>> {
		
		private static final int		VERSION	= 1;
		
		private final DecoderHelper<Y>	decoderHelper;
		
		public Decoder(DecoderHelper<Y> decoderHelper) {
			super(SkippingSensitiveLexicalEntryScorer.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				SkippingSensitiveLexicalEntryScorer<Y> object) {
			final Map<String, String> attributes = new HashMap<String, String>();
			
			// Skipping cost
			attributes
					.put("skippingCost", Double.toString(object.skippingCost));
			
			return attributes;
		}
		
		@Override
		protected SkippingSensitiveLexicalEntryScorer<Y> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			// Get default scorer
			final IScorer<LexicalEntry<Y>> defaultScorer = DecoderServices
					.decode(dependentFiles.get("defaultScorer"), decoderHelper);
			
			// Get skipping cost
			final double skippingCost = Double.valueOf(attributes
					.get("skippingCost"));
			
			return new SkippingSensitiveLexicalEntryScorer<Y>(decoderHelper
					.getCategoryServices().getEmptyCategory(), skippingCost,
					defaultScorer);
		}
		
		@Override
		protected void doEncode(SkippingSensitiveLexicalEntryScorer<Y> object,
				BufferedWriter writer) throws IOException {
			// Nothing to do here
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				SkippingSensitiveLexicalEntryScorer<Y> object, File directory,
				File parentFile) throws IOException {
			final Map<String, File> files = new HashMap<String, File>();
			
			// Encode default scorer
			final File defaultScorerFile = new File(directory,
					parentFile.getName() + ".defaultScorer");
			DecoderServices.encode(object.defaultScorer, defaultScorerFile,
					decoderHelper);
			files.put("defaultScorer", defaultScorerFile);
			
			return files;
		}
		
	}
	
}
