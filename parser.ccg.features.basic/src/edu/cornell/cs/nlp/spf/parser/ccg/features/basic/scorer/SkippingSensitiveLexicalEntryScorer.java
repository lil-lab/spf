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
package edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;

/**
 * Scorer able to empty lexical entries used for sloppy parsing (i.e., parsing
 * with empty entries that allow skipping of words with certain cost).
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation type.
 */
public class SkippingSensitiveLexicalEntryScorer<MR> implements
		ISerializableScorer<LexicalEntry<MR>> {

	private static final long				serialVersionUID	= 1517659515042456049L;

	private final IScorer<LexicalEntry<MR>>	defaultScorer;
	private final Category<MR>				emptyCategory;
	private final double					skippingCost;

	public SkippingSensitiveLexicalEntryScorer(Category<MR> emptyCategory,
			double skippingCost, IScorer<LexicalEntry<MR>> defaultScorer) {
		this.emptyCategory = emptyCategory;
		this.skippingCost = skippingCost;
		this.defaultScorer = defaultScorer;
	}

	@Override
	public double score(LexicalEntry<MR> lex) {
		if (emptyCategory.equals(lex.getCategory())) {
			return skippingCost * lex.getTokens().size();
		} else {
			return defaultScorer.score(lex);
		}
	}

	public static class Creator<MR>
			extends
			AbstractScaledScorerCreator<LexicalEntry<MR>, SkippingSensitiveLexicalEntryScorer<MR>> {

		@SuppressWarnings("unchecked")
		@Override
		public SkippingSensitiveLexicalEntryScorer<MR> createScorer(
				Parameters parameters, IResourceRepository resourceRepo) {
			return new SkippingSensitiveLexicalEntryScorer<MR>(
					((ICategoryServices<MR>) resourceRepo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE))
							.getEmptyCategory(), Double.valueOf(parameters
							.get("cost")),
					(IScorer<LexicalEntry<MR>>) resourceRepo.get(parameters
							.get("baseScorer")));
		}

		@Override
		public String type() {
			return "scorer.lex.skipping";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					SkippingSensitiveLexicalEntryScorer.class)
					.setDescription(
							"Lexical entry scorer with the ability to score EMPTY categories (skipped words)")
					.addParam("scale", "double",
							"Scaling factor for the scorer output")
					.addParam("cost", "double",
							"Cost of skipping a word (should usually be a negative number)")
					.addParam("baseScorer", "id",
							"Scorer to use for all non EMPTY categories (all words not skipped)")
					.build();
		}

	}

}
