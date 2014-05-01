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
package edu.uw.cs.lil.tiny.parser.ccg.cky.chart;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.lil.tiny.parser.ccg.rules.UnaryRuleName;

public class CKYLexicalStep<MR> extends AbstractCKYParseStep<MR> implements
		ILexicalParseStep<MR> {
	
	public CKYLexicalStep(Category<MR> root, LexicalEntry<MR> lexicalEntry,
			boolean isFullParse, IDataItemModel<MR> model) {
		this(root, lexicalEntry, isFullParse, LEXICAL_DERIVATION_STEP_RULENAME,
				model);
	}
	
	public CKYLexicalStep(LexicalEntry<MR> lexicalEntry, boolean isFullParse,
			IDataItemModel<MR> model) {
		this(lexicalEntry.getCategory(), lexicalEntry, isFullParse,
				LEXICAL_DERIVATION_STEP_RULENAME, model);
	}
	
	private CKYLexicalStep(Category<MR> root, LexicalEntry<MR> lexicalEntry,
			boolean isFullParse, RuleName ruleName, IDataItemModel<MR> model) {
		super(root, lexicalEntry, ruleName, isFullParse, model);
	}
	
	@Override
	public AbstractCKYParseStep<MR> cloneWithUnary(
			ParseRuleResult<MR> unaryRuleResult, IDataItemModel<MR> model,
			boolean fullParseAfterUnary) {
		if (!(unaryRuleResult.getRuleName() instanceof UnaryRuleName)) {
			throw new IllegalStateException(
					"Provided result is not from a unary rule: "
							+ unaryRuleResult);
		}
		return new CKYLexicalStep<MR>(
				unaryRuleResult.getResultCategory(),
				lexicalEntry,
				fullParseAfterUnary,
				ruleName.overload((UnaryRuleName) unaryRuleResult.getRuleName()),
				model);
		
	}
	
	@Override
	public LexicalEntry<MR> getLexicalEntry() {
		return lexicalEntry;
	}
	
}
