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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.AbstractApplication;

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
	private static String								RULE_LABEL			= "plural_exists";

	private static final long							serialVersionUID	= 9132040294387137257L;

	private final ComplexCategory<LogicalExpression>	workerCategory;

	public PluralExistentialTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices) {
		super(RULE_LABEL, Direction.FORWARD, categoryServices);
		this.workerCategory = (ComplexCategory<LogicalExpression>) categoryServices
				.read("(S\\NP)/(S\\NP/NP)/N : (lambda $0:<e,t> (lambda $1:<e,<e,t>> (lambda $2:e (exists:<<e,t>,t> (lambda $3:e (and:<t*,t> ($0 $3) ($1 $3 $2)))))))");
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> left,
			Category<LogicalExpression> right, SentenceSpan span) {

		// TODO [Yoav] [limitation] make sure this function can't be applied on
		// top of any
		// unary type shifting rules

		if (right.getSyntax().unify(Syntax.N) == null) {
			return null;
		}

		final ParseRuleResult<LogicalExpression> first = doApplication(
				workerCategory, right, false);
		if (first == null) {
			return null;
		}
		return doApplication(first.getResultCategory(), left, false);
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
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
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
