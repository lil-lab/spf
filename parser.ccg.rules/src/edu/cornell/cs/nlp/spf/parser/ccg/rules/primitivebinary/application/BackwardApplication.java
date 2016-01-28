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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;

/**
 * An abstract rule for logical application. Backward application rule:
 * <ul>
 * <li>Y X\Y => X</li>
 * </ul>
 *
 * @author Yoav Artzi
 */
public class BackwardApplication<MR> extends AbstractApplication<MR> {

	private static final long	serialVersionUID	= -5453231357175805033L;

	public BackwardApplication(ICategoryServices<MR> categoryServices) {
		super(RULE_LABEL, Direction.BACKWARD, categoryServices);
	}

	@Override
	public ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right,
			SentenceSpan span) {
		return doApplication(right, left, true);
	}

	public static class Creator<MR> implements
			IResourceObjectCreator<BackwardApplication<MR>> {

		private String	type;

		public Creator() {
			this("rule.application.backward");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public BackwardApplication<MR> create(Parameters params,
				IResourceRepository repo) {
			return new BackwardApplication<MR>(
					(ICategoryServices<MR>) repo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, BackwardApplication.class)
					.build();
		}

	}
}
