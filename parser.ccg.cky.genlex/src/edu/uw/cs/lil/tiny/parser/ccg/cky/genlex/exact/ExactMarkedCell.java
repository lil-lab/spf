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
package edu.uw.cs.lil.tiny.parser.ccg.cky.genlex.exact;

import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCKYParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.cky.genlex.IMarkedEntriesCounter;

/**
 * {@link Chart} cell that maintains a count of the number of GENLEX lexical
 * entries in its subtree.
 * 
 * @author Yoav Artzi
 */
public class ExactMarkedCell<MR> extends Cell<MR> implements
		IMarkedEntriesCounter {
	
	private final int	numMarkedLexicalEntries;
	
	@SuppressWarnings("rawtypes")
	protected ExactMarkedCell(AbstractCKYParseStep<MR> parseStep, int start,
			int end, boolean isCompleteSpan, int numMarkedLexicalEntries) {
		super(parseStep, start, end, isCompleteSpan);
		if (parseStep instanceof ILexicalParseStep) {
			this.numMarkedLexicalEntries = ((ILexicalParseStep) parseStep)
					.getLexicalEntry().getOrigin()
					.equals(ILexiconGenerator.GENLEX_LEXICAL_ORIGIN) ? 1 : 0;
		} else {
			this.numMarkedLexicalEntries = numMarkedLexicalEntries;
		}
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
		final ExactMarkedCell other = (ExactMarkedCell) obj;
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
	public String toString(boolean recursive, String tokens) {
		return super.toString(recursive, tokens)
				+ String.format("{%d}", numMarkedLexicalEntries);
	}
	
}
