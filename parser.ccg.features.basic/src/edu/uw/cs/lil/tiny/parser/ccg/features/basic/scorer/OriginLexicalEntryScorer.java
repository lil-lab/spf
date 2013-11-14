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
package edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer;

import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Scores lexical entries by their origin.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation type.
 */
public class OriginLexicalEntryScorer<MR> implements
		ISerializableScorer<LexicalEntry<MR>> {
	
	private static final long								serialVersionUID	= 1019435407737659964L;
	
	private final IScorer<LexicalEntry<MR>>					defaultScorer;
	private final Map<String, IScorer<LexicalEntry<MR>>>	originScorers;
	
	public OriginLexicalEntryScorer(
			Map<String, IScorer<LexicalEntry<MR>>> originScorers,
			IScorer<LexicalEntry<MR>> defaultScorer) {
		this.originScorers = originScorers;
		this.defaultScorer = defaultScorer;
	}
	
	@Override
	public double score(LexicalEntry<MR> lex) {
		if (originScorers.containsKey(lex.getOrigin())) {
			return originScorers.get(lex.getOrigin()).score(lex);
		} else {
			return defaultScorer.score(lex);
		}
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<OriginLexicalEntryScorer<MR>> {
		
		@Override
		public OriginLexicalEntryScorer<MR> create(Parameters params,
				final IResourceRepository repo) {
			final IScorer<LexicalEntry<MR>> defaultScorer = repo
					.getResource(params.get("default"));
			
			final Map<String, IScorer<LexicalEntry<MR>>> originScorers = new HashMap<String, IScorer<LexicalEntry<MR>>>();
			
			if (params.contains("scorers")) {
				for (final String entry : params.getSplit("scorers")) {
					final String[] split = entry.split(":", 2);
					final String origin = split[0];
					final IScorer<LexicalEntry<MR>> scorer = repo
							.getResource(split[1]);
					originScorers.put(origin, scorer);
				}
			}
			
			return new OriginLexicalEntryScorer<MR>(originScorers,
					defaultScorer);
		}
		
		@Override
		public String type() {
			return "scorer.lex.origin";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					OriginLexicalEntryScorer.class)
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
}
