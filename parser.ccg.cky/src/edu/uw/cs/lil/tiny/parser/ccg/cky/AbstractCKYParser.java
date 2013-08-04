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
package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYLexicalStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.graph.AbstractGraphParser;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.DirectAccessBoundedPriorityQueue;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public abstract class AbstractCKYParser<MR> extends
		AbstractGraphParser<Sentence, MR> {
	private static final ILogger						LOG	= LoggerFactory
																	.create(AbstractCKYParser.class);
	
	/**
	 * The maximum number of cells to hold for each span.
	 */
	private final int									beamSize;
	
	/**
	 * Binary CCG parsing rules.
	 */
	private final List<CKYBinaryParsingRule<MR>>		binaryRules;
	
	private final IFilter<Category<MR>>					completeParseFilter;
	
	/**
	 * List of lexical generators that use the sentence itself to generate
	 * lexical entries.
	 */
	private final List<ISentenceLexiconGenerator<MR>>	sentenceLexiconGenerators;
	
	/**
	 * Lexical generator to create lexical entries that enable word-skipping.
	 */
	private final ISentenceLexiconGenerator<MR>			wordSkippingLexicalGenerator;
	
	protected final ICategoryServices<MR>				categoryServices;
	
	protected final boolean								pruneLexicalCells;
	
	public AbstractCKYParser(int beamSize,
			List<CKYBinaryParsingRule<MR>> binaryParseRules,
			List<ISentenceLexiconGenerator<MR>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<MR>> completeParseFilter) {
		this.beamSize = beamSize;
		this.binaryRules = binaryParseRules;
		this.sentenceLexiconGenerators = sentenceLexiconGenerators;
		this.wordSkippingLexicalGenerator = wordSkippingLexicalGenerator;
		this.categoryServices = categoryServices;
		this.pruneLexicalCells = pruneLexicalCells;
		this.completeParseFilter = completeParseFilter;
	}
	
	/**
	 * Checks if the current category in the given span can be considered a
	 * complete parse.
	 * 
	 * @param start
	 * @param end
	 * @param category
	 * @param sentenceLength
	 * @return
	 */
	public boolean isFullParse(int start, int end, Category<MR> category,
			int sentenceLength) {
		return start == 0 && end == sentenceLength - 1
				&& completeParseFilter.isValid(category);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<MR> model) {
		return parse(dataItem, model, false);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon, Integer altBeamSize) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon,
				altBeamSize, null);
	}
	
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon, Integer altBeamSize,
			AbstractCellFactory<MR> cellFactory) {
		return parse(dataItem, null, model, allowWordSkipping, tempLexicon,
				altBeamSize, cellFactory);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model) {
		return parse(dataItem, pruningFilter, model, false);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexicon<MR> tempLexicon) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping,
				tempLexicon, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexicon<MR> tempLexicon,
			Integer altBeamSize) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping,
				tempLexicon, altBeamSize, null);
	}
	
	public CKYParserOutput<MR> parse(IDataItem<Sentence> dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexicon<MR> tempLexicon,
			Integer altBeamSize, AbstractCellFactory<MR> scoreSensitiveFactory) {
		// Store starting time
		final long start = System.currentTimeMillis();
		
		final List<String> tokens = dataItem.getSample().getTokens();
		
		// Factory to create cells
		final AbstractCellFactory<MR> cellFactory;
		if (scoreSensitiveFactory == null) {
			// Case we use model scoring for pruning
			cellFactory = new CellFactory<MR>(dataItem.getSample().getTokens()
					.size());
		} else {
			// Case we use an external scoring function for pruning
			cellFactory = scoreSensitiveFactory;
		}
		
		// Create a chart and add the input words
		final Chart<MR> chart = new Chart<MR>(tokens,
				altBeamSize == null ? beamSize : altBeamSize, cellFactory,
				!pruneLexicalCells);
		
		// Create the list of active lexicons
		final List<ILexiconImmutable<MR>> lexicons = new ArrayList<ILexiconImmutable<MR>>();
		
		// Lexicon for work skipping
		if (allowWordSkipping) {
			lexicons.add(new Lexicon<MR>(
					wordSkippingLexicalGenerator.generateLexicon(
							dataItem.getSample(), dataItem.getSample())));
		}
		
		// Lexicon with hueristically generated lexical entries. The entries are
		// generated given the string of the sentence.
		for (final ISentenceLexiconGenerator<MR> generator : sentenceLexiconGenerators) {
			lexicons.add(new Lexicon<MR>(generator.generateLexicon(
					dataItem.getSample(), dataItem.getSample())));
		}
		
		// The model lexicon
		lexicons.add(model.getLexicon());
		
		// If there's a temporary lexicon, add it too
		if (tempLexicon != null) {
			lexicons.add(tempLexicon);
		}
		
		return new CKYParserOutput<MR>(doParse(pruningFilter, model, chart,
				tokens.size(), cellFactory, lexicons),
				System.currentTimeMillis() - start);
	}
	
	private boolean isCompleteSpan(int begin, int end, int sentenceSize) {
		return begin == 0 && end == sentenceSize - 1;
	}
	
	protected abstract Chart<MR> doParse(IFilter<MR> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> currentChart, int numTokens,
			AbstractCellFactory<MR> cellFactory,
			List<ILexiconImmutable<MR>> lexicons);
	
	/**
	 * Adds all of the cells to the chart that can be created by lexical
	 * insertion in the given span. The work to find valid lexical entries for
	 * each split is done by getLexEntries.
	 */
	protected List<Cell<MR>> generateLexicalCells(int begin, int end,
			Chart<MR> chart, List<ILexiconImmutable<MR>> lexicons,
			IDataItemModel<MR> model) {
		final AbstractCellFactory<MR> cellFactory = chart.getCellFactory();
		final List<String> subString = CollectionUtils.subList(
				chart.getTokens(), begin, end + 1);
		final List<Cell<MR>> cells = new LinkedList<Cell<MR>>();
		// Iterate over all lexicons and get lexical entries
		for (final ILexiconImmutable<MR> lexicon : lexicons) {
			final List<? extends LexicalEntry<MR>> matchingEntries = lexicon
					.getLexEntries(subString);
			// For each item containing the current word sequence, create a
			// cell and add it the chart
			for (final LexicalEntry<MR> entry : matchingEntries) {
				cells.add(cellFactory.create(
						new CKYLexicalStep<MR>(entry,
								isFullParse(begin, end, entry.getCategory(),
										chart.getSentenceLength()), model),
						begin, end));
			}
		}
		return cells;
	}
	
	/**
	 * Processing a (single) split of a (single) span.
	 */
	protected List<Cell<MR>> processSplit(int start, int end, int split,
			Chart<MR> chart, AbstractCellFactory<MR> cellFactory,
			int numTokens, IFilter<MR> pruningFilter, IDataItemModel<MR> model) {
		// Processing a (single) split of a (single) span
		
		LOG.debug("Processing split (%d, %d)[%d] with %d x %d cells", start,
				end, split, chart.spanSize(start, start + split),
				chart.spanSize(start + split + 1, end));
		
		final List<Cell<MR>> newCells = new LinkedList<Cell<MR>>();
		int counter = 0;
		final Iterator<Cell<MR>> leftIter = chart.getSpanIterator(start, start
				+ split);
		while (leftIter.hasNext()) {
			final Cell<MR> left = leftIter.next();
			final Iterator<Cell<MR>> rightIter = chart.getSpanIterator(start
					+ split + 1, end);
			while (rightIter.hasNext()) {
				final Cell<MR> right = rightIter.next();
				final Iterator<CKYBinaryParsingRule<MR>> rules = binaryRules
						.iterator();
				while (rules.hasNext()) {
					for (final ParseRuleResult<MR> prr : rules.next().apply(
							left,
							right,
							isCompleteSpan(left.getStart(), right.getEnd(),
									numTokens))) {
						counter += 1;
						// Filter cells, only keep cells that pass
						// pruning over the semantics, if there's a pruning
						// filter and
						// they have semantics
						if (prune(pruningFilter, prr.getResultCategory())) {
							LOG.debug("Pruned (hard pruning): [%d,%d] %s",
									start, end, prr);
						} else {
							// Create the parse step
							final CKYParseStep<MR> parseStep = new CKYParseStep<MR>(
									prr.getResultCategory(), left, right,
									isFullParse(start, end,
											prr.getResultCategory(),
											chart.getSentenceLength()),
									prr.getRuleName(), model);
							
							// Create the chart cell
							final Cell<MR> newCell = cellFactory.create(
									parseStep, start, end);
							
							newCells.add(newCell);
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished processing split %s[%d], generated %d cells, returning %d cells",
				start, end, split, counter, newCells.size());
		
		return newCells;
	}
	
	/**
	 * Processing a (single) split of a (single) span. Does pre-chart pruning
	 * using the size of the beam. Using this method creates a further
	 * approximation of the packed chart, which influences non-maximal children
	 * of cells.
	 */
	protected List<Cell<MR>> processSplitAndPrune(int start, int end,
			int split, Chart<MR> chart, AbstractCellFactory<MR> cellFactory,
			int numTokens, IFilter<MR> pruningFilter, int chartBeamSize,
			IDataItemModel<MR> model) {
		// Processing a (single) split of a (single) span
		
		LOG.debug("Processing split (%d, %d)[%d] with %d x %d cells", start,
				end, split, chart.spanSize(start, start + split),
				chart.spanSize(start + split + 1, end));
		
		final DirectAccessBoundedPriorityQueue<Cell<MR>> queue = new DirectAccessBoundedPriorityQueue<Cell<MR>>(
				chartBeamSize * 2 + 1, new Cell.ScoreComparator<MR>());
		
		int counter = 0;
		final Iterator<Cell<MR>> leftIter = chart.getSpanIterator(start, start
				+ split);
		while (leftIter.hasNext()) {
			final Cell<MR> left = leftIter.next();
			final Iterator<Cell<MR>> rightIter = chart.getSpanIterator(start
					+ split + 1, end);
			while (rightIter.hasNext()) {
				final Cell<MR> right = rightIter.next();
				final Iterator<CKYBinaryParsingRule<MR>> rules = binaryRules
						.iterator();
				while (rules.hasNext()) {
					for (final ParseRuleResult<MR> prr : rules.next().apply(
							left,
							right,
							isCompleteSpan(left.getStart(), right.getEnd(),
									numTokens))) {
						counter += 1;
						// Prune, only keep categories that pass
						// pruning over the semantics, if there's a pruning
						// filter and
						// they have semantics
						if (prune(pruningFilter, prr.getResultCategory())) {
							LOG.debug("Pruned (hard pruning): [%d,%d] %s",
									start, end, prr);
						} else {
							// Create a CKY parse step from the result
							final CKYParseStep<MR> parseStep = new CKYParseStep<MR>(
									prr.getResultCategory(), left, right,
									isFullParse(start, end,
											prr.getResultCategory(),
											chart.getSentenceLength()),
									prr.getRuleName(), model);
							
							// Create the cell
							final Cell<MR> newCell = cellFactory.create(
									parseStep, start, end);
							
							if (queue.contains(newCell)) {
								// Case the cell signature is already
								// contained in the queue. Remove the old
								// cell, add the new one to it, which might
								// change its score, and then re-add to the
								// queue.
								
								final Cell<MR> oldCell = queue.get(newCell);
								// Add the new cell to the old one
								if (oldCell.addCell(newCell)) {
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
				start, end, split, counter, queue.size());
		
		return new ArrayList<Cell<MR>>(queue);
	}
	
	/**
	 * Hard pruning (not based on score and beam). Prunes if the semantics are
	 * null and this is not an EMPTY entry. or if there's a pruning filter and
	 * it returns 'false'.
	 * 
	 * @param pruningFilter
	 *            may be null
	 * @return
	 */
	protected boolean prune(IFilter<MR> pruningFilter, Category<MR> category) {
		return (category.getSem() == null && !category.getSyntax().equals(
				Syntax.EMPTY))
				|| (pruningFilter != null && category.getSem() != null && !pruningFilter
						.isValid(category.getSem()));
	}
}
