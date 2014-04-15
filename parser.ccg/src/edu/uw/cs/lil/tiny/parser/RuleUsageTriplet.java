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
package edu.uw.cs.lil.tiny.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.utils.composites.Pair;

/**
 * Capture rule application information (the rule applied and on what spans).
 * 
 * @author Yoav Artzi
 */
public class RuleUsageTriplet {
	/** (start,end) pair for all children of this parsing step */
	private final List<Pair<Integer, Integer>>	children;
	private final RuleName						ruleName;
	
	public RuleUsageTriplet(RuleName ruleName,
			List<Pair<Integer, Integer>> children) {
		this.children = Collections.unmodifiableList(children);
		this.ruleName = ruleName;
	}
	
	public RuleUsageTriplet(RuleName ruleName,
			Pair<Integer, Integer>... children) {
		this(ruleName, Arrays.asList(children));
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
		final RuleUsageTriplet other = (RuleUsageTriplet) obj;
		if (children == null) {
			if (other.children != null) {
				return false;
			}
		} else if (!children.equals(other.children)) {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result
				+ ((ruleName == null) ? 0 : ruleName.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(ruleName.toString())
				.append("[");
		final Iterator<Pair<Integer, Integer>> iterator = children.iterator();
		while (iterator.hasNext()) {
			final Pair<Integer, Integer> child = iterator.next();
			sb.append(child.first()).append("-").append(child.second());
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.append("]").toString();
	}
	
}
