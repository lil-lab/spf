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
package edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition;

import java.util.Collection;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;

/**
 * A rule for logical composition. Forward composition rule:
 * <ul>
 * <li>X/Y Y/Z => X/Z</li>
 * </ul>
 * 
 * @author Yoav Artzi
 */
public class ForwardComposition<MR> extends AbstractComposition<MR> {
	
	public ForwardComposition(ICategoryServices<MR> categoryServices, int order) {
		super(RULE_LABEL, Direction.FORWARD, order, categoryServices);
	}
	
	@Override
	public Collection<ParseRuleResult<MR>> apply(Category<MR> left,
			Category<MR> right) {
		return doComposition(left, right, false);
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<ForwardComposition<MR>> {
		
		private String	type;
		
		public Creator() {
			this("rule.composition.forward");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public ForwardComposition<MR> create(Parameters params,
				IResourceRepository repo) {
			return new ForwardComposition<MR>(
					(ICategoryServices<MR>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					params.getAsInteger("order", 0));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, ForwardComposition.class)
					.addParam("eisnerNormalForm", "boolean",
							"Use Eisner normal form for composition (default: false).")
					.addParam("order", Integer.class,
							"Composition order (for English, around 3 should be the max, default: 0)")
					.build();
		}
		
	}
	
}
