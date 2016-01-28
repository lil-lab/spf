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

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.AbstractScaledScorerCreator;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;

public class LexicalEntryLexemeBasedScorer implements
		ISerializableScorer<LexicalEntry<LogicalExpression>> {
	
	private static final long		serialVersionUID	= -2859797331618576944L;
	
	private final IScorer<Lexeme>	lexemeScorer;
	
	public LexicalEntryLexemeBasedScorer(IScorer<Lexeme> lexemeScorer) {
		this.lexemeScorer = lexemeScorer;
	}
	
	@Override
	public double score(LexicalEntry<LogicalExpression> e) {
		return lexemeScorer.score(FactoringServices.factor(e).getLexeme());
	}
	
	public static class Creator
			extends
			AbstractScaledScorerCreator<LexicalEntry<LogicalExpression>, LexicalEntryLexemeBasedScorer> {
		
		@SuppressWarnings("unchecked")
		@Override
		public LexicalEntryLexemeBasedScorer createScorer(
				Parameters parameters, IResourceRepository resourceRepo) {
			return new LexicalEntryLexemeBasedScorer(
					(IScorer<Lexeme>) resourceRepo.get(parameters
							.get("baseScorer")));
		}
		
		@Override
		public String type() {
			return "scorer.lex.lexemebased";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					LexicalEntryLexemeBasedScorer.class)
					.setDescription(
							"Scorer to score a lexical entry based on its lexeme")
					.addParam("scale", "double", "Scaling factor")
					.addParam("baseScorer", "id",
							"Scorer to assign a score to a lexeme").build();
		}
		
	}
	
}
