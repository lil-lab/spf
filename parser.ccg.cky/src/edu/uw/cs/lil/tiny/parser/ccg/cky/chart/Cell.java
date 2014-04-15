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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorUtils;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;
import edu.uw.cs.utils.math.LogSumExp;

/**
 * A single {@link Chart} cell of a specific span with specific syntax and
 * semantic data.
 * 
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 * @author Tom Kwiatkowski
 */
public class Cell<MR> {
	public static final ILogger						LOG					= LoggerFactory
																				.create(Cell.class);
	
	/** The starting index of the span of the input string covered by this cell. */
	private final int								begin;
	
	private final Category<MR>						category;
	
	/** The end index of the span of the input string covered by this cell. */
	private final int								end;
	
	private final Set<RuleName>						generatingRules		= new HashSet<RuleName>();
	
	/**
	 * Mutable cache for the hashing code. This field is for internal use only!
	 * It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private int										hashCodeCache;
	
	/**
	 * Mutable flag to indicate if the hash code cache is populated. This field
	 * is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object.
	 */
	private boolean									hashCodeCalculated	= false;
	
	/**
	 * A flag that the {@link Chart) can set to indicate this cell spans the
	 * entire sentence, even if it's not a full parse.
	 */
	private final boolean							isCompleteSpan;
	
	/**
	 * Flag to indicate if this cell represents a complete parse.
	 */
	private final boolean							isFullParse;
	
	private boolean									isMax;
	
	private double									logInsideScore		= Double.NEGATIVE_INFINITY;
	
	/**
	 * Log outside score.
	 */
	private double									logOutsideScore		= Double.NEGATIVE_INFINITY;
	
	/**
	 * Lists of derivation steps that created this cell
	 */
	private final Set<AbstractCKYParseStep<MR>>		steps				= new HashSet<AbstractCKYParseStep<MR>>();
	
	/**
	 * Linear viterbi score.
	 */
	private double									viterbiScore		= -Double.MAX_VALUE;
	
	/**
	 * Number of (partial) parses with this cell at their root.
	 */
	protected long									numParses			= 0;
	
	/**
	 * Number of (partial) viterbi parses with this cell at their root.
	 */
	protected long									numViterbiParses	= 0;
	
	/**
	 * Derivation steps that have the {@link #viterbiScore}.
	 */
	protected final Set<AbstractCKYParseStep<MR>>	viterbiSteps		= new HashSet<AbstractCKYParseStep<MR>>();
	
	protected Cell(AbstractCKYParseStep<MR> parseStep, int start, int end,
			boolean isCompleteSpan) {
		this.isCompleteSpan = isCompleteSpan;
		this.isFullParse = parseStep.isFullParse();
		this.category = parseStep.getRoot();
		this.begin = start;
		this.end = end;
		this.steps.add(parseStep);
		this.generatingRules.add(parseStep.getRuleName());
		updateScores(parseStep);
	}
	
	/**
	 * Add the children of another cell to the current cell. This indicates that
	 * this cell has more ways of being created during parsing. This method
	 * assumes the other cell equals to this one (as far as semantics and
	 * syntax). Assumes that the new derivation steps aren't already present
	 * (which is guaranteed with CKY parsing)
	 * 
	 * @return 'true' iff max children lists changed
	 */
	public boolean addCell(Cell<MR> other) {
		// Iterate over the added children and add them to steps list and inside
		// score
		boolean addedToMaxChildren = false;
		for (final AbstractCKYParseStep<MR> derivationStep : other.steps) {
			if (steps.add(derivationStep)) {
				generatingRules.add(derivationStep.getRuleName());
				addedToMaxChildren |= updateScores(derivationStep);
			}
		}
		return addedToMaxChildren;
	}
	
	/**
	 * Recursively compute the mean (linear) viterbi feature vector.
	 */
	public IHashVector computeMaxAvgFeaturesRecursively() {
		return computeMaxAvgFeaturesRecursively(new HashMap<Cell<MR>, IHashVector>());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final Cell other = (Cell) obj;
		if (begin != other.begin) {
			return false;
		}
		if (category == null) {
			if (other.category != null) {
				return false;
			}
		} else if (!category.equals(other.category)) {
			return false;
		}
		if (end != other.end) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns all lexical entries used in all the trees for which this cell is
	 * the root. This method doesn't rely on the Chart structure.
	 * 
	 * @return List of lexical entries
	 */
	public LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntriesRecursively() {
		final LinkedHashSet<LexicalEntry<MR>> result = new LinkedHashSet<LexicalEntry<MR>>();
		recursiveGetAllLexicalEntries(result, new HashSet<Cell<MR>>());
		return result;
	}
	
	public Category<MR> getCategory() {
		return category;
	}
	
	public int getEnd() {
		return end;
	}
	
	public Set<RuleName> getGeneratingRules() {
		return Collections.unmodifiableSet(generatingRules);
	}
	
	public boolean getIsMax() {
		return isMax;
	}
	
	public double getLogInsideScore() {
		return logInsideScore;
	}
	
	/**
	 * Recursively drills down to the max children and returns the lexical
	 * entries at the based of the tree. This method doesn't rely on the Chart
	 * structure.
	 * 
	 * @return List of lexical entries
	 */
	public LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntriesRecursively() {
		final LinkedHashSet<LexicalEntry<MR>> result = new LinkedHashSet<LexicalEntry<MR>>();
		recursiveGetMaxLexicalEntries(result, new HashSet<Cell<MR>>());
		return result;
	}
	
	public LinkedHashSet<RuleUsageTriplet> getMaxRulesUsedRecursively() {
		final LinkedHashSet<RuleUsageTriplet> result = new LinkedHashSet<RuleUsageTriplet>();
		recursiveGetMaxRulesUsed(result, new HashSet<Cell<MR>>());
		return result;
	}
	
	public long getNumParses() {
		return numParses;
	}
	
	public long getNumViterbiParses() {
		return numViterbiParses;
	}
	
	public double getPruneScore() {
		return viterbiScore;
	}
	
	public double getSecondPruneScore() {
		return getPruneScore();
	}
	
	public int getStart() {
		return begin;
	}
	
	public Set<AbstractCKYParseStep<MR>> getSteps() {
		return Collections.unmodifiableSet(steps);
	}
	
	/**
	 * Returns the lexical entries in the current cell (not recursive)
	 * participating in any viterbi parses.
	 * 
	 * @return
	 */
	public Set<LexicalEntry<MR>> getViterbiLexicalEntries() {
		final Set<LexicalEntry<MR>> entries = new HashSet<LexicalEntry<MR>>();
		for (final AbstractCKYParseStep<MR> step : viterbiSteps) {
			if (step instanceof CKYLexicalStep) {
				entries.add(((CKYLexicalStep<MR>) step).getLexicalEntry());
			}
		}
		return entries;
		
	}
	
	/**
	 * Linear viterbi score.
	 * 
	 * @return
	 */
	public double getViterbiScore() {
		return viterbiScore;
	}
	
	public Set<AbstractCKYParseStep<MR>> getViterbiSteps() {
		return Collections.unmodifiableSet(viterbiSteps);
	}
	
	@Override
	public int hashCode() {
		if (!hashCodeCalculated) {
			hashCodeCache = calcHashCode();
			hashCodeCalculated = true;
		}
		return hashCodeCache;
	}
	
	public boolean hasLexicalMaxStep() {
		for (final AbstractCKYParseStep<MR> step : viterbiSteps) {
			if (step instanceof ILexicalParseStep) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasLexicalStep() {
		for (final AbstractCKYParseStep<MR> step : steps) {
			if (step instanceof ILexicalParseStep) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isCompleteSpan() {
		return isCompleteSpan;
	}
	
	public boolean isFullParse() {
		return isFullParse;
	}
	
	/**
	 * The number of parsing steps leading to this cell.
	 */
	public int numSteps() {
		return steps.size();
	}
	
	@Override
	public String toString() {
		return toString(false, null);
	}
	
	public String toString(boolean recursive, String tokens) {
		final StringBuffer result = new StringBuffer();
		result.append("[");
		result.append(begin).append("-").append(end).append(" : ")
				.append(tokens == null ? "" : tokens)
				.append(tokens == null ? "" : " :- ").append(category)
				.append(" : ").append("prune=").append(getPruneScore())
				.append(" : ").append("numParses=").append(numParses)
				.append(" : ").append("numViterbiParses=")
				.append(numViterbiParses).append(" : ").append("hash=")
				.append(hashCode()).append(" : ").append(steps.size())
				.append(" : ").append(viterbiScore).append(" : ");
		
		final Iterator<AbstractCKYParseStep<MR>> iterator = viterbiSteps
				.iterator();
		result.append("[");
		while (iterator.hasNext()) {
			result.append("[").append(iterator.next().toString(recursive))
					.append("]");
			if (iterator.hasNext()) {
				result.append(", ");
			}
		}
		result.append("]");
		
		result.append("]");
		
		return result.toString();
	}
	
	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + begin;
		result = prime * result
				+ ((category == null) ? 0 : category.hashCode());
		result = prime * result + end;
		return result;
	}
	
	/**
	 * @param cache
	 * @return
	 */
	private IHashVector computeMaxAvgFeaturesRecursively(
			Map<Cell<MR>, IHashVector> cache) {
		final IHashVector result = HashVectorFactory.create();
		int numSubTrees = 0;
		
		// Check if already visited, if so - return cached
		final IHashVector cached = cache.get(this);
		if (cached != null) {
			return cached;
		}
		
		// Iterate over derivation steps to compute features (this includes both
		// lexical and non-lexical steps)
		for (final AbstractCKYParseStep<MR> derivationStep : viterbiSteps) {
			// Get the features from the children
			for (final Cell<MR> child : derivationStep) {
				child.computeMaxAvgFeaturesRecursively(cache).addTimesInto(1.0,
						result);
			}
			// Parsing feature values
			derivationStep.getLocalFeatures().addTimesInto(1.0, result);
			++numSubTrees;
		}
		
		// Average -- all viterbi steps have the same weight, so can get the
		// mean
		if (numSubTrees > 1) {
			// Case we have more than 1 sub trees, do simple average
			result.divideBy(numSubTrees);
		}
		
		// Cache the result
		cache.put(this, result);
		
		return result;
	}
	
	/**
	 * @param result
	 * @return
	 * @see Cell#getAllLexicalEntriesRecursively()
	 */
	@SuppressWarnings("unchecked")
	private void recursiveGetAllLexicalEntries(
			LinkedHashSet<LexicalEntry<MR>> result, Set<Cell<MR>> visited) {
		if (visited.contains(this)) {
			// no need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final AbstractCKYParseStep<MR> derivationStep : steps) {
				if (derivationStep instanceof ILexicalParseStep) {
					result.add(((ILexicalParseStep<MR>) derivationStep)
							.getLexicalEntry());
				}
				for (final Cell<MR> child : derivationStep) {
					child.recursiveGetAllLexicalEntries(result, visited);
				}
			}
			visited.add(this);
		}
	}
	
	/**
	 * @param result
	 * @return
	 * @see Cell#getMaxLexicalEntriesRecursively()
	 */
	private void recursiveGetMaxLexicalEntries(
			LinkedHashSet<LexicalEntry<MR>> result, Set<Cell<MR>> visited) {
		if (visited.contains(this)) {
			// No need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final AbstractCKYParseStep<MR> derivationStep : viterbiSteps) {
				if (derivationStep instanceof CKYLexicalStep) {
					result.add(((CKYLexicalStep<MR>) derivationStep)
							.getLexicalEntry());
				}
				for (final Cell<MR> child : derivationStep) {
					child.recursiveGetMaxLexicalEntries(result, visited);
				}
			}
			visited.add(this);
		}
	}
	
	private void recursiveGetMaxRulesUsed(
			LinkedHashSet<RuleUsageTriplet> result, HashSet<Cell<MR>> visited) {
		if (visited.contains(this)) {
			// no need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final AbstractCKYParseStep<MR> derivationStep : viterbiSteps) {
				final List<Pair<Integer, Integer>> children = new ArrayList<Pair<Integer, Integer>>();
				for (final Cell<MR> child : derivationStep) {
					child.recursiveGetMaxRulesUsed(result, visited);
					children.add(Pair.of(child.getStart(), child.getEnd()));
				}
				result.add(new RuleUsageTriplet(derivationStep.getRuleName(),
						children));
			}
			visited.add(this);
		}
	}
	
	/**
	 * Update the cell scores (viterbi and inside scores) given a new derivation
	 * step. If necessary, the list of viterbi steps is also updated.
	 */
	private boolean updateScores(AbstractCKYParseStep<MR> derivationStep) {
		// Given the cells participating in the parse step (as children),
		// compute the viterbi score of the step, the number of parses it
		// represents and the value to add to the cell's inside score
		double stepViterbiScore = derivationStep.getLocalScore();
		long numParsesInStep = 1;
		double logAddToInsideScore = derivationStep.getLocalScore();
		for (final Cell<MR> child : derivationStep) {
			logAddToInsideScore += child.getLogInsideScore();
			stepViterbiScore += child.getViterbiScore();
			numParsesInStep *= child.numParses;
		}
		logInsideScore = LogSumExp.of(logInsideScore, logAddToInsideScore);
		
		// Update the total number of parses.
		numParses += numParsesInStep;
		
		if (stepViterbiScore == viterbiScore) {
			if (viterbiSteps.add(derivationStep)) {
				numViterbiParses += numParsesInStep;
				return true;
			} else {
				return false;
			}
		} else if (stepViterbiScore > viterbiScore) {
			viterbiScore = stepViterbiScore;
			viterbiSteps.clear();
			viterbiSteps.add(derivationStep);
			numViterbiParses = numParsesInStep;
			return true;
		}
		
		// Case step not added to max children.
		return false;
	}
	
	/**
	 * Update the cell's log expected feature values to the given hash vector.
	 * Assumes outside and inside scores computed.
	 */
	void collectLogExpectedFeatures(final IHashVector expectedFeatures) {
		// Iterate over all derivations steps (incl. both lexical and
		// non-lexical steps)
		if (logOutsideScore != Double.NEGATIVE_INFINITY) {
			for (final AbstractCKYParseStep<MR> step : steps) {
				// Accumulate the weight for using this parse step: the outside
				// of the root, the inside of each child and the local score
				// associated with the current step.
				double logWeight = logOutsideScore + step.getLocalScore();
				for (final Cell<MR> child : step) {
					logWeight += child.logInsideScore;
				}
				// Update the weighted values of the local features into the
				// result vector.
				HashVectorUtils.logSumExpAdd(logWeight,
						step.getLocalFeatures(), expectedFeatures);
			}
		}
	}
	
	/**
	 * Init outside probability with a constraining semantic filter.
	 */
	void initializeLogOutsideProbabilities(IScorer<MR> initialScorer) {
		if (isFullParse()) {
			logOutsideScore = initialScorer.score(category.getSem());
		} else {
			logOutsideScore = Double.NEGATIVE_INFINITY;
		}
	}
	
	/**
	 * Propagate the isMax flag to max children that were created from a binary
	 * parse rule. This method is not recursive and relies on the Chart to cycle
	 * through the cells in a top down manner.
	 */
	void propMaxNonUnary() {
		// Iterate over all max children
		for (final AbstractCKYParseStep<MR> derivationStep : viterbiSteps) {
			if (!derivationStep.isUnary()) {
				// Mark both children as participating in the max parse
				for (final Cell<MR> child : derivationStep) {
					child.setIsMax(true);
				}
			}
		}
	}
	
	/**
	 * Propagate the isMax flag to max children that were created from an unary
	 * parse rule. This method is not recursive and relies on the Chart to cycle
	 * through the cells in a top down manner.
	 */
	void propMaxUnary() {
		// Iterate over all max children
		for (final AbstractCKYParseStep<MR> derivationStep : viterbiSteps) {
			if (derivationStep.isUnary()) {
				// Mark the unary child as participating in the max parse
				for (final Cell<MR> child : derivationStep) {
					child.setIsMax(true);
				}
			}
		}
	}
	
	void setIsMax(boolean isMax) {
		this.isMax = isMax;
	}
	
	/**
	 * Compute the contribution of the current cell to the log outside score of
	 * its children, in all binary production for which it's the root.
	 */
	void updateBinaryChildrenLogOutsideScore() {
		if (logOutsideScore != Double.NEGATIVE_INFINITY) {
			// Iterate through all derivation steps: all ways of producing this
			// cell
			for (final AbstractCKYParseStep<MR> derivationStep : steps) {
				// Only process binary derivations steps
				if (derivationStep.numChildren() == 2) {
					final double logScore = derivationStep.getLocalScore();
					final Cell<MR> child1 = derivationStep.getChildCell(0);
					final Cell<MR> child2 = derivationStep.getChildCell(1);
					child1.logOutsideScore = LogSumExp.of(
							child1.logOutsideScore,
							logOutsideScore + child2.getLogInsideScore()
									+ logScore);
					child2.logOutsideScore = LogSumExp.of(
							child2.logOutsideScore,
							logOutsideScore + child1.getLogInsideScore()
									+ logScore);
				}
			}
		}
	}
	
	/**
	 * Compute the contribution of the current cell to the log outside score of
	 * its children, in all unary production for which it's the root.
	 */
	void updateUnaryChildrenLogOutsideScore() {
		// Iterate through all derivation steps: all ways of producing this cell
		for (final AbstractCKYParseStep<MR> derivationStep : steps) {
			// Only process unary steps
			if (derivationStep.numChildren() == 1) {
				// The unary case of outside score is a bit tricky. It becomes
				// clear when considering first principles: the outside score is
				// the sum of potentials for all outside trees with a given
				// non-terminal for a given span. For the unary case, there are
				// no siblings, so no need to take any inside score into
				// account, unlike the binary case.
				derivationStep.getChildCell(0).logOutsideScore = LogSumExp.of(
						derivationStep.getChildCell(0).logOutsideScore,
						logOutsideScore + derivationStep.getLocalScore());
			}
		}
	}
	
	public static class ScoreComparator<MR> implements Comparator<Cell<MR>> {
		
		@Override
		public int compare(Cell<MR> o1, Cell<MR> o2) {
			final int scoreComparison = Double.compare(o1.getPruneScore(),
					o2.getPruneScore());
			if (scoreComparison == 0) {
				return Double.compare(o1.getSecondPruneScore(),
						o2.getSecondPruneScore());
			} else {
				return scoreComparison;
			}
		}
		
	}
}
