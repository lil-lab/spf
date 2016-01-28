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
package edu.cornell.cs.nlp.spf.genlex.ccg.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringSignature;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;

public class GenerationRepositoryWithConstants extends GenerationRepository {

	private static final long											serialVersionUID	= 3823769455773586642L;
	private final Set<LogicalConstant>									constants;
	private final Map<FactoringSignature, List<List<LogicalConstant>>>	signaturesAndSeqs;

	protected GenerationRepositoryWithConstants(Set<LogicalConstant> constants,
			Set<LexicalTemplate> templates,
			Map<LexicalTemplate, List<List<String>>> templatesAndAttributes,
			Set<String> attributes,
			Map<FactoringSignature, List<List<LogicalConstant>>> signaturesAndSeqs) {
		super(templates, templatesAndAttributes, attributes);
		this.constants = constants;
		this.signaturesAndSeqs = signaturesAndSeqs;
	}

	@Override
	public boolean addTemplate(LexicalTemplate template) {
		if (super.addTemplate(template)) {
			if (!signaturesAndSeqs.containsKey(template.getSignature())) {
				signaturesAndSeqs.put(template.getSignature(),
						Collections.unmodifiableList(
								createPotentialConstantSeqs(constants,
										template.getSignature())));
			}
			return true;
		}
		return false;
	}

	/**
	 * Generate lexemes for a sequence of tokens.
	 *
	 * @param tokens
	 *            Source tokens.
	 * @param maxTokens
	 *            Maximum number of tokens for sub-sequence.
	 * @param properties
	 *            The properties to assign to generated lexemes.
	 * @return Set of lexemes. Although a list is returned, each lexeme is
	 *         guaranteed to be unique and the ordering carries no significance.
	 */
	public List<Lexeme> generate(TokenSeq tokens, int maxTokens,
			Map<String, String> properties) {
		final long startTime = System.currentTimeMillis();

		// Prepare all token sequences.
		final int numTokens = tokens.size();
		final List<TokenSeq> tokenSeqs = new ArrayList<>();
		for (int i = 0; i < numTokens; ++i) {
			for (int j = i; j < numTokens && j - i + 1 <= maxTokens; ++j) {
				tokenSeqs.add(tokens.sub(i, j + 1));
					}
				}

		// Use all signatures, constants, and attributes to generate new lexemes
		// for all possible tokens sequences. While using the templates may be
		// more conservative, repeating signature will lead to significant
		// repeating work. Therefore, we work at the signature level here. This
		// entire computation is packed into stream operations with the goal of
		// doing it in parallel.

		final List<Lexeme> lexemes = getSignatures().stream().parallel()
				.unordered().map(signature -> {
					// Create a list of streams, each one contains all the
					// lexemes for a specific signature.
					final List<Stream<Lexeme>> lexemeStreams = new LinkedList<>();
					final List<List<LogicalConstant>> constSeqs = getConstantSeqs(
							signature);
					for (final List<String> attributes : getAttributeLists(
							signature.getNumAttributes())) {
						for (final List<LogicalConstant> constSeq : constSeqs) {
							lexemeStreams.add(
									tokenSeqs.stream().parallel().unordered()
											.map(tokenSeq -> new Lexeme(
													tokenSeq, constSeq,
													attributes, properties)));
			}
		}
					return lexemeStreams;
				}).flatMap(list -> list.stream().parallel().unordered())
				.flatMap(Function.identity()).collect(Collectors.toList());

		LOG.debug(
				"Generated %d lexemes for %d signautres (and %d templates) and %d constants (%.3fsec)",
				lexemes.size(), numSignatures(), numTemplates(),
				constants.size(),
				(System.currentTimeMillis() - startTime) / 1000.0);
		return lexemes;
	}

	public List<List<LogicalConstant>> getConstantSeqs(
			FactoringSignature signature) {
		return signaturesAndSeqs.get(signature);
	}

	public List<List<LogicalConstant>> getConstantSeqs(
			LexicalTemplate template) {
		return getConstantSeqs(template.getSignature());
	}

}
