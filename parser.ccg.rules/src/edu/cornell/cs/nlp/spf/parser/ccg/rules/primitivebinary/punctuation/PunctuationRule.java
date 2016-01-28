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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.punctuation;

import java.util.Collections;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

/**
 * Punctuation rule: A : f PUNCT -> A : f.
 *
 * @param <MR>
 *            Meaning representation type.
 */
public class PunctuationRule<MR> implements IBinaryReversibleParseRule<MR> {

	private static final String	LABEL				= "punct";

	private static final long	serialVersionUID	= -1283499870946459748L;

	private final RuleName		name;

	public PunctuationRule() {
		this.name = RuleName.create(LABEL, Direction.FORWARD);
	}

	@Override
	public ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right,
			SentenceSpan span) {
		if (Syntax.PUNCT.equals(right.getSyntax())) {
			return new ParseRuleResult<MR>(name, left);
		}
		return null;
	}

	@Override
	public RuleName getName() {
		return name;
	}

	@Override
	public Set<Category<MR>> reverseApplyLeft(Category<MR> left,
			Category<MR> result, SentenceSpan span) {
		if (result.equals(left)) {
			return SetUtils.createSingleton(Category.<MR> create(Syntax.PUNCT));
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<Category<MR>> reverseApplyRight(Category<MR> right,
			Category<MR> result, SentenceSpan span) {
		if (Syntax.PUNCT.equals(right.getSyntax())) {
			return SetUtils.createSingleton(result);
		}
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		return name.toString();
	}

	public static class Creator<MR> implements
			IResourceObjectCreator<PunctuationRule<MR>> {

		private String	type;

		public Creator() {
			this("rule.punctuation");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public PunctuationRule<MR> create(Parameters params,
				IResourceRepository repo) {
			return new PunctuationRule<MR>();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, PunctuationRule.class)
					.setDescription("Punctuation rule: A : f PUNCT -> A : f.")
					.build();
		}

	}

}
