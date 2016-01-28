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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.single;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Function;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ISentenceLexiconGenerator;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.AbstractCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYBinaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYUnaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.AbstractCellFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.AggressiveWordSkippingLexicalGenerator;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.BackwardSkippingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.ForwardSkippingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.SimpleWordSkippingLexicalGenerator;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.unaryconstraint.UnaryConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ILexicalRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.LexicalRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleSet;
import edu.cornell.cs.nlp.utils.collections.SetUtils;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.filter.FilterUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 * @param <DI>
 *            Data item.
 * @param <MR>
 *            Meaning representation.
 */
public class CKYParser<DI extends Sentence, MR>
		extends AbstractCKYParser<DI, MR> {
	public static final ILogger	LOG					= LoggerFactory
			.create(CKYParser.class);
	private static final long	serialVersionUID	= 8501917886959223073L;

	private CKYParser(int maxNumberOfCellsInSpan,
			CKYBinaryParsingRule<MR>[] binaryRules,
			List<ISentenceLexiconGenerator<DI, MR>> sentenceLexiconGenerators,
			List<ISentenceLexiconGenerator<DI, MR>> sloppyLexicalGenerators,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<MR>> completeParseFilter,
			CKYUnaryParsingRule<MR>[] unaryRules, ILexicalRule<MR> lexicalRule,
			boolean breakTies) {
		super(maxNumberOfCellsInSpan, binaryRules, sentenceLexiconGenerators,
				sloppyLexicalGenerators, categoryServices, pruneLexicalCells,
				completeParseFilter, unaryRules, lexicalRule, breakTies);
	}

	/**
	 * Add all the cells to the chart.
	 *
	 * @param newCells
	 *            list of new cells.
	 * @param chart
	 *            Chart to add the cells to.
	 */
	protected static <MR> void addAllToChart(List<Cell<MR>> newCells,
			Chart<MR> chart) {
		for (final Cell<MR> newCell : newCells) {
			chart.add(newCell);
		}
	}

	@Override
	protected Chart<MR> doParse(final Predicate<ParsingOp<MR>> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> chart, int numTokens,
			AbstractCellFactory<MR> cellFactory,
			ILexiconImmutable<MR> lexicon) {

		final int sentenceLength = chart.getSentenceLength();

		// Add lexical entries from all active lexicons
		for (int start = 0; start < numTokens; start++) {
			for (int end = start; end < numTokens; end++) {
				final Pair<Collection<Cell<MR>>, Boolean> processingPair = generateLexicalCells(
						start, end, chart, lexicon, model, pruningFilter);
				for (final Cell<MR> newCell : processingPair.first()) {
					chart.add(newCell);
				}
				if (processingPair.second()) {
					chart.externalPruning(start, end);
				}
				// Apply unary rules to cells added by lexical entries.
				final Pair<List<Cell<MR>>, Boolean> unaryProcessingResult = unaryProcessSpan(
						start, end, sentenceLength, chart, cellFactory,
						pruningFilter, model);
				if (unaryProcessingResult.second()) {
					chart.externalPruning(start, end);
				}
				for (final Cell<MR> cell : unaryProcessingResult.first()) {
					chart.add(cell);
				}
			}
		}

		// now do the CKY parsing:
		for (int len = 1; len < numTokens; len++) {
			for (int begin = 0; begin < numTokens - len; begin++) {
				for (int split = 0; split < len; split++) {
					final Pair<List<Cell<MR>>, Boolean> processingPair = processSplit(
							begin, begin + len, split, sentenceLength, chart,
							cellFactory, pruningFilter, model);
					addAllToChart(processingPair.first(), chart);
					if (processingPair.second()) {
						chart.externalPruning(begin, begin + len);
					}
				}
				final Pair<List<Cell<MR>>, Boolean> processingPair = unaryProcessSpan(
						begin, begin + len, sentenceLength, chart, cellFactory,
						pruningFilter, model);
				addAllToChart(processingPair.first(), chart);
				if (processingPair.second()) {
					chart.externalPruning(begin, begin + len);
				}
			}
		}

		return chart;
	}

	/**
	 * Builder for {@link CKYParser}.
	 *
	 * @author Yoav Artzi
	 */
	public static class Builder<DI extends Sentence, MR> {

		private final Set<CKYBinaryParsingRule<MR>>				binaryRules					= new HashSet<CKYBinaryParsingRule<MR>>();

		private boolean											breakTies					= false;

		private final ICategoryServices<MR>						categoryServices;

		private IFilter<Category<MR>>							completeParseFilter			= FilterUtils
				.stubTrue();

		private ILexicalRule<MR>								lexicalRule					= new LexicalRule<MR>();

		/** The maximum number of cells allowed in each span */
		private int												maxNumberOfCellsInSpan		= 50;

		private boolean											pruneLexicalCells			= false;

		private final List<ISentenceLexiconGenerator<DI, MR>>	sentenceLexicalGenerators	= new ArrayList<ISentenceLexiconGenerator<DI, MR>>();

		private final List<ISentenceLexiconGenerator<DI, MR>>	sloppyLexicalGenerators		= new ArrayList<ISentenceLexiconGenerator<DI, MR>>();

		private final Set<CKYUnaryParsingRule<MR>>				unaryRules					= new HashSet<CKYUnaryParsingRule<MR>>();

		public Builder(ICategoryServices<MR> categoryServices) {
			this.categoryServices = categoryServices;
		}

		public Builder<DI, MR> addParseRule(CKYBinaryParsingRule<MR> rule) {
			binaryRules.add(rule);
			return this;
		}

		public Builder<DI, MR> addParseRule(CKYUnaryParsingRule<MR> rule) {
			unaryRules.add(rule);
			return this;
		}

		public Builder<DI, MR> addSentenceLexicalGenerator(
				ISentenceLexiconGenerator<DI, MR> generator) {
			sentenceLexicalGenerators.add(generator);
			return this;
		}

		public Builder<DI, MR> addSloppyLexicalGenerator(
				ISentenceLexiconGenerator<DI, MR> sloppyGenerator) {
			sloppyLexicalGenerators.add(sloppyGenerator);
			return this;
		}

		@SuppressWarnings("unchecked")
		public CKYParser<DI, MR> build() {
			return new CKYParser<DI, MR>(maxNumberOfCellsInSpan,
					binaryRules.toArray((CKYBinaryParsingRule<MR>[]) Array
							.newInstance(CKYBinaryParsingRule.class,
									binaryRules.size())),
					sentenceLexicalGenerators, sloppyLexicalGenerators,
					categoryServices, pruneLexicalCells, completeParseFilter,
					unaryRules.toArray((CKYUnaryParsingRule<MR>[]) Array
							.newInstance(CKYUnaryParsingRule.class,
									unaryRules.size())),
					lexicalRule, breakTies);
		}

		public Builder<DI, MR> setBreakTies(boolean breakTies) {
			this.breakTies = breakTies;
			return this;
		}

		public Builder<DI, MR> setCompleteParseFilter(
				IFilter<Category<MR>> completeParseFilter) {
			this.completeParseFilter = completeParseFilter;
			return this;
		}

		public void setLexicalRule(ILexicalRule<MR> lexicalRule) {
			this.lexicalRule = lexicalRule;
		}

		public Builder<DI, MR> setMaxNumberOfCellsInSpan(
				int maxNumberOfCellsInSpan) {
			this.maxNumberOfCellsInSpan = maxNumberOfCellsInSpan;
			return this;
		}

		public Builder<DI, MR> setPruneLexicalCells(boolean pruneLexicalCells) {
			this.pruneLexicalCells = pruneLexicalCells;
			return this;
		}
	}

	public static class Creator<DI extends Sentence, MR>
			implements IResourceObjectCreator<CKYParser<DI, MR>> {

		private String type;

		public Creator() {
			this("parser.cky");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public CKYParser<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			final Builder<DI, MR> builder = new Builder<DI, MR>(
					(ICategoryServices<MR>) repo.get(
							ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));

			if (params.contains("breakTies")) {
				builder.setBreakTies(params.getAsBoolean("breakTies"));
			}

			if (params.contains("parseFilter")) {
				builder.setCompleteParseFilter((IFilter<Category<MR>>) repo
						.get(params.get("parseFilter")));
			}

			if (params.contains("beam")) {
				builder.setMaxNumberOfCellsInSpan(params.getAsInteger("beam"));
			}

			if (params.contains("lex")) {
				builder.setLexicalRule(
						(ILexicalRule<MR>) repo.get(params.get("lex")));
			}

			if (params.contains("pruneLexicalCells")) {
				builder.setPruneLexicalCells(
						params.getAsBoolean("pruneLexicalCells"));
			}

			for (final String id : params.getSplit("generators")) {
				builder.addSentenceLexicalGenerator(
						(ISentenceLexiconGenerator<DI, MR>) repo.get(id));
			}

			for (final String id : params.getSplit("sloppyGenerators")) {
				builder.addSloppyLexicalGenerator(
						(ISentenceLexiconGenerator<DI, MR>) repo.get(id));
			}

			NormalFormValidator nfValidator;
			if (params.contains("nfValidator")) {
				nfValidator = repo.get(params.get("nfValidator"));
			} else {
				nfValidator = null;
			}

			final String wordSkippingType = params.get("wordSkipping", "none");
			if (wordSkippingType.equals("simple")) {
				// Skipping lexical generator.
				builder.addSloppyLexicalGenerator(
						new SimpleWordSkippingLexicalGenerator<DI, MR>(
								(ICategoryServices<MR>) repo.get(
										ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE)));

				// Skipping rules.
				final ForwardSkippingRule<MR> forwardSkip = new ForwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo.get(
								ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
				final BackwardSkippingRule<MR> backSkip = new BackwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo
								.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
						false);

				// Add a normal form constraint to disallow unary steps after
				// skipping.
				final NormalFormValidator.Builder nfBuilder = new NormalFormValidator.Builder();
				if (nfValidator != null) {
					nfBuilder.addConstraints(nfValidator);
				}
				nfBuilder.addConstraint(new UnaryConstraint(SetUtils
						.createSet(forwardSkip.getName(), backSkip.getName())));
				nfValidator = nfBuilder.build();

				// Add the rules.
				addRule(builder, backSkip, nfValidator);
				addRule(builder, forwardSkip, nfValidator);
			} else if (wordSkippingType.equals("aggressive")) {
				// Skipping lexical generator.
				builder.addSloppyLexicalGenerator(
						new AggressiveWordSkippingLexicalGenerator<DI, MR>(
								(ICategoryServices<MR>) repo.get(
										ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE)));
				// Skipping rules.
				final ForwardSkippingRule<MR> forwardSkip = new ForwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo.get(
								ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
				final BackwardSkippingRule<MR> backSkip = new BackwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo
								.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
						true);

				// Add a normal form constraint to disallow unary steps after
				// skipping.
				final NormalFormValidator.Builder nfBuilder = new NormalFormValidator.Builder();
				if (nfValidator != null) {
					nfBuilder.addConstraints(nfValidator);
				}
				nfBuilder.addConstraint(new UnaryConstraint(SetUtils
						.createSet(forwardSkip.getName(), backSkip.getName())));
				nfValidator = nfBuilder.build();

				// Add the rules.
				addRule(builder, backSkip, nfValidator);
				addRule(builder, forwardSkip, nfValidator);
			}

			for (final String id : params.getSplit("rules")) {
				final Object rule = repo.get(id);
				if (rule instanceof BinaryRuleSet) {
					for (final IBinaryParseRule<MR> singleRule : (BinaryRuleSet<MR>) rule) {
						addRule(builder, singleRule, nfValidator);
					}
				} else if (rule instanceof UnaryRuleSet) {
					for (final IUnaryParseRule<MR> singleRule : (UnaryRuleSet<MR>) rule) {
						addRule(builder, singleRule, nfValidator);
					}
				} else {
					addRule(builder, rule, nfValidator);
				}
			}

			return builder.build();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, CKYParser.class)
					.addParam("breakTies", Boolean.class,
							"Breaks ties during pruning using the order of insertion to the queue. In a single-threaded parser, this is essentially deterministic (default: false)")
					.addParam("parseFilter", IFilter.class,
							"Filter to determine complete parses.")
					.addParam("beam", Integer.class,
							"Beam to use for cell pruning (default: 50).")
					.addParam("lex", ILexicalRule.class,
							"Lexical rule (default: simple generic rule)")
					.addParam("pruneLexicalCells", Boolean.class,
							"Prune lexical entries similarly to conventional categories (default: false)")
					.addParam("wordSkipping", String.class,
							"Type of word skpping to use during sloppy inference: none, simple or aggressive (default: none)")
					.addParam("generators", ISentenceLexiconGenerator.class,
							"List of dynamic sentence lexical generators.")
					.addParam("sloppyGenerators",
							ISentenceLexiconGenerator.class,
							"List of dynamic sentence lexical generators for sloppy inference.")
					.addParam("transformation", Function.class,
							"Transformation to be applied to each category before it's added to the chart (default: none).")
					.addParam("rules", IBinaryParseRule.class,
							"Binary parsing rules.")
					.build();
		}

		@SuppressWarnings("unchecked")
		private void addRule(Builder<DI, MR> builder, Object rule,
				NormalFormValidator nfValidator) {
			if (rule instanceof IBinaryParseRule) {
				builder.addParseRule(new CKYBinaryParsingRule<MR>(
						(IBinaryParseRule<MR>) rule, nfValidator));
			} else if (rule instanceof IUnaryParseRule) {
				builder.addParseRule(new CKYUnaryParsingRule<MR>(
						(IUnaryParseRule<MR>) rule, nfValidator));
			} else if (rule instanceof CKYBinaryParsingRule) {
				builder.addParseRule((CKYBinaryParsingRule<MR>) rule);
			} else if (rule instanceof CKYUnaryParsingRule) {
				builder.addParseRule((CKYUnaryParsingRule<MR>) rule);
			} else {
				throw new IllegalArgumentException(
						"Invalid rule class: " + rule);
			}
		}

	}
}
