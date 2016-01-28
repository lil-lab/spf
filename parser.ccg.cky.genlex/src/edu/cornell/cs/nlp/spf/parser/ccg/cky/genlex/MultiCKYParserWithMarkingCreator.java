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

import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYBinaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYUnaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.multi.MultiCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.multi.MultiCKYParser.Builder;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

public class MultiCKYParserWithMarkingCreator<DI extends Sentence, MR>
		implements IResourceObjectCreator<WrappedCKYParser<DI, MR>> {
	public static final ILogger						LOG				= LoggerFactory
																			.create(MultiCKYParserWithMarkingCreator.class);

	private final MultiCKYParser.Creator<DI, MR>	parserCreator	= new MultiCKYParser.Creator<DI, MR>() {
																		@Override
																		@SuppressWarnings("unchecked")
																		protected void addRule(
																				Builder<DI, MR> builder,
																				Object rule,
																				NormalFormValidator nfValidator,
																				Parameters params) {
																			if (rule instanceof IBinaryParseRule) {
																				builder.addParseRule(new MarkedCKYBinaryParsingRule<MR>(
																						(IBinaryParseRule<MR>) rule,
																						nfValidator,
																						params.getAsInteger(
																								"maxMarked",
																								1)));
																			} else if (rule instanceof IUnaryParseRule) {
																				builder.addParseRule(new CKYUnaryParsingRule<MR>(
																						(IUnaryParseRule<MR>) rule,
																						nfValidator));
																			} else if (rule instanceof CKYBinaryParsingRule) {
																				LOG.warn(
																						"Adding a unwarped CKY binary rule -- this rule won't be used to filter based on markings: %s",
																						rule);
																				builder.addParseRule((CKYBinaryParsingRule<MR>) rule);
																			} else if (rule instanceof CKYUnaryParsingRule) {
																				builder.addParseRule((CKYUnaryParsingRule<MR>) rule);
																			} else {
																				throw new IllegalArgumentException(
																						"Invalid rule class: "
																								+ rule);
																			}
																		}
																	};
	private String									type;

	public MultiCKYParserWithMarkingCreator() {
		this("parser.cky.multi.marked");
	}

	public MultiCKYParserWithMarkingCreator(String type) {
		this.type = type;
	}

	@Override
	public WrappedCKYParser<DI, MR> create(Parameters params,
			IResourceRepository repo) {
		return new WrappedCKYParser<DI, MR>(parserCreator.create(params, repo),
				params.getAsBoolean("markPruning", false));
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public ResourceUsage usage() {
		return ResourceUsage
				.builder(parserCreator.usage())
				.addParam("maxMarked", Integer.class,
						"Max number of marked cells for each parse (default: 1)")
				.addParam(
						"markPruning",
						Boolean.class,
						"Use the negation of the marking value as the primary pruning score during parsing (default: false)")
				.build();
	}

}
