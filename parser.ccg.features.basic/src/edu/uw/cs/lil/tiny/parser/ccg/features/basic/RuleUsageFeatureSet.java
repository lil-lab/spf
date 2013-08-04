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
package edu.uw.cs.lil.tiny.parser.ccg.features.basic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

public class RuleUsageFeatureSet<DI extends IDataItem<?>, MR> implements
		IParseFeatureSet<DI, MR> {
	
	private static final String	FEATURE_TAG			= "RULE";
	private static final long	serialVersionUID	= -2924052883973590335L;
	private final double		scale;
	
	public RuleUsageFeatureSet(double scale) {
		this.scale = scale;
	}
	
	public static <DI extends IDataItem<?>, MR> IDecoder<RuleUsageFeatureSet<DI, MR>> getDecoder() {
		return new Decoder<DI, MR>();
	}
	
	@Override
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(
			IHashVector theta) {
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		for (final Pair<KeyArgs, Double> feature : theta.getAll(FEATURE_TAG)) {
			weights.add(Triplet.of(feature.first(), feature.second(),
					(String) null));
		}
		return weights;
	}
	
	@Override
	public boolean isValidWeightVector(IHashVectorImmutable update) {
		// No protected features
		return true;
	}
	
	@Override
	public double score(IParseStep<MR> obj, IHashVector theta, DI dataItem) {
		return setFeats(obj.getRuleName(), HashVectorFactory.create())
				.vectorMultiply(theta);
		
	}
	
	@Override
	public void setFeats(IParseStep<MR> obj, IHashVector feats, DI dataItem) {
		setFeats(obj.getRuleName(), feats);
		
	}
	
	private IHashVectorImmutable setFeats(String ruleName, IHashVector features) {
		if (ruleName.startsWith("shift")) {
			features.set(FEATURE_TAG, ruleName,
					features.get(FEATURE_TAG, ruleName) + 1.0 * scale);
		}
		return features;
	}
	
	private static class Decoder<DI extends IDataItem<?>, MR> extends
			AbstractDecoderIntoFile<RuleUsageFeatureSet<DI, MR>> {
		private static final int	VERSION	= 1;
		
		public Decoder() {
			super(RuleUsageFeatureSet.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				RuleUsageFeatureSet<DI, MR> object) {
			final HashMap<String, String> attributes = new HashMap<String, String>();
			attributes.put("scale", Double.toString(object.scale));
			return attributes;
		}
		
		@Override
		protected RuleUsageFeatureSet<DI, MR> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			return new RuleUsageFeatureSet<DI, MR>(Double.valueOf(attributes
					.get("scale")));
		}
		
		@Override
		protected void doEncode(RuleUsageFeatureSet<DI, MR> object,
				BufferedWriter writer) throws IOException {
			// Nothing to do here
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				RuleUsageFeatureSet<DI, MR> object, File directory,
				File parentFile) throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
	
}
