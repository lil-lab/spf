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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.coordination;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;

class CXRule<MR> implements IBinaryParseRule<MR> {

	private static final RuleName			RULE_NAME			= RuleName
																		.create("cx",
																				Direction.FORWARD);

	private static final long				serialVersionUID	= -8182813419817976425L;

	private final ICoordinationServices<MR>	services;

	public CXRule(ICoordinationServices<MR> services) {
		this.services = services;
	}

	@Override
	public ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right,
			SentenceSpan span) {
		if (left.getSyntax() instanceof ComplexSyntax
				&& ((ComplexSyntax) left.getSyntax()).getSlash().equals(
						Slash.FORWARD) && right.getSemantics() != null
				&& left.getSemantics() != null) {
			final Syntax argType = ((ComplexSyntax) left.getSyntax())
					.getRight();
			if (SyntaxCoordinationServices.isCoordinationOfType(
					right.getSyntax(), argType)) {
				final MR applied = services.applyCoordination(
						left.getSemantics(), right.getSemantics());
				if (applied != null) {
					return new ParseRuleResult<MR>(RULE_NAME, Category.create(
							((ComplexSyntax) left.getSyntax()).getLeft(),
							applied));
				}
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
		@SuppressWarnings("rawtypes")
		final CXRule other = (CXRule) obj;
		if (services == null) {
			if (other.services != null) {
				return false;
			}
		} else if (!services.equals(other.services)) {
			return false;
		}
		return true;
	}

	@Override
	public RuleName getName() {
		return RULE_NAME;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (RULE_NAME == null ? 0 : RULE_NAME.hashCode());
		result = prime * result + (services == null ? 0 : services.hashCode());
		return result;
	}

}
