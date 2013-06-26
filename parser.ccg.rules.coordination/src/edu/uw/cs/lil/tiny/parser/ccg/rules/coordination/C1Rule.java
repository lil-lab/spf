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
package edu.uw.cs.lil.tiny.parser.ccg.rules.coordination;

import java.util.Collection;
import java.util.Collections;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.utils.collections.ListUtils;

class C1Rule<Y> implements IBinaryParseRule<Y> {
	
	private static final String				RULE_NAME	= "c1";
	private final ICoordinationServices<Y>	services;
	
	public C1Rule(ICoordinationServices<Y> services) {
		this.services = services;
	}
	
	@Override
	public Collection<ParseRuleResult<Y>> apply(Category<Y> left,
			Category<Y> right, boolean isCompleteSentence) {
		if (left.getSyntax().equals(Syntax.C) && left.getSem() != null
				&& right.getSem() != null) {
			// Simple coordination is the case of an argument of type 't' that
			// is coordinated through simple conjuncton/disjunction with others
			final Y simpleCoordination = services.createSimpleCoordination(
					right.getSem(), left.getSem());
			
			if (simpleCoordination == null) {
				// Case simple coordination failed, try the more complex case
				final Y semantics = services.createPartialCoordination(
						right.getSem(), left.getSem());
				if (semantics != null) {
					return ListUtils
							.createSingletonList(new ParseRuleResult<Y>(
									RULE_NAME, Category.create(
											new ComplexSyntax(
													new ComplexSyntax(left
															.getSyntax(), right
															.getSyntax(),
															Slash.VERTICAL),
													right.getSyntax(),
													Slash.BACKWARD), semantics)));
				}
			} else {
				return ListUtils.createSingletonList(new ParseRuleResult<Y>(
						RULE_NAME, Category.create(
								new ComplexSyntax(right.getSyntax(), right
										.getSyntax(), Slash.BACKWARD),
								simpleCoordination)));
			}
		}
		return Collections.emptyList();
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
		final C1Rule other = (C1Rule) obj;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((RULE_NAME == null) ? 0 : RULE_NAME.hashCode());
		result = prime * result
				+ ((services == null) ? 0 : services.hashCode());
		return result;
	}
	
	@Override
	public boolean isOverLoadable() {
		return true;
	}
	
}
