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
package edu.cornell.cs.nlp.spf.parser.ccg.cky;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;

public class CKYBinaryParsingRule<MR> implements Serializable {
	private static final long			serialVersionUID	= -5629394704296771855L;
	private final NormalFormValidator	nfValidator;
	private final IBinaryParseRule<MR>	rule;

	public CKYBinaryParsingRule(IBinaryParseRule<MR> rule) {
		this(rule, null);
	}

	public CKYBinaryParsingRule(IBinaryParseRule<MR> rule,
			NormalFormValidator nfValidator) {
		this.rule = rule;
		this.nfValidator = nfValidator;
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
		final CKYBinaryParsingRule other = (CKYBinaryParsingRule) obj;
		if (nfValidator == null) {
			if (other.nfValidator != null) {
				return false;
			}
		} else if (!nfValidator.equals(other.nfValidator)) {
			return false;
		}
		if (rule == null) {
			if (other.rule != null) {
				return false;
			}
		} else if (!rule.equals(other.rule)) {
			return false;
		}
		return true;
	}

	public RuleName getName() {
		return rule.getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (nfValidator == null ? 0 : nfValidator.hashCode());
		result = prime * result + (rule == null ? 0 : rule.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]",
				CKYBinaryParsingRule.class.getSimpleName(), rule);
	}

	/**
	 * Takes two cell, left and right, as input. Assumes these cells are
	 * adjacent.
	 */
	protected ParseRuleResult<MR> apply(Cell<MR> left, Cell<MR> right,
			SentenceSpan span) {
		assert left.getEnd() + 1 == right.getStart();
		if (nfValidator != null
				&& !nfValidator.isValid(left, right, rule.getName())) {
			return null;
		}
		return rule.apply(left.getCategory(), right.getCategory(), span);
	}
}
