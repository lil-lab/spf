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
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;

public class JointModel<DI extends IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP>
		extends Model<DI, MR> implements
		IJointModelImmutable<DI, STATE, MR, ESTEP> {
	
	private static final long								serialVersionUID	= 4171988104744030985L;
	private final List<IJointFeatureSet<DI, STATE, ESTEP>>	jointFeatures;
	
	protected JointModel(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures,
			List<IJointFeatureSet<DI, STATE, ESTEP>> jointFeatures,
			ILexicon<MR> lexicon) {
		this(lexicalFeatures, parseFeatures, jointFeatures, lexicon,
				HashVectorFactory.create());
	}
	
	protected JointModel(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures,
			List<IJointFeatureSet<DI, STATE, ESTEP>> jointFeatures,
			ILexicon<MR> lexicon, IHashVector theta) {
		super(lexicalFeatures, parseFeatures, lexicon, theta);
		this.jointFeatures = Collections.unmodifiableList(jointFeatures);
	}
	
	@Override
	public IHashVector computeFeatures(ESTEP executionStep, DI dataItem) {
		final IHashVector features = HashVectorFactory.create();
		for (final IJointFeatureSet<DI, STATE, ESTEP> featureSet : jointFeatures) {
			featureSet.setFeats(executionStep, features, dataItem);
		}
		return features;
	}
	
	@Override
	public IJointDataItemModel<MR, ESTEP> createJointDataItemModel(DI dataItem) {
		return new JointDataItemModel<DI, STATE, MR, ESTEP>(this, dataItem);
	}
	
	public List<IJointFeatureSet<DI, STATE, ESTEP>> getJointFeatures() {
		return jointFeatures;
	}
	
	@Override
	public double score(ESTEP executionStep, DI dataItem) {
		double score = 0.0;
		for (final IJointFeatureSet<DI, STATE, ESTEP> featureSet : jointFeatures) {
			score += featureSet.score(executionStep, getTheta(), dataItem);
		}
		return score;
	}
	
	public static class Builder<DI extends IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP> {
		
		private final List<IJointFeatureSet<DI, STATE, ESTEP>>		jointFeatures	= new LinkedList<IJointFeatureSet<DI, STATE, ESTEP>>();
		private final List<IIndependentLexicalFeatureSet<DI, MR>>	lexicalFeatures	= new LinkedList<IIndependentLexicalFeatureSet<DI, MR>>();
		private ILexicon<MR>										lexicon			= new Lexicon<MR>();
		private final List<IParseFeatureSet<DI, MR>>				parseFeatures	= new LinkedList<IParseFeatureSet<DI, MR>>();
		
		public Builder<DI, STATE, MR, ESTEP> addJointFeatureSet(
				IJointFeatureSet<DI, STATE, ESTEP> featureSet) {
			jointFeatures.add(featureSet);
			return this;
		}
		
		public Builder<DI, STATE, MR, ESTEP> addLexicalFeatureSet(
				IIndependentLexicalFeatureSet<DI, MR> featureSet) {
			lexicalFeatures.add(featureSet);
			return this;
		}
		
		public Builder<DI, STATE, MR, ESTEP> addParseFeatureSet(
				IParseFeatureSet<DI, MR> featureSet) {
			parseFeatures.add(featureSet);
			return this;
		}
		
		public JointModel<DI, STATE, MR, ESTEP> build() {
			return new JointModel<DI, STATE, MR, ESTEP>(lexicalFeatures,
					parseFeatures, jointFeatures, lexicon);
		}
		
		public Builder<DI, STATE, MR, ESTEP> setLexicon(ILexicon<MR> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
		
	}
}
