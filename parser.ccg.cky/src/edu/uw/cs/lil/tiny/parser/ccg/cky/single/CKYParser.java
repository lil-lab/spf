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
package edu.uw.cs.lil.tiny.parser.ccg.cky.single;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Function;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYUnaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.SimpleWordSkippingLexicalGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 * @author Tom Kwiatkowski
 */
public class CKYParser<MR> extends AbstractCKYParser<MR> {
	public static final ILogger	LOG	= LoggerFactory.create(CKYParser.class);
	
	private CKYParser(int maxNumberOfCellsInSpan,
			List<CKYBinaryParsingRule<MR>> binaryRules,
			List<ISentenceLexiconGenerator<MR>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<MR>> completeParseFilter,
			List<CKYUnaryParsingRule<MR>> unaryRules,
			Function<Category<MR>, Category<MR>> categoryTransformation) {
		super(maxNumberOfCellsInSpan, binaryRules, sentenceLexiconGenerators,
				wordSkippingLexicalGenerator, categoryServices,
				pruneLexicalCells, completeParseFilter, unaryRules,
				categoryTransformation);
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
	protected Chart<MR> doParse(final IFilter<MR> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> chart, int numTokens,
			AbstractCellFactory<MR> cellFactory,
			List<ILexiconImmutable<MR>> lexicons) {
		
		final int sentenceLength = chart.getSentenceLength();
		
		// Add lexical entries from all active lexicons
		for (int start = 0; start < numTokens; start++) {
			for (int end = start; end < numTokens; end++) {
				final List<Cell<MR>> newCells = generateLexicalCells(start,
						end, chart, lexicons, model);
				
				// Filter cells, only keep cells that have semantics and pass
				// pruning (if there's a pruning filter)
				if (CollectionUtils.filterInPlace(newCells,
						new IFilter<Cell<MR>>() {
							@Override
							public boolean isValid(Cell<MR> e) {
								return !prune(pruningFilter, e.getCategory(),
										e.getStart(), e.getEnd(),
										sentenceLength, true);
							}
						})) {
					chart.externalPruning(start, end);
				}
				
				for (final Cell<MR> newCell : newCells) {
					chart.add(newCell);
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
	public static class Builder<MR> {
		
		private final List<CKYBinaryParsingRule<MR>>		binaryRules					= new ArrayList<CKYBinaryParsingRule<MR>>();
		
		private final ICategoryServices<MR>					categoryServices;
		
		private Function<Category<MR>, Category<MR>>		categoryTransformation		= new Function<Category<MR>, Category<MR>>() {
																							
																							@Override
																							public Category<MR> apply(
																									Category<MR> input) {
																								return input;
																							}
																						};
		
		private final IFilter<Category<MR>>					completeParseFilter;
		
		/** The maximum number of cells allowed in each span */
		private int											maxNumberOfCellsInSpan		= 50;
		
		private boolean										pruneLexicalCells			= false;
		
		private final List<ISentenceLexiconGenerator<MR>>	sentenceLexicalGenerators	= new LinkedList<ISentenceLexiconGenerator<MR>>();
		
		private final List<CKYUnaryParsingRule<MR>>			unaryRules					= new ArrayList<CKYUnaryParsingRule<MR>>();
		
		private ISentenceLexiconGenerator<MR>				wordSkippingLexicalGenerator;
		
		public Builder(ICategoryServices<MR> categoryServices,
				IFilter<Category<MR>> completeParseFilter) {
			this.categoryServices = categoryServices;
			this.completeParseFilter = completeParseFilter;
			wordSkippingLexicalGenerator = new SimpleWordSkippingLexicalGenerator<MR>(
					categoryServices);
		}
		
		public Builder<MR> addBinaryParseRule(CKYBinaryParsingRule<MR> rule) {
			binaryRules.add(rule);
			return this;
		}
		
		public Builder<MR> addBinaryParseRule(CKYUnaryParsingRule<MR> rule) {
			unaryRules.add(rule);
			return this;
		}
		
		public Builder<MR> addSentenceLexicalGenerator(
				ISentenceLexiconGenerator<MR> generator) {
			sentenceLexicalGenerators.add(generator);
			return this;
		}
		
		public CKYParser<MR> build() {
			return new CKYParser<MR>(maxNumberOfCellsInSpan, binaryRules,
					sentenceLexicalGenerators, wordSkippingLexicalGenerator,
					categoryServices, pruneLexicalCells, completeParseFilter,
					unaryRules, categoryTransformation);
		}
		
		public Builder<MR> setCategoryTransformation(
				Function<Category<MR>, Category<MR>> categoryTransformation) {
			this.categoryTransformation = categoryTransformation;
			return this;
		}
		
		public Builder<MR> setMaxNumberOfCellsInSpan(int maxNumberOfCellsInSpan) {
			this.maxNumberOfCellsInSpan = maxNumberOfCellsInSpan;
			return this;
		}
		
		public Builder<MR> setPruneLexicalCells(boolean pruneLexicalCells) {
			this.pruneLexicalCells = pruneLexicalCells;
			return this;
		}
		
		public Builder<MR> setWordSkippingLexicalGenerator(
				ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator) {
			this.wordSkippingLexicalGenerator = wordSkippingLexicalGenerator;
			return this;
		}
	}
}
