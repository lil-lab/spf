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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;

/**
 * A single CKY parse step.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public abstract class AbstractCKYStep<MR> implements ICKYStep<MR> {

	/**
	 * TODO: remove the list from here or add a comment to clarify. It's
	 * confusing and not efficient.
	 */
	private final List<Cell<MR>>	children;
	private final int				end;
	private int						hashCode;
	private final boolean			isFullParse;
	private final boolean			isUnary;

	private final Category<MR> root;

	private final RuleName	ruleName;
	private final int		start;

	public AbstractCKYStep(Category<MR> root, Cell<MR> child,
			boolean isFulleParse, RuleName ruleName, int start, int end) {
		this(root, child, null, isFulleParse, ruleName, start, end);
	}

	public AbstractCKYStep(Category<MR> root, Cell<MR> leftChild,
			Cell<MR> rightChild, boolean isFullParse, RuleName ruleName,
			int start, int end) {
		assert root != null;
		assert ruleName != null;
		this.root = root;
		this.isFullParse = isFullParse;
		this.start = start;
		this.end = end;
		this.isUnary = rightChild == null;
		List<Cell<MR>> list;
		if (isUnary) {
			assert leftChild != null;
			list = new ArrayList<Cell<MR>>(1);
			list.add(leftChild);
			assert leftChild.getStart() == start && leftChild.getEnd() == end;
		} else {
			assert leftChild != null;
			assert rightChild != null;
			list = new ArrayList<Cell<MR>>(2);
			list.add(leftChild);
			list.add(rightChild);
			assert leftChild.getStart() == start && rightChild.getEnd() == end
					&& leftChild.getEnd() + 1 == rightChild.getStart();
		}
		this.children = Collections.unmodifiableList(list);
		this.ruleName = ruleName;
		this.hashCode = calcHashCode();
	}

	protected AbstractCKYStep(Category<MR> root, RuleName ruleName,
			boolean isFullParse, int start, int end) {
		assert root != null;
		assert ruleName != null;
		this.root = root;
		this.isFullParse = isFullParse;
		this.start = start;
		this.end = end;
		this.isUnary = false;
		this.ruleName = ruleName;
		this.children = Collections.emptyList();
		this.hashCode = calcHashCode();
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
		final AbstractCKYStep<MR> other = (AbstractCKYStep<MR>) obj;
		if (isFullParse != other.isFullParse) {
			return false;
		}
		if (end != other.end) {
			return false;
		}
		if (start != other.start) {
			return false;
		}
		if (!ruleName.equals(other.ruleName)) {
			return false;
		}
		if (!root.equals(other.root)) {
			return false;
		}
		if (!children.equals(other.children)) {
			return false;
		}
		return true;
	}

	@Override
	public Category<MR> getChild(int i) {
		return getChildCell(i).getCategory();
	}

	@Override
	public Cell<MR> getChildCell(int i) {
		return children.get(i);
	}

	@Override
	public int getEnd() {
		return end;
	}

	@Override
	public Category<MR> getRoot() {
		return root;
	}

	@Override
	public RuleName getRuleName() {
		return ruleName;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean isFullParse() {
		return isFullParse;
	}

	@Override
	public boolean isUnary() {
		return isUnary;
	}

	@Override
	public Iterator<Cell<MR>> iterator() {
		return children.iterator();
	}

	@Override
	public int numChildren() {
		return children.size();
	}

	/**
	 * Create a new step by replacing the root with the category of the result
	 * and appending the result rule name to the step rule name.
	 */
	@Override
	public abstract AbstractCKYStep<MR> overloadWithUnary(
			ParseRuleResult<MR> unaryRuleResult, boolean fullParseAfterUnary);

	@Override
	public String toString() {
		return toString(true, true);
	}

	@Override
	public String toString(boolean verbose, boolean recursive) {
		final StringBuilder ret = new StringBuilder("[").append(start)
				.append("-").append(end).append(" :: ").append(ruleName)
				.append(" :: ");
		final Iterator<Cell<MR>> iterator = children.iterator();
		while (iterator.hasNext()) {
			final Cell<MR> child = iterator.next();
			ret.append(child.getStart()).append("-").append(child.getEnd());
			if (iterator.hasNext()) {
				ret.append(", ");
			}
		}
		if (verbose) {
			ret.append(" :: ");
			final Iterator<Cell<MR>> iter = children.iterator();
			while (iter.hasNext()) {
				if (recursive) {
					ret.append(iter.next().toString());
				} else {
					ret.append(iter.next().hashCode());
				}
				if (iter.hasNext()) {
					ret.append(", ");
				}
			}
		}
		ret.append("]");

		return ret.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + children.hashCode();
		result = prime * result + end;
		result = prime * result + (isFullParse ? 1231 : 1237);
		result = prime * result + (isUnary ? 1231 : 1237);
		result = prime * result + root.hashCode();
		result = prime * result + ruleName.hashCode();
		result = prime * result + start;
		return result;
	}
}
