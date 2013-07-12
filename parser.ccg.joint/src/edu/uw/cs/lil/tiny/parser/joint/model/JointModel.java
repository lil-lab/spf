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
package edu.uw.cs.lil.tiny.parser.joint.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;

public class JointModel<LANG, STATE, LF, ESTEP> extends Model<LANG, LF>
		implements IJointModelImmutable<LANG, STATE, LF, ESTEP> {
	
	private final List<IJointFeatureSet<LANG, STATE, LF, ESTEP>>	jointFeatures;
	
	protected JointModel(
			List<IIndependentLexicalFeatureSet<LANG, LF>> lexicalFeatures,
			List<IParseFeatureSet<LANG, LF>> parseFeatures,
			List<IJointFeatureSet<LANG, STATE, LF, ESTEP>> jointFeatures,
			ILexicon<LF> lexicon) {
		super(lexicalFeatures, parseFeatures, lexicon);
		this.jointFeatures = Collections.unmodifiableList(jointFeatures);
	}
	
	@Override
	public IHashVector computeFeatures(ESTEP executionStep,
			IDataItem<Pair<LANG, STATE>> dataItem) {
		final IHashVector features = HashVectorFactory.create();
		for (final IJointFeatureSet<LANG, STATE, LF, ESTEP> featureSet : jointFeatures) {
			featureSet.setFeats(executionStep, features, dataItem);
		}
		return features;
	}
	
	@Override
	public IJointDataItemModel<LF, ESTEP> createJointDataItemModel(
			IDataItem<Pair<LANG, STATE>> dataItem) {
		return new JointDataItemModel<LANG, STATE, LF, ESTEP>(this, dataItem);
	}
	
	@Override
	public double score(ESTEP executionStep,
			IDataItem<Pair<LANG, STATE>> dataItem) {
		double score = 0.0;
		for (final IJointFeatureSet<LANG, STATE, LF, ESTEP> featureSet : jointFeatures) {
			score += featureSet.score(executionStep, getTheta(), dataItem);
		}
		return score;
	}
	
	public static class Builder<X, W, Y, Z> {
		
		private final List<IJointFeatureSet<X, W, Y, Z>>		jointFeatures	= new LinkedList<IJointFeatureSet<X, W, Y, Z>>();
		private final List<IIndependentLexicalFeatureSet<X, Y>>	lexicalFeatures	= new LinkedList<IIndependentLexicalFeatureSet<X, Y>>();
		private ILexicon<Y>										lexicon			= new Lexicon<Y>();
		private final List<IParseFeatureSet<X, Y>>				parseFeatures	= new LinkedList<IParseFeatureSet<X, Y>>();
		
		public Builder<X, W, Y, Z> addJointFeatureSet(
				IJointFeatureSet<X, W, Y, Z> featureSet) {
			jointFeatures.add(featureSet);
			return this;
		}
		
		public Builder<X, W, Y, Z> addLexicalFeatureSet(
				IIndependentLexicalFeatureSet<X, Y> featureSet) {
			lexicalFeatures.add(featureSet);
			return this;
		}
		
		public Builder<X, W, Y, Z> addParseFeatureSet(
				IParseFeatureSet<X, Y> featureSet) {
			parseFeatures.add(featureSet);
			return this;
		}
		
		public JointModel<X, W, Y, Z> build() {
			return new JointModel<X, W, Y, Z>(lexicalFeatures, parseFeatures,
					jointFeatures, lexicon);
		}
		
		public Builder<X, W, Y, Z> setLexicon(ILexicon<Y> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
		
	}
}
