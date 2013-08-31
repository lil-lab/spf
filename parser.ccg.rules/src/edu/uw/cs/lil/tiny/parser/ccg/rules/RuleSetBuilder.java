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

/**
 * Makes it easier to collect rules and generate the desired combinations. Takes
 * a set of binary rules {@link IBinaryParseRule} and a set of unary functions
 * {@link ITypeShiftingFunction} and creates all combinations of operating any
 * type-shifting rule and then the binary rule. Also includes just operating any
 * binary rule on the raw categories (i.e., without type-shifting).
 * 
 * @author Yoav Artzi
 */
public class RuleSetBuilder<MR> {
	
	private final Set<IBinaryParseRule<MR>>			binaryRules			= new HashSet<IBinaryParseRule<MR>>();
	private final Set<ITypeShiftingFunction<MR>>	shiftingFunctions	= new HashSet<ITypeShiftingFunction<MR>>();
	
	public RuleSetBuilder() {
	}
	
	public RuleSetBuilder<MR> add(IBinaryParseRule<MR> rule) {
		binaryRules.add(rule);
		return this;
	}
	
	public RuleSetBuilder<MR> add(ITypeShiftingFunction<MR> function) {
		shiftingFunctions.add(function);
		return this;
	}
	
	public BinaryRulesSet<MR> build() {
		final List<IBinaryParseRule<MR>> rules = new ArrayList<IBinaryParseRule<MR>>();
		final BinaryRulesSet<MR> baseRules = new BinaryRulesSet<MR>(
				new ArrayList<IBinaryParseRule<MR>>(binaryRules));
		
		// Binary rules on their own
		rules.add(baseRules);
		
		// Type shifting rules, in combination with base rules
		for (final ITypeShiftingFunction<MR> function : shiftingFunctions) {
			rules.add(new TypeShiftingRule<MR>(function, baseRules));
		}
		
		return new BinaryRulesSet<MR>(rules);
	}
	
}
