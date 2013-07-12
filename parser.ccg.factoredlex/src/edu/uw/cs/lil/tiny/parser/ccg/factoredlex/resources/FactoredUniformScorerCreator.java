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
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;

/**
 * Creator for {@link UniformScorer}.
 * 
 * @author Yoav Artzi
 */
public class FactoredUniformScorerCreator implements
		IResourceObjectCreator<UniformScorer<?>> {
	
	@Override
	public UniformScorer<?> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		final String c = parameters.get("class");
		final Double weight = Double.valueOf(parameters.get("weight"));
		if ("lexeme".equals(c)) {
			return new UniformScorer<Lexeme>(weight);
		} else if ("lexTemplate".equals(c)) {
			return new UniformScorer<LexicalTemplate>(weight);
		} else {
			throw new IllegalArgumentException(
					"Invalid class for factored uniform scorer: " + c);
		}
	}
	
	@Override
	public String type() {
		return "scorer.uniform.factored";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), UniformScorer.class)
				.setDescription(
						"Uniform scorer for lexemes or lexical templates")
				.addParam("weight", "double",
						"Scorer weight. The score assigned to an object")
				.addParam("class", "enum",
						"The type of object to score (options: lexeme, lexTemplate)")
				.build();
	}
	
}
