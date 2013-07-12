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

import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.OriginLexicalEntryScorer;
import edu.uw.cs.utils.collections.IScorer;

public class OriginLexicalEntryScorerCreator<Y> implements
		IResourceObjectCreator<OriginLexicalEntryScorer<Y>> {
	
	@Override
	public OriginLexicalEntryScorer<Y> create(Parameters params,
			final IResourceRepository repo) {
		final IScorer<LexicalEntry<Y>> defaultScorer = repo.getResource(params
				.get("default"));
		
		final Map<String, IScorer<LexicalEntry<Y>>> originScorers = new HashMap<String, IScorer<LexicalEntry<Y>>>();
		
		if (params.contains("scorers")) {
			for (final String entry : params.getSplit("scorers")) {
				final String[] split = entry.split(":", 2);
				final String origin = split[0];
				final IScorer<LexicalEntry<Y>> scorer = repo
						.getResource(split[1]);
				originScorers.put(origin, scorer);
			}
		}
		
		return new OriginLexicalEntryScorer<Y>(originScorers, defaultScorer);
	}
	
	@Override
	public String type() {
		return "scorer.lex.origin";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), OriginLexicalEntryScorer.class)
				.setDescription(
						"Lexical entry scorer that assigns different constants to lexical entries based on their origin")
				.addParam("default", "id",
						"Default scorer to use for origins not specified in the scorer")
				.addParam(
						"scores",
						"list",
						"List of origin-score pairs. For each origin controlled by this scorer, a weight should be specified (e.g., 'FIXED_DOMAIN:1.0,LEARNED:3.0')")
				.build();
	}
	
}
