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
package edu.uw.cs.lil.tiny.mr.lambda.ccg;

import java.util.HashSet;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsTypeConsistent;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;

/**
 * Filters full parses based on syntax, and well-typed semantic content.
 * 
 * @author Yoav Artzi
 */
public class SimpleFullParseFilter implements
		IFilter<Category<LogicalExpression>> {
	
	private final Set<Syntax>	fullSentenceSyntaxes;
	
	public SimpleFullParseFilter(Set<Syntax> fullSentenceSyntaxes) {
		this.fullSentenceSyntaxes = fullSentenceSyntaxes;
	}
	
	@Override
	public boolean isValid(Category<LogicalExpression> category) {
		return category.getSem() != null
				&& fullSentenceSyntaxes.contains(category.getSyntax())
				&& IsTypeConsistent.of(category.getSem());
	}
	
	public static class Creator implements
			IResourceObjectCreator<SimpleFullParseFilter> {
		
		private final String	type;
		
		public Creator() {
			this("parsefilter.lambda.simple");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public SimpleFullParseFilter create(Parameters params,
				IResourceRepository repo) {
			return new SimpleFullParseFilter(new HashSet<Syntax>(ListUtils.map(
					params.getSplit("syntax"),
					new ListUtils.Mapper<String, Syntax>() {
						
						@Override
						public Syntax process(String obj) {
							return Syntax.valueOf(obj);
						}
					})));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, SimpleFullParseFilter.class)
					.addParam("syntax", Syntax.class,
							"Valid syntax for complete parses.").build();
		}
		
	}
	
}
