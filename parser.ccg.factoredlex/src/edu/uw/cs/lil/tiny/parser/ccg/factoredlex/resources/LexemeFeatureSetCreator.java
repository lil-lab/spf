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
package edu.uw.cs.lil.tiny.parser.ccg.factoredlex.resources;

import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.Lexeme;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.LexemeFeatureSet;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Creator for {@link LexemeFeatureSet}.
 * 
 * @author Yoav Artzi
 */
public class LexemeFeatureSetCreator<DI extends IDataItem<?>> implements
		IResourceObjectCreator<LexemeFeatureSet<DI>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public LexemeFeatureSet<DI> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		final LexemeFeatureSet.Builder<DI> builder = new LexemeFeatureSet.Builder<DI>();
		
		if (parameters.contains("scale")) {
			builder.setScale(Double.valueOf(parameters.get("scale")));
		}
		
		if (parameters.contains("tag")) {
			builder.setFeatureTag(parameters.get("tag"));
		}
		
		if (parameters.contains("initFixed")) {
			builder.setInitialFixedScorer((ISerializableScorer<Lexeme>) resourceRepo
					.getResource(parameters.get("initFixed")));
		}
		
		if (parameters.contains("init")) {
			builder.setInitialScorer((ISerializableScorer<Lexeme>) resourceRepo
					.getResource(parameters.get("init")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return "feat.lexeme";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), LexemeFeatureSet.class)
				.setDescription(
						"Feature set that generates features for using lexmes")
				.addParam("scale", "double",
						"Scaling factor for generated features (default: 1.0)")
				.addParam("tag", "string",
						"Feature tag to use for generated features (default: XEME)")
				.addParam(
						"initFixed",
						"id",
						"Scorer used to score unknown lexemes for fixed lexical entries (default: uniform scorer with value 0.0)")
				.addParam("init", "id",
						"Scorer used to score unknown lexemes (default: uniform scorer with value 0.0)")
				.build();
	}
	
}
