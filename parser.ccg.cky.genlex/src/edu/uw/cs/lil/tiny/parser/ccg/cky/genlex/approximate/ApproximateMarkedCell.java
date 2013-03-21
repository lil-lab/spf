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
package edu.uw.cs.lil.tiny.parser.ccg.cky.genlex.approximate;

import java.util.Iterator;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.genlex.IMarkedEntriesCounter;
import edu.uw.cs.lil.tiny.parser.ccg.genlex.ILexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;

public class ApproximateMarkedCell extends Cell<LogicalExpression> implements
		IMarkedEntriesCounter {
	
	private int	numMarkedLexicalEntries;
	
	protected ApproximateMarkedCell(Category<LogicalExpression> category,
			String ruleName, Cell<LogicalExpression> child,
			boolean isCompleteSpan, boolean isFullParse,
			int numMarkedLexicalEntries) {
		super(category, ruleName, child, isCompleteSpan, isFullParse);
		this.numMarkedLexicalEntries = numMarkedLexicalEntries;
	}
	
	protected ApproximateMarkedCell(Category<LogicalExpression> category,
			String ruleName, Cell<LogicalExpression> leftChild,
			Cell<LogicalExpression> rightChild, boolean isCompleteSpan,
			boolean isFullParse, int numGeneratedLexicalEntries) {
		super(category, ruleName, leftChild, rightChild, isCompleteSpan,
				isFullParse);
		this.numMarkedLexicalEntries = numGeneratedLexicalEntries;
	}
	
	protected ApproximateMarkedCell(
			LexicalEntry<LogicalExpression> lexicalEntry, int begin, int end,
			boolean isCompleteSpan, boolean isFullParse) {
		super(lexicalEntry, begin, end, isCompleteSpan, isFullParse);
		this.numMarkedLexicalEntries = lexicalEntry.getOrigin().equals(
				ILexiconGenerator.GENLEX_LEXICAL_ORIGIN) ? 1 : 0;
	}
	
	@Override
	public int getNumMarkedLexicalEntries() {
		return numMarkedLexicalEntries;
	}
	
	private int getNumGeneratedLexicalEntries(DerivationStep step) {
		int numGenLexEntries = 0;
		for (final Cell<LogicalExpression> child : step) {
			if (child instanceof ApproximateMarkedCell) {
				numGenLexEntries += ((ApproximateMarkedCell) child).numMarkedLexicalEntries;
			}
		}
		return numGenLexEntries;
	}
	
	@Override
	protected boolean updateInsideScore(DerivationStep derivationStep,
			IDataItemModel<LogicalExpression> model) {
		// Remember the number of parses in the top steps
		final int initialNumParses = numParses;
		
		if (super.updateInsideScore(derivationStep, model)) {
			// Case the derivation step was added to the list of max steps
			final int stepNumGeneratedLexicalEntries = getNumGeneratedLexicalEntries(derivationStep);
			if (viterbiSteps.size() == 1) {
				// Case we have a single step in the list of max steps, since we
				// just added the current step, it must be the one in the list,
				// so need to update the number of generated lexical entries
				numMarkedLexicalEntries = stepNumGeneratedLexicalEntries;
			} else {
				if (numMarkedLexicalEntries > stepNumGeneratedLexicalEntries) {
					// Case the number of generated lexical entries in the new
					// step is smaller than other max steps, remove these steps
					// from the list and update numGeneratedLexicalEntries
					final Iterator<DerivationStep> iterator = viterbiSteps
							.iterator();
					while (iterator.hasNext()) {
						if (iterator.next() != derivationStep) {
							iterator.remove();
						}
					}
					numMarkedLexicalEntries = stepNumGeneratedLexicalEntries;
					// Update the number of parses represented by this cell
					numParses -= initialNumParses;
				} else if (numMarkedLexicalEntries < stepNumGeneratedLexicalEntries) {
					// Case the new step is using more genlex lexical entries
					// than previously known max steps, remove the new step
					final Iterator<DerivationStep> iterator = viterbiSteps
							.iterator();
					while (iterator.hasNext()) {
						if (iterator.next() == derivationStep) {
							iterator.remove();
							break;
						}
					}
					// Restore the original number of parses
					numParses = initialNumParses;
				}
			}
			return true;
		} else {
			// Case the derivation step wasn't added, no need to change anything
			return false;
		}
	}
	
}
