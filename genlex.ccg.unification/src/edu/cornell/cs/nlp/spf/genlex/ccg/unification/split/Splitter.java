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
package edu.cornell.cs.nlp.spf.genlex.ccg.unification.split;

import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

/**
 * Splitting service object.
 * 
 * @author Yoav Artzi
 */
public class Splitter implements IUnificationSplitter {
	private final ICategoryServices<LogicalExpression>	categoryServices;
	
	public Splitter(ICategoryServices<LogicalExpression> categoryServices) {
		this.categoryServices = categoryServices;
	}
	
	public Set<SplittingPair> getSplits(Category<LogicalExpression> category) {
		final Set<SplittingPair> splits = new HashSet<SplittingPair>();
		splits.addAll(MakeApplicationSplits.of(category, categoryServices));
		splits.addAll(MakeCompositionSplits.of(category, categoryServices));
		return splits;
	}
	
	public static class Creator implements IResourceObjectCreator<Splitter> {
		
		@SuppressWarnings("unchecked")
		@Override
		public Splitter create(Parameters parameters,
				IResourceRepository resourceRepo) {
			return new Splitter(
					(ICategoryServices<LogicalExpression>) resourceRepo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}
		
		@Override
		public String type() {
			return "splitter.unification";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), Splitter.class)
					.setDescription(
							"Logical expression splitter for unification-based GENLEX")
					.build();
		}
		
	}
}
