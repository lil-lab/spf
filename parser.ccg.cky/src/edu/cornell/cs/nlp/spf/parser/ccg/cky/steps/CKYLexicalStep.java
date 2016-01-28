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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.steps;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.IOverloadedParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.OverloadedRuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

public class CKYLexicalStep<MR> extends AbstractCKYStep<MR>
		implements ILexicalParseStep<MR> {

	private int						hashCode;

	private final LexicalEntry<MR>	lexicalEntry;

	public CKYLexicalStep(Category<MR> root, LexicalEntry<MR> lexicalEntry,
			boolean isFullParse, int start, int end) {
		this(root, lexicalEntry, isFullParse, LEXICAL_DERIVATION_STEP_RULENAME,
				start, end);
		this.hashCode = calcHashCode();
	}

	public CKYLexicalStep(LexicalEntry<MR> lexicalEntry, boolean isFullParse,
			int start, int end) {
		this(lexicalEntry.getCategory(), lexicalEntry, isFullParse,
				LEXICAL_DERIVATION_STEP_RULENAME, start, end);
	}

	private CKYLexicalStep(Category<MR> root, LexicalEntry<MR> lexicalEntry,
			boolean isFullParse, RuleName ruleName, int start, int end) {
		super(root, ruleName, isFullParse, start, end);
		this.lexicalEntry = lexicalEntry;
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
		@SuppressWarnings("unchecked")
		final CKYLexicalStep<MR> other = (CKYLexicalStep<MR>) obj;
		if (lexicalEntry == null) {
			if (other.lexicalEntry != null) {
				return false;
			}
		} else if (!lexicalEntry.equals(other.lexicalEntry)) {
			return false;
		}
		return true;
	}

	@Override
	public LexicalEntry<MR> getLexicalEntry() {
		return lexicalEntry;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public CKYLexicalStep<MR> overloadWithUnary(
			ParseRuleResult<MR> unaryRuleResult, boolean fullParseAfterUnary) {
		if (!(unaryRuleResult.getRuleName() instanceof UnaryRuleName)) {
			throw new IllegalStateException(
					"Provided result is not from a unary rule: "
							+ unaryRuleResult);
		}
		return new Overloaded<MR>(unaryRuleResult.getResultCategory(),
				getRoot(), lexicalEntry, fullParseAfterUnary,
				getRuleName().overload(
						(UnaryRuleName) unaryRuleResult.getRuleName()),
				getStart(), getEnd());

	}

	@Override
	public IWeightedCKYStep<MR> overloadWithUnary(
			ParseRuleResult<MR> unaryRuleResult, boolean fullParseAfterUnary,
			IDataItemModel<MR> model) {
		final CKYLexicalStep<MR> overloaded = overloadWithUnary(unaryRuleResult,
				fullParseAfterUnary);
		return new WeightedCKYLexicalStep<MR>(overloaded, model);
	}

	@Override
	public String toString(boolean verbose, boolean recursive) {
		return new StringBuilder("[").append(getStart()).append("-")
				.append(getEnd()).append(" :: ").append(getRuleName())
				.append(" :: ").append(lexicalEntry).append("{")
				.append(lexicalEntry.getOrigin()).append("}]").toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ (lexicalEntry == null ? 0 : lexicalEntry.hashCode());
		return result;
	}

	private static class Overloaded<MR> extends CKYLexicalStep<MR>
			implements IOverloadedParseStep<MR> {

		private final Category<MR> intermediate;

		private Overloaded(Category<MR> root, Category<MR> intermediate,
				LexicalEntry<MR> lexicalEntry, boolean isFullParse,
				OverloadedRuleName ruleName, int start, int end) {
			super(root, lexicalEntry, isFullParse, ruleName, start, end);
			this.intermediate = intermediate;
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
			final Overloaded other = (Overloaded) obj;
			if (intermediate == null) {
				if (other.intermediate != null) {
					return false;
				}
			} else if (!intermediate.equals(other.intermediate)) {
				return false;
			}
			return true;
		}

		@Override
		public Category<MR> getIntermediate() {
			return intermediate;
		}

		@Override
		public OverloadedRuleName getRuleName() {
			return (OverloadedRuleName) super.getRuleName();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ (intermediate == null ? 0 : intermediate.hashCode());
			return result;
		}

		@Override
		public CKYLexicalStep<MR> overloadWithUnary(
				ParseRuleResult<MR> unaryRuleResult,
				boolean fullParseAfterUnary) {
			throw new IllegalStateException(
					"Can't overload an already overloaded step");
		}

	}

}
