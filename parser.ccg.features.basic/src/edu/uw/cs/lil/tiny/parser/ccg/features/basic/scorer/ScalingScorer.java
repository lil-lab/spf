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
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Scorer to scale an existing base scorer.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public class ScalingScorer<Y> implements ISerializableScorer<Y> {
	
	private static final long	serialVersionUID	= 4118718964164528806L;
	private final IScorer<Y>	baseScorer;
	private final double		scale;
	
	public ScalingScorer(double scale, IScorer<Y> baseScorer) {
		this.scale = scale;
		this.baseScorer = baseScorer;
	}
	
	public static <Y> IDecoder<ScalingScorer<Y>> getDecoder(
			DecoderHelper<Y> decoderHelper) {
		return new Decoder<Y>(decoderHelper);
	}
	
	@Override
	public double score(Y lex) {
		return scale * baseScorer.score(lex);
	}
	
	private static class Decoder<Y> extends
			AbstractDecoderIntoFile<ScalingScorer<Y>> {
		private static final int		VERSION	= 1;
		private final DecoderHelper<Y>	decoderHelper;
		
		public Decoder(DecoderHelper<Y> decoderHelper) {
			super(ScalingScorer.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				ScalingScorer<Y> object) {
			final Map<String, String> attributes = new HashMap<String, String>();
			
			attributes.put("scale", Double.toString(object.scale));
			
			return attributes;
		}
		
		@Override
		protected ScalingScorer<Y> doDecode(Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			
			final double scale = Double.valueOf(attributes.get("scale"));
			
			// Get base scorer
			final IScorer<Y> baseScorer = DecoderServices.decode(
					dependentFiles.get("baseScorer"), decoderHelper);
			
			return new ScalingScorer<Y>(scale, baseScorer);
		}
		
		@Override
		protected void doEncode(ScalingScorer<Y> object, BufferedWriter writer)
				throws IOException {
			// Nothing to do here
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				ScalingScorer<Y> object, File directory, File parentFile)
				throws IOException {
			final Map<String, File> files = new HashMap<String, File>();
			
			// Encode base scorer
			final File defaultScorerFile = new File(directory,
					parentFile.getName() + ".baseScorer");
			DecoderServices.encode(object.baseScorer, defaultScorerFile,
					decoderHelper);
			files.put("baseScorer", defaultScorerFile);
			
			return files;
		}
	}
	
}
