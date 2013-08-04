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
package edu.uw.cs.lil.tiny.parser.ccg.features.basic.resources;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.LexicalFeatureSet;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Creator for {@link LexicalFeatureSet}.
 * 
 * @author Yoav Artzi
 */
public class LexicalFeatureSetCreator<DI extends IDataItem<?>, MR> implements
		IResourceObjectCreator<LexicalFeatureSet<DI, MR>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public LexicalFeatureSet<DI, MR> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		final LexicalFeatureSet.Builder<DI, MR> builder = new LexicalFeatureSet.Builder<DI, MR>();
		
		if (parameters.contains("tag")) {
			builder.setFeatureTag(parameters.get("tag"));
		}
		
		if (parameters.contains("initFixed")) {
			builder.setInitialFixedScorer((ISerializableScorer<LexicalEntry<MR>>) resourceRepo
					.getResource(parameters.get("initFixed")));
		}
		
		if (parameters.contains("init")) {
			builder.setInitialScorer((ISerializableScorer<LexicalEntry<MR>>) resourceRepo
					.getResource(parameters.get("init")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return "feat.lex";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), LexicalFeatureSet.class)
				.setDescription(
						"Feature set that generates features for using lexical entries")
				.addParam("tag", "string",
						"Feature tag to be used for generated features (default: LEX)")
				.addParam("initFixed", "id",
						"Scorer to initialize fixed lexical entries")
				.addParam("init", "id",
						"Scorer to initialize lexical entries (all non fixed entries)")
				.build();
	}
	
}
