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
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
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
 * N S\NP --> N or N S/NP --> N
 * </ul>
 *
 * @author Luke Zettlemoyer
 */
public class ThatlessRelative extends AbstractApplication<LogicalExpression> {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7532193703731942477L;

	private static String								RULE_LABEL	= "thatless";

	private final ComplexCategory<LogicalExpression>	workerCategoryBackSlash;
	private final ComplexCategory<LogicalExpression>	workerCategoryForwardSlash;

	public ThatlessRelative(
			ICategoryServices<LogicalExpression> categoryServices) {
		super(RULE_LABEL, Direction.FORWARD, categoryServices);
		this.workerCategoryForwardSlash = (ComplexCategory<LogicalExpression>) categoryServices
				.read("N/(S/NP)/N : (lambda $0:<e,t> (lambda $1:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) ($1 $2)))))");
		this.workerCategoryBackSlash = (ComplexCategory<LogicalExpression>) categoryServices
				.read("N/(S\\NP)/N : (lambda $0:<e,t> (lambda $1:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) ($1 $2)))))");
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> left, Category<LogicalExpression> right, SentenceSpan span) {

		if (!(right.getSyntax() instanceof ComplexSyntax)) {
			return null;
		}
		final ComplexSyntax complexSyntax = (ComplexSyntax) right.getSyntax();
		if (!complexSyntax.getLeft().equals(Syntax.S)
				|| !complexSyntax.getRight().equals(Syntax.NP)) {
			return null;
		}

		ComplexCategory<LogicalExpression> workerCategory;
		if (complexSyntax.getSlash().equals(Slash.FORWARD)) {
			workerCategory = workerCategoryForwardSlash;
		} else {
			workerCategory = workerCategoryBackSlash;
		}
		final ParseRuleResult<LogicalExpression> first = doApplication(
				workerCategory, left, false);
		if (first == null) {
			return null;
		}
		return doApplication(first.getResultCategory(), right, false);
	}

	public static class Creator implements
			IResourceObjectCreator<ThatlessRelative> {

		private final String	type;

		public Creator() {
			this("rule.thatless");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ThatlessRelative create(Parameters params,
				IResourceRepository repo) {
			return new ThatlessRelative(
					(ICategoryServices<LogicalExpression>) repo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, ThatlessRelative.class).build();
		}

	}
}
