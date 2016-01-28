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

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;

/**
 * A CKY rule wrapping a {@link IUnaryParseRule}.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class CKYUnaryParsingRule<MR> implements Serializable {

	private static final long			serialVersionUID	= -9166304224061539978L;
	private final NormalFormValidator	nfValidator;
	private final IUnaryParseRule<MR>	rule;

	public CKYUnaryParsingRule(IUnaryParseRule<MR> rule) {
		this(rule, null);
	}

	public CKYUnaryParsingRule(IUnaryParseRule<MR> rule,
			NormalFormValidator nfValidator) {
		this.rule = rule;
		this.nfValidator = nfValidator;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]",
				CKYBinaryParsingRule.class.getSimpleName(), rule);
	}

	/**
	 * Applies the underlying parse rule to a single cell.
	 */
	protected ParseRuleResult<MR> apply(Cell<MR> cell, SentenceSpan span) {
		if (nfValidator != null && !nfValidator.isValid(cell, rule.getName())) {
			return null;
		}
		return rule.apply(cell.getCategory(), span);
	}

	/**
	 * @see IUnaryParseRule#isValidArgument(Category)
	 */
	boolean isValidArgument(Category<MR> category, SentenceSpan span) {
		return rule.isValidArgument(category, span);
	}
}
