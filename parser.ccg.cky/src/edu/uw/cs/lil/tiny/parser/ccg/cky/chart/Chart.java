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
package edu.uw.cs.lil.tiny.parser.ccg.cky.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParse;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.CompositeIterator;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.collections.OrderInvariantBoundedPriorityQueue;
import edu.uw.cs.utils.collections.OrderInvariantDirectAccessBoundedQueue;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * A CKY chart.
 * 
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 * @author Tom Kwiatkowski
 * @see Cell
 */
public class Chart<MR> implements Iterable<Cell<MR>> {
	private static final ILogger			LOG	= LoggerFactory
														.create(Chart.class
																.getName());
	
	private final int						beamSize;
	
	private final AbstractCellFactory<MR>	cellFactory;
	
	/** An array of spans for every starting and end indices */
	private final AbstractSpan<MR>[][]		chart;
	
	/** Number of words in input sentence */
	private final int						sentenceLength;
	
	/**
	 * The tokens this chart is created for.
	 */
	private final List<String>				tokens;
	
	@SuppressWarnings("unchecked")
	public Chart(List<String> tokens, int maxNumberOfCellPerSpan,
			AbstractCellFactory<MR> cellFactory, boolean separateLexicalQueue) {
		this.beamSize = maxNumberOfCellPerSpan;
		this.tokens = Collections.unmodifiableList(tokens);
		this.cellFactory = cellFactory;
		this.sentenceLength = tokens.size();
		this.chart = new AbstractSpan[sentenceLength][sentenceLength];
		for (int i = 0; i < sentenceLength; i++) {
			for (int j = i; j < sentenceLength; j++) {
				chart[i][j] = separateLexicalQueue ? new TwoQueueSpan<MR>(
						maxNumberOfCellPerSpan) : new SingleQueueSpan<MR>(
						maxNumberOfCellPerSpan);
			}
		}
	}
	
	/**
	 * Add a cell to the chart. The cell will be added if there's room in the
	 * relevant span or its score is higher than the current minimum for that
	 * span. Otherwise, it won't be added. Adding might cause pruning of a
	 * previously added cell.
	 * 
	 * @param cell
	 */
	public void add(Cell<MR> cell, IDataItemModel<MR> model) {
		
		if (cell.isCompleteSpan() && !cell.isFullParse()) {
			// Case the complete span, but not a complete parse, don't add to
			// the chart
			return;
		}
		
		final AbstractSpan<MR> span = chart[cell.getStart()][cell.getEnd()];
		final Cell<MR> existingCell = span.get(cell);
		if (existingCell == null) {
			// Case we are adding a new cell
			addNew(cell);
		} else {
			// Case adding the content of this cell to an existing cell
			LOG.debug("IN-to-EXIST: %s --> %s", cell, existingCell);
			// Adding to existing is done through a special model. In some cases
			// it requires special operations on the queue, due to the potential
			// of changing the score of the original cell.
			span.addToExisting(existingCell, cell, model);
			LOG.debug("Added to cell: %s", existingCell);
		}
	}
	
	/**
	 * Traditional expected features. Outside scores of complete parses that
	 * pass the filter are set to 1.0, all others are set to 0.0.
	 * 
	 * @param filter
	 * @return
	 */
	public IHashVector expectedFeatures(final IFilter<MR> filter) {
		return expectedFeatures(new IScorer<MR>() {
			@Override
			public double score(MR e) {
				if (filter.isValid(e)) {
					return 1.0;
				} else {
					return 0.0;
				}
			}
		});
	}
	
	public IHashVector expectedFeatures(IScorer<MR> initialScorer) {
		// Step I: compute outside probabilities
		// Initialize outside probabilities
		initializeOutsideProbabilities(initialScorer);
		
		// Propagate outside probabilities
		propagateOutsideProbabilities();
		
		// Step II: Collected expected features
		return collectExpectedFeatures();
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
	
	public List<CKYParse<MR>> getParseResults() {
		// Need a bounded queue here to make sure we don't return more than the
		// beam, because lexical cells might exist outside of the beam
		final OrderInvariantBoundedPriorityQueue<CKYParse<MR>> ret = new OrderInvariantBoundedPriorityQueue<CKYParse<MR>>(
				beamSize, new Comparator<CKYParse<MR>>() {
					@Override
					public int compare(CKYParse<MR> o1, CKYParse<MR> o2) {
						return Double.compare(o1.getScore(), o2.getScore());
					}
				});
		for (final Cell<MR> cell : fullparses()) {
			ret.offer(new CKYParse<MR>(cell));
		}
		return new ArrayList<CKYParse<MR>>(ret);
	}
	
	public int getSentenceLength() {
		return sentenceLength;
	}
	
	/**
	 * Return an iterator over the cells in a given span.
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public Iterator<Cell<MR>> getSpanIterator(int startIndex, int endIndex) {
		return getSpanIterator(startIndex, endIndex, null);
	}
	
	/**
	 * Return a sorted iterator over the cells in a given span.
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public Iterator<Cell<MR>> getSpanIterator(int startIndex, int endIndex,
			Comparator<Cell<MR>> comparator) {
		if (comparator != null) {
			final List<Cell<MR>> cells = new LinkedList<Cell<MR>>();
			for (final Cell<MR> cell : chart[startIndex][endIndex]) {
				cells.add(cell);
			}
			return CollectionUtils.sorted(cells, comparator).iterator();
		} else {
			return chart[startIndex][endIndex].iterator();
		}
	}
	
	public List<String> getTokens() {
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
	
	/**
	 * Compute the total probability of all parses with their root category
	 * passing the filter.
	 * 
	 * @param filter
	 * @return
	 */
	public double norm(IFilter<MR> filter) {
		double norm = 0.0;
		for (final Cell<MR> c : fullparses()) {
			if (filter.isValid(c.getCategroy().getSem())) {
				norm += c.getInsideScore();
			}
		}
		return norm;
	}
	
	/**
	 * Compute outside probabilities with a constraining category. Assumes that
	 * we are parsing with a probabilistic model.
	 * 
	 * @param constrainingCategory
	 */
	public void recomputeInsideScore() {
		// TODO [yoav] Clarify this method and its uses -- not clear what it's
		// supposed to do and if it actually does that
		
		// Iterate over all spans from the entire sentence to the token level
		// and propagate outside probabilities
		for (int len = 0; len < sentenceLength; len++) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				
				// Must first do cells that results from unary parsing rules
				final Iterator<Cell<MR>> spanIterator = getSpanIterator(begin,
						begin + len);
				while (spanIterator.hasNext()) {
					spanIterator.next().recomputeInsideScore();
				}
			}
		}
	}
	
	/**
	 * Flag all cells that participate in the parses with the highest score that
	 * lead to given semantics.
	 * 
	 * @param semantics
	 */
	public void setMaxes(MR semantics) {
		// First, clear out all of the maxes
		resetMaxes();
		
		// Find the max parses for the given expression
		final List<Cell<MR>> maxCells = new LinkedList<Cell<MR>>();
		double highest = -Double.MAX_VALUE;
		for (final Cell<MR> cell : fullparses()) {
			if (semantics.equals(cell.getCategroy().getSem())) {
				if (cell.getViterbiScore() > highest) {
					highest = cell.getViterbiScore();
					maxCells.clear();
					maxCells.add(cell);
				} else if (cell.getViterbiScore() == highest) {
					maxCells.add(cell);
				}
			}
		}
		
		// Flag the cells found as participating in max-score parse
		for (final Cell<MR> cell : maxCells) {
			cell.setIsMax(true);
		}
		
		// Propagate the max flags through the chart
		propogateMaxes();
	}
	
	public int spanSize(int begin, int end) {
		return chart[begin][end].size();
	}
	
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		
		final Iterator<Cell<MR>> iterator = iterator();
		while (iterator.hasNext()) {
			final Cell<MR> cell = iterator.next();
			result.append(
					cell.toString(
							false,
							ListUtils.join(
									tokens.subList(cell.getStart(),
											cell.getEnd() + 1), " "))).append(
					"\n");
		}
		
		return result.toString();
	}
	
	/**
	 * Add entry to chart and do pruning on the cell that it's added to if there
	 * are pruneN entries already there
	 * 
	 * @param begin
	 * @param end
	 * @param cell
	 */
	private void addNew(Cell<MR> cell) {
		
		final int begin = cell.getStart();
		final int end = cell.getEnd();
		final AbstractSpan<MR> span = chart[begin][end];
		
		LOG.debug("IN: %s", cell);
		LOG.debug("Pre-offer size of span: %d", span.size());
		LOG.debug("Pre-offer span minimum score: %s", span.minNonLexicalScore());
		if (span.offer(cell)) {
			LOG.debug("Cell added");
		} else {
			LOG.debug("Cell rejected");
		}
		LOG.debug("Size of span: %d", span.size());
		LOG.debug("Span minimum score: %s", span.minNonLexicalScore());
	}
	
	/**
	 * Iterates over the chart and collects expected feature values. Assumes
	 * outside probabilities were computed.
	 * 
	 * @return
	 */
	private IHashVector collectExpectedFeatures() {
		final IHashVector feats = HashVectorFactory.create();
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				final Iterator<Cell<MR>> i = getSpanIterator(begin, begin + len);
				while (i.hasNext()) {
					final Cell<MR> c = i.next();
					c.collectExpectedFeatures(feats);
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
	 * Initializes outside probabilities based on the given filter in preparation to propagate them (see {@link #propagateOutsideProbabilities()).
	 * 
	 * @param initialScorer
	 */
	private void initializeOutsideProbabilities(IScorer<MR> initialScorer) {
		// First, init all outside probabilities. All roots of complete parses
		// are scored using the given scorer.
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				final Iterator<Cell<MR>> spanIterator = getSpanIterator(begin,
						begin + len);
				while (spanIterator.hasNext()) {
					spanIterator.next().initializeOutsideProbabilities(
							initialScorer);
				}
			}
		}
		
	}
	
	/**
	 * Propagates outside probabilities. Assumes that all appropriate source
	 * cells were initialized appropriately (usually to 1.0) and the others were
	 * initialized to 0.0.
	 */
	private void propagateOutsideProbabilities() {
		// Iterate over all spans from the entire sentence to the token level
		for (int len = sentenceLength - 1; len >= 0; len--) {
			for (int begin = 0; begin < sentenceLength - len; begin++) {
				// Must first process unary derivation steps
				final Iterator<Cell<MR>> unarySpanIterator = getSpanIterator(
						begin, begin + len);
				while (unarySpanIterator.hasNext()) {
					unarySpanIterator.next().updateUnaryChildrenOutsideScore();
				}
				// Now do the rest of the steps (i.e., results of binary steps)
				final Iterator<Cell<MR>> binarySpanIterator = getSpanIterator(
						begin, begin + len);
				while (binarySpanIterator.hasNext()) {
					binarySpanIterator.next()
							.updateBinaryChildrenOutsideScore();
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
				// First do cells that come from unary parsing rules
				final Iterator<Cell<MR>> iteratorForUnaries = getSpanIterator(
						begin, begin + len);
				while (iteratorForUnaries.hasNext()) {
					iteratorForUnaries.next().propMaxUnary();
				}
				// Do the rest of the cells
				final Iterator<Cell<MR>> iteratorForTheRest = getSpanIterator(
						begin, begin + len);
				while (iteratorForTheRest.hasNext()) {
					iteratorForTheRest.next().propMaxNonUnary();
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
	private static abstract class AbstractSpan<Y> implements Iterable<Cell<Y>> {
		public abstract void addToExisting(Cell<Y> existingCell,
				Cell<Y> newCell, IDataItemModel<Y> model);
		
		public abstract Cell<Y> get(Cell<Y> cell);
		
		public abstract Pair<Double, Double> minNonLexicalScore();
		
		public abstract boolean offer(Cell<Y> cell);
		
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
			this.spanIterator = getSpanIterator(i, j, comparator);
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
					spanIterator = getSpanIterator(i, j, comparator);
				} else {
					++i;
					if (i < end) {
						j = i;
						spanIterator = getSpanIterator(i, j, comparator);
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
	 * @param <Y>
	 */
	private static class SingleQueueSpan<Y> extends AbstractSpan<Y> {
		private final OrderInvariantDirectAccessBoundedQueue<Cell<Y>>	queue;
		
		public SingleQueueSpan(int capacity) {
			this.queue = new OrderInvariantDirectAccessBoundedQueue<Cell<Y>>(
					capacity, new Cell.ScoreComparator<Y>());
		}
		
		@Override
		public void addToExisting(Cell<Y> existingCell, Cell<Y> newCell,
				IDataItemModel<Y> model) {
			// Adding the cell into an existing one, may change the score of the
			// cell, so we have to remove it from the queue and re-insert it, if
			// its max-children changed
			if (existingCell.addCell(newCell)) {
				queue.remove(existingCell);
				queue.add(existingCell);
			}
		}
		
		@Override
		public Cell<Y> get(Cell<Y> cell) {
			return queue.get(cell);
		}
		
		@Override
		public Iterator<Cell<Y>> iterator() {
			return queue.iterator();
		}
		
		@Override
		public Pair<Double, Double> minNonLexicalScore() {
			if (queue.isEmpty()) {
				return null;
			} else {
				final Cell<Y> peek = queue.peek();
				return Pair
						.of(peek.getPruneScore(), peek.getSecondPruneScore());
			}
		}
		
		@Override
		public boolean offer(Cell<Y> cell) {
			return queue.offer(cell);
		}
		
		@Override
		public int size() {
			return queue.size();
		}
		
	}
	
	private static class TwoQueueSpan<Y> extends AbstractSpan<Y> {
		private final Map<Cell<Y>, Cell<Y>>								lexicals	= new HashMap<Cell<Y>, Cell<Y>>();
		private final OrderInvariantDirectAccessBoundedQueue<Cell<Y>>	nonLexicalQueue;
		
		public TwoQueueSpan(int capacity) {
			this.nonLexicalQueue = new OrderInvariantDirectAccessBoundedQueue<Cell<Y>>(
					capacity, new Cell.ScoreComparator<Y>());
		}
		
		@Override
		public void addToExisting(Cell<Y> existingCell, Cell<Y> newCell,
				IDataItemModel<Y> model) {
			if (existingCell.hasLexicalStep()) {
				// No need to remove and re-insert since the lexical map
				// maintains no ordering
				existingCell.addCell(newCell);
			} else {
				// Adding the cell into an existing one, may change the score of
				// the cell, so we have to remove it from the queue and
				// re-insert it, if its max-children changed
				if (existingCell.addCell(newCell)) {
					nonLexicalQueue.remove(existingCell);
					nonLexicalQueue.add(existingCell);
				}
			}
		}
		
		@Override
		public Cell<Y> get(Cell<Y> cell) {
			if (lexicals.containsKey(cell)) {
				return lexicals.get(cell);
			} else {
				return nonLexicalQueue.get(cell);
			}
		}
		
		@Override
		public Iterator<Cell<Y>> iterator() {
			final List<Iterator<? extends Cell<Y>>> iterators = new ArrayList<Iterator<? extends Cell<Y>>>(
					2);
			iterators.add(lexicals.values().iterator());
			iterators.add(nonLexicalQueue.iterator());
			return new CompositeIterator<Cell<Y>>(iterators);
		}
		
		@Override
		public Pair<Double, Double> minNonLexicalScore() {
			if (nonLexicalQueue.isEmpty()) {
				return null;
			} else {
				final Cell<Y> peek = nonLexicalQueue.peek();
				return Pair
						.of(peek.getPruneScore(), peek.getSecondPruneScore());
			}
		}
		
		@Override
		public boolean offer(Cell<Y> cell) {
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
