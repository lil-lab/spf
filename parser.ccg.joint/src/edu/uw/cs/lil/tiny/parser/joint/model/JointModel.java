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
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;

/**
 * Model for joint inference of parsing and logical form evaluation/execution.
 * Extends Model in a way that all feature sets that are inside the parser see
 * the complete data item, including the initial world state. This allows
 * parsing features to use the state of the world, allowing for situated
 * inference.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Data item
 * @param <STATE>
 *            World state
 * @param <MR>
 *            Semantics
 * @param <ESTEP>
 *            Execution step
 */
public class JointModel<DI extends ISituatedDataItem<?, ?>, MR, ESTEP> extends
		Model<DI, MR> implements IJointModelImmutable<DI, MR, ESTEP> {
	
	private static final long						serialVersionUID	= 4171988104744030985L;
	private final List<IJointFeatureSet<DI, ESTEP>>	jointFeatures;
	
	protected JointModel(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures,
			List<IJointFeatureSet<DI, ESTEP>> jointFeatures,
			ILexicon<MR> lexicon) {
		this(lexicalFeatures, parseFeatures, jointFeatures, lexicon,
				HashVectorFactory.create());
	}
	
	protected JointModel(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures,
			List<IJointFeatureSet<DI, ESTEP>> jointFeatures,
			ILexicon<MR> lexicon, IHashVector theta) {
		super(lexicalFeatures, parseFeatures, lexicon, theta);
		this.jointFeatures = Collections.unmodifiableList(jointFeatures);
	}
	
	@Override
	public IHashVector computeFeatures(ESTEP executionStep, DI dataItem) {
		final IHashVector features = HashVectorFactory.create();
		for (final IJointFeatureSet<DI, ESTEP> featureSet : jointFeatures) {
			featureSet.setFeats(executionStep, features, dataItem);
		}
		return features;
	}
	
	@Override
	public IJointDataItemModel<MR, ESTEP> createJointDataItemModel(DI dataItem) {
		return new JointDataItemModel<DI, MR, ESTEP>(this, dataItem);
	}
	
	public List<IJointFeatureSet<DI, ESTEP>> getJointFeatures() {
		return jointFeatures;
	}
	
	@Override
	public double score(ESTEP executionStep, DI dataItem) {
		double score = 0.0;
		for (final IJointFeatureSet<DI, ESTEP> featureSet : jointFeatures) {
			score += featureSet.score(executionStep, getTheta(), dataItem);
		}
		return score;
	}
	
	public static class Builder<DI extends ISituatedDataItem<?, ?>, MR, ESTEP> {
		
		private final List<IJointFeatureSet<DI, ESTEP>>				jointFeatures	= new LinkedList<IJointFeatureSet<DI, ESTEP>>();
		private final List<IIndependentLexicalFeatureSet<DI, MR>>	lexicalFeatures	= new LinkedList<IIndependentLexicalFeatureSet<DI, MR>>();
		private ILexicon<MR>										lexicon			= new Lexicon<MR>();
		private final List<IParseFeatureSet<DI, MR>>				parseFeatures	= new LinkedList<IParseFeatureSet<DI, MR>>();
		
		public Builder<DI, MR, ESTEP> addJointFeatureSet(
				IJointFeatureSet<DI, ESTEP> featureSet) {
			jointFeatures.add(featureSet);
			return this;
		}
		
		public Builder<DI, MR, ESTEP> addLexicalFeatureSet(
				IIndependentLexicalFeatureSet<DI, MR> featureSet) {
			lexicalFeatures.add(featureSet);
			return this;
		}
		
		public Builder<DI, MR, ESTEP> addParseFeatureSet(
				IParseFeatureSet<DI, MR> featureSet) {
			parseFeatures.add(featureSet);
			return this;
		}
		
		public JointModel<DI, MR, ESTEP> build() {
			return new JointModel<DI, MR, ESTEP>(lexicalFeatures,
					parseFeatures, jointFeatures, lexicon);
		}
		
		public Builder<DI, MR, ESTEP> setLexicon(ILexicon<MR> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
		
	}
}
