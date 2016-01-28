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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.chart;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYDerivation;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.IWeightedCKYStep;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.Span;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.collections.iterators.CompositeIterator;
import edu.cornell.cs.nlp.utils.collections.queue.DirectAccessBoundedPriorityQueue;
import edu.cornell.cs.nlp.utils.collections.queue.IDirectAccessBoundedPriorityQueue;
import edu.cornell.cs.nlp.utils.collections.queue.OrderInvariantDirectAccessBoundedQueue;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.math.LogSumExp;

/**
 * A CKY chart.
 *
 * @author Yoav Artzi
 * @author Tom Kwiatkowski
 * @see Cell
 */
public class Chart<MR> implements Iterable<Cell<MR>> {
	public static final ILogger				LOG	= LoggerFactory
														.create(Chart.class
																.getName());

	private final int						beamSize;

	private final AbstractCellFactory<MR>	cellFactory;

	/** An array of spans for every starting and end indices. */
	private final AbstractSpan<MR>[][]		chart;

	/** Number of words in input sentence. */
	private final int						sentenceLength;

	/**
	 * The tokens this chart is created for.
	 */
	private final TokenSeq					tokens;

	/**
	 * @param breakTies
	 *            Breaks ties during pruning using the order of insertion to the
	 *            queue. In a multi-threaded parser, this is essentially random.
	 */
	@SuppressWarnings("unchecked")
	public Chart(TokenSeq tokens, int maxNumberOfCellPerSpan,
			AbstractCellFactory<MR> cellFactory, boolean separateLexicalQueue,
			boolean breakTies) {
		this.beamSize = maxNumberOfCellPerSpan;
		this.tokens = tokens;
		this.cellFactory = cellFactory;
		this.sentenceLength = tokens.size();
		// Somewhat complex init to avoid warnings due to untyped generic
		// classes. This way, it only generated an unchecked warning, which we
		// suppress.
		this.chart = (AbstractSpan<MR>[][]) Array.newInstance(
				AbstractSpan.class, sentenceLength, sentenceLength);
		for (int i = 0; i < sentenceLength; i++) {
			for (int j = i; j < sentenceLength; j++) {
				chart[i][j] = separateLexicalQueue ? new TwoQueueSpan<MR>(
						maxNumberOfCellPerSpan, !breakTies)
						: new SingleQueueSpan<MR>(maxNumberOfCellPerSpan,
								!breakTies);
			}
		}
	}

	/**
	 * Add a cell to the chart. The cell will be added if there's room in the
	 * relevant span or its score is higher than the current minimum for that
	 * span. Otherwise, it won't be added. Adding might cause pruning of a
	 * previously added cell.
	 */
	public void add(Cell<MR> cell) {
		final AbstractSpan<MR> span = chart[cell.getStart()][cell.getEnd()];
		final Cell<MR> existingCell = span.get(cell);
		if (existingCell == null) {
			// Case we are adding a new cell.
			addNew(cell);
		} else {
			// Case adding the content of this cell to an existing cell.
			LOG.debug("Adding to existing cell: %s --> %s", cell, existingCell);
			// Adding to existing is done through a special model. In some cases
			// it requires special operations on the queue, due to the potential
			// of changing the score of the original cell.
			span.addToExisting(existingCell, cell);
			LOG.debug("Added to cell: %s", existingCell);
		}
	}

	public boolean contains(Cell<MR> cell) {
		return chart[cell.getStart()][cell.getEnd()].get(cell) != null;
	}

	/**
	 * Signals that the span of the given start and end indices was pruned
	 * externally (i.e., before anything was added to the chart).
	 */
	public void externalPruning(int start, int end) {
		chart[start][end].externallyPruned = true;
	}

	public int getBeamSize() {
		return beamSize;
	}

	public Cell<MR> getCell(Cell<MR> cell) {
		return chart[cell.getStart()][cell.getEnd()].get(cell);
	}

	public AbstractCellFactory<MR> getCellFactory() {
		return cellFactory;
	}

	/**
	 * Given a cell, usually not from this chart, return longest non-overlapping
	 * spans from the parse packed in the cell paired with their categories. If
	 * the category of the cell is present in the chart in the same span, will
	 * return an empty set. Otherwise, will return a set of spans such that each
	 * span is present in a max-scoring parse in the given cell, but not in this
	 * chart, while both children are in the chart.
	 *
	 * @param viterbiOnly
	 *            Only process viterbi steps.
	 */
	public Map<Span, Set<Cell<MR>>> getMaxNonOverlappingSpans(Cell<MR> cell,
			boolean viterbiOnly) {
		return getMaxNonOverlappingSpans(cell,
				new HashMap<Span, Set<Cell<MR>>>(), new HashSet<Cell<MR>>(),
				viterbiOnly);
	}

	public List<CKYDerivation<MR>> getParseResults() {
		// If we don't prune lexical entries, this might return more parses than
		// the size of the beam (in case of complete parses that are made out of
		// a single lexical entry).
		return ListUtils.map(fullparses(), obj -> new CKYDerivation<MR>(obj));
	}

	/**
	 * Span boundaries (as <start,end> pairs) for all span that experienced
	 * pruning, if internally when adding things to the chart or externally (as
	 * signaled by {@link #externalPruning(int, int)}).
	 */
	public List<Pair<Integer, Integer>> getPrunedSpans() {
		final List<Pair<Integer, Integer>> spans = new LinkedList<Pair<Integer, Integer>>();
		for (int i = 0; i < sentenceLength; i++) {
			for (int j = i; j < sentenceLength; j++) {
				if (chart[i][j].isPruned()) {
					spans.add(Pair.of(i, j));
				}
			}
		}
		return spans;
	}

	public int getSentenceLength() {
		return sentenceLength;
	}

	public Iterable<Cell<MR>> getSpanIterable(final int startIndex,
			final int endIndex) {
		return () -> getSpanIterator(startIndex, endIndex);
	}

	public Iterable<Cell<MR>> getSpanIterable(final int startIndex,
			final int endIndex, final Comparator<Cell<MR>> comparator) {
		return () -> getSpanIterator(startIndex, endIndex, comparator);
	}

	/**
	 * Return an iterator over the cells in a given span.
	 */
	public Iterator<Cell<MR>> getSpanIterator(int startIndex, int endIndex) {
		return chart[startIndex][endIndex].iterator();
	}

	/**
	 * Return a sorted iterator over the cells in a given span.
	 */
	public Iterator<Cell<MR>> getSpanIterator(int startIndex, int endIndex,
			Comparator<Cell<MR>> comparator) {
		assert comparator != null : "Method requires a comparator";
		final List<Cell<MR>> cells = new LinkedList<Cell<MR>>();
		for (final Cell<MR> cell : chart[startIndex][endIndex]) {
			cells.add(cell);
		}
		return CollectionUtils.sorted(cells, comparator).iterator();
	}

	public TokenSeq getTokens() {
		return tokens;
	}

	@Override
	public Iterator<Cell<MR>> iterator() {
		return iterator(null);
	}

	public Iterator<Cell<MR>> iterator(Comparator<Cell<MR>> comparator) {
		return iterator(0, sentenceLength, comparator);
	}

	public Iterator<Cell<MR>> iterator(int start, int end) {
		return iterator(start, end, null);
	}

	public Iterator<Cell<MR>> iterator(int start, int end,
			Comparator<Cell<MR>> comparator) {
		return new CellIterator(start, end, comparator);
	}

	public IHashVector logExpectedFeatures(
			Function<Category<MR>, Double> initialScorer, Span span) {
		// Step I: compute outside probabilities.
		// Initialize outside probabilities.
		initializeLogOutsideProbabilities(initialScorer, span);

		// Propagate outside probabilities.
		propagateLogOutsideProbabilities();

		// Step II: Collected expected features.
		return collectLogExpectedFeatures();
	}

	/**
	 * Traditional expected features. Log outside scores of complete parses that
	 * pass the filter are set to 0.0, all others are set to NEGATIVE_INFINITE.
	 */
	public IHashVector logExpectedFeatures(final IFilter<Category<MR>> filter) {
		return logExpectedFeatures((IScorer<Category<MR>>) e -> {
			if (filter.test(e)) {
				return 0.0;
			} else {
				return Double.NEGATIVE_INFINITY;
			}
		});
	}

	public IHashVector logExpectedFeatures(IFilter<Category<MR>> filter,
			Span span) {
		return logExpectedFeatures((Function<Category<MR>, Double>) e -> {
			if (filter.test(e)) {
				return 0.0;
			} else {
				return Double.NEGATIVE_INFINITY;
			}
		}, span);
	}

	/**
	 * Complete expected features over all complete parses that pass the filter.
	 */
	public IHashVector logExpectedFeatures(IScorer<Category<MR>> initialScorer) {
		return logExpectedFeatures(
				(Function<Category<MR>, Double>) c -> initialScorer.score(c),
				Span.of(0, tokens.size() - 1));
	}

	/**
	 * Compute the log norm for all complete parses that pass the filter.
	 */
	public double logNorm(IFilter<Category<MR>> filter) {
		return logNorm(filter, Span.of(0, tokens.size() - 1));
	}

	/**
	 * Compute the log norm for all parses a the given span that pass the
	 * filter.
	 */
	public double logNorm(IFilter<Category<MR>> filter, Span span) {
		final List<Double> logInsideScores = new ArrayList<Double>(
				chart[span.getStart()][span.getEnd()].size());
		for (final Cell<MR> c : chart[span.getStart()][span.getEnd()]) {
			if (filter.test(c.getCategory())) {
				logInsideScores.add(c.getLogInsideScore());
			}
		}
		return LogSumExp.of(logInsideScores);
	}

	/**
	 * Bottom-up re-computation of chart scores. Doesn't update the local scores
	 * of any {@link IParseStep}, but recomputes the inside and viterbi scores
	 * of {@link Cell}. This is required to propagate modifications to the
	 * chart.
	 */
	public void recomputeScores() {
		for (int len = 0; len < sentenceLength; len++) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				for (final Cell<MR> cell : chart[begin][begin + len]) {
					cell.recomputeScores();
				}
			}
		}
	}

	/**
	 * Flag all cells that participate in the parses with the highest score that
	 * lead to given semantics.
	 */
	public void setMaxes(MR semantics) {
		// First, clear out all of the maxes.
		resetMaxes();

		// Find the max parses for the given expression.
		final List<Cell<MR>> maxCells = new LinkedList<Cell<MR>>();
		double highest = -Double.MAX_VALUE;
		for (final Cell<MR> cell : fullparses()) {
			if (semantics.equals(cell.getCategory().getSemantics())) {
				if (cell.getViterbiScore() > highest) {
					highest = cell.getViterbiScore();
					maxCells.clear();
					maxCells.add(cell);
				} else if (cell.getViterbiScore() == highest) {
					maxCells.add(cell);
				}
			}
		}

		// Flag the cells found as participating in max-score parse.
		for (final Cell<MR> cell : maxCells) {
			cell.setIsMax(true);
		}

		// Propagate the max flags through the chart.
		propogateMaxes();
	}

	public int spanSize(int begin, int end) {
		return chart[begin][end].size();
	}

	@Override
	public String toString() {
		return toString(true, true, null);
	}

	/**
	 * @param sortCells
	 *            Sort the cells in each span according to their pruning score (
	 *            {@link Cell#getPruneScore()}).
	 * @param viterbi
	 *            For each cell, list only its viterbi steps (
	 *            {@link Cell#getViterbiSteps()}).
	 * @param theta
	 *            Model weights to print weights for parse step local features
	 *            (may be null).
	 * @return String representation of the chart.
	 */
	public String toString(boolean sortCells, boolean viterbi,
			IHashVectorImmutable theta) {
		final StringBuilder result = new StringBuilder();

		final Iterator<Cell<MR>> iterator = sortCells ? iterator(new Comparator<Cell<MR>>() {
			private final Comparator<Cell<MR>>	cellComparator	= new Cell.ScoreComparator<MR>();

			@Override
			public int compare(Cell<MR> o1, Cell<MR> o2) {
				final int compare = cellComparator.compare(o1, o2);
				return compare == 0 ? Double.compare(o1.hashCode(),
						o2.hashCode()) : -compare;
			}
		})
				: iterator();
		while (iterator.hasNext()) {
			final Cell<MR> cell = iterator.next();
			result.append(
					cell.toString(
							false,
							ListUtils.join(
									tokens.subList(cell.getStart(),
											cell.getEnd() + 1), " "), viterbi,
							theta)).append("\n");
		}
		result.append("Spans pruned: ").append(getPrunedSpans());
		return result.toString();
	}

	/**
	 * Add entry to chart and do pruning on the cell that it's added to if there
	 * are pruneN entries already there
	 */
	private void addNew(Cell<MR> cell) {

		final int begin = cell.getStart();
		final int end = cell.getEnd();
		final AbstractSpan<MR> span = chart[begin][end];

		LOG.debug("Offering a new cell: %s", cell);
		LOG.debug("Pre-offer size of span: %d", span.size());
		LOG.debug("Pre-offer span minimum score: %s", span.minQeueuScore());
		if (span.offer(cell)) {
			LOG.debug("Cell added");
		} else {
			LOG.debug("Cell rejected");
		}
		LOG.debug("Size of span: %d", span.size());
		LOG.debug("Span minimum score: %s", span.minQeueuScore());
	}

	/**
	 * Iterates over the chart and collects log expected feature values. Assumes
	 * log outside scores were computed.
	 */
	private IHashVector collectLogExpectedFeatures() {
		final IHashVector feats = HashVectorFactory.create();
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				final Iterator<Cell<MR>> i = getSpanIterator(begin, begin + len);
				while (i.hasNext()) {
					i.next().collectLogExpectedFeatures(feats);
				}
			}
		}
		return feats;
	}

	private List<Cell<MR>> fullparses() {
		final List<Cell<MR>> result = new LinkedList<Cell<MR>>();
		final Iterator<Cell<MR>> k = getSpanIterator(0, sentenceLength - 1);
		while (k.hasNext()) {
			final Cell<MR> c = k.next();
			if (c.isFullParse()) {
				result.add(c);
			}
		}
		return result;
	}

	/**
	 * Recursive method to find max overlapping spans and their corresponding
	 * categories with a given cell.
	 *
	 * @see #getMaxNonOverlappingSpans(Cell)
	 */
	private Map<Span, Set<Cell<MR>>> getMaxNonOverlappingSpans(Cell<MR> cell,
			Map<Span, Set<Cell<MR>>> spanDictionary,
			Set<Cell<MR>> visitedCells, boolean viterbiOnly) {
		// Book keeping to short-circuit already visited cells.
		if (visitedCells.contains(cell)) {
			return spanDictionary;
		}
		visitedCells.add(cell);

		// If this cell is in the chart, just return, no need to continue or
		// record its span.
		if (!contains(cell)) {
			final Span span = Span.of(cell.getStart(), cell.getEnd());

			// This cell is not in the chart. For each viterbi step in the cell,
			// check to see if the chart contains both its children.
			for (final IWeightedCKYStep<MR> step : viterbiOnly ? cell
					.getViterbiSteps() : cell.getSteps()) {
				boolean containsAll = true;
				final int numChildren = step.numChildren();
				for (int i = 0; i < numChildren; ++i) {
					final Cell<MR> childCell = step.getChildCell(i);
					if (!contains(childCell)) {
						containsAll = false;
						break;
					}
				}
				if (containsAll) {
					// If the chart contains all children, add the span of this
					// cell and return.
					if (!spanDictionary.containsKey(span)) {
						spanDictionary.put(span, new HashSet<>());
					}
					spanDictionary.get(span).add(cell);
					// No need to visit the other steps or children.
					return spanDictionary;
				}
			}

			// If there's no viterbi step for which the chart contains all
			// children, recurse into all the children.
			for (final IWeightedCKYStep<MR> step : viterbiOnly ? cell
					.getViterbiSteps() : cell.getSteps()) {
				final int numChildren = step.numChildren();
				for (int i = 0; i < numChildren; ++i) {
					final Cell<MR> childCell = step.getChildCell(i);
					getMaxNonOverlappingSpans(childCell, spanDictionary,
							visitedCells, viterbiOnly);
				}
			}
		}
		return spanDictionary;
	}

	/**
	 * Initializes log outside probabilities based on the given filter in
	 * preparation to propagate them (see
	 * {@link #propagateLogOutsideProbabilities()).
	 */
	private void initializeLogOutsideProbabilities(
			Function<Category<MR>, Double> initialScorer, Span span) {
		// First, init all outside probabilities. All roots of complete parses
		// are scored using the given scorer.
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				final Iterator<Cell<MR>> spanIterator = getSpanIterator(begin,
						begin + len);
				while (spanIterator.hasNext()) {
					spanIterator.next().initializeLogOutsideProbabilities(
							initialScorer, span);
				}
			}
		}
	}

	/**
	 * Propagates log outside probabilities. Assumes that all appropriate source
	 * cells were initialized.
	 */
	private void propagateLogOutsideProbabilities() {
		// Iterate over all spans from the entire sentence to the token level.
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				// Must first process unary derivation steps.
				final Iterator<Cell<MR>> unarySpanIterator = getSpanIterator(
						begin, begin + len);
				while (unarySpanIterator.hasNext()) {
					unarySpanIterator.next()
							.updateUnaryChildrenLogOutsideScore();
				}
				// Now do the rest of the steps (i.e., results of binary steps)
				final Iterator<Cell<MR>> binarySpanIterator = getSpanIterator(
						begin, begin + len);
				while (binarySpanIterator.hasNext()) {
					binarySpanIterator.next()
							.updateBinaryChildrenLogOutsideScore();
				}
			}
		}
	}

	/**
	 * Propagate existing max flags through the chart.
	 */
	private void propogateMaxes() {
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				final Iterator<Cell<MR>> spanIterator = getSpanIterator(begin,
						begin + len);
				while (spanIterator.hasNext()) {
					final Cell<MR> cell = spanIterator.next();
					if (cell.isMax()) {
						for (final IWeightedCKYStep<MR> step : cell
								.getViterbiSteps()) {
							for (final Cell<MR> child : step) {
								child.setIsMax(true);
							}
						}
					}
				}
			}
		}
	}

	private void resetMaxes() {
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				final Iterator<Cell<MR>> spanIterator = getSpanIterator(begin,
						begin + len);
				while (spanIterator.hasNext()) {
					spanIterator.next().setIsMax(false);
				}
			}
		}
	}

	/**
	 * A single span in a chart
	 *
	 * @author Yoav Artzi
	 */
	private static abstract class AbstractSpan<MR> implements
			Iterable<Cell<MR>> {
		/**
		 * A flag to indicate if this abstract was pruned externally (i.e.,
		 * outside the chart).
		 */
		protected boolean	externallyPruned	= false;

		public abstract void addToExisting(Cell<MR> existingCell,
				Cell<MR> newCell);

		public abstract Cell<MR> get(Cell<MR> cell);

		public abstract boolean isPruned();

		public abstract Pair<Double, Double> minQeueuScore();

		public abstract boolean offer(Cell<MR> cell);

		public abstract int size();

	}

	private class CellIterator implements Iterator<Cell<MR>> {
		private final Comparator<Cell<MR>>	comparator;
		private final int					end;
		private int							i;
		private int							j;
		private Iterator<Cell<MR>>			spanIterator;

		public CellIterator(int start, int end, Comparator<Cell<MR>> comparator) {
			this.end = end;
			this.i = start;
			this.comparator = comparator;
			this.j = i;
			this.spanIterator = comparator == null ? getSpanIterator(i, j)
					: getSpanIterator(i, j, comparator);
		}

		@Override
		public boolean hasNext() {
			if (spanIterator.hasNext()) {
				return true;
			} else {
				loadNextIteratorIfAvailable();
				return spanIterator.hasNext();
			}
		}

		@Override
		public Cell<MR> next() {
			if (hasNext()) {
				return spanIterator.next();
			} else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			spanIterator.remove();
		}

		private void loadNextIteratorIfAvailable() {
			while (!spanIterator.hasNext()) {
				++j;
				if (j < end) {
					spanIterator = comparator == null ? getSpanIterator(i, j)
							: getSpanIterator(i, j, comparator);
				} else {
					++i;
					if (i < end) {
						j = i;
						spanIterator = comparator == null ? getSpanIterator(i,
								j) : getSpanIterator(i, j, comparator);
					} else {
						return;
					}
				}
			}
		}

	}

	/**
	 * Span that maintains a single queue for both lexical and non-lexical
	 * cells. The queue is order invariant.
	 *
	 * @author Yoav Artzi
	 * @param <MR>
	 */
	private static class SingleQueueSpan<MR> extends AbstractSpan<MR> {
		private final IDirectAccessBoundedPriorityQueue<Cell<MR>>	queue;

		public SingleQueueSpan(int capacity, boolean orderInvariant) {
			this.queue = orderInvariant ? new OrderInvariantDirectAccessBoundedQueue<Cell<MR>>(
					capacity, new Cell.ScoreComparator<MR>())
					: new DirectAccessBoundedPriorityQueue<>(capacity,
							new Cell.ScoreComparator<MR>());
		}

		@Override
		public void addToExisting(Cell<MR> existingCell, Cell<MR> newCell) {
			// Adding the cell into an existing one, may change the score of the
			// cell, so we have to remove it from the queue and re-insert it, if
			// its max-children changed. The score can only increase, so this
			// step can't cause pruning.
			if (existingCell.addCell(newCell)) {
				if (!queue.remove(existingCell)) {
					throw new IllegalStateException(
							"Failed to remove existing cell -- this is a bug");
				}
				queue.add(existingCell);
			}
		}

		@Override
		public Cell<MR> get(Cell<MR> cell) {
			return queue.get(cell);
		}

		@Override
		public boolean isPruned() {
			return externallyPruned || queue.isPruned();
		}

		@Override
		public Iterator<Cell<MR>> iterator() {
			return queue.iterator();
		}

		@Override
		public Pair<Double, Double> minQeueuScore() {
			if (queue.isEmpty()) {
				return null;
			} else {
				final Cell<MR> peek = queue.peek();
				return Pair
						.of(peek.getPruneScore(), peek.getSecondPruneScore());
			}
		}

		@Override
		public boolean offer(Cell<MR> cell) {
			return queue.offer(cell);
		}

		@Override
		public int size() {
			return queue.size();
		}

	}

	private static class TwoQueueSpan<MR> extends AbstractSpan<MR> {
		private final Map<Cell<MR>, Cell<MR>>						lexicals	= new HashMap<Cell<MR>, Cell<MR>>();
		private final IDirectAccessBoundedPriorityQueue<Cell<MR>>	nonLexicalQueue;

		public TwoQueueSpan(int capacity, boolean orderInvariant) {
			this.nonLexicalQueue = orderInvariant ? new OrderInvariantDirectAccessBoundedQueue<Cell<MR>>(
					capacity, new Cell.ScoreComparator<MR>())
					: new DirectAccessBoundedPriorityQueue<>(capacity,
							new Cell.ScoreComparator<MR>());
		}

		@Override
		public void addToExisting(Cell<MR> existingCell, Cell<MR> newCell) {
			if (existingCell.hasLexicalStep()) {
				// No need to remove and re-insert since the lexical map
				// maintains no ordering.
				existingCell.addCell(newCell);
			} else {
				// Adding the cell into an existing one, may change the score of
				// the cell, so we have to remove it from the queue and
				// re-insert it, if its max-children changed. The score can only
				// increase, so this step can't lead to pruning.
				if (existingCell.addCell(newCell)) {
					nonLexicalQueue.remove(existingCell);
					nonLexicalQueue.add(existingCell);
				}
			}
		}

		@Override
		public Cell<MR> get(Cell<MR> cell) {
			if (lexicals.containsKey(cell)) {
				return lexicals.get(cell);
			} else {
				return nonLexicalQueue.get(cell);
			}
		}

		@Override
		public boolean isPruned() {
			return externallyPruned || nonLexicalQueue.isPruned();
		}

		@Override
		public Iterator<Cell<MR>> iterator() {
			final List<Iterator<? extends Cell<MR>>> iterators = new ArrayList<Iterator<? extends Cell<MR>>>(
					2);
			iterators.add(lexicals.values().iterator());
			iterators.add(nonLexicalQueue.iterator());
			return new CompositeIterator<Cell<MR>>(iterators);
		}

		@Override
		public Pair<Double, Double> minQeueuScore() {
			if (nonLexicalQueue.isEmpty()) {
				return null;
			} else {
				final Cell<MR> peek = nonLexicalQueue.peek();
				return Pair
						.of(peek.getPruneScore(), peek.getSecondPruneScore());
			}
		}

		@Override
		public boolean offer(Cell<MR> cell) {
			if (cell.hasLexicalStep()) {
				lexicals.put(cell, cell);
				return true;
			} else {
				return nonLexicalQueue.offer(cell);
			}
		}

		@Override
		public int size() {
			return lexicals.size() + nonLexicalQueue.size();
		}

	}

}
