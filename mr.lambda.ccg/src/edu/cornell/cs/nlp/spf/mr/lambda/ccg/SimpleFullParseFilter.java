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
package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import java.util.HashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsTypeConsistent;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;

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
	public boolean test(Category<LogicalExpression> category) {
		return category.getSemantics() != null
				&& fullSentenceSyntaxes.contains(category.getSyntax())
				&& IsTypeConsistent.of(category.getSemantics());
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
							return Syntax.read(obj);
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
