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

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;

/**
 * A single CKY parse step.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public abstract class AbstractCKYParseStep<MR> implements Iterable<Cell<MR>>,
		IParseStep<MR> {
	
	protected final List<Cell<MR>>		children;
	protected final boolean				isFullParse;
	protected final boolean				isUnary;
	/**
	 * If this is a lexical step, this is the entry that is responsible for it.
	 * Otherwise, this is null. The lexical entry is included here to allow us
	 * to initialize this member before we compute the local features and score.
	 * Not an ideal solution. Better suggestions are welcome. It's only exposed
	 * to the outside by the lexical step class, so it's a fairly isolated
	 * issue.
	 */
	protected final LexicalEntry<MR>	lexicalEntry;
	protected final IHashVector			localFeatures;
	protected final double				localScore;
	protected final Category<MR>		root;
	
	protected final RuleName			ruleName;
	
	public AbstractCKYParseStep(Category<MR> root, Cell<MR> child,
			boolean isFulleParse, RuleName ruleName, IDataItemModel<MR> model) {
		this(root, child, null, isFulleParse, ruleName, model);
	}
	
	public AbstractCKYParseStep(Category<MR> root, Cell<MR> leftChild,
			Cell<MR> rightChild, boolean isFullParse, RuleName ruleName,
			IDataItemModel<MR> model) {
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
		this.lexicalEntry = null;
		// Doing this in two separate steps due to the way lexical feature sets
		// behave when scoring unknown lexical entries. Therefore, it's not
		// possible to simply recycle the feature vector and multiply it by
		// the model's weight vector (theta).
		this.localFeatures = model.computeFeatures(this);
		this.localScore = model.score(this);
	}
	
	protected AbstractCKYParseStep(Category<MR> root,
			LexicalEntry<MR> lexicalEntry, RuleName ruleName,
			boolean isFullParse, IDataItemModel<MR> model) {
		this.root = root;
		this.lexicalEntry = lexicalEntry;
		this.isFullParse = isFullParse;
		this.isUnary = false;
		this.ruleName = ruleName;
		this.children = Collections.emptyList();
		// Doing this in two separate steps due to the way lexical feature sets
		// behave when scoring unknown lexical entries. Therefore, it's not
		// possible to simply recycle the feature vector and multiply it by
		// the model's weight vector (theta).
		this.localFeatures = model.computeFeatures(this);
		this.localScore = model.score(this);
	}
	
	/**
	 * Create a new step by replacing the root with the category of the result
	 * and appending the result rule name to the step rule name.
	 */
	public abstract AbstractCKYParseStep<MR> cloneWithUnary(
			ParseRuleResult<MR> unaryRuleResult, IDataItemModel<MR> model,
			boolean fullParseAfterUnary);
	
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
		@SuppressWarnings({ "rawtypes" })
		final AbstractCKYParseStep other = (AbstractCKYParseStep) obj;
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
		if (lexicalEntry == null) {
			if (other.lexicalEntry != null) {
				return false;
			}
		} else if (!lexicalEntry.equals(other.lexicalEntry)) {
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
		return getChildCell(i).getCategory();
	}
	
	public Cell<MR> getChildCell(int i) {
		return children.get(i);
	}
	
	public IHashVector getLocalFeatures() {
		return localFeatures;
	}
	
	public double getLocalScore() {
		return localScore;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + (isFullParse ? 1231 : 1237);
		result = prime * result + (isUnary ? 1231 : 1237);
		result = prime * result
				+ ((lexicalEntry == null) ? 0 : lexicalEntry.hashCode());
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
		ret.append(" :: localFeatures=").append(localFeatures);
		ret.append(":: localScore=").append(localScore);
		ret.append("]");
		
		return ret.toString();
	}
}
