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

import com.google.common.base.Function;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCKYParseStep;
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
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public abstract class AbstractCKYParser<MR> extends
		AbstractGraphParser<Sentence, MR> {
	public static final ILogger							LOG	= LoggerFactory
																	.create(AbstractCKYParser.class);
	
	/**
	 * The maximum number of cells to hold for each span.
	 */
	private final int									beamSize;
	
	/**
	 * Binary CCG parsing rules.
	 */
	private final List<CKYBinaryParsingRule<MR>>		binaryRules;
	
	/**
	 * A transformation applied to each category before it's added to the chart.
	 */
	private final Function<Category<MR>, Category<MR>>	categoryTransformation;
	
	private final IFilter<Category<MR>>					completeParseFilter;
	
	/**
	 * List of lexical generators that use the sentence itself to generate
	 * lexical entries.
	 */
	private final List<ISentenceLexiconGenerator<MR>>	sentenceLexiconGenerators;
	
	private final List<CKYUnaryParsingRule<MR>>			unaryRules;
	
	/**
	 * Lexical generator to create lexical entries that enable word-skipping.
	 */
	private final ISentenceLexiconGenerator<MR>			wordSkippingLexicalGenerator;
	
	protected final ICategoryServices<MR>				categoryServices;
	
	protected final boolean								pruneLexicalCells;
	
	protected AbstractCKYParser(int beamSize,
			List<CKYBinaryParsingRule<MR>> binaryRules,
			List<ISentenceLexiconGenerator<MR>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<MR>> completeParseFilter,
			List<CKYUnaryParsingRule<MR>> unaryRules,
			Function<Category<MR>, Category<MR>> categoryTransformation) {
		this.beamSize = beamSize;
		this.binaryRules = binaryRules;
		this.sentenceLexiconGenerators = sentenceLexiconGenerators;
		this.wordSkippingLexicalGenerator = wordSkippingLexicalGenerator;
		this.categoryServices = categoryServices;
		this.pruneLexicalCells = pruneLexicalCells;
		this.completeParseFilter = completeParseFilter;
		this.unaryRules = unaryRules;
		this.categoryTransformation = categoryTransformation;
		LOG.info("Init :: %s: binary rules=%s",
				AbstractCKYParser.class.getSimpleName(), binaryRules);
		LOG.info("Init :: %s: unary rules=%s",
				AbstractCKYParser.class.getSimpleName(), unaryRules);
		
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem, IDataItemModel<MR> model) {
		return parse(dataItem, model, false);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon, Integer altBeamSize) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon,
				altBeamSize, null);
	}
	
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IDataItemModel<MR> model, boolean allowWordSkipping,
			ILexicon<MR> tempLexicon, Integer altBeamSize,
			AbstractCellFactory<MR> cellFactory) {
		return parse(dataItem, null, model, allowWordSkipping, tempLexicon,
				altBeamSize, cellFactory);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model) {
		return parse(dataItem, pruningFilter, model, false);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexicon<MR> tempLexicon) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping,
				tempLexicon, null);
	}
	
	@Override
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexicon<MR> tempLexicon,
			Integer altBeamSize) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping,
				tempLexicon, altBeamSize, null);
	}
	
	public CKYParserOutput<MR> parse(Sentence dataItem,
			IFilter<MR> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexicon<MR> tempLexicon,
			Integer altBeamSize, AbstractCellFactory<MR> scoreSensitiveFactory) {
		// Store starting time
		final long start = System.currentTimeMillis();
		
		final List<String> tokens = dataItem.getTokens();
		
		// Factory to create cells
		final AbstractCellFactory<MR> cellFactory;
		if (scoreSensitiveFactory == null) {
			// Case we use model scoring for pruning
			cellFactory = new CellFactory<MR>(dataItem.getTokens().size());
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
			lexicons.add(new Lexicon<MR>(wordSkippingLexicalGenerator
					.generateLexicon(dataItem, dataItem)));
		}
		
		// Lexicon with heuristically generated lexical entries. The entries are
		// generated given the string of the sentence.
		for (final ISentenceLexiconGenerator<MR> generator : sentenceLexiconGenerators) {
			lexicons.add(new Lexicon<MR>(generator.generateLexicon(dataItem,
					dataItem)));
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
	
	private ParseRuleResult<MR> transformRuleResult(ParseRuleResult<MR> prr) {
		if (categoryTransformation == null) {
			return prr;
		} else {
			final Category<MR> transformed = categoryTransformation.apply(prr
					.getResultCategory());
			if (transformed == prr.getResultCategory()) {
				return prr;
			} else {
				return new ParseRuleResult<MR>(prr.getRuleName(), transformed);
			}
		}
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
						new CKYLexicalStep<MR>(
								categoryTransformation == null ? entry
										.getCategory() : categoryTransformation
										.apply(entry.getCategory()), entry,
								isFullParse(begin, end, entry.getCategory(),
										chart.getSentenceLength()), model),
						begin, end));
			}
		}
		return cells;
	}
	
	/**
	 * Checks if the given span is over the entire sentence.
	 */
	protected boolean isCompleteSpan(int start, int end, int sentenceLength) {
		return start == 0 && end == sentenceLength - 1;
	}
	
	/**
	 * Checks if the current category in the given span can be considered a
	 * complete parse.
	 */
	protected boolean isFullParse(int start, int end, Category<MR> category,
			int sentenceLength) {
		return isCompleteSpan(start, end, sentenceLength)
				&& completeParseFilter.isValid(category);
	}
	
	/**
	 * Processing a (single) split of a (single) span.
	 * 
	 * @return Pair of cells to add to the chart and a pruning flag (to indicate
	 *         pruning external to the chart).
	 */
	protected Pair<List<Cell<MR>>, Boolean> processSplit(int start, int end,
			int split, int sentenceLength, Chart<MR> chart,
			AbstractCellFactory<MR> cellFactory, IFilter<MR> pruningFilter,
			IDataItemModel<MR> model) {
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
				LOG.debug("Processing: left=%d , right=%d", left.hashCode(),
						right.hashCode());
				final Iterator<CKYBinaryParsingRule<MR>> rules = binaryRules
						.iterator();
				while (rules.hasNext()) {
					for (final ParseRuleResult<MR> prr : rules.next().apply(
							left, right)) {
						final ParseRuleResult<MR> transformed = transformRuleResult(prr);
						LOG.debug("Applied %s --> %s",
								transformed.getRuleName(),
								transformed.getResultCategory());
						counter += 1;
						// Filter cells, only keep cells that pass
						// pruning over the semantics, if there's a pruning
						// filter and
						// they have semantics
						if (prune(pruningFilter,
								transformed.getResultCategory(), start, end,
								sentenceLength, true)) {
							LOG.debug("Pruned (hard pruning): [%d,%d] %s",
									start, end, transformed);
						} else {
							// Create the parse step
							final CKYParseStep<MR> parseStep = new CKYParseStep<MR>(
									transformed.getResultCategory(), left,
									right, isFullParse(start, end,
											transformed.getResultCategory(),
											chart.getSentenceLength()),
									transformed.getRuleName(), model);
							
							// Create the chart cell
							final Cell<MR> newCell = cellFactory.create(
									parseStep, start, end);
							LOG.debug("Created new cell: %s", newCell);
							
							newCells.add(newCell);
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished processing split (%d, %d)[%d], generated %d cells, returning %d cells",
				start, end, split, counter, newCells.size());
		
		return Pair.of(newCells, false);
	}
	
	/**
	 * Processing a (single) split of a (single) span. Does pre-chart pruning
	 * using the size of the beam. Using this method creates a further
	 * approximation of the packed chart, which influences non-maximal children
	 * of cells.
	 * 
	 * @return Pair of cells to add to the chart and a pruning flag (to indicate
	 *         pruning external to the chart).
	 */
	protected Pair<List<Cell<MR>>, Boolean> processSplitAndPrune(int start,
			int end, int split, int sentenceLength, Chart<MR> chart,
			AbstractCellFactory<MR> cellFactory, IFilter<MR> pruningFilter,
			int chartBeamSize, IDataItemModel<MR> model) {
		// Processing a (single) split of a (single) span.
		
		LOG.debug("Processing split (%d, %d)[%d] with %d x %d cells", start,
				end, split, chart.spanSize(start, start + split),
				chart.spanSize(start + split + 1, end));
		
		final DirectAccessBoundedPriorityQueue<Cell<MR>> queue = new DirectAccessBoundedPriorityQueue<Cell<MR>>(
				chartBeamSize * 2 + 1, new Cell.ScoreComparator<MR>());
		
		// Flag to track if external pruning happened.
		boolean pruned = false;
		
		int counter = 0;
		final Iterator<Cell<MR>> leftIter = chart.getSpanIterator(start, start
				+ split);
		while (leftIter.hasNext()) {
			final Cell<MR> left = leftIter.next();
			final Iterator<Cell<MR>> rightIter = chart.getSpanIterator(start
					+ split + 1, end);
			while (rightIter.hasNext()) {
				final Cell<MR> right = rightIter.next();
				LOG.debug("Processing: left=%d , right=%d", left.hashCode(),
						right.hashCode());
				LOG.debug("Left: %s", left);
				LOG.debug("Right: %s", right);
				final Iterator<CKYBinaryParsingRule<MR>> rules = binaryRules
						.iterator();
				while (rules.hasNext()) {
					final CKYBinaryParsingRule<MR> rule = rules.next();
					LOG.debug("Applying %s", rule);
					for (final ParseRuleResult<MR> prr : rule
							.apply(left, right)) {
						final ParseRuleResult<MR> transformed = transformRuleResult(prr);
						LOG.debug("Applied %s --> %s",
								transformed.getRuleName(),
								transformed.getResultCategory());
						counter += 1;
						// Prune, only keep categories that pass pruning over
						// the semantics, if there's a pruning filter and they
						// have semantics.
						if (prune(pruningFilter,
								transformed.getResultCategory(), start, end,
								sentenceLength, true)) {
							LOG.debug("Pruned (hard pruning): [%d,%d] %s",
									start, end, transformed);
						} else {
							// Create a CKY parse step from the result.
							final CKYParseStep<MR> parseStep = new CKYParseStep<MR>(
									transformed.getResultCategory(), left,
									right, isFullParse(start, end,
											transformed.getResultCategory(),
											chart.getSentenceLength()),
									transformed.getRuleName(), model);
							
							// Create the cell.
							final Cell<MR> newCell = cellFactory.create(
									parseStep, start, end);
							LOG.debug("Created new cell: %s", newCell);
							if (queue.contains(newCell)) {
								// Case the cell signature is already contained
								// in the queue. Remove the old cell, add the
								// new one to it, which might change its score,
								// and then re-add to the queue.
								
								final Cell<MR> oldCell = queue.get(newCell);
								LOG.debug(
										"Adding new cell to existing one in pre-chart queue: %s",
										oldCell);
								// Add the new cell to the old one.
								if (oldCell.addCell(newCell)) {
									// Max-children changed, score might have
									// changed, so need to remove and re-queue.
									LOG.debug("Cell viterbi score updated: %s",
											oldCell);
									
									// Remove the old cell, to re-add it.
									queue.remove(oldCell);
									// Adding here, not offering, since we just
									// removed it, it should be added without
									// any fear of exception.
									queue.add(oldCell);
								}
							} else {
								// Case new cell signature.
								LOG.debug("Adding new cell to pre-chart queue.");
								if (!queue.offer(newCell)) {
									LOG.debug("Pruned (pre-chart pruning): %s",
											newCell);
									pruned = true;
								}
							}
							LOG.debug("Pre-chart queue size = %d", queue.size());
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished processing split (%d, %d)[%d], generated %d cells, returning %d cells",
				start, end, split, counter, queue.size());
		
		final List<Cell<MR>> cells = new ArrayList<Cell<MR>>(queue);
		return Pair.of(cells, pruned);
	}
	
	/**
	 * Hard pruning (not based on score and beam).
	 * 
	 * @param pruningFilter
	 *            Pruning filter to use. Can be null.
	 * @param category
	 *            The category to validate.
	 * @param completeSpan
	 *            Indicates if this category is over the complete span (the
	 *            entire sentence).
	 */
	protected boolean prune(IFilter<MR> pruningFilter, Category<MR> category,
			int start, int end, int sentenceLength, boolean preUnary) {
		if (category.getSem() == null
				&& !category.getSyntax().equals(Syntax.EMPTY)) {
			// Prune categories with no semantics, unless supposed to be empty.
			return true;
		}
		
		if (pruningFilter != null && category.getSem() != null
				&& !pruningFilter.isValid(category.getSem())) {
			// Prune according to the specified pruning filter.
			return true;
		}
		
		// For the complete span, we can prune stronger.
		if (isCompleteSpan(start, end, sentenceLength)) {
			final boolean fullParse = isFullParse(start, end, category,
					sentenceLength);
			if (preUnary && !fullParse) {
				// The category spans the entire sentence, but might still be
				// modified by a unary rule. See if any unary rule can accept it
				// as
				// an argument.
				for (final CKYUnaryParsingRule<MR> rule : unaryRules) {
					if (rule.isValidArgument(category)) {
						// Case there's a unary rule that can still process this
						// category.
						return false;
					}
				}
				// No unary rule can accept this category, so if is not a full
				// parse, we should prune it.
				return true;
			} else if (!preUnary && !fullParse) {
				// If complete span, not pre-unary and not a full parse. Prune.
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Apply unary rules to all cells in the span. The cells generated combine
	 * the unary step and the binary step that lead to it, so they don't depend
	 * on cells in the same span (which might be pruned). Assumes the span was
	 * already processed completely with binary or lexical rules.
	 */
	protected Pair<List<Cell<MR>>, Boolean> unaryProcessSpan(int start,
			int end, int sentenceLength, Chart<MR> chart,
			AbstractCellFactory<MR> cellFactory, IFilter<MR> pruningFilter,
			IDataItemModel<MR> model) {
		LOG.debug("Unary processing span (%d, %d) with %d  cells", start, end,
				chart.spanSize(start, end));
		
		final List<Cell<MR>> newCells = new LinkedList<Cell<MR>>();
		int counter = 0;
		final Iterator<Cell<MR>> iterator = chart.getSpanIterator(start, end);
		while (iterator.hasNext()) {
			final Cell<MR> cell = iterator.next();
			LOG.debug("Processing: cell=%d", cell.hashCode());
			final Iterator<CKYUnaryParsingRule<MR>> rulesIterator = unaryRules
					.iterator();
			while (rulesIterator.hasNext()) {
				for (final ParseRuleResult<MR> prr : rulesIterator.next()
						.apply(cell)) {
					final ParseRuleResult<MR> transformed = transformRuleResult(prr);
					LOG.debug("Applied %s --> %s", transformed.getRuleName(),
							transformed.getResultCategory());
					counter += cell.numSteps();
					// Filter cells, only keep cells that pass pruning over the
					// semantics, if there's a pruning filter and they have
					// semantics.
					if (prune(pruningFilter, transformed.getResultCategory(),
							start, end, sentenceLength, false)) {
						LOG.debug("Pruned (hard pruning): [%d,%d] %s", start,
								end, transformed);
					} else {
						// Create combined parse step. Each step combine all
						// binary steps that lead to this cell, and the unary
						// step just created.
						for (final AbstractCKYParseStep<MR> step : cell
								.getSteps()) {
							// Create the combined parse step and the new cell.
							final Cell<MR> newCell = cellFactory.create(step
									.cloneWithUnary(
											transformed,
											model,
											isFullParse(start, end, transformed
													.getResultCategory(),
													sentenceLength)), start,
									end);
							LOG.debug("Created new cell: %s", newCell);
							newCells.add(newCell);
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished unary processing span (%d, %d), generated %d cells, returning %d cells",
				start, end, counter, newCells.size());
		
		return Pair.of(newCells, false);
	}
	
	/**
	 * Function is identical to
	 * {@link #unaryProcessSpan(int, int, Chart, AbstractCellFactory, IFilter, IDataItemModel)}
	 * , except pre-chart pruning using the size of the beam. Using this method
	 * creates a further approximation of the packed chart, which influences
	 * non-maximal children of cells.
	 */
	protected Pair<List<Cell<MR>>, Boolean> unaryProcessSpanAndPrune(int start,
			int end, int sentenceLength, Chart<MR> chart,
			AbstractCellFactory<MR> cellFactory, IFilter<MR> pruningFilter,
			int chartBeamSize, IDataItemModel<MR> model) {
		LOG.debug("Unary processing span (%d, %d) with %d  cells", start, end,
				chart.spanSize(start, end));
		
		final DirectAccessBoundedPriorityQueue<Cell<MR>> queue = new DirectAccessBoundedPriorityQueue<Cell<MR>>(
				chartBeamSize * 2 + 1, new Cell.ScoreComparator<MR>());
		
		// Flag to track if external pruning happened.
		boolean pruned = false;
		
		int counter = 0;
		final Iterator<Cell<MR>> iterator = chart.getSpanIterator(start, end);
		while (iterator.hasNext()) {
			final Cell<MR> cell = iterator.next();
			LOG.debug("Processing: cell=%d", cell.hashCode());
			final Iterator<CKYUnaryParsingRule<MR>> rulesIterator = unaryRules
					.iterator();
			while (rulesIterator.hasNext()) {
				for (final ParseRuleResult<MR> prr : rulesIterator.next()
						.apply(cell)) {
					final ParseRuleResult<MR> transformed = transformRuleResult(prr);
					LOG.debug("Applied %s --> %s", transformed.getRuleName(),
							transformed.getResultCategory());
					counter += cell.numSteps();
					// Filter cells, only keep cells that pass pruning over the
					// semantics, if there's a pruning filter and they have
					// semantics.
					if (prune(pruningFilter, transformed.getResultCategory(),
							start, end, sentenceLength, false)) {
						LOG.debug("Pruned (hard pruning): [%d,%d] %s", start,
								end, transformed);
					} else {
						// Create combined parse step. Each step combine all
						// binary steps that lead to this cell, and the unary
						// step just created.
						for (final AbstractCKYParseStep<MR> step : cell
								.getSteps()) {
							// Create the combined parse step and the new cell.
							final Cell<MR> newCell = cellFactory.create(step
									.cloneWithUnary(
											transformed,
											model,
											isFullParse(start, end, transformed
													.getResultCategory(),
													sentenceLength)), start,
									end);
							LOG.debug("Created new cell: %s", newCell);
							
							if (queue.contains(newCell)) {
								// Case the cell signature is already in the
								// queue. Remove the old cell, add the new one
								// to it, which might change its score, and then
								// re-add to the queue.
								
								final Cell<MR> oldCell = queue.get(newCell);
								LOG.debug(
										"Adding new cell to existing one in pre-chart queue: %s",
										oldCell);
								// Add the new cell to the old one
								if (oldCell.addCell(newCell)) {
									// Max-children changed, score might have
									// changed, so need to remove and re-queue
									LOG.debug("Cell viterbi score updated: %s",
											oldCell);
									
									// Remove the old cell, to re-add it
									queue.remove(oldCell);
									// Adding here, not offering, since we just
									// removed it, it should be added without
									// any fear of exception
									queue.add(oldCell);
								}
							} else {
								// Case new cell signature.
								LOG.debug("Adding new cell to pre-chart queue.");
								if (!queue.offer(newCell)) {
									LOG.debug("Pruned (pre-chart pruning): %s",
											newCell);
									pruned = true;
								}
							}
							LOG.debug("Pre-chart queue size = %d", queue.size());
						}
					}
				}
			}
		}
		
		LOG.debug(
				"Finished unary processing span (%d, %d), generated %d cells, returning %d cells",
				start, end, counter, queue.size());
		
		final List<Cell<MR>> cells = new ArrayList<Cell<MR>>(queue);
		return Pair.of(cells, pruned);
	}
}
