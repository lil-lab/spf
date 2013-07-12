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
package edu.uw.cs.lil.tiny.parser.ccg.cky.genlex;

import java.util.Collection;
import java.util.Collections;

import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class MarkAwareCKYBinaryParsingRule<MR> extends CKYBinaryParsingRule<MR> {
	
	private final int	maxMarkedLexicalEntries;
	
	public MarkAwareCKYBinaryParsingRule(IBinaryParseRule<MR> ccgParseRule,
			int maxMarkedLexicalEntries) {
		super(ccgParseRule);
		this.maxMarkedLexicalEntries = maxMarkedLexicalEntries;
	}
	
	@Override
	protected Collection<ParseRuleResult<MR>> apply(Cell<MR> left,
			Cell<MR> right, boolean isCompleteSentence) {
		// If both cells contains a GENLEX lexical entry, don't apply the rule,
		// just return
		if (left instanceof IMarkedEntriesCounter
				&& right instanceof IMarkedEntriesCounter
				&& ((IMarkedEntriesCounter) left).getNumMarkedLexicalEntries()
						+ ((IMarkedEntriesCounter) right)
								.getNumMarkedLexicalEntries() > maxMarkedLexicalEntries) {
			return Collections.emptyList();
		}
		
		return super.apply(left, right, isCompleteSentence);
	}
}
