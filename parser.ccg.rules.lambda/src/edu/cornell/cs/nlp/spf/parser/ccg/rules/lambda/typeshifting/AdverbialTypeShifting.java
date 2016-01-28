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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

/**
 * AP => S\S
 *
 * @author Yoav Artzi
 */
public class AdverbialTypeShifting extends AbstractShiftingRule {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -2976480805663613422L;

	public AdverbialTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices) {
		this("shift_ap", categoryServices);
	}

	public AdverbialTypeShifting(String name,
			ICategoryServices<LogicalExpression> categoryServices) {
		super(name, Syntax.AP, Syntax.S, Slash.BACKWARD, categoryServices);
	}

	public static class Creator implements
			IResourceObjectCreator<AdverbialTypeShifting> {

		private final String	type;

		public Creator() {
			this("rule.shifting.ap");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public AdverbialTypeShifting create(Parameters params,
				IResourceRepository repo) {
			return new AdverbialTypeShifting(
					(ICategoryServices<LogicalExpression>) repo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, AdverbialTypeShifting.class)
					.build();
		}

	}

}
