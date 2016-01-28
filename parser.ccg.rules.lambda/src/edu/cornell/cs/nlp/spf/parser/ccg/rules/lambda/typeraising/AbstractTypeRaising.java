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
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.Unification;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ITypeRaisingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.utils.collections.ArrayUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;

public abstract class AbstractTypeRaising implements
		ITypeRaisingRule<LogicalExpression, Type> {
	private final Direction			direction;
	private final UnaryRuleName		ruleName;
	private final IFilter<Syntax>	validSyntaxFilter;

	public AbstractTypeRaising(Direction direction,
			IFilter<Syntax> validSyntaxFilter) {
		this.direction = direction;
		this.validSyntaxFilter = validSyntaxFilter;
		this.ruleName = TypeRaisingNameServices.createRuleName(direction);
	}

	private static LogicalExpression raiseSemantics(LogicalExpression sem,
			Type finalResultSemanticType) {
		final Variable variable = new Variable(LogicLanguageServices
				.getTypeRepository().getTypeCreateIfNeeded(
						LogicLanguageServices.getTypeRepository()
								.generalizeType(finalResultSemanticType),
						LogicLanguageServices.getTypeRepository()
								.generalizeType(sem.getType())));
		return new Lambda(variable, new Literal(variable,
				ArrayUtils.create(sem)));
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> category, Syntax innerArgument,
			Syntax finalResult, Type finalResultSemanticType) {
		final Unification innerArgumentUnification = innerArgument
				.unify(category.getSyntax());
		if (innerArgumentUnification != null
				&& validSyntaxFilter.test(category.getSyntax())) {

			// This process shouldn't create new syntactic dependencies.
			if (innerArgumentUnification.getUnifiedSyntax()
					.hasAttributeVariable()
					&& finalResult.hasAttributeVariable()) {
				return null;
			}

			final LogicalExpression raisedSemantics = raiseSemantics(
					category.getSemantics(), finalResultSemanticType);

			// Create the raised category, including the syntactic component.
			if (direction.equals(Direction.FORWARD)) {
				return new ParseRuleResult<LogicalExpression>(
						ruleName,
						new ComplexCategory<LogicalExpression>(
								new ComplexSyntax(finalResult,
										new ComplexSyntax(finalResult,
												innerArgumentUnification
														.getUnifiedSyntax(),
												Slash.BACKWARD), Slash.FORWARD),
								raisedSemantics));
			} else if (direction.equals(Direction.BACKWARD)) {
				return new ParseRuleResult<LogicalExpression>(
						ruleName,
						new ComplexCategory<LogicalExpression>(
								new ComplexSyntax(finalResult,
										new ComplexSyntax(finalResult,
												innerArgumentUnification
														.getUnifiedSyntax(),
												Slash.FORWARD), Slash.BACKWARD),
								raisedSemantics));
			} else {
				throw new IllegalStateException("invalid direction");
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractTypeRaising other = (AbstractTypeRaising) obj;
		if (direction == null) {
			if (other.direction != null) {
				return false;
			}
		} else if (!direction.equals(other.direction)) {
			return false;
		}
		if (ruleName == null) {
			if (other.ruleName != null) {
				return false;
			}
		} else if (!ruleName.equals(other.ruleName)) {
			return false;
		}
		if (validSyntaxFilter == null) {
			if (other.validSyntaxFilter != null) {
				return false;
			}
		} else if (!validSyntaxFilter.equals(other.validSyntaxFilter)) {
			return false;
		}
		return true;
	}

	@Override
	public RuleName getName() {
		return ruleName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (direction == null ? 0 : direction.hashCode());
		result = prime * result + (ruleName == null ? 0 : ruleName.hashCode());
		result = prime
				* result
				+ (validSyntaxFilter == null ? 0 : validSyntaxFilter.hashCode());
		return result;
	}

	@Override
	public boolean isValidArgument(Category<LogicalExpression> category) {
		return validSyntaxFilter.test(category.getSyntax());
	}
}
