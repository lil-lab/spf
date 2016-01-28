/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.scorers;

import java.util.HashMap;
import java.util.Map;

import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;

/**
 * Scores {@link Lexeme}s by their origin.
 *
 * @author Yoav Artzi
 */
public class OriginLexemeScorer implements ISerializableScorer<Lexeme> {

	private static final long								serialVersionUID	= -1378145149635670935L;
	private final ISerializableScorer<Lexeme>				defaultScorer;
	private final Map<String, ISerializableScorer<Lexeme>>	originScorers;

	public OriginLexemeScorer(
			Map<String, ISerializableScorer<Lexeme>> originScorers,
			ISerializableScorer<Lexeme> defaultScorer) {
		this.originScorers = originScorers;
		this.defaultScorer = defaultScorer;
	}

	@Override
	public double score(Lexeme lexeme) {
		final String origin = lexeme.getOrigin();
		if (origin != null && originScorers.containsKey(origin)) {
			return originScorers.get(origin).score(lexeme);
		} else {
			return defaultScorer.score(lexeme);
		}
	}

	public static class Creator implements
			IResourceObjectCreator<OriginLexemeScorer> {

		@Override
		public OriginLexemeScorer create(Parameters params,
				final IResourceRepository repo) {
			final ISerializableScorer<Lexeme> defaultScorer = repo.get(params
					.get("default"));

			final Map<String, ISerializableScorer<Lexeme>> originScorers = new HashMap<>();

			if (params.contains("scorers")) {
				for (final String entry : params.getSplit("scorers")) {
					final String[] split = entry.split(":", 2);
					final String origin = split[0];
					final ISerializableScorer<Lexeme> scorer = repo
							.get(split[1]);
					originScorers.put(origin, scorer);
				}
			}

			return new OriginLexemeScorer(originScorers, defaultScorer);
		}

		@Override
		public String type() {
			return "scorer.lexeme.origin";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), OriginLexemeScorer.class)
					.setDescription(
							"Lexeme scorer that assigns different constants to lexical entries based on their origin")
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
