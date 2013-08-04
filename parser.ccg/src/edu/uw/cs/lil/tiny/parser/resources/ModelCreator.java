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
package edu.uw.cs.lil.tiny.parser.resources;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model.Builder;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;

public class ModelCreator<DI extends IDataItem<?>, MR> implements
		IResourceObjectCreator<Model<DI, MR>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public Model<DI, MR> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		final Builder<DI, MR> builder = new Model.Builder<DI, MR>();
		
		// Lexicon
		builder.setLexicon((ILexicon<MR>) resourceRepo.getResource(parameters
				.get("lexicon")));
		
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
		
		final Model<DI, MR> model = builder.build();
		
		return model;
	}
	
	@Override
	public String type() {
		return "model.new";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), Model.class)
				.setDescription(
						"Parsing model, including lexicon, features and a weight vector")
				.addParam("lexicon", "id", "Lexicon to use with this model")
				.addParam("lexicalFeatures", "[id]",
						"Lexical feature sets to use (e.g., 'lfs1,lfs2,lfs3')")
				.addParam("parseFeatures", "[id]",
						"Parse feature sets to use (e.g., 'pfs1,pfs2,pfs3')")
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
