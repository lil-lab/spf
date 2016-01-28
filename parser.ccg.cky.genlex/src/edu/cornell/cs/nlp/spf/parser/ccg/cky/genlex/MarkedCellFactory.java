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

import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.CellFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.IWeightedCKYStep;

/**
 * Cell factory for {@link MarkedCell}.
 *
 * @author Yoav Artzi
 */
public class MarkedCellFactory<MR> extends CellFactory<MR> {

	private final boolean	useMarkingForPruning;

	public MarkedCellFactory(int sentenceSize, boolean useMarkingForPruning) {
		super(sentenceSize);
		this.useMarkingForPruning = useMarkingForPruning;
	}

	public Cell<MR> create(IWeightedCKYStep<MR> parseStep, int marking) {
		return create(parseStep, marking,
				isCompleteSpan(parseStep.getStart(), parseStep.getEnd()));
	}

	private Cell<MR> create(IWeightedCKYStep<MR> parseStep, int marking,
			boolean isCompleteSpan) {
		if (useMarkingForPruning) {
			return new MarkedCell<MR>(parseStep, isCompleteSpan, marking) {
				@Override
				public double getPruneScore() {
					return -getNumMarkedLexicalEntries();
				}

				@Override
				public double getSecondPruneScore() {
					return super.getPruneScore();
				}
			};
		} else {
			return new MarkedCell<MR>(parseStep, isCompleteSpan, marking);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Cell<MR> doCreate(IWeightedCKYStep<MR> parseStep,
			boolean isCompleteSpan) {
		int markedEntries = 0;

		// Only mark entries if this is not a complete span. First, tracking at
		// the top of the chart is useless. Second, it leads to multiple
		// derivations with identical categories (both syntax and logical form).
		if (!isCompleteSpan) {
			if (parseStep.numChildren() > 0
					&& parseStep.getChildCell(0) instanceof MarkedCell) {
				markedEntries += ((MarkedCell<MR>) parseStep.getChildCell(0))
						.getNumMarkedLexicalEntries();
			}

			if (parseStep.numChildren() > 1
					&& parseStep.getChildCell(1) instanceof MarkedCell) {
				markedEntries += ((MarkedCell<MR>) parseStep.getChildCell(1))
						.getNumMarkedLexicalEntries();
			}

			if (parseStep instanceof ILexicalParseStep
					&& ((ILexicalParseStep<MR>) parseStep).getLexicalEntry()
							.hasProperty(
									ILexiconGenerator.GENLEX_MARKING_PROPERTY)) {
				markedEntries += 1;
			}
		}

		return create(parseStep, markedEntries, isCompleteSpan);
	}
}
