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
package edu.cornell.cs.nlp.spf.parser;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Capture rule application information (the rule applied and on what spans).
 *
 * @author Yoav Artzi
 */
public class RuleUsageTriplet {
	private final RuleName						ruleName;
	/** (start,end) pair for all children of this parsing step */
	private final List<Pair<Integer, Integer>>	spans;

	public RuleUsageTriplet(RuleName ruleName,
			List<Pair<Integer, Integer>> spans) {
		this.spans = Collections.unmodifiableList(spans);
		this.ruleName = ruleName;
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
		if (spans == null) {
			if (other.spans != null) {
				return false;
			}
		} else if (!spans.equals(other.spans)) {
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

	public RuleName getRuleName() {
		return ruleName;
	}

	public List<Pair<Integer, Integer>> getSpans() {
		return spans;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (spans == null ? 0 : spans.hashCode());
		result = prime * result + (ruleName == null ? 0 : ruleName.hashCode());
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(ruleName.toString())
				.append("[");
		final Iterator<Pair<Integer, Integer>> iterator = spans.iterator();
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
