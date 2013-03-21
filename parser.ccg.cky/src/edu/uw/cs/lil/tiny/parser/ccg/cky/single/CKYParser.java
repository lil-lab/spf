/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.Pruner;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.SimpleWordSkippingLexicalGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ISentenceLexiconGenerator;
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
public class CKYParser<Y> extends AbstractCKYParser<Y> {
	private static final ILogger				LOG	= LoggerFactory
															.create(CKYParser.class);
	
	/**
	 * Unary CCG parsing rules.
	 */
	private final List<CKYUnaryParsingRule<Y>>	unaryRules;
	
	private CKYParser(int maxNumberOfCellsInSpan,
			List<CKYBinaryParsingRule<Y>> binaryParseRules,
			List<CKYUnaryParsingRule<Y>> unaryParseRules,
			List<ISentenceLexiconGenerator<Y>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<Y> wordSkippingLexicalGenerator,
			ICategoryServices<Y> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<Y>> completeParseFilter) {
		super(maxNumberOfCellsInSpan, binaryParseRules,
				sentenceLexiconGenerators, wordSkippingLexicalGenerator,
				categoryServices, pruneLexicalCells, completeParseFilter);
		this.unaryRules = unaryParseRules;
	}
	
	/**
	 * Add all the cells to the chart.
	 * 
	 * @param newCells
	 *            list of new cells
	 * @param chart
	 *            Chart to add the cells to
	 * @param pruner
	 *            'true' if to prune
	 */
	private static <Y> void addAllToChart(List<Cell<Y>> newCells,
			Chart<Y> chart, IDataItemModel<Y> model) {
		for (final Cell<Y> newCell : newCells) {
			chart.add(newCell, model);
		}
	}
	
	private List<Cell<Y>> unaryParse(int begin, int end, Chart<Y> currentChart,
			AbstractCellFactory<Y> cellFactory, Pruner<Sentence, Y> pruner) {
		final Iterator<Cell<Y>> cells = currentChart
				.getSpanIterator(begin, end);
		final List<Cell<Y>> newCells = new LinkedList<Cell<Y>>();
		while (cells.hasNext()) {
			final Cell<Y> c = cells.next();
			final Iterator<CKYUnaryParsingRule<Y>> rules = unaryRules
					.iterator();
			while (rules.hasNext()) {
				for (final Cell<Y> newCell : rules.next().newCellsFrom(c,
						cellFactory)) {
					// Filter cells, only keep cells that pass
					// pruning over the semantics, if there's a pruner and
					// they have semantics
					if (pruner == null
							|| newCell.getCategroy().getSem() != null
							|| !pruner.prune(newCell.getCategroy().getSem())) {
						newCells.add(newCell);
					} else {
						LOG.debug("Pruned (hard pruning): %s", newCell);
					}
				}
			}
		}
		return newCells;
	}
	
	@Override
	protected Chart<Y> doParse(Pruner<Sentence, Y> pruner,
			IDataItemModel<Y> model, Chart<Y> chart, int numTokens,
			AbstractCellFactory<Y> cellFactory,
			List<ILexiconImmutable<Y>> lexicons) {
		
		// Add lexical entries from all active lexicons
		for (int i = 0; i < numTokens; i++) {
			for (int j = i; j < numTokens; j++) {
				final List<Cell<Y>> newCells = generateLexicalCells(i, j,
						chart, lexicons);
				
				// Filter cells, only keep cells that have semantics and pass
				// pruning (if there's a pruner)
				CollectionUtils.filterInPlace(newCells, new IFilter<Cell<Y>>() {
					@Override
					public boolean isValid(Cell<Y> e) {
						return e.getCategroy().getSyntax().equals(Syntax.EMPTY)
								|| e.getCategroy().getSem() != null;
					}
				});
				
				for (final Cell<Y> newCell : newCells) {
					chart.add(newCell, model);
				}
			}
		}
		
		// Use unary parse rules
		for (int i = 0; i < numTokens; ++i) {
			addAllToChart(unaryParse(i, i, chart, cellFactory, pruner), chart,
					model);
		}
		
		// now do the CKY parsing:
		for (int len = 1; len < numTokens; len++) {
			for (int begin = 0; begin < numTokens - len; begin++) {
				for (int split = 0; split < len; split++) {
					addAllToChart(
							processSplit(begin, begin + len, split, chart,
									cellFactory, numTokens, pruner), chart,
							model);
				}
				addAllToChart(
						unaryParse(begin, begin + len, chart, cellFactory,
								pruner), chart, model);
			}
		}
		
		return chart;
	}
	
	/**
	 * Builder for {@link CKYParser}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Builder<Y> {
		
		private final List<CKYBinaryParsingRule<Y>>			binaryParseRules			= new LinkedList<CKYBinaryParsingRule<Y>>();
		
		private final ICategoryServices<Y>					categoryServices;
		
		private final IFilter<Category<Y>>					completeParseFilter;
		
		/** The maximum number of cells allowed in each span */
		private int											maxNumberOfCellsInSpan		= 50;
		
		private boolean										pruneLexicalCells			= false;
		
		private final List<ISentenceLexiconGenerator<Y>>	sentenceLexicalGenerators	= new LinkedList<ISentenceLexiconGenerator<Y>>();
		
		private final List<CKYUnaryParsingRule<Y>>			unaryParseRules				= new LinkedList<CKYUnaryParsingRule<Y>>();
		private ISentenceLexiconGenerator<Y>				wordSkippingLexicalGenerator;
		
		public Builder(ICategoryServices<Y> categoryServices,
				IFilter<Category<Y>> completeParseFilter) {
			this.categoryServices = categoryServices;
			this.completeParseFilter = completeParseFilter;
			wordSkippingLexicalGenerator = new SimpleWordSkippingLexicalGenerator<Y>(
					categoryServices);
		}
		
		public Builder<Y> addBinaryParseRule(CKYBinaryParsingRule<Y> rule) {
			binaryParseRules.add(rule);
			return this;
		}
		
		public Builder<Y> addSentenceLexicalGenerator(
				ISentenceLexiconGenerator<Y> generator) {
			sentenceLexicalGenerators.add(generator);
			return this;
		}
		
		public Builder<Y> addUnaryParseRule(CKYUnaryParsingRule<Y> rule) {
			unaryParseRules.add(rule);
			return this;
		}
		
		public CKYParser<Y> build() {
			return new CKYParser<Y>(maxNumberOfCellsInSpan, binaryParseRules,
					unaryParseRules, sentenceLexicalGenerators,
					wordSkippingLexicalGenerator, categoryServices,
					pruneLexicalCells, completeParseFilter);
		}
		
		public Builder<Y> setMaxNumberOfCellsInSpan(int maxNumberOfCellsInSpan) {
			this.maxNumberOfCellsInSpan = maxNumberOfCellsInSpan;
			return this;
		}
		
		public Builder<Y> setPruneLexicalCells(boolean pruneLexicalCells) {
			this.pruneLexicalCells = pruneLexicalCells;
			return this;
		}
		
		public Builder<Y> setWordSkippingLexicalGenerator(
				ISentenceLexiconGenerator<Y> wordSkippingLexicalGenerator) {
			this.wordSkippingLexicalGenerator = wordSkippingLexicalGenerator;
			return this;
		}
	}
}
