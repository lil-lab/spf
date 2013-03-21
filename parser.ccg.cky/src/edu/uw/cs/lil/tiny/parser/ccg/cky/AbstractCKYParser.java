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
package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.AbstractParser;
import edu.uw.cs.lil.tiny.parser.Pruner;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.utils.collections.DirectAccessBoundedPriorityQueue;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public abstract class AbstractCKYParser<Y> extends AbstractParser<Sentence, Y> {
	private static final ILogger						LOG	= LoggerFactory
																	.create(AbstractCKYParser.class);
	
	/**
	 * The maximum number of cells to hold for each span.
	 */
	private final int									beamSize;
	
	/**
	 * Binary CCG parsing rules.
	 */
	private final List<CKYBinaryParsingRule<Y>>			binaryRules;
	
	private final IFilter<Category<Y>>					completeParseFilter;
	
	/**
	 * List of lexical generators that use the sentence itself to generate
	 * lexical entries.
	 */
	private final List<ISentenceLexiconGenerator<Y>>	sentenceLexiconGenerators;
	
	/**
	 * Lexical generator to create lexical entries that enable word-skipping.
	 */
	private final ISentenceLexiconGenerator<Y>			wordSkippingLexicalGenerator;
	
	protected final ICategoryServices<Y>				categoryServices;
	
	protected final boolean								pruneLexicalCells;
	
	public AbstractCKYParser(int beamSize,
			List<CKYBinaryParsingRule<Y>> binaryParseRules,
			List<ISentenceLexiconGenerator<Y>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<Y> wordSkippingLexicalGenerator,
			ICategoryServices<Y> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<Y>> completeParseFilter) {
		this.beamSize = beamSize;
		this.binaryRules = binaryParseRules;
		this.sentenceLexiconGenerators = sentenceLexiconGenerators;
		this.wordSkippingLexicalGenerator = wordSkippingLexicalGenerator;
		this.categoryServices = categoryServices;
		this.pruneLexicalCells = pruneLexicalCells;
		this.completeParseFilter = completeParseFilter;
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<Y> model) {
		return parse(dataItem, model, false);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping,
			ILexicon<Y> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, null);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping,
			ILexicon<Y> tempLexicon, Integer altBeamSize) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon,
				altBeamSize, null);
	}
	
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<Y> model, boolean allowWordSkipping,
			ILexicon<Y> tempLexicon, Integer altBeamSize,
			AbstractCellFactory<Y> cellFactory) {
		return parse(dataItem, null, model, allowWordSkipping, tempLexicon,
				altBeamSize, cellFactory);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			Pruner<Sentence, Y> pruner, IDataItemModel<Y> model) {
		return parse(dataItem, pruner, model, false);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			Pruner<Sentence, Y> pruner, IDataItemModel<Y> model,
			boolean allowWordSkipping) {
		return parse(dataItem, pruner, model, allowWordSkipping, null);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			Pruner<Sentence, Y> pruner, IDataItemModel<Y> model,
			boolean allowWordSkipping, ILexicon<Y> tempLexicon) {
		return parse(dataItem, pruner, model, allowWordSkipping, tempLexicon,
				null);
	}
	
	@Override
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			Pruner<Sentence, Y> pruner, IDataItemModel<Y> model,
			boolean allowWordSkipping, ILexicon<Y> tempLexicon,
			Integer altBeamSize) {
		return parse(dataItem, pruner, model, allowWordSkipping, tempLexicon,
				altBeamSize, null);
	}
	
	public CKYParserOutput<Y> parse(IDataItem<Sentence> dataItem,
			Pruner<Sentence, Y> pruner, IDataItemModel<Y> model,
			boolean allowWordSkipping, ILexicon<Y> tempLexicon,
			Integer altBeamSize, AbstractCellFactory<Y> scoreSensitiveFactory) {
		// Store starting time
		final long start = System.currentTimeMillis();
		
		final List<String> tokens = dataItem.getSample().getTokens();
		
		// Factory to create cells
		final AbstractCellFactory<Y> cellFactory;
		if (scoreSensitiveFactory == null) {
			// Case we use model scoring for pruning
			cellFactory = new CellFactory<Y>(model, dataItem.getSample()
					.getTokens().size(), completeParseFilter);
		} else {
			// Case we use an external scoring function for pruning
			cellFactory = scoreSensitiveFactory;
		}
		
		// Create a chart and add the input words
		final Chart<Y> chart = new Chart<Y>(tokens,
				altBeamSize == null ? beamSize : altBeamSize, cellFactory,
				!pruneLexicalCells);
		
		// Create the list of active lexicons
		final List<ILexiconImmutable<Y>> lexicons = new ArrayList<ILexiconImmutable<Y>>();
		
		// Lexicon for work skipping
		if (allowWordSkipping) {
			lexicons.add(new Lexicon<Y>(
					wordSkippingLexicalGenerator.generateLexicon(
							dataItem.getSample(), dataItem.getSample())));
		}
		
		// Lexicon with hueristically generated lexical entries. The entries are
		// generated given the string of the sentence.
		for (final ISentenceLexiconGenerator<Y> generator : sentenceLexiconGenerators) {
			lexicons.add(new Lexicon<Y>(generator.generateLexicon(
					dataItem.getSample(), dataItem.getSample())));
		}
		
		// The model lexicon
		lexicons.add(model.getLexicon());
		
		// If there's a temporary lexicon, add it too
		if (tempLexicon != null) {
			lexicons.add(tempLexicon);
		}
		
		return new CKYParserOutput<Y>(doParse(pruner, model, chart,
				tokens.size(), cellFactory, lexicons), model,
				System.currentTimeMillis() - start);
	}
	
	private boolean isCompleteSpan(int begin, int end, int sentenceSize) {
		return begin == 0 && end == sentenceSize - 1;
	}
	
	protected abstract Chart<Y> doParse(Pruner<Sentence, Y> pruner,
			IDataItemModel<Y> model, Chart<Y> currentChart, int numTokens,
			AbstractCellFactory<Y> cellFactory,
			List<ILexiconImmutable<Y>> lexicons);
	
	/**
	 * Adds all of the cells to the chart that can be created by lexical
	 * insertion in the given span. The work to find valid lexical entries for
	 * each split is done by getLexEntries.
	 */
	protected List<Cell<Y>> generateLexicalCells(int begin, int end,
			Chart<Y> chart, List<ILexiconImmutable<Y>> lexicons) {
		final AbstractCellFactory<Y> cellFactory = chart.getCellFactory();
		final List<String> subString = chart.getTokens()
				.subList(begin, end + 1);
		final List<Cell<Y>> cells = new LinkedList<Cell<Y>>();
		// Iterate over all lexicons and get lexical entries
		for (final ILexiconImmutable<Y> lexicon : lexicons) {
			final List<? extends LexicalEntry<Y>> matchingEntries = lexicon
					.getLexEntries(subString);
			// For each item containing the current word sequence, create a
			// cell and add it the chart
			for (final LexicalEntry<Y> entry : matchingEntries) {
				cells.add(cellFactory.create(entry, begin, end));
			}
		}
		return cells;
	}
	
	/**
	 * Processing a (single) split of a (single) span.
	 */
	protected List<Cell<Y>> processSplit(int begin, int end, int split,
			Chart<Y> chart, AbstractCellFactory<Y> cellFactory, int numTokens,
			Pruner<Sentence, Y> pruner) {
		// Processing a (single) split of a (single) span
		
		LOG.debug("Processing split (%d, %d)[%d] with %d x %d cells", begin,
				end, split, chart.spanSize(begin, begin + split),
				chart.spanSize(begin + split + 1, end));
		
		final List<Cell<Y>> newCells = new LinkedList<Cell<Y>>();
		int counter = 0;
		final Iterator<Cell<Y>> leftIter = chart.getSpanIterator(begin, begin
				+ split);
		while (leftIter.hasNext()) {
			final Cell<Y> left = leftIter.next();
			final Iterator<Cell<Y>> rightIter = chart.getSpanIterator(begin
					+ split + 1, end);
			while (rightIter.hasNext()) {
				final Cell<Y> right = rightIter.next();
				final Iterator<CKYBinaryParsingRule<Y>> rules = binaryRules
						.iterator();
				while (rules.hasNext()) {
					for (final Cell<Y> newCell : rules.next().newCellsFrom(
							left,
							right,
							cellFactory,
							isCompleteSpan(left.getStart(), right.getEnd(),
									numTokens))) {
						counter += 1;
						// Filter cells, only keep cells that pass
						// pruning over the semantics, if there's a pruner and
						// they have semantics
						if (prune(pruner, newCell.getCategroy())) {
							LOG.debug("Pruned (hard pruning): %s", newCell);
						} else {
							newCells.add(newCell);
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished processing split %s[%d], generated %d cells, returning %d cells",
				begin, end, split, counter, newCells.size());
		
		return newCells;
	}
	
	/**
	 * Processing a (single) split of a (single) span. Does pre-chart pruning
	 * using the size of the beam. Using this method creates a further
	 * approximation of the packed chart, which influences non-maximal children
	 * of cells.
	 */
	protected List<Cell<Y>> processSplitAndPrune(int begin, int end, int split,
			Chart<Y> chart, AbstractCellFactory<Y> cellFactory, int numTokens,
			Pruner<Sentence, Y> pruner, int chartBeamSize,
			IDataItemModel<Y> model) {
		// Processing a (single) split of a (single) span
		
		LOG.debug("Processing split (%d, %d)[%d] with %d x %d cells", begin,
				end, split, chart.spanSize(begin, begin + split),
				chart.spanSize(begin + split + 1, end));
		
		final DirectAccessBoundedPriorityQueue<Cell<Y>> queue = new DirectAccessBoundedPriorityQueue<Cell<Y>>(
				chartBeamSize * 2 + 1, new Cell.ScoreComparator<Y>());
		
		int counter = 0;
		final Iterator<Cell<Y>> leftIter = chart.getSpanIterator(begin, begin
				+ split);
		while (leftIter.hasNext()) {
			final Cell<Y> left = leftIter.next();
			final Iterator<Cell<Y>> rightIter = chart.getSpanIterator(begin
					+ split + 1, end);
			while (rightIter.hasNext()) {
				final Cell<Y> right = rightIter.next();
				final Iterator<CKYBinaryParsingRule<Y>> rules = binaryRules
						.iterator();
				while (rules.hasNext()) {
					for (final Cell<Y> newCell : rules.next().newCellsFrom(
							left,
							right,
							cellFactory,
							isCompleteSpan(left.getStart(), right.getEnd(),
									numTokens))) {
						counter += 1;
						// Filter cells, only keep cells that pass
						// pruning over the semantics, if there's a pruner and
						// they have semantics
						if (prune(pruner, newCell.getCategroy())) {
							LOG.debug("Pruned (hard pruning): %s", newCell);
						} else {
							if (queue.contains(newCell)) {
								// Case the cell signature is already
								// contained in the queue. Remove the old
								// cell, add the new one to it, which might
								// change its score, and then re-add to the
								// queue.
								
								final Cell<Y> oldCell = queue.get(newCell);
								// Add the new cell to the old one
								if (oldCell.addCell(newCell, model)) {
									// Max-children changed, score might have
									// changed, so need to remove and re-queue
									
									// Remove the old cell, to re-add it
									queue.remove(oldCell);
									// Adding here, not offering, since we just
									// removed it, it should be added without
									// any fear of exception
									queue.add(oldCell);
								}
							} else {
								// Case new cell signature
								if (!queue.offer(newCell)) {
									LOG.debug("Pruned (pre-chart pruning): %s",
											newCell);
								}
							}
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished processing split %s[%d], generated %d cells, returning %d cells",
				begin, end, split, counter, queue.size());
		
		return new ArrayList<Cell<Y>>(queue);
	}
	
	/**
	 * Hard pruning (not based on score and beam). Prunes if the semantics are
	 * null and this is not an EMPTY entry. or if there's a pruner and it
	 * returns 'true'.
	 * 
	 * @param pruner
	 *            may be null
	 * @return
	 */
	protected boolean prune(Pruner<Sentence, Y> pruner, Category<Y> category) {
		return (category.getSem() == null && !category.getSyntax().equals(
				Syntax.EMPTY))
				|| (pruner != null && category.getSem() != null && pruner
						.prune(category.getSem()));
	}
}
