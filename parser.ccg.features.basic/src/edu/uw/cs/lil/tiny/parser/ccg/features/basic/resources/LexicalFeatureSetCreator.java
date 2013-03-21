/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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

import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.LexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.utils.collections.IScorer;

/**
 * Creator for {@link LexicalFeatureSet}.
 * 
 * @author Yoav Artzi
 */
public class LexicalFeatureSetCreator<X, Y> implements
		IResourceObjectCreator<LexicalFeatureSet<X, Y>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public LexicalFeatureSet<X, Y> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		final LexicalFeatureSet.Builder<X, Y> builder = new LexicalFeatureSet.Builder<X, Y>();
		
		if (parameters.contains("tag")) {
			builder.setFeatureTag(parameters.get("tag"));
		}
		
		if (parameters.contains("initFixed")) {
			builder.setInitialFixedScorer((IScorer<LexicalEntry<Y>>) resourceRepo
					.getResource(parameters.get("initFixed")));
		}
		
		if (parameters.contains("init")) {
			builder.setInitialScorer((IScorer<LexicalEntry<Y>>) resourceRepo
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
