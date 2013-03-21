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

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;

/**
 * A single chart cell of a specific span with specific syntax and semantic
 * data.
 * 
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 * @author Tom Kwiatkowski
 * @see Chart
 */
public class Cell<Y> {
	
	/** The starting index of the span of the input string covered by this cell. */
	private final int					begin;
	
	private final Category<Y>			category;
	
	/** The end index of the span of the input string covered by this cell. */
	private final int					end;
	
	/**
	 * Mutable cache for the hashing code. This field is for internal use only!
	 * It mustn't be used when copying/comparing/storing/etc. the object.
	 */
	private int							hashCodeCache;
	
	/**
	 * Mutable flag to indicate if the hash code cache is populated. This field
	 * is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object.
	 */
	private boolean						hashCodeCalculated	= false;
	
	/**
	 * Inside score (exponentiated).
	 */
	private double						insideScore			= 0;
	
	/**
	 * A flag that the {@link Chart) can set to indicate this cell spans the
	 * entire sentence, even if it's not a full parse.
	 */
	private final boolean				isCompleteSpan;
	
	/**
	 * Flag to indicate if this cell represents a complete parse.
	 */
	private final boolean				isFullParse;
	
	private boolean						isMax;
	
	/**
	 * Outside score
	 */
	private double						outsideScore		= 0;
	
	/**
	 * A flag to be used as a safety measure to {@link #score(IModelImmutable)}.
	 * This field is for internal use only! It mustn't be used when
	 * copying/comparing/storing/etc. the object. Don't use it for anything
	 * else. Just don't touch it.
	 */
	private boolean						scored				= false;
	
	/**
	 * Lists of derivation steps that created this cell
	 */
	private final Set<DerivationStep>	steps				= new HashSet<DerivationStep>();
	
	/**
	 * Linear viterbi score.
	 */
	private double						viterbiScore		= -Double.MAX_VALUE;
	
	protected int						numParses			= 0;
	
	/**
	 * Derivation steps that have the {@link #viterbiScore}.
	 */
	protected final Set<DerivationStep>	viterbiSteps		= new HashSet<DerivationStep>();
	
	protected Cell(Category<Y> category, String ruleName, Cell<Y> child,
			boolean isCompleteSpan, boolean isFullParse) {
		this.isCompleteSpan = isCompleteSpan;
		this.isFullParse = isFullParse;
		this.category = category;
		this.begin = child.getStart();
		this.end = child.getEnd();
		this.steps.add(new DerivationStep(child, ruleName));
	}
	
	protected Cell(Category<Y> category, String ruleName, Cell<Y> leftChild,
			Cell<Y> rightChild, boolean isCompleteSpan, boolean isFullParse) {
		this.isCompleteSpan = isCompleteSpan;
		this.isFullParse = isFullParse;
		this.category = category;
		this.begin = leftChild.getStart();
		this.end = rightChild.getEnd();
		this.steps.add(new DerivationStep(leftChild, rightChild, ruleName));
	}
	
	protected Cell(LexicalEntry<Y> lexicalEntry, int begin, int end,
			boolean isCompleteSpan, boolean isFullParse) {
		this.isCompleteSpan = isCompleteSpan;
		this.isFullParse = isFullParse;
		this.category = lexicalEntry.getCategory();
		this.begin = begin;
		this.end = end;
		this.steps.add(new LexicalDerivationStep(lexicalEntry));
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
	public boolean addCell(Cell<Y> other, IDataItemModel<Y> model) {
		// Iterate over the added children and add them to steps list and inside
		// score
		boolean addedToMaxChildren = false;
		for (final DerivationStep derivationStep : other.steps) {
			if (steps.add(derivationStep)) {
				addedToMaxChildren |= updateInsideScore(derivationStep, model);
			}
		}
		return addedToMaxChildren;
	}
	
	/**
	 * Recursively compute the mean (linear) viterbi feature vector.
	 * 
	 * @param model
	 *            Features to use
	 */
	public IHashVector computeMaxAvgFeaturesRecursively(IDataItemModel<Y> model) {
		return computeMaxAvgFeaturesRecursively(model,
				new HashMap<Cell<Y>, IHashVector>());
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
	public LinkedHashSet<LexicalEntry<Y>> getAllLexicalEntriesRecursively() {
		final LinkedHashSet<LexicalEntry<Y>> result = new LinkedHashSet<LexicalEntry<Y>>();
		recursiveGetAllLexicalEntries(result, new HashSet<Cell<Y>>());
		return result;
	}
	
	public Category<Y> getCategroy() {
		return category;
	}
	
	public int getEnd() {
		return end;
	}
	
	public boolean getIsMax() {
		return isMax;
	}
	
	/**
	 * Recursively drills down to the max children and returns the lexical
	 * entries at the based of the tree. This method doesn't rely on the Chart
	 * structure.
	 * 
	 * @return List of lexical entries
	 */
	public LinkedHashSet<LexicalEntry<Y>> getMaxLexicalEntriesRecursively() {
		final LinkedHashSet<LexicalEntry<Y>> result = new LinkedHashSet<LexicalEntry<Y>>();
		recursiveGetMaxLexicalEntries(result, new HashSet<Cell<Y>>());
		return result;
	}
	
	public LinkedHashSet<RuleUsageTriplet> getMaxRulesUsedRecursively() {
		final LinkedHashSet<RuleUsageTriplet> result = new LinkedHashSet<RuleUsageTriplet>();
		recursiveGetMaxRulesUsed(result, new HashSet<Cell<Y>>());
		return result;
	}
	
	public int getNumParses() {
		return numParses;
	}
	
	public double getOutsideScore() {
		return outsideScore;
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
	
	/**
	 * Returns the lexical entries in the current cell (not recursive)
	 * participating in any viterbi parses.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Set<LexicalEntry<Y>> getViterbiLexicalEntries() {
		final Set<LexicalEntry<Y>> entries = new HashSet<LexicalEntry<Y>>();
		for (final DerivationStep step : viterbiSteps) {
			if (step instanceof Cell.LexicalDerivationStep) {
				entries.add(((LexicalDerivationStep) step).getLexicalEntry());
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
	
	@Override
	public int hashCode() {
		if (!hashCodeCalculated) {
			hashCodeCache = calcHashCode();
			hashCodeCalculated = true;
		}
		return hashCodeCache;
	}
	
	public boolean hasLexicalMaxStep() {
		for (final DerivationStep step : viterbiSteps) {
			if (step instanceof ILexicalParseStep) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasLexicalStep() {
		for (final DerivationStep step : steps) {
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
				.append(" : ").append("hash=").append(hashCode()).append(" : ")
				.append(steps.size()).append(" : ").append(viterbiScore)
				.append(" : ");
		
		final Iterator<DerivationStep> iterator = viterbiSteps.iterator();
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
	 * @see #computeMaxAvgFeaturesRecursively(IDataItemModel)
	 * @param model
	 * @param cache
	 * @return
	 */
	private IHashVector computeMaxAvgFeaturesRecursively(
			IDataItemModel<Y> model, Map<Cell<Y>, IHashVector> cache) {
		final IHashVector result = HashVectorFactory.create();
		int numSubTrees = 0;
		
		final IHashVector cached = cache.get(this);
		if (cached != null) {
			return cached;
		}
		
		// Iterate over derivation steps to compute features (this includes both
		// lexical and non-lexical steps)
		for (final DerivationStep derivationStep : viterbiSteps) {
			// Get the features from the children
			for (final Cell<Y> child : derivationStep) {
				child.computeMaxAvgFeaturesRecursively(model, cache)
						.addTimesInto(1.0, result);
			}
			// Parsing feature values
			model.computeFeatures(derivationStep, result);
			++numSubTrees;
		}
		
		// Average
		if (numSubTrees > 1) {
			// Case we have more than 1 sub trees, do simple average
			result.divideBy(numSubTrees);
		}
		
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
			LinkedHashSet<LexicalEntry<Y>> result, Set<Cell<Y>> visited) {
		if (visited.contains(this)) {
			// no need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final DerivationStep derivationStep : steps) {
				if (derivationStep instanceof Cell.LexicalDerivationStep) {
					result.add(((LexicalDerivationStep) derivationStep)
							.getLexicalEntry());
				}
				for (final Cell<Y> child : derivationStep) {
					child.recursiveGetMaxLexicalEntries(result, visited);
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
	@SuppressWarnings("unchecked")
	private void recursiveGetMaxLexicalEntries(
			LinkedHashSet<LexicalEntry<Y>> result, Set<Cell<Y>> visited) {
		if (visited.contains(this)) {
			// no need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final DerivationStep derivationStep : viterbiSteps) {
				if (derivationStep instanceof Cell.LexicalDerivationStep) {
					result.add(((LexicalDerivationStep) derivationStep)
							.getLexicalEntry());
				}
				for (final Cell<Y> child : derivationStep) {
					child.recursiveGetMaxLexicalEntries(result, visited);
				}
			}
			visited.add(this);
		}
	}
	
	private void recursiveGetMaxRulesUsed(
			LinkedHashSet<RuleUsageTriplet> result, HashSet<Cell<Y>> visited) {
		if (visited.contains(this)) {
			// no need to go further, we have already found the items in a
			// previous recursive call
			return;
		} else {
			for (final DerivationStep derivationStep : viterbiSteps) {
				if (!(derivationStep instanceof Cell.LexicalDerivationStep)) {
					final List<Pair<Integer, Integer>> children = new ArrayList<Pair<Integer, Integer>>(
							2);
					for (final Cell<Y> child : derivationStep) {
						child.recursiveGetMaxRulesUsed(result, visited);
						children.add(Pair.of(child.getStart(), child.getEnd()));
					}
					result.add(new RuleUsageTriplet(derivationStep
							.getRuleName(), children));
				}
			}
			visited.add(this);
		}
	}
	
	/**
	 * Add the given derivation step to the inside score. If necessary, add to
	 * the list of viterbi steps and/or update the viterbi score.
	 * 
	 * @param derivationStep
	 * @param model
	 */
	protected boolean updateInsideScore(DerivationStep derivationStep,
			IDataItemModel<Y> model) {
		// Score the derivation step using the model
		final double score = model.score(derivationStep);
		
		// Given the cells participating in the parse step (as children),
		// compute the viterbi score of the step, the number of parses it
		// represents and the value to add to the cell's inside score
		double stepViterbiScore = score;
		int numParsesInStep = 1;
		double addToInsideScore = Math.exp(score);
		for (final Cell<Y> child : derivationStep) {
			addToInsideScore *= child.getInsideScore();
			stepViterbiScore += child.getViterbiScore();
			numParsesInStep *= child.numParses;
		}
		insideScore += addToInsideScore;
		
		if (stepViterbiScore == viterbiScore) {
			if (viterbiSteps.add(derivationStep)) {
				numParses += numParsesInStep;
				return true;
			} else {
				return false;
			}
		} else if (stepViterbiScore > viterbiScore) {
			viterbiScore = stepViterbiScore;
			viterbiSteps.clear();
			viterbiSteps.add(derivationStep);
			numParses = numParsesInStep;
			return true;
		}
		
		// Case step not added to max children
		return false;
	}
	
	// Assumes that the inside probabilities have already been computed
	void computeOutsideBinary(IDataItemModel<Y> model) {
		if (!steps.isEmpty()) {
			// Iterate through the ways of building this cell
			for (final DerivationStep derivationStep : steps) {
				if (derivationStep.numChildren() == 2) {
					// Case binary parse rule
					final double score = Math.exp(model.score(derivationStep));
					final Cell<Y> child1 = derivationStep.getChildCell(0);
					final Cell<Y> child2 = derivationStep.getChildCell(1);
					child1.outsideScore += outsideScore * child2.insideScore
							* score;
					child2.outsideScore += outsideScore * child1.insideScore
							* score;
				}
			}
		}
	}
	
	// Assumes that the inside probabilities have already been computed
	void computeOutsideUnary(IDataItemModel<Y> model) {
		if (!steps.isEmpty()) {
			// Iterate through the ways of building this cell
			for (final DerivationStep derivationStep : steps) {
				if (derivationStep.numChildren() == 1) {
					// Case unary parse rule
					final double score = Math.exp(model.score(derivationStep));
					final Cell<Y> child = derivationStep.getChildCell(0);
					child.outsideScore += outsideScore * score;
				}
			}
		}
	}
	
	double getInsideScore() {
		return insideScore;
	}
	
	/**
	 * Init outside probability with a constraining semantic category. Outside
	 * probability will be set to 1.0 for every full parse that results in the
	 * given semantic category.
	 * 
	 * @param constrainingCategory
	 *            If null, no constraint on output category will be enforced.
	 */
	void initializeOutsideProbabilities(Category<Y> constrainingCategory) {
		if (isFullParse()
				&& (constrainingCategory == null || category
						.equals(constrainingCategory))) {
			outsideScore = 1.0;
		} else {
			outsideScore = 0.0;
		}
	}
	
	/**
	 * Propagate the isMax flag to max children that were created from a binary
	 * parse rule. This method is not recursive and relies on the Chart to cycle
	 * through the cells in a top down manner.
	 */
	void propMaxNonUnary() {
		// Iterate over all max children
		for (final DerivationStep derivationStep : viterbiSteps) {
			if (!derivationStep.isUnary()) {
				// Mark both children as participating in the max parse
				for (final Cell<Y> child : derivationStep) {
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
		for (final DerivationStep derivationStep : viterbiSteps) {
			if (derivationStep.isUnary()) {
				// Mark the unary child as participating in the max parse
				for (final Cell<Y> child : derivationStep) {
					child.setIsMax(true);
				}
			}
		}
	}
	
	// WARNING: this will not work correctly if you have type shifting rules...
	void recomputeInsideScore(IDataItemModel<Y> model) {
		numParses = 0;
		viterbiSteps.clear();
		// now add in the parsing steps
		for (final DerivationStep derivationStep : steps) {
			updateInsideScore(derivationStep, model);
		}
		
	}
	
	/**
	 * This function should only be called once per cell. It's not done in the
	 * constructor to save on scoring cells that are not added to the chart. It
	 * has a security measure that prevents it from being called more than once.
	 * 
	 * @param model
	 */
	void score(IDataItemModel<Y> model) {
		if (scored) {
			throw new IllegalStateException(
					"Trying to init a cell's score for the second time");
		} else {
			scored = true;
		}
		
		viterbiScore = -Double.MAX_VALUE;
		viterbiSteps.clear();
		numParses = 0;
		for (final DerivationStep derivationStep : steps) {
			updateInsideScore(derivationStep, model);
		}
	}
	
	void setIsMax(boolean isMax) {
		this.isMax = isMax;
	}
	
	void updateExpFeats(IHashVector expFeats, IDataItemModel<Y> model) {
		// Iterate over all derivations steps (incl. both lexical and
		// non-lexical steps)
		if (!steps.isEmpty()) {
			// Iterate through the ways of building this cell
			for (final DerivationStep derivationStep : steps) {
				final IHashVector feats = HashVectorFactory.create();
				model.computeFeatures(derivationStep, feats);
				double pInside = 1.0;
				for (final Cell<Y> child : derivationStep) {
					pInside *= child.insideScore;
				}
				final double prob = outsideScore * pInside
						* Math.exp(model.score(derivationStep));
				feats.addTimesInto(prob, expFeats);
			}
		}
	}
	
	/**
	 * A single derivation step holding the children and the name of the rule
	 * used to combine them. In the case of an unary step, the right child will
	 * be null.
	 * 
	 * @author Yoav Artzi
	 */
	public class DerivationStep implements Iterable<Cell<Y>>, IParseStep<Y> {
		
		private final List<Cell<Y>>	children;
		private final boolean		isUnary;
		private final String		ruleName;
		
		private DerivationStep(Cell<Y> leftChild, Cell<Y> rightChild,
				String ruleName) {
			this.isUnary = rightChild == null;
			List<Cell<Y>> list;
			if (isUnary) {
				list = new ArrayList<Cell<Y>>(1);
				list.add(leftChild);
			} else {
				list = new ArrayList<Cell<Y>>(2);
				list.add(leftChild);
				list.add(rightChild);
			}
			this.children = Collections.unmodifiableList(list);
			this.ruleName = ruleName;
		}
		
		private DerivationStep(Cell<Y> child, String ruleName) {
			this(child, null, ruleName);
		}
		
		private DerivationStep(String ruleName) {
			this.isUnary = false;
			this.ruleName = ruleName;
			this.children = Collections.emptyList();
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
			@SuppressWarnings("unchecked")
			final DerivationStep other = (DerivationStep) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (children == null) {
				if (other.children != null) {
					return false;
				}
			} else if (!children.equals(other.children)) {
				return false;
			}
			if (isUnary != other.isUnary) {
				return false;
			}
			if (ruleName == null) {
				if (other.ruleName != null) {
					return false;
				}
			} else if (!ruleName.equals(other.ruleName)) {
				return false;
			}
			return true;
		}
		
		@Override
		public Category<Y> getChild(int i) {
			return getChildCell(i).getCategroy();
		}
		
		public Cell<Y> getChildCell(int i) {
			return children.get(i);
		}
		
		@Override
		public Category<Y> getRoot() {
			return category;
		}
		
		@Override
		public String getRuleName() {
			return ruleName;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((children == null) ? 0 : children.hashCode());
			result = prime * result + (isUnary ? 1231 : 1237);
			result = prime * result
					+ ((ruleName == null) ? 0 : ruleName.hashCode());
			return result;
		}
		
		@Override
		public boolean isFullParse() {
			return isFullParse;
		}
		
		public boolean isUnary() {
			return isUnary;
		}
		
		@Override
		public Iterator<Cell<Y>> iterator() {
			return children.iterator();
		}
		
		@Override
		public int numChildren() {
			return children.size();
		}
		
		@Override
		public String toString() {
			return toString(true);
		}
		
		public String toString(boolean recursive) {
			final StringBuilder ret = new StringBuilder("[").append(ruleName)
					.append(" :: ");
			final Iterator<Cell<Y>> iterator = children.iterator();
			while (iterator.hasNext()) {
				if (recursive) {
					ret.append(iterator.next().toString());
				} else {
					ret.append(iterator.next().hashCode());
				}
				if (iterator.hasNext()) {
					ret.append(", ");
				}
			}
			
			return ret.toString();
		}
		
		private Cell<?> getOuterType() {
			return Cell.this;
		}
	}
	
	public class LexicalDerivationStep extends DerivationStep implements
			ILexicalParseStep<Y> {
		private final LexicalEntry<Y>	lexicalEntry;
		
		private LexicalDerivationStep(LexicalEntry<Y> lexicalEntry) {
			super(LEXICAL_DERIVATION_STEP_RULENAME);
			this.lexicalEntry = lexicalEntry;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			@SuppressWarnings("unchecked")
			final LexicalDerivationStep other = (LexicalDerivationStep) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (lexicalEntry == null) {
				if (other.lexicalEntry != null) {
					return false;
				}
			} else if (!lexicalEntry.equals(other.lexicalEntry)) {
				return false;
			}
			return true;
		}
		
		@Override
		public LexicalEntry<Y> getLexicalEntry() {
			return lexicalEntry;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((lexicalEntry == null) ? 0 : lexicalEntry.hashCode());
			return result;
		}
		
		private Cell<?> getOuterType() {
			return Cell.this;
		}
		
	}
	
	public static class ScoreComparator<Y> implements Comparator<Cell<Y>> {
		
		@Override
		public int compare(Cell<Y> o1, Cell<Y> o2) {
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
