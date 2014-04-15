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
package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.AbstractApplication;

/**
 * A rule that fills in miss 'that' phrases for that-less relative
 * constructions.
 * <ul>
 * (S\NP)/NP N --> (S\NP) [while introducing an existential to model plurality]
 * </ul>
 * 
 * @author Luke Zettlemoyer
 */
public class PluralExistentialTypeShifting extends
		AbstractApplication<LogicalExpression> {
	private static String								RULE_LABEL	= "plural_exists";
	
	private final ComplexCategory<LogicalExpression>	workerCategory;
	
	public PluralExistentialTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices) {
		super(RULE_LABEL, Direction.FORWARD, categoryServices);
		this.workerCategory = (ComplexCategory<LogicalExpression>) categoryServices
				.parse("(S\\NP)/(S\\NP/NP)/N : (lambda $0:<e,t> (lambda $1:<e,<e,t>> (lambda $2:e (exists:<<e,t>,t> (lambda $3:e (and:<t*,t> ($0 $3) ($1 $3 $2)))))))");
	}
	
	@Override
	public Collection<ParseRuleResult<LogicalExpression>> apply(
			Category<LogicalExpression> left, Category<LogicalExpression> right) {
		
		// TODO [Yoav] [limitation] make sure this function can't be applied on
		// top of any
		// unary type shifting rules
		
		if (!(right.getSyntax().equals(Syntax.N))) {
			return Collections.emptyList();
		}
		
		final List<ParseRuleResult<LogicalExpression>> first = doApplication(
				workerCategory, right, false);
		if (first.size() == 0) {
			return Collections.emptyList();
		}
		return doApplication(first.get(0).getResultCategory(), left, false);
	}
	
	public static class Creator implements
			IResourceObjectCreator<PluralExistentialTypeShifting> {
		
		private final String	type;
		
		public Creator() {
			this("rule.shift.pluralexists");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public PluralExistentialTypeShifting create(Parameters params,
				IResourceRepository repo) {
			return new PluralExistentialTypeShifting(
					(ICategoryServices<LogicalExpression>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type,
					PluralExistentialTypeShifting.class).build();
		}
		
	}
}
