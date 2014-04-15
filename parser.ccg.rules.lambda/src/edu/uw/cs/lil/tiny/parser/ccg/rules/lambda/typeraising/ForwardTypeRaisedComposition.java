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
package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition.AbstractComposition;
import edu.uw.cs.utils.filter.IFilter;

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
 * @author Luke Zettlemoyer
 */
public class ForwardTypeRaisedComposition extends
		AbstractComposition<LogicalExpression> {
	private static final String			RULE_LABEL	= "trcomp";
	private final ForwardTypeRaising	typeRaising;
	
	public ForwardTypeRaisedComposition(
			ICategoryServices<LogicalExpression> categoryServices) {
		super(RULE_LABEL, Direction.FORWARD, 0, categoryServices);
		this.typeRaising = new ForwardTypeRaising(new IFilter<Syntax>() {
			
			@Override
			public boolean isValid(Syntax e) {
				return true;
			}
		});
	}
	
	@Override
	public Collection<ParseRuleResult<LogicalExpression>> apply(
			Category<LogicalExpression> left, Category<LogicalExpression> right) {
		
		// Right side must be a complex category.
		if (!(right instanceof ComplexCategory<?>)) {
			return Collections.emptyList();
		}
		// It's the secondary argument of the composition.
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) right;
		
		// Verify secondary slash.
		if (!secondary.hasSlash(Slash.FORWARD)) {
			return Collections.emptyList();
		}
		
		// Get the embedded X from the secondary. First verify we have the right
		// structure.
		if (!(secondary.getSyntax().getLeft() instanceof ComplexSyntax)
				|| !((ComplexSyntax) secondary.getSyntax().getLeft())
						.getSlash().equals(Slash.BACKWARD)) {
			return Collections.emptyList();
		}
		final Syntax secondaryT = ((ComplexSyntax) secondary.getSyntax()
				.getLeft()).getLeft();
		final Syntax secondaryX = ((ComplexSyntax) secondary.getSyntax()
				.getLeft()).getRight();
		
		// Verify the Xs (see javadoc above) match.
		if (!secondaryX.equals(left.getSyntax())) {
			return Collections.emptyList();
		}
		
		// Get the semantic type of the final result in the type raised
		// semantics.
		final Type secondaryReturnType = secondary.getSem().getType()
				.getRange();
		// Verify it matches the type of the primary semantics.
		if (secondaryReturnType == null
				|| !secondaryReturnType.isComplex()
				|| !left.getSem()
						.getType()
						.isExtendingOrExtendedBy(
								secondaryReturnType.getDomain())) {
			return Collections.emptyList();
		}
		
		// Apply type raising.
		final ParseRuleResult<LogicalExpression> raisingResult = typeRaising
				.apply(left, left.getSyntax(), secondaryT,
						secondaryReturnType.getRange());
		if (raisingResult == null) {
			return Collections.emptyList();
		}
		
		final List<ParseRuleResult<LogicalExpression>> compositionResults = doComposition(
				raisingResult.getResultCategory(), right, false);
		
		final List<ParseRuleResult<LogicalExpression>> results = new ArrayList<ParseRuleResult<LogicalExpression>>(
				compositionResults.size());
		
		for (final ParseRuleResult<LogicalExpression> result : compositionResults) {
			results.add(new ParseRuleResult<LogicalExpression>(getName(),
					result.getResultCategory()));
		}
		
		return results;
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
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
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
