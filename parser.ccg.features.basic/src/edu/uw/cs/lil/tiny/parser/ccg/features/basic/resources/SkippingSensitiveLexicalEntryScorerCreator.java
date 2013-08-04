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

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.SkippingSensitiveLexicalEntryScorer;
import edu.uw.cs.utils.collections.IScorer;

public class SkippingSensitiveLexicalEntryScorerCreator<Y>
		extends
		AbstractScaledScorerCreator<LexicalEntry<Y>, SkippingSensitiveLexicalEntryScorer<Y>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public SkippingSensitiveLexicalEntryScorer<Y> createScorer(
			Parameters parameters, IResourceRepository resourceRepo) {
		return new SkippingSensitiveLexicalEntryScorer<Y>(
				((ICategoryServices<Y>) resourceRepo.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE))
						.getEmptyCategory(), Double.valueOf(parameters
						.get("cost")), (IScorer<LexicalEntry<Y>>) resourceRepo
						.getResource(parameters.get("baseScorer")));
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
