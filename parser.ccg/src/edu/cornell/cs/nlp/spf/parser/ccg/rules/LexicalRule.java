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
package edu.cornell.cs.nlp.spf.parser.ccg.rules;

import java.util.Iterator;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.utils.collections.iterators.TransformedIterator;

/**
 * Generic lexical rule that simply takes entries from the lexicon.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class LexicalRule<MR> implements ILexicalRule<MR> {

	private static final long	serialVersionUID	= 7733692964387524627L;
	private final UnaryRuleName	name;

	public LexicalRule() {
		this(ILexicalParseStep.LEXICAL_DERIVATION_STEP_RULENAME);
	}

	public LexicalRule(UnaryRuleName name) {
		this.name = name;
	}

	@Override
	public Iterator<LexicalResult<MR>> apply(TokenSeq tokens, SentenceSpan span,
			ILexiconImmutable<MR> lexicon) {

		final Iterator<? extends LexicalEntry<MR>> iterator = lexicon
				.get(tokens);

		return new TransformedIterator<LexicalEntry<MR>, LexicalResult<MR>>(
				t -> new LexicalResult<MR>(name, t.getCategory(), t), iterator);
	}

	@Override
	public UnaryRuleName getName() {
		return name;
	}

	public static class Creator<MR>
			implements IResourceObjectCreator<LexicalRule<MR>> {

		private String type;

		public Creator() {
			this("rule.lex");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public LexicalRule<MR> create(Parameters params,
				IResourceRepository repo) {
			return new LexicalRule<>(
					ILexicalParseStep.LEXICAL_DERIVATION_STEP_RULENAME);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, LexicalResult.class)
					.setDescription(
							"Generic lexical rule that simply takes entries from the lexicon.")
					.build();
		}

	}

}
