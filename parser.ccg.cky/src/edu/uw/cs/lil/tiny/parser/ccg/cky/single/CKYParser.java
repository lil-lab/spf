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

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.SimpleWordSkippingLexicalGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.utils.collections.CollectionUtils;
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
			List<CKYBinaryParsingRule<MR>> binaryParseRules,
			List<ISentenceLexiconGenerator<MR>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<MR>> completeParseFilter) {
		super(maxNumberOfCellsInSpan, binaryParseRules,
				sentenceLexiconGenerators, wordSkippingLexicalGenerator,
				categoryServices, pruneLexicalCells, completeParseFilter);
	}
	
	/**
	 * Add all the cells to the chart.
	 * 
	 * @param newCells
	 *            list of new cells
	 * @param chart
	 *            Chart to add the cells to
	 */
	private static <MR> void addAllToChart(List<Cell<MR>> newCells,
			Chart<MR> chart, IDataItemModel<MR> model) {
		for (final Cell<MR> newCell : newCells) {
			chart.add(newCell, model);
		}
	}
	
	@Override
	protected Chart<MR> doParse(IFilter<MR> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> chart, int numTokens,
			AbstractCellFactory<MR> cellFactory,
			List<ILexiconImmutable<MR>> lexicons) {
		
		// Add lexical entries from all active lexicons
		for (int i = 0; i < numTokens; i++) {
			for (int j = i; j < numTokens; j++) {
				final List<Cell<MR>> newCells = generateLexicalCells(i, j,
						chart, lexicons, model);
				
				// Filter cells, only keep cells that have semantics and pass
				// pruning (if there's a pruning filter)
				CollectionUtils.filterInPlace(newCells,
						new IFilter<Cell<MR>>() {
							@Override
							public boolean isValid(Cell<MR> e) {
								return e.getCategory().getSyntax()
										.equals(Syntax.EMPTY)
										|| e.getCategory().getSem() != null;
							}
						});
				
				for (final Cell<MR> newCell : newCells) {
					chart.add(newCell, model);
				}
			}
		}
		
		// now do the CKY parsing:
		for (int len = 1; len < numTokens; len++) {
			for (int begin = 0; begin < numTokens - len; begin++) {
				for (int split = 0; split < len; split++) {
					addAllToChart(
							processSplit(begin, begin + len, split, chart,
									cellFactory, numTokens, pruningFilter,
									model), chart, model);
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
		
		private final List<CKYBinaryParsingRule<MR>>		binaryParseRules			= new LinkedList<CKYBinaryParsingRule<MR>>();
		
		private final ICategoryServices<MR>					categoryServices;
		
		private final IFilter<Category<MR>>					completeParseFilter;
		
		/** The maximum number of cells allowed in each span */
		private int											maxNumberOfCellsInSpan		= 50;
		
		private boolean										pruneLexicalCells			= false;
		
		private final List<ISentenceLexiconGenerator<MR>>	sentenceLexicalGenerators	= new LinkedList<ISentenceLexiconGenerator<MR>>();
		
		private ISentenceLexiconGenerator<MR>				wordSkippingLexicalGenerator;
		
		public Builder(ICategoryServices<MR> categoryServices,
				IFilter<Category<MR>> completeParseFilter) {
			this.categoryServices = categoryServices;
			this.completeParseFilter = completeParseFilter;
			wordSkippingLexicalGenerator = new SimpleWordSkippingLexicalGenerator<MR>(
					categoryServices);
		}
		
		public Builder<MR> addBinaryParseRule(CKYBinaryParsingRule<MR> rule) {
			binaryParseRules.add(rule);
			return this;
		}
		
		public Builder<MR> addSentenceLexicalGenerator(
				ISentenceLexiconGenerator<MR> generator) {
			sentenceLexicalGenerators.add(generator);
			return this;
		}
		
		public CKYParser<MR> build() {
			return new CKYParser<MR>(maxNumberOfCellsInSpan, binaryParseRules,
					sentenceLexicalGenerators, wordSkippingLexicalGenerator,
					categoryServices, pruneLexicalCells, completeParseFilter);
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
