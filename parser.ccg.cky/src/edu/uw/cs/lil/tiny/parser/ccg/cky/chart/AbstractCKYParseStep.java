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
import java.util.Iterator;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;

/**
 * A single CKY parse step.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public abstract class AbstractCKYParseStep<MR> implements Iterable<Cell<MR>>,
		IParseStep<MR> {
	
	private final List<Cell<MR>>	children;
	private final boolean			isFullParse;
	private final boolean			isUnary;
	private final Category<MR>		root;
	private final String			ruleName;
	
	public AbstractCKYParseStep(Category<MR> root, Cell<MR> child,
			boolean isFulleParse, String ruleName) {
		this(root, child, null, isFulleParse, ruleName);
	}
	
	public AbstractCKYParseStep(Category<MR> root, Cell<MR> leftChild,
			Cell<MR> rightChild, boolean isFullParse, String ruleName) {
		this.root = root;
		this.isFullParse = isFullParse;
		this.isUnary = rightChild == null;
		List<Cell<MR>> list;
		if (isUnary) {
			list = new ArrayList<Cell<MR>>(1);
			list.add(leftChild);
		} else {
			list = new ArrayList<Cell<MR>>(2);
			list.add(leftChild);
			list.add(rightChild);
		}
		this.children = Collections.unmodifiableList(list);
		this.ruleName = ruleName;
	}
	
	protected AbstractCKYParseStep(Category<MR> root, String ruleName,
			boolean isFullParse) {
		this.root = root;
		this.isFullParse = isFullParse;
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
		final AbstractCKYParseStep<MR> other = (AbstractCKYParseStep<MR>) obj;
		if (children == null) {
			if (other.children != null) {
				return false;
			}
		} else if (!children.equals(other.children)) {
			return false;
		}
		if (isFullParse != other.isFullParse) {
			return false;
		}
		if (isUnary != other.isUnary) {
			return false;
		}
		if (root == null) {
			if (other.root != null) {
				return false;
			}
		} else if (!root.equals(other.root)) {
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
	public Category<MR> getChild(int i) {
		return getChildCell(i).getCategroy();
	}
	
	public Cell<MR> getChildCell(int i) {
		return children.get(i);
	}
	
	public abstract IHashVectorImmutable getLocalFeatures();
	
	public abstract double getLocalScore();
	
	@Override
	public Category<MR> getRoot() {
		return root;
	}
	
	@Override
	public String getRuleName() {
		return ruleName;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + (isFullParse ? 1231 : 1237);
		result = prime * result + (isUnary ? 1231 : 1237);
		result = prime * result + ((root == null) ? 0 : root.hashCode());
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
	public Iterator<Cell<MR>> iterator() {
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
		final Iterator<Cell<MR>> iterator = children.iterator();
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
}
