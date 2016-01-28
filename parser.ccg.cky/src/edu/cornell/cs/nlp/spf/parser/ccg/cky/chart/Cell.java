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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorUtils;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.parser.RuleUsageTriplet;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.IWeightedCKYStep;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IArrayRuleNameSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.Span;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.math.LogSumExp;

/**
 * A single {@link Chart} cell of a specific span with specific syntax and
 * semantic data.
 *
 * @author Yoav Artzi
 */
public class Cell<MR> implements IArrayRuleNameSet {
	public static final ILogger				LOG					= LoggerFactory
																		.create(Cell.class);

	private final Category<MR>				category;

	/** The end index of the span of the input string covered by this cell. */
	private final int						end;

	/**
	 * Cached set of rules used to generate this cell. Every time the set is
	 * requested it's computed, or the cached version is saved. It's set to null
	 * every time the cell changes.
	 */
	private RuleName[]						generatingRules		= null;

	/**
	 * Immutable cache for the hashing code. This field is for internal use
	 * only! It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private final int						hashCodeCache;

	/**
	 * A flag that the {@link Chart) can set to indicate this cell spans the
	 * entire sentence, even if it's not a full parse.
	 */
	private final boolean					isCompleteSpan;

	/**
	 * Flag to indicate if this cell represents a complete parse.
	 */
	private final boolean					isFullParse;

	private boolean							isMax;

	private double							logInsideScore		= Double.NEGATIVE_INFINITY;

	/**
	 * Log outside score.
	 */
	private double							logOutsideScore		= Double.NEGATIVE_INFINITY;

	private int								numViterbiSteps		= 0;

	/** The starting index of the span of the input string covered by this cell. */
	private final int						start;

	/**
	 * Lists of derivation steps that created this cell
	 */
	private final Set<IWeightedCKYStep<MR>>	steps				= new HashSet<IWeightedCKYStep<MR>>();

	/**
	 * Linear viterbi score.
	 */
	private double							viterbiScore		= -Double.MAX_VALUE;

	/**
	 * Number of (partial) parses with this cell at their root.
	 */
	protected long							numParses			= 0;

	/**
	 * Number of (partial) viterbi parses with this cell at their root.
	 */
	protected long							numViterbiParses	= 0;

	/**
	 * Derivation steps that have the {@link #viterbiScore}. This list is
	 * calculated only when needed and is reseted whenever the cell changes. In
	 * practice, since it's calculated from the {@link #steps} set, it's
	 * actually a set.
	 */
	protected List<IWeightedCKYStep<MR>>	viterbiSteps		= null;

	protected Cell(IWeightedCKYStep<MR> parseStep, boolean isCompleteSpan) {
		this.isCompleteSpan = isCompleteSpan;
		this.isFullParse = parseStep.isFullParse();
		this.category = parseStep.getRoot();
		this.start = parseStep.getStart();
		this.end = parseStep.getEnd();
		this.steps.add(parseStep);
		updateScores(parseStep);
		this.hashCodeCache = calcHashCode();
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
		for (final IWeightedCKYStep<MR> derivationStep : other.steps) {
			assert derivationStep.getStart() == start
					&& derivationStep.getEnd() == end;
			if (steps.add(derivationStep)) {
				// Reset the cached set of generating rules and viterbi steps.
				viterbiSteps = null;
				generatingRules = null;
				addedToMaxChildren = updateScores(derivationStep)
						|| addedToMaxChildren;
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
		if (start != other.start) {
			return false;
		}
		if (!category.equals(other.category)) {
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
		recursiveGetLexicalEntries(result, new HashSet<Cell<MR>>(), false);
		return result;
	}

	/**
	 * Get all parse steps participating in trees leading to this cell.
	 */
	public Set<IWeightedCKYStep<MR>> getAllSteps() {
		final LinkedHashSet<IWeightedCKYStep<MR>> result = new LinkedHashSet<IWeightedCKYStep<MR>>();
		recursiveGetParseSteps(result, new HashSet<Cell<MR>>(), false);
		return result;
	}

	public Category<MR> getCategory() {
		return category;
	}

	public int getEnd() {
		return end;
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
		recursiveGetLexicalEntries(result, new HashSet<Cell<MR>>(), true);
		return result;
	}

	public LinkedHashSet<RuleUsageTriplet> getMaxRulesUsedRecursively() {
		final LinkedHashSet<RuleUsageTriplet> result = new LinkedHashSet<RuleUsageTriplet>();
		recursiveGetMaxRulesUsed(result, new HashSet<Cell<MR>>());
		return result;
	}

	/**
	 * Get all viterbi parse steps participating in trees leading to this cell.
	 */
	public LinkedHashSet<IWeightedCKYStep<MR>> getMaxSteps() {
		final LinkedHashSet<IWeightedCKYStep<MR>> result = new LinkedHashSet<IWeightedCKYStep<MR>>();
		recursiveGetParseSteps(result, new HashSet<Cell<MR>>(), true);
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

	@Override
	public RuleName getRuleName(int index) {
		if (generatingRules == null) {
			createGeneratingRules();
		}
		return generatingRules[index];
	}

	public double getSecondPruneScore() {
		return getPruneScore();
	}

	public int getStart() {
		return start;
	}

	public Set<IWeightedCKYStep<MR>> getSteps() {
		return Collections.unmodifiableSet(steps);
	}

	/**
	 * Returns the lexical entries in the current cell (not recursive)
	 * participating in any viterbi parses.
	 */
	@SuppressWarnings("unchecked")
	public Set<LexicalEntry<MR>> getViterbiLexicalEntries() {
		final Set<LexicalEntry<MR>> entries = new HashSet<LexicalEntry<MR>>();
		for (final IWeightedCKYStep<MR> step : getViterbiSteps()) {
			if (step instanceof ILexicalParseStep) {
				entries.add(((ILexicalParseStep<MR>) step).getLexicalEntry());
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

	public List<IWeightedCKYStep<MR>> getViterbiSteps() {
		if (viterbiSteps == null) {
			computeViterbiSteps();
		}
		return Collections.unmodifiableList(viterbiSteps);
	}

	@Override
	public int hashCode() {
		return hashCodeCache;
	}

	public boolean hasLexicalMaxStep() {
		for (final IWeightedCKYStep<MR> step : getViterbiSteps()) {
			if (step instanceof ILexicalParseStep) {
				return true;
			}
		}
		return false;
	}

	public boolean hasLexicalStep() {
		for (final IWeightedCKYStep<MR> step : steps) {
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

	public boolean isMax() {
		return isMax;
	}

	@Override
	public int numRuleNames() {
		if (generatingRules == null) {
			createGeneratingRules();
		}
		return generatingRules.length;
	}

	/**
	 * The number of parsing steps leading to this cell.
	 */
	public int numSteps() {
		return steps.size();
	}

	@Override
	public String toString() {
		return toString(false, null, true, null);
	}

	public String toString(boolean recursive, String tokens, boolean viterbi,
			IHashVectorImmutable theta) {
		final StringBuffer result = new StringBuffer();
		result.append("[");
		result.append(start)
				.append("-")
				.append(end)
				.append(" : ")
				.append(tokens == null ? "" : tokens)
				.append(tokens == null ? "" : " :- ")
				.append(category)
				.append(" : ")
				.append("prune=")
				.append(getPruneScore() == getSecondPruneScore() ? getPruneScore()
						: String.format("(%f,%f)", getPruneScore(),
								getSecondPruneScore())).append(" : ")
				.append("numParses=").append(numParses).append(" : ")
				.append("numViterbiParses=").append(numViterbiParses)
				.append(" : ").append("hash=").append(hashCode()).append(" : ")
				.append(steps.size()).append(" : ").append(viterbiScore)
				.append(" : ");

		// Print the steps that created this cell.
		result.append("[");
		if (viterbi) {
			// Case only viterbi steps.
			final Iterator<IWeightedCKYStep<MR>> iterator = getViterbiSteps()
					.iterator();
			while (iterator.hasNext()) {
				result.append(iterator.next().toString(true, recursive, theta));
				if (iterator.hasNext()) {
					result.append(", ");
				}
			}
		} else {
			// Case all steps.
			final Iterator<IWeightedCKYStep<MR>> iterator = steps.iterator();
			while (iterator.hasNext()) {
				final IWeightedCKYStep<MR> step = iterator.next();
				if (getViterbiSteps().contains(step)) {
					result.append("*");
				}
				result.append(step.toString(true, recursive, theta));
				if (iterator.hasNext()) {
					result.append(", ");
				}
			}
		}
		result.append("]");

		result.append("]");

		return result.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + start;
		result = prime * result + (category == null ? 0 : category.hashCode());
		result = prime * result + end;
		return result;
	}

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
		for (final IWeightedCKYStep<MR> derivationStep : getViterbiSteps()) {
			// Get the features from the children
			for (final Cell<MR> child : derivationStep) {
				child.computeMaxAvgFeaturesRecursively(cache).addTimesInto(1.0,
						result);
			}
			// Parsing feature values
			derivationStep.getStepFeatures().addTimesInto(1.0, result);
			++numSubTrees;
		}

		// Average -- all viterbi steps have the same weight, so can get the
		// mean.
		if (numSubTrees > 1) {
			// Case we have more than 1 sub trees, do simple average
			result.divideBy(numSubTrees);
		}

		// Cache the result
		cache.put(this, result);

		return result;
	}

	private void computeViterbiSteps() {
		@SuppressWarnings("unchecked")
		final IWeightedCKYStep<MR>[] stepsArray = (IWeightedCKYStep<MR>[]) Array
				.newInstance(IWeightedCKYStep.class, numViterbiSteps);
		int index = 0;
		for (final IWeightedCKYStep<MR> step : steps) {
			// Compute the step's viterbi score.
			double stepViterbiScore = step.getStepScore();
			for (final Cell<MR> child : step) {
				stepViterbiScore += child.getViterbiScore();
			}
			assert stepViterbiScore <= viterbiScore;
			if (stepViterbiScore == viterbiScore) {
				stepsArray[index++] = step;
			}
		}
		assert index == numViterbiSteps;
		this.viterbiSteps = Arrays.asList(stepsArray);
	}

	private void createGeneratingRules() {
		final Set<RuleName> set = new HashSet<RuleName>();
		for (final IWeightedCKYStep<MR> step : steps) {
			set.add(step.getRuleName());
		}
		generatingRules = set.toArray(new RuleName[set.size()]);
	}

	/**
	 * @see Cell#getMaxLexicalEntriesRecursively()
	 * @see Cell#getAllLexicalEntriesRecursively()
	 */
	@SuppressWarnings("unchecked")
	private void recursiveGetLexicalEntries(
			LinkedHashSet<LexicalEntry<MR>> result, Set<Cell<MR>> visited,
			boolean viterbiOnly) {
		if (visited.contains(this)) {
			// No need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final IWeightedCKYStep<MR> derivationStep : viterbiOnly ? getViterbiSteps()
					: steps) {
				if (derivationStep instanceof ILexicalParseStep) {
					result.add(((ILexicalParseStep<MR>) derivationStep)
							.getLexicalEntry());
				}
				for (final Cell<MR> child : derivationStep) {
					child.recursiveGetLexicalEntries(result, visited,
							viterbiOnly);
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
			for (final IWeightedCKYStep<MR> derivationStep : getViterbiSteps()) {
				final List<Pair<Integer, Integer>> children = new ArrayList<Pair<Integer, Integer>>();
				for (final Cell<MR> child : derivationStep) {
					child.recursiveGetMaxRulesUsed(result, visited);
					children.add(Pair.of(child.getStart(), child.getEnd()));
				}
				if (children.isEmpty()) {
					// Case no children, usually for lexical rules, simply
					// assign the span (as for unary rules).
					children.add(Pair.of(start, end));
				}
				result.add(new RuleUsageTriplet(derivationStep.getRuleName(),
						children));
			}
			visited.add(this);
		}
	}

	/**
	 * @see #getAllSteps()
	 */
	private void recursiveGetParseSteps(
			LinkedHashSet<IWeightedCKYStep<MR>> result,
			HashSet<Cell<MR>> visited, boolean viterbiOnly) {
		if (visited.contains(this)) {
			// no need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final IWeightedCKYStep<MR> derivationStep : viterbiOnly ? getViterbiSteps()
					: steps) {
				for (final Cell<MR> child : derivationStep) {
					child.recursiveGetParseSteps(result, visited, viterbiOnly);
				}
			}
			result.addAll(viterbiOnly ? getViterbiSteps() : steps);
			visited.add(this);
		}
	}

	/**
	 * Update the cell scores (viterbi and inside scores) given a new derivation
	 * step. If necessary, the list of viterbi steps is also updated.
	 */
	private boolean updateScores(IWeightedCKYStep<MR> derivationStep) {
		// Given the cells participating in the parse step (as children),
		// compute the viterbi score of the step, the number of parses it
		// represents and the value to add to the cell's inside score.
		LOG.debug("Updating score from: %s", derivationStep);
		double stepViterbiScore = derivationStep.getStepScore();
		LOG.debug("Step local score: %f", stepViterbiScore);
		long numParsesInStep = 1;
		long numViterbiParsesInStep = 1;
		double logAddToInsideScore = derivationStep.getStepScore();
		for (final Cell<MR> child : derivationStep) {
			logAddToInsideScore += child.getLogInsideScore();
			stepViterbiScore += child.getViterbiScore();
			numParsesInStep *= child.numParses;
			numViterbiParsesInStep *= child.numViterbiParses;
		}
		LOG.debug("Step viterbi score: %f", stepViterbiScore);
		LOG.debug("Step contribution to inside score: %f", logAddToInsideScore);
		LOG.debug("# parses in step: %d", numParsesInStep);
		LOG.debug("# viterbi parses in step: %d", numViterbiParsesInStep);
		logInsideScore = LogSumExp.of(logInsideScore, logAddToInsideScore);

		// Update the total number of parses.
		numParses += numParsesInStep;

		if (stepViterbiScore == viterbiScore) {
			LOG.debug("Step is a viterbi step");
			numViterbiParses += numViterbiParsesInStep;
			numViterbiSteps += 1;
			return true;
		} else if (stepViterbiScore > viterbiScore) {
			LOG.debug("Step re-set viterbi score, step is a viterbi step");
			viterbiScore = stepViterbiScore;
			numViterbiParses = numViterbiParsesInStep;
			numViterbiSteps = 1;
			return true;
		}

		// Case step not a viterbi step.
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
			for (final IWeightedCKYStep<MR> step : steps) {
				// Accumulate the weight for using this parse step: the outside
				// of the root, the inside of each child and the local score
				// associated with the current step.
				double logWeight = logOutsideScore + step.getStepScore();
				for (final Cell<MR> child : step) {
					logWeight += child.logInsideScore;
				}
				// Update the weighted values of the local features into the
				// result vector.
				HashVectorUtils.logSumExpAdd(logWeight, step.getStepFeatures(),
						expectedFeatures);
			}
		}
	}

	/**
	 * Init outside probability with a constraining semantic filter.
	 */
	void initializeLogOutsideProbabilities(
			Function<Category<MR>, Double> initialScorer, Span span) {
		if (span.getStart() == start && span.getEnd() == end) {
			logOutsideScore = initialScorer.apply(category);
		} else {
			logOutsideScore = Double.NEGATIVE_INFINITY;
		}
	}

	/**
	 * Recompute cell scores. Doesn't update the local scores of steps, but
	 * propagates modifications of the chart.
	 */
	void recomputeScores() {
		numParses = 0;
		numViterbiParses = 0;
		viterbiScore = -Double.MAX_VALUE;
		logInsideScore = Double.NEGATIVE_INFINITY;
		viterbiSteps = null;
		generatingRules = null;
		for (final IWeightedCKYStep<MR> step : steps) {
			updateScores(step);
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
			for (final IWeightedCKYStep<MR> derivationStep : steps) {
				// Only process binary derivations steps
				if (derivationStep.numChildren() == 2) {
					final double logScore = derivationStep.getStepScore();
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
		for (final IWeightedCKYStep<MR> derivationStep : steps) {
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
						logOutsideScore + derivationStep.getStepScore());
			}
		}
	}

	public static class ScoreComparator<MR> implements Comparator<Cell<MR>>,
			Serializable {

		private static final long	serialVersionUID	= 5348011347391634770L;

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
