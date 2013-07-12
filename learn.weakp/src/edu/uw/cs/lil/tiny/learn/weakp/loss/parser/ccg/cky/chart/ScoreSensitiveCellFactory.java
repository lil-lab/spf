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
package edu.uw.cs.lil.tiny.learn.weakp.loss.parser.ccg.cky.chart;

import edu.uw.cs.lil.tiny.learn.weakp.loss.parser.IScoreFunction;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYLexicalStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;

/**
 * Cell factory for {@link ScoreSensitiveCell}.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public class ScoreSensitiveCellFactory<MR> extends AbstractCellFactory<MR> {
	
	private final boolean				scoreIsPrimary;
	private final IScoreFunction<MR>	scoringFunction;
	
	public ScoreSensitiveCellFactory(IScoreFunction<MR> scoringFunction,
			boolean scoreIsPrimary, int sentenceSize) {
		super(sentenceSize);
		this.scoringFunction = scoringFunction;
		this.scoreIsPrimary = scoreIsPrimary;
	}
	
	@Override
	protected Cell<MR> doCreate(CKYLexicalStep<MR> parseStep, int start,
			int end, boolean isCompleteSpan) {
		return new ScoreSensitiveCell<MR>(parseStep, start, end,
				isCompleteSpan, scoringFunction, scoreIsPrimary);
	}
	
	@Override
	protected Cell<MR> doCreate(CKYParseStep<MR> parseStep, int start, int end,
			boolean isCompleteSpan) {
		return new ScoreSensitiveCell<MR>(parseStep, start, end,
				isCompleteSpan, scoringFunction, scoreIsPrimary);
	}
	
}
