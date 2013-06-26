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
package edu.uw.cs.lil.tiny.parser.ccg.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting.ITypeShiftingFunction;
import edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting.TypeShiftingRule;
import edu.uw.cs.utils.composites.Pair;

/**
 * Makes it easier to collect rules and generate the desired combinations. Takes
 * a set of binary rules {@link IBinaryParseRule} and a set of unary functions
 * {@link ITypeShiftingFunction} and creates all combinations of operating any
 * type-shifting rule and then the binary rule. Also includes just operating any
 * binary rule on the raw categories (i.e., without type-shifting).
 * 
 * @author Yoav Artzi
 */
public class RuleSetBuilder<Y> {
	
	private final Set<IBinaryParseRule<Y>>						binaryRules			= new HashSet<IBinaryParseRule<Y>>();
	private final Set<Pair<String, ITypeShiftingFunction<Y>>>	shiftingFunctions	= new HashSet<Pair<String, ITypeShiftingFunction<Y>>>();
	
	public RuleSetBuilder() {
	}
	
	public RuleSetBuilder<Y> add(IBinaryParseRule<Y> rule) {
		binaryRules.add(rule);
		return this;
	}
	
	public RuleSetBuilder<Y> add(String name, ITypeShiftingFunction<Y> function) {
		shiftingFunctions.add(Pair.of(name, function));
		return this;
	}
	
	public BinaryRulesSet<Y> build() {
		final List<IBinaryParseRule<Y>> rules = new ArrayList<IBinaryParseRule<Y>>();
		final BinaryRulesSet<Y> baseRules = new BinaryRulesSet<Y>(
				new ArrayList<IBinaryParseRule<Y>>(binaryRules));
		
		// Binary rules on their own
		rules.add(baseRules);
		
		// Type shifting rules, in combination with base rules
		for (final Pair<String, ITypeShiftingFunction<Y>> pair : shiftingFunctions) {
			rules.add(new TypeShiftingRule<Y>(pair.first(), pair.second(),
					baseRules));
		}
		
		return new BinaryRulesSet<Y>(rules);
	}
	
}
