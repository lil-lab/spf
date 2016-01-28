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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeraising;

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
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.AbstractComposition;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Combined type raising of primary argument and forward composition: X (T\X)/Z
 * => T/Z. <br>
 * The rule decomposes into two operations:
 * <ul>
 * <li>First, on the primary argument: X=> T/(T\X)</li>
 * <li>Second, forward compose both: T/(T\X) (T\X)/Z => T/Z</li>
 * </ul>
 * <br>
 * We do this form of just in time type raising to avoid computing the left half
 * when it will not combine with anything on the right.
 *
 * @author Yoav Artzi
 */
public class ForwardTypeRaisedComposition extends
		AbstractComposition<LogicalExpression> {
	private static final String			RULE_LABEL			= "trcomp";
	private static final long			serialVersionUID	= -3292792773344287162L;
	private final ForwardTypeRaising	typeRaising;

	public ForwardTypeRaisedComposition(
			ICategoryServices<LogicalExpression> categoryServices) {
		super(RULE_LABEL, Direction.FORWARD, 1, categoryServices, false);
		this.typeRaising = new ForwardTypeRaising(new IFilter<Syntax>() {

			@Override
			public boolean test(Syntax e) {
				return true;
			}
		});
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> left,
			Category<LogicalExpression> right, SentenceSpan span) {

		// Right side must be a complex category.
		if (!(right instanceof ComplexCategory<?>)) {
			return null;
		}
		// It's the secondary argument of the composition.
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) right;

		// Verify secondary slash.
		if (!secondary.hasSlash(Slash.FORWARD)) {
			return null;
		}

		// Get the embedded X from the secondary. First verify we have the right
		// structure.
		if (!(secondary.getSyntax().getLeft() instanceof ComplexSyntax)
				|| !((ComplexSyntax) secondary.getSyntax().getLeft())
						.getSlash().equals(Slash.BACKWARD)) {
			return null;
		}
		final Syntax secondaryT = ((ComplexSyntax) secondary.getSyntax()
				.getLeft()).getLeft();
		final Syntax secondaryX = ((ComplexSyntax) secondary.getSyntax()
				.getLeft()).getRight();

		// Verify the Xs (see javadoc above) match.
		if (secondaryX.unify(left.getSyntax()) == null) {
			return null;
		}

		// Get the semantic type of the final result in the type raised
		// semantics.
		final Type secondaryReturnType = secondary.getSemantics().getType()
				.getRange();
		// Verify it matches the type of the primary semantics.
		if (secondaryReturnType == null
				|| !secondaryReturnType.isComplex()
				|| !left.getSemantics()
						.getType()
						.isExtendingOrExtendedBy(
								secondaryReturnType.getDomain())) {
			return null;
		}

		// Apply type raising.
		final ParseRuleResult<LogicalExpression> raisingResult = typeRaising
				.apply(left, left.getSyntax(), secondaryT,
						secondaryReturnType.getRange());
		if (raisingResult == null) {
			return null;
		}

		final ParseRuleResult<LogicalExpression> compositionResult = doComposition(
				raisingResult.getResultCategory(), right, false);

		if (compositionResult == null) {
			return null;
		}

		return new ParseRuleResult<LogicalExpression>(getName(),
				compositionResult.getResultCategory());
	}

	public static class Creator implements
			IResourceObjectCreator<ForwardTypeRaisedComposition> {

		private final String	type;

		public Creator() {
			this("rule.typeraise.composition.forward");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ForwardTypeRaisedComposition create(Parameters params,
				IResourceRepository repo) {
			return new ForwardTypeRaisedComposition(
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
					ForwardTypeRaisedComposition.class).build();
		}

	}
}
