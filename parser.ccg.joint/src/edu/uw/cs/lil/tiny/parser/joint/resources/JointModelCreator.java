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
package edu.uw.cs.lil.tiny.parser.joint.resources;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointFeatureSet;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.utils.composites.Pair;

public class JointModelCreator<DI extends IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP>
		implements IResourceObjectCreator<JointModel<DI, STATE, MR, ESTEP>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public JointModel<DI, STATE, MR, ESTEP> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		final JointModel.Builder<DI, STATE, MR, ESTEP> builder = new JointModel.Builder<DI, STATE, MR, ESTEP>();
		
		// Lexicon
		builder.setLexicon((ILexicon<MR>) resourceRepo.getResource((parameters
				.get("lexicon"))));
		
		// Lexical feature sets
		for (final String setId : parameters.getSplit("lexicalFeatures")) {
			builder.addLexicalFeatureSet((IIndependentLexicalFeatureSet<DI, MR>) resourceRepo
					.getResource(setId));
		}
		
		// Parse feature sets
		for (final String setId : parameters.getSplit("parseFeatures")) {
			builder.addParseFeatureSet((IParseFeatureSet<DI, MR>) resourceRepo
					.getResource(setId));
		}
		
		// Joint feature sets
		for (final String setId : parameters.getSplit("jointFeatures")) {
			builder.addJointFeatureSet((IJointFeatureSet<DI, STATE, ESTEP>) resourceRepo
					.getResource(setId));
		}
		
		final JointModel<DI, STATE, MR, ESTEP> model = builder.build();
		
		return model;
	}
	
	@Override
	public String type() {
		return "model.joint";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), JointModel.class)
				.setDescription(
						"Model for joint inference of parsing and 'execution'")
				.addParam("lexicon", "id", "The model's lexicon (ILexicon)")
				.addParam(
						"lexicalFeatures",
						"ids",
						"Lexical features sets (IIndependentLexicalFeatureSet) (e.g., 'lfs1,lfs2,lfs3')")
				.addParam("parseFeatures", "ids",
						"Parsing feature sets (IParseFeatureSet) (e.g., 'pfs1,pfs2,pfs3')")
				.addParam(
						"jointFeatures",
						"ids",
						"Joint feature sets to be used in execution (IJointFeatureSet) (e.g., 'jfs1,jfs2,jfs3')")
				.build();
	}
	
	protected ILexicon<MR> createLexicon(String lexiconType) {
		if ("conventional".equals(lexiconType)) {
			return new Lexicon<MR>();
		} else {
			throw new IllegalArgumentException("Invalid lexicon type: "
					+ lexiconType);
		}
	}
	
}
