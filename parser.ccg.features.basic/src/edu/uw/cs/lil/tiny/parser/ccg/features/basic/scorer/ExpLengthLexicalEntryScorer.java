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

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.collections.ISerializableScorer;

/**
 * Lexical entry scorer that takes the number of tokens into account. coef *
 * n^exp, where n is the number of tokens in the lexical entry.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation type.
 */
public class ExpLengthLexicalEntryScorer<MR> implements
		ISerializableScorer<LexicalEntry<MR>> {
	
	private static final long	serialVersionUID	= 3086307252253251483L;
	
	private final double		coef;
	private final double		exponent;
	
	public ExpLengthLexicalEntryScorer(double coef, double exponent) {
		this.coef = coef;
		this.exponent = exponent;
	}
	
	@Override
	public double score(LexicalEntry<MR> lex) {
		return coef * Math.pow(lex.getTokens().size(), exponent);
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<ExpLengthLexicalEntryScorer<MR>> {
		
		@Override
		public ExpLengthLexicalEntryScorer<MR> create(Parameters params,
				IResourceRepository repo) {
			return new ExpLengthLexicalEntryScorer<MR>(Double.valueOf(params
					.get("coef")), Double.valueOf(params.get("exp")));
		}
		
		@Override
		public String type() {
			return "scorer.lenexp";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					ExpLengthLexicalEntryScorer.class)
					.setDescription(
							"Lexical entry scorer that computes the function coef * n^exp, where n is the number of tokens in the lexical entry")
					.addParam("coef", "double", "Scoring function coefficient")
					.addParam("exp", "double", "Scoring function exponent")
					.build();
		}
		
	}
}
