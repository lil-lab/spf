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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.genlex;

import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYBinaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;

/**
 * {@link CKYBinaryParsingRule} to test for the number of marked lexical entries
 * before applying the actual rule.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class MarkedCKYBinaryParsingRule<MR> extends CKYBinaryParsingRule<MR> {

	private static final long	serialVersionUID	= -2179955393871218371L;
	private final int			maxMarkedLexicalEntries;

	public MarkedCKYBinaryParsingRule(IBinaryParseRule<MR> ccgParseRule,
			NormalFormValidator nfValidator, int maxMarkedLexicalEntries) {
		super(ccgParseRule, nfValidator);
		this.maxMarkedLexicalEntries = maxMarkedLexicalEntries;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final MarkedCKYBinaryParsingRule other = (MarkedCKYBinaryParsingRule) obj;
		if (maxMarkedLexicalEntries != other.maxMarkedLexicalEntries) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + maxMarkedLexicalEntries;
		return result;
	}

	@Override
	protected ParseRuleResult<MR> apply(Cell<MR> left, Cell<MR> right,
			SentenceSpan span) {
		// If both cells contains a GENLEX lexical entry, don't apply the rule,
		// just return
		if (left instanceof IMarkedEntriesCounter
				&& right instanceof IMarkedEntriesCounter
				&& ((IMarkedEntriesCounter) left).getNumMarkedLexicalEntries()
						+ ((IMarkedEntriesCounter) right)
								.getNumMarkedLexicalEntries() > maxMarkedLexicalEntries) {
			return null;
		}

		return super.apply(left, right, span);
	}
}
