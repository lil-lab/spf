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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.genlex;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.IWeightedCKYStep;

/**
 * {@link Chart} cell that maintains a count of the number of GENLEX lexical
 * entries in its subtree.
 *
 * @author Yoav Artzi
 */
public class MarkedCell<MR> extends Cell<MR> implements IMarkedEntriesCounter {

	private final int	numMarkedLexicalEntries;

	protected MarkedCell(IWeightedCKYStep<MR> parseStep,
			boolean isCompleteSpan, int numMarkedLexicalEntries) {
		super(parseStep, isCompleteSpan);
		this.numMarkedLexicalEntries = numMarkedLexicalEntries;
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
		@SuppressWarnings("rawtypes")
		final MarkedCell other = (MarkedCell) obj;
		if (numMarkedLexicalEntries != other.numMarkedLexicalEntries) {
			return false;
		}
		return true;
	}

	@Override
	public int getNumMarkedLexicalEntries() {
		return numMarkedLexicalEntries;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + numMarkedLexicalEntries;
		return result;
	}

	@Override
	public String toString(boolean recursive, String tokens, boolean viterbi,
			IHashVectorImmutable theta) {
		return super.toString(recursive, tokens, viterbi, theta)
				+ String.format("{%d}", numMarkedLexicalEntries);
	}

}
