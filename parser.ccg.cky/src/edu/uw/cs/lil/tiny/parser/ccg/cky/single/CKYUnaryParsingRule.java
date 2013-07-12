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
package edu.uw.cs.lil.tiny.parser.ccg.cky.single;

import java.util.Collection;
import java.util.Collections;

import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IUnaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class CKYUnaryParsingRule<Y> {
	private final boolean				applyOnlyToCompleteSentences;
	private final IUnaryParseRule<Y>	ccgParseRule;
	
	public CKYUnaryParsingRule(IUnaryParseRule<Y> ccgParseRule) {
		this(ccgParseRule, false);
	}
	
	public CKYUnaryParsingRule(IUnaryParseRule<Y> ccgParseRule,
			boolean applyOnlyToCompleteSentences) {
		this.ccgParseRule = ccgParseRule;
		this.applyOnlyToCompleteSentences = applyOnlyToCompleteSentences;
	}
	
	/**
	 * Takes two cell, left and right, as input. Assumes these cells are
	 * adjacent. Adds any new cells it can produce to the result list.
	 */
	public Collection<ParseRuleResult<Y>> apply(Cell<Y> cell) {
		if (!applyOnlyToCompleteSentences || cell.isCompleteSpan()) {
			return ccgParseRule.apply(cell.getCategroy());
		}
		return Collections.emptyList();
	}
}
