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
package edu.cornell.cs.nlp.spf.parser.ccg.cky;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.CompositeImmutableLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.parser.ISentenceLexiconGenerator;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.AbstractCellFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell.ScoreComparator;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.CellFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.CKYLexicalStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.CKYParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.IWeightedCKYStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.WeightedCKYLexicalStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.WeightedCKYParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ILexicalRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.LexicalResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParser;
import edu.cornell.cs.nlp.utils.collections.queue.DirectAccessBoundedPriorityQueue;
import edu.cornell.cs.nlp.utils.collections.queue.IDirectAccessBoundedPriorityQueue;
import edu.cornell.cs.nlp.utils.collections.queue.OrderInvariantDirectAccessBoundedQueue;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

public abstract class AbstractCKYParser<DI extends Sentence, MR>
		implements IGraphParser<DI, MR> {

	public static final ILogger								LOG					= LoggerFactory
			.create(AbstractCKYParser.class);

	private static final long								serialVersionUID	= -1141905985877531704L;

	/**
	 * The maximum number of cells to hold for each span.
	 */
	private final int										beamSize;

	/**
	 * Binary CCG parsing rules.
	 */
	private final CKYBinaryParsingRule<MR>[]				binaryRules;

	/**
	 * Randomly break ties during pruning using the order of insertion to the
	 * queue. In a multi-threaded parser, this is essentially random.
	 */
	private final boolean									breakTies;

	private final ScoreComparator<MR>						cellScoreComparator	= new Cell.ScoreComparator<MR>();

	private final IFilter<Category<MR>>						completeParseFilter;

	private final ILexicalRule<MR>							lexicalRule;

	private final boolean									pruneLexicalCells;

	/**
	 * List of lexical generators that use the sentence itself to generate
	 * lexical entries.
	 */
	private final List<ISentenceLexiconGenerator<DI, MR>>	sentenceLexiconGenerators;

	/**
	 * Lexical generators to create lexical entries for sloppy inference.
	 */
	private final List<ISentenceLexiconGenerator<DI, MR>>	sloppyLexicalGenerators;

	private final CKYUnaryParsingRule<MR>[]					unaryRules;

	protected final ICategoryServices<MR>					categoryServices;

	protected AbstractCKYParser(int beamSize,
			CKYBinaryParsingRule<MR>[] binaryRules,
			List<ISentenceLexiconGenerator<DI, MR>> sentenceLexiconGenerators,
			List<ISentenceLexiconGenerator<DI, MR>> sloppyLexicalGenerators,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			IFilter<Category<MR>> completeParseFilter,
			CKYUnaryParsingRule<MR>[] unaryRules, ILexicalRule<MR> lexicalRule,
			boolean breakTies) {
		this.beamSize = beamSize;
		this.binaryRules = binaryRules;
		this.sentenceLexiconGenerators = sentenceLexiconGenerators;
		this.sloppyLexicalGenerators = sloppyLexicalGenerators;
		this.categoryServices = categoryServices;
		this.pruneLexicalCells = pruneLexicalCells;
		this.completeParseFilter = completeParseFilter;
		this.unaryRules = unaryRules;
		this.lexicalRule = lexicalRule;
		this.breakTies = breakTies;
		LOG.info("Init :: %s: pruneLexicalCells=%s beamSize=%d ...", getClass(),
				pruneLexicalCells, beamSize);
		LOG.info("Init :: %s: ... sloppyLexicalGenerator=%s ...", getClass(),
				sloppyLexicalGenerators);
		LOG.info("Init :: %s: ... binary rules=%s ...", getClass(),
				Arrays.toString(binaryRules));
		LOG.info("Init :: %s: ... unary rules=%s ...", getClass(),
				Arrays.toString(unaryRules));
		LOG.info("Init :: %s: ... lexical rule=%s ...", getClass(),
				lexicalRule);
		LOG.info("Init :: %s: ... breakTies=%s", getClass(), breakTies);
	}

	/**
	 * Checks if the given span is over the entire sentence.
	 */
	protected static boolean isCompleteSpan(SentenceSpan span) {
		return span.isStart() && span.isEnd();
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model) {
		return parse(dataItem, model, false);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean sloppy) {
		return parse(dataItem, model, sloppy, null);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean sloppy, ILexiconImmutable<MR> tempLexicon) {
		return parse(dataItem, model, sloppy, tempLexicon, null);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean sloppy, ILexiconImmutable<MR> tempLexicon,
			Integer altBeamSize) {
		return parse(dataItem, model, sloppy, tempLexicon, altBeamSize, null);
	}

	public CKYParserOutput<MR> parse(DI dataItem, IDataItemModel<MR> model,
			boolean sloppy, ILexiconImmutable<MR> tempLexicon,
			Integer altBeamSize, AbstractCellFactory<MR> cellFactory) {
		return parse(dataItem, null, model, sloppy, tempLexicon, altBeamSize,
				cellFactory);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model) {
		return parse(dataItem, pruningFilter, model, false);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model,
			boolean sloppy) {
		return parse(dataItem, pruningFilter, model, sloppy, null);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model,
			boolean sloppy, ILexiconImmutable<MR> tempLexicon) {
		return parse(dataItem, pruningFilter, model, sloppy, tempLexicon, null);
	}

	@Override
	public CKYParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model,
			boolean sloppy, ILexiconImmutable<MR> tempLexicon,
			Integer altBeamSize) {
		return parse(dataItem, pruningFilter, model, sloppy, tempLexicon,
				altBeamSize, null);
	}

	public CKYParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model,
			boolean sloppy, ILexiconImmutable<MR> tempLexicon,
			Integer altBeamSize,
			AbstractCellFactory<MR> scoreSensitiveFactory) {
		// Store starting time
		final long start = System.currentTimeMillis();

		final TokenSeq tokens = dataItem.getTokens();

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
				!pruneLexicalCells, breakTies);

		// Create the list of active lexicons
		final List<ILexiconImmutable<MR>> lexicons = new ArrayList<ILexiconImmutable<MR>>();

		// Lexicon for sloppy inference.
		if (sloppy) {
			boolean createdSloppyEntries = false;
			for (final ISentenceLexiconGenerator<DI, MR> generator : sloppyLexicalGenerators) {
				final Lexicon<MR> sloppyLexicon = new Lexicon<MR>(
						generator.generateLexicon(dataItem));
				if (sloppyLexicon.size() != 0) {
					createdSloppyEntries = true;
				}
				lexicons.add(sloppyLexicon);
			}
			if (!createdSloppyEntries) {
				LOG.warn(
						"Sloppy inference but no sloppy entries created -- verify the parser is setup to allow sloppy inference");
			}
		}

		// Lexicon with heuristically generated lexical entries. The entries are
		// generated given the string of the sentence.
		for (final ISentenceLexiconGenerator<DI, MR> generator : sentenceLexiconGenerators) {
			lexicons.add(new Lexicon<MR>(generator.generateLexicon(dataItem)));
		}

		// The model lexicon
		lexicons.add(model.getLexicon());

		// If there's a temporary lexicon, add it too
		if (tempLexicon != null) {
			lexicons.add(tempLexicon);
		}

		return new CKYParserOutput<MR>(
				doParse(pruningFilter, model, chart, tokens.size(), cellFactory,
						new CompositeImmutableLexicon<MR>(lexicons)),
				System.currentTimeMillis() - start);

	}

	protected abstract Chart<MR> doParse(Predicate<ParsingOp<MR>> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> currentChart, int numTokens,
			AbstractCellFactory<MR> cellFactory, ILexiconImmutable<MR> lexicon);

	/**
	 * Adds all of the cells to the chart that can be created by lexical
	 * insertion in the given span. The work to find valid lexical entries for
	 * each split is done by getLexEntries.
	 */
	protected Pair<Collection<Cell<MR>>, Boolean> generateLexicalCells(
			int start, int end, Chart<MR> chart, ILexiconImmutable<MR> lexicon,
			IDataItemModel<MR> model, Predicate<ParsingOp<MR>> filter) {

		final AbstractCellFactory<MR> cellFactory = chart.getCellFactory();
		final TokenSeq tokens = chart.getTokens().sub(start, end + 1);
		final SentenceSpan span = new SentenceSpan(start, end,
				chart.getSentenceLength());

		LOG.debug("Populating lexical entries for: %s", tokens);

		if (pruneLexicalCells) {
			final IDirectAccessBoundedPriorityQueue<Cell<MR>> queue = breakTies
					? new DirectAccessBoundedPriorityQueue<Cell<MR>>(
							chart.getBeamSize(), cellScoreComparator)
					: new OrderInvariantDirectAccessBoundedQueue<Cell<MR>>(
							chart.getBeamSize(), cellScoreComparator);

			// Statistics counters and flags are atomic to allow writing
			// from within the stream. Except the pruned flag, which is
			// returned, the others are only updated if the log level id
			// DEBUG. This avoids extra locking.
			final AtomicInteger hardPruningCounter = new AtomicInteger(0);
			final AtomicInteger counter = new AtomicInteger(0);
			final AtomicBoolean pruned = new AtomicBoolean(false);

			final Iterator<LexicalResult<MR>> resultIterator = lexicalRule
					.apply(tokens, span, lexicon);

			// Create a stream to distribute the computation. During DEBUG
			// logging, the stream is sequential to simplify log reading.
			final Iterable<LexicalResult<MR>> iterable = () -> resultIterator;
			final Stream<LexicalResult<MR>> lexicalStream = LOG
					.getLogLevel() == LogLevel.DEBUG
							? StreamSupport.stream(iterable.spliterator(), true)
									.sequential()
							: StreamSupport.stream(iterable.spliterator(), true)
									.parallel().unordered();

			// To protect the queue from concurrent modification, the stream
			// is converted to sequential before items are added to the
			// queue.
			lexicalStream.filter(result -> {
				// Pruning.
				LOG.debug(() -> counter.incrementAndGet());
				if (filter != null && !filter.test(new ParsingOp<MR>(
						result.getResultCategory(), span,
						ILexicalParseStep.LEXICAL_DERIVATION_STEP_RULENAME))) {
					LOG.debug("Pruned lexical entry: %s %s", span,
							result.getEntry());
					LOG.debug(() -> hardPruningCounter.incrementAndGet());
					return false;
				} else {
					return true;
				}
			}).map(result -> {
				// Create the cell.
				final Cell<MR> cell = cellFactory.create(
						new WeightedCKYLexicalStep<MR>(new CKYLexicalStep<MR>(
								result.getResultCategory(), result.getEntry(),
								isFullParse(span, result.getResultCategory()),
								start, end), model));
				LOG.debug(
						"[%d,%d] generated cell from lexical entry (cell=%d): %s",
						start, end, cell.hashCode(), result.getEntry());
				return cell;
			}).sequential().forEach(cell -> {
				// Add to the queue.
				if (queue.contains(cell)) {
					// Case the cell signature is already contained
					// in the queue. Remove the old cell, add the
					// new one to it, which might change its score,
					// and then re-add to the queue.

					final Cell<MR> oldCell = queue.get(cell);
					LOG.debug(
							"Adding new cell to existing one in pre-chart queue: %s",
							oldCell);
					// Add the new cell to the old one.
					if (oldCell.addCell(cell)) {
						// Max-children changed, score might have
						// changed, so need to remove and re-queue.
						LOG.debug("Cell viterbi score updated: %s", oldCell);

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
					if (!queue.offer(cell)) {
						LOG.debug("Pruned (pre-chart pruning): %s", cell);
						pruned.set(true);
					}
				}
			});

			// The counters used here are only updated when the log level is at
			// least DEBUG.
			LOG.debug(
					"(%d-%d): Generated %d entries, %d survived hard (%d) and soft pruning",
					start, end, counter, queue.size(), hardPruningCounter);

			return Pair.of(queue, pruned.get());
		} else {
			// If the chart doesn't prune lexical cells, don't do pruning here
			// as well, just validate.

			// The counter is only updated if the log level id
			// DEBUG. This avoids extra locking.
			final AtomicInteger counter = new AtomicInteger(0);

			final Iterator<LexicalResult<MR>> resultIterator = lexicalRule
					.apply(tokens, span, lexicon);

			// Create a stream to distribute the computation. During DEBUG
			// logging, the stream is sequential to simplify log reading.
			final Iterable<LexicalResult<MR>> iterable = () -> resultIterator;
			final Stream<LexicalResult<MR>> lexicalStream = LOG
					.getLogLevel() == LogLevel.DEBUG
							? StreamSupport.stream(iterable.spliterator(), true)
									.sequential()
							: StreamSupport.stream(iterable.spliterator(), true)
									.parallel().unordered();

			// To protect the queue from concurrent modification, the stream
			// is converted to sequential before items are added to the
			// queue.
			final List<Cell<MR>> cells = lexicalStream.filter(result -> {
				// Pruning.
				LOG.debug(() -> counter.incrementAndGet());
				if (filter != null && !filter.test(new ParsingOp<MR>(
						result.getResultCategory(), span,
						ILexicalParseStep.LEXICAL_DERIVATION_STEP_RULENAME))) {
					LOG.debug("Pruned lexical entry: %s %s", span,
							result.getEntry());
					return false;
				} else {
					return true;
				}
			}).map(result -> {
				// Create the cell.
				final Cell<MR> cell = cellFactory.create(
						new WeightedCKYLexicalStep<MR>(new CKYLexicalStep<MR>(
								result.getResultCategory(), result.getEntry(),
								isFullParse(span, result.getResultCategory()),
								start, end), model));
				LOG.debug(
						"[%d,%d] generated cell from lexical entry (cell=%d): %s",
						start, end, cell.hashCode(), result.getEntry());
				return cell;
			}).collect(Collectors.toList());

			// The counters used here are only updated when the log level is at
			// least DEBUG.
			LOG.debug(
					"(%d-%d): Generated %d entries, %d survived hard pruning (soft pruning disabled) -- this may include duplicates, which will collapsed when added to the chart",
					start, end, counter, cells.size());

			return Pair.of(cells, false);
		}
	}

	/**
	 * Checks if the current category in the given span can be considered a
	 * complete parse.
	 */
	protected boolean isFullParse(SentenceSpan span, Category<MR> category) {
		return isCompleteSpan(span) && completeParseFilter.test(category);
	}

	/**
	 * Processing a (single) split of a (single) span.
	 *
	 * @return Pair of cells to add to the chart and a pruning flag (to indicate
	 *         pruning external to the chart).
	 */
	protected Pair<List<Cell<MR>>, Boolean> processSplit(int start, int end,
			int split, int sentenceLength, Chart<MR> chart,
			AbstractCellFactory<MR> cellFactory,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model) {
		// Processing a (single) split of a (single) span

		final int leftStart = start;
		final int leftEnd = start + split;
		final int rightStart = start + split + 1;
		final int rightEnd = end;

		LOG.debug("Processing split (%d:%d, %d:%d) ", leftStart, leftEnd,
				rightStart, rightEnd);
		LOG.debug("... with %d x %d cells", chart.spanSize(leftStart, leftEnd),
				chart.spanSize(rightStart, rightEnd));

		final SentenceSpan span = new SentenceSpan(start, end, sentenceLength);

		// Copy the cells of the right span to a local array to avoid
		// re-creating the iterator.
		final Iterator<Cell<MR>> rightIter = chart.getSpanIterator(rightStart,
				rightEnd);
		final int numRightCells = chart.spanSize(rightStart, rightEnd);
		@SuppressWarnings("unchecked")
		final Cell<MR>[] rightCells = (Cell<MR>[]) Array.newInstance(Cell.class,
				numRightCells);
		int i = 0;
		while (rightIter.hasNext()) {
			rightCells[i++] = rightIter.next();
		}

		final int numRules = binaryRules.length;
		final AtomicInteger counter = new AtomicInteger(0);

		// Create a list from left cells. This will allow the stream() to
		// distribute better.
		final List<Cell<MR>> leftCells = new ArrayList<>();
		final Iterator<Cell<MR>> iterator = chart.getSpanIterator(leftStart,
				leftEnd);
		while (iterator.hasNext()) {
			leftCells.add(iterator.next());
		}

		// When debugging, it's easier to read the logs with a sequential
		// stream. Naturally, this has performance costs.
		final Stream<Cell<MR>> leftStream = LOG.getLogLevel() == LogLevel.DEBUG
				? leftCells.stream().sequential()
				: leftCells.stream().parallel().unordered();

		final List<Cell<MR>> newCells = leftStream.map(left -> {
			final List<Cell<MR>> newCellsFromLeft = new LinkedList<>();
			for (int j = 0; j < numRightCells; ++j) {
				final Cell<MR> right = rightCells[j];
				LOG.debug("Processing: left=%d , right=%d", left.hashCode(),
						right.hashCode());
				for (int ruleIndex = 0; ruleIndex < numRules; ++ruleIndex) {
					final CKYBinaryParsingRule<MR> rule = binaryRules[ruleIndex];
					LOG.debug("Applying %s", rule);
					final ParseRuleResult<MR> prr = rule.apply(left, right,
							span);
					if (prr != null) {
						counter.incrementAndGet();
						// Filter cells, only keep cells that pass
						// pruning over the semantics, if there's a
						// pruning
						// filter and
						// they have semantics
						if (!prune(pruningFilter,
								new ParsingOp<MR>(prr.getResultCategory(), span,
										rule.getName()),
								true)) {
							// Create the parse step
							final CKYParseStep<MR> parseStep = new CKYParseStep<MR>(
									prr.getResultCategory(), left, right,
									isFullParse(span, prr.getResultCategory()),
									prr.getRuleName(), start, end);

							// Create the chart cell
							final Cell<MR> newCell = cellFactory.create(
									new WeightedCKYParseStep<MR>(parseStep,
											model));
							LOG.debug("Created new cell: %s", newCell);

							newCellsFromLeft.add(newCell);
						}
					}
				}
			}
			return newCellsFromLeft;
		}).flatMap(l -> l.stream()).collect(Collectors.toList());

		LOG.debug(
				"Finished processing split (%d, %d)[%d], generated %d cells, returning %d cells",
				start, end, split, counter.get(), newCells.size());

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
			AbstractCellFactory<MR> cellFactory,
			Predicate<ParsingOp<MR>> pruningFilter, int chartBeamSize,
			IDataItemModel<MR> model) {
		// Processing a (single) split of a (single) span.

		final int leftStart = start;
		final int leftEnd = start + split;
		final int rightStart = start + split + 1;
		final int rightEnd = end;

		LOG.debug("Processing split (%d:%d, %d:%d) ", start, start + split,
				start + split + 1, end);
		LOG.debug("... with %d x %d cells", chart.spanSize(leftStart, leftEnd),
				chart.spanSize(rightStart, rightEnd));

		final OrderInvariantDirectAccessBoundedQueue<Cell<MR>> queue = new OrderInvariantDirectAccessBoundedQueue<Cell<MR>>(
				chartBeamSize * 2 + 1, cellScoreComparator);

		// Flag to track if external pruning happened.
		final AtomicBoolean pruned = new AtomicBoolean(false);

		final SentenceSpan span = new SentenceSpan(start, end, sentenceLength);

		// Copy the cells of the right span to a local array to avoid
		// re-creating the iterator.
		final Iterator<Cell<MR>> rightIter = chart.getSpanIterator(rightStart,
				rightEnd);
		final int numRightCells = chart.spanSize(rightStart, rightEnd);
		@SuppressWarnings("unchecked")
		final Cell<MR>[] rightCells = (Cell<MR>[]) Array.newInstance(Cell.class,
				numRightCells);
		int i = 0;
		while (rightIter.hasNext()) {
			rightCells[i++] = rightIter.next();
		}

		final AtomicInteger counter = new AtomicInteger(0);
		final int numRules = binaryRules.length;

		// Create a list from left cells. This will allow the stream() to
		// distribute better.
		final List<Cell<MR>> leftCells = new ArrayList<>();
		final Iterator<Cell<MR>> iterator = chart.getSpanIterator(leftStart,
				leftEnd);
		while (iterator.hasNext()) {
			leftCells.add(iterator.next());
		}

		// When debugging, it's easier to read the logs with a sequential
		// stream. Naturally, this has performance costs.
		final Stream<Cell<MR>> leftStream = LOG.getLogLevel() == LogLevel.DEBUG
				? leftCells.stream().sequential()
				: leftCells.stream().parallel().unordered();

		leftStream.forEach(left -> {
			for (int j = 0; j < numRightCells; ++j) {
				final Cell<MR> right = rightCells[j];
				LOG.debug("Processing: left=%d , right=%d", left.hashCode(),
						right.hashCode());
				LOG.debug("Left: %s", left);
				LOG.debug("Right: %s", right);
				for (int ruleIndex = 0; ruleIndex < numRules; ++ruleIndex) {
					final CKYBinaryParsingRule<MR> rule = binaryRules[ruleIndex];
					LOG.debug("Applying %s", rule);
					final ParseRuleResult<MR> prr = rule.apply(left, right,
							span);
					if (prr != null) {
						counter.incrementAndGet();
						// Prune, only keep categories that pass
						// pruning over
						// the semantics, if there's a pruning
						// filter and they
						// have semantics.
						if (!prune(pruningFilter,
								new ParsingOp<MR>(prr.getResultCategory(), span,
										rule.getName()),
								true)) {
							// Create a CKY parse step from the
							// result.
							final CKYParseStep<MR> parseStep = new CKYParseStep<MR>(
									prr.getResultCategory(), left, right,
									isFullParse(span, prr.getResultCategory()),
									prr.getRuleName(), start, end);

							// Create the cell.
							final Cell<MR> newCell = cellFactory.create(
									new WeightedCKYParseStep<MR>(parseStep,
											model));
							LOG.debug("Created new cell: %s", newCell);
							synchronized (queue) {

								if (queue.contains(newCell)) {
									// Case the cell signature
									// is already contained
									// in the queue. Remove the
									// old cell, add the
									// new one to it, which
									// might change its score,
									// and then re-add to the
									// queue.

									final Cell<MR> oldCell = queue.get(newCell);
									LOG.debug(
											"Adding new cell to existing one in pre-chart queue: %s",
											oldCell);
									// Add the new cell to the
									// old one.
									if (oldCell.addCell(newCell)) {
										// Max-children changed,
										// score might have
										// changed, so need to
										// remove and re-queue.
										LOG.debug(
												"Cell viterbi score updated: %s",
												oldCell);

										// Remove the old cell,
										// to re-add it.
										queue.remove(oldCell);
										// Adding here, not
										// offering, since we
										// just
										// removed it, it should
										// be added without
										// any fear of
										// exception.
										queue.add(oldCell);
									}
								} else {
									// Case new cell signature.
									LOG.debug(
											"Adding new cell to pre-chart queue.");
									if (!queue.offer(newCell)) {
										LOG.debug(
												"Pruned (pre-chart pruning): %s",
												newCell);
										pruned.getAndSet(true);
									}
								}
								LOG.debug("Pre-chart queue size = %d",
										queue.size());
							}
						}
					}
				}
			}

		});

		LOG.debug(
				"Finished processing split (%d, %d)[%d], generated %d cells, returning %d cells",
				start, end, split, counter.get(), queue.size());

		final List<Cell<MR>> cells = new ArrayList<Cell<MR>>(queue);
		return Pair.of(cells, pruned.get() || queue.hasThreshold());
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
	protected boolean prune(Predicate<ParsingOp<MR>> pruningFilter,
			ParsingOp<MR> parsingOp, boolean preUnary) {
		final Category<MR> category = parsingOp.getCategory();
		if (category.getSemantics() == null
				&& category.getSyntax().unify(Syntax.EMPTY) == null) {
			// Prune categories with no semantics, unless supposed to be empty.
			LOG.debug("Pruned (no semantics and not empty): %s", parsingOp);
			return true;
		}

		if (pruningFilter != null && category.getSemantics() != null
				&& !pruningFilter.test(parsingOp)) {
			// Prune according to the specified pruning filter.
			LOG.debug("Pruned (pruning filter): %s", parsingOp);
			return true;
		}

		// For the complete span, we can prune stronger.
		final SentenceSpan span = parsingOp.getSpan();
		if (isCompleteSpan(span)) {
			final boolean fullParse = isFullParse(span, category);
			if (preUnary && !fullParse) {
				// The category spans the entire sentence, but might still be
				// modified by a unary rule. See if any unary rule can accept it
				// as
				// an argument.
				for (final CKYUnaryParsingRule<MR> rule : unaryRules) {
					if (rule.isValidArgument(category, span)) {
						// Case there's a unary rule that can still process this
						// category.
						return false;
					}
				}
				// No unary rule can accept this category, so if is not a full
				// parse, we should prune it.
				LOG.debug(
						"Pruned (complete span, no unary rule can accept and not a full parse): %s",
						parsingOp);
				return true;
			} else if (!preUnary && !fullParse) {
				// If complete span, not pre-unary and not a full parse. Prune.
				LOG.debug(
						"Pruned (complete span, not pre-unary and not a full parse): %s",
						parsingOp);
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
	protected Pair<List<Cell<MR>>, Boolean> unaryProcessSpan(int start, int end,
			int sentenceLength, Chart<MR> chart,
			AbstractCellFactory<MR> cellFactory,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model) {
		LOG.debug("Unary processing span (%d, %d) with %d  cells", start, end,
				chart.spanSize(start, end));

		final SentenceSpan span = new SentenceSpan(start, end, sentenceLength);

		// Create a list from left cells. This will allow the stream() to
		// distribute better.
		final Iterator<Cell<MR>> iterator = chart.getSpanIterator(start, end);
		final List<Cell<MR>> cells = new ArrayList<>();
		while (iterator.hasNext()) {
			cells.add(iterator.next());
		}

		final AtomicInteger counter = new AtomicInteger(0);
		final int numRules = unaryRules.length;
		final List<Cell<MR>> newCells = StreamSupport
				.stream(Spliterators.spliterator(cells, Spliterator.IMMUTABLE),
						LOG.getLogLevel() == LogLevel.DEBUG ? false : true)
				.map(cell -> {
					LOG.debug("Processing: cell=%d", cell.hashCode());
					for (int ruleIndex = 0; ruleIndex < numRules; ++ruleIndex) {
						final ParseRuleResult<MR> prr = unaryRules[ruleIndex]
								.apply(cell, span);
						if (prr != null) {
							counter.addAndGet(cell.numSteps());
							// Filter cells, only keep cells that pass pruning
							// over the
							// semantics, if there's a pruning filter and they
							// have
							// semantics.
							if (!prune(pruningFilter,
									new ParsingOp<MR>(prr.getResultCategory(),
											span, prr.getRuleName()),
									false)) {
								// Create combined parse step. Each step combine
								// all
								// binary steps that lead to this cell, and the
								// unary
								// step just created.
								for (final IWeightedCKYStep<MR> step : cell
										.getSteps()) {
									// Create the combined parse step and the
									// new cell.
									final Cell<MR> newCell = cellFactory
											.create(step.overloadWithUnary(prr,
													isFullParse(span,
															prr.getResultCategory()),
													model));
									LOG.debug("Created new cell: %s", newCell);
									return newCell;
								}
							}
						}
					}
					return null;
				}).filter(c -> c != null).collect(Collectors.toList());

		LOG.debug(
				"Finished unary processing span (%d, %d), generated %d cells, returning %d cells",
				start, end, counter.get(), newCells.size());

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
			AbstractCellFactory<MR> cellFactory,
			Predicate<ParsingOp<MR>> pruningFilter, int chartBeamSize,
			IDataItemModel<MR> model) {
		LOG.debug("Unary processing span (%d, %d) with %d  cells", start, end,
				chart.spanSize(start, end));

		final SentenceSpan span = new SentenceSpan(start, end, sentenceLength);
		final OrderInvariantDirectAccessBoundedQueue<Cell<MR>> queue = new OrderInvariantDirectAccessBoundedQueue<Cell<MR>>(
				chartBeamSize * 2 + 1, new Cell.ScoreComparator<MR>());

		// Flag to track if external pruning happened.
		final AtomicBoolean pruned = new AtomicBoolean(false);

		final AtomicInteger counter = new AtomicInteger(0);
		final int numRules = unaryRules.length;

		// Create a list from left cells. This will allow the stream() to
		// distribute better.
		final Iterator<Cell<MR>> iterator = chart.getSpanIterator(start, end);
		final List<Cell<MR>> cells = new ArrayList<>();
		while (iterator.hasNext()) {
			cells.add(iterator.next());
		}

		StreamSupport
				.stream(Spliterators.spliterator(cells, Spliterator.IMMUTABLE),
						LOG.getLogLevel() == LogLevel.DEBUG ? false : true)
				.forEach(cell -> {
					LOG.debug("Processing: cell=%d", cell.hashCode());
					for (int ruleIndex = 0; ruleIndex < numRules; ++ruleIndex) {
						final ParseRuleResult<MR> prr = unaryRules[ruleIndex]
								.apply(cell, span);
						if (prr != null) {
							counter.addAndGet(cell.numSteps());
							// Filter cells, only keep cells that pass
							// pruning over the
							// semantics, if there's a pruning filter
							// and they have
							// semantics.
							if (!prune(pruningFilter,
									new ParsingOp<MR>(prr.getResultCategory(),
											span, prr.getRuleName()),
									false)) {
								// Create combined parse step. Each step
								// combine all
								// binary steps that lead to this cell,
								// and the unary
								// step just created.
								for (final IWeightedCKYStep<MR> step : cell
										.getSteps()) {
									// Create the combined parse step
									// and the new cell.
									final Cell<MR> newCell = cellFactory
											.create(step.overloadWithUnary(prr,
													isFullParse(span,
															prr.getResultCategory()),
													model));
									LOG.debug("Created new cell: %s", newCell);

									synchronized (queue) {
										if (queue.contains(newCell)) {
											// Case the cell signature
											// is already in the
											// queue. Remove the old
											// cell, add the new one
											// to it, which might change
											// its score, and then
											// re-add to the queue.

											final Cell<MR> oldCell = queue
													.get(newCell);
											LOG.debug(
													"Adding new cell to existing one in pre-chart queue: %s",
													oldCell);
											// Add the new cell to the
											// old one
											if (oldCell.addCell(newCell)) {
												// Max-children changed,
												// score might have
												// changed, so need to
												// remove and re-queue
												LOG.debug(
														"Cell viterbi score updated: %s",
														oldCell);

												// Remove the old cell,
												// to re-add it
												queue.remove(oldCell);
												// Adding here, not
												// offering, since we
												// just
												// removed it, it should
												// be added without
												// any fear of exception
												queue.add(oldCell);
											}
										} else {
											// Case new cell signature.
											LOG.debug(
													"Adding new cell to pre-chart queue.");
											if (!queue.offer(newCell)) {
												LOG.debug(
														"Pruned (pre-chart pruning): %s",
														newCell);
												pruned.set(true);
											}
										}
										LOG.debug("Pre-chart queue size = %d",
												queue.size());
									}
								}
							}
						}
					}

				});

		LOG.debug(
				"Finished unary processing span (%d, %d), generated %d cells, returning %d cells",
				start, end, counter, queue.size());

		return Pair.of(new ArrayList<Cell<MR>>(queue),
				pruned.get() || queue.hasThreshold());
	}
}
