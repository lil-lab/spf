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
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYLexicalStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;

/**
 * CKY chart cell that allows two scoring metric to break ties, with one
 * considered as the primary.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
public class ScoreSensitiveCell<MR> extends Cell<MR> {
	private final boolean				calculatedScore	= false;
	private double						score;
	private final boolean				scoreIsPrimary;
	private final IScoreFunction<MR>	scoringFunction;
	
	ScoreSensitiveCell(CKYLexicalStep<MR> parseStep, int start, int end,
			boolean isCompleteSpan, IScoreFunction<MR> scoringFunction,
			boolean scoreIsPrimary) {
		super(parseStep, start, end, isCompleteSpan);
		this.scoringFunction = scoringFunction;
		this.scoreIsPrimary = scoreIsPrimary;
	}
	
	ScoreSensitiveCell(CKYParseStep<MR> parseStep, int start, int end,
			boolean isCompleteSpan, IScoreFunction<MR> scoringFunction,
			boolean scoreIsPrimary) {
		super(parseStep, start, end, isCompleteSpan);
		this.scoringFunction = scoringFunction;
		this.scoreIsPrimary = scoreIsPrimary;
	}
	
	@Override
	public double getPruneScore() {
		if (scoreIsPrimary) {
			return calculateScore();
		} else {
			return super.getPruneScore();
		}
	}
	
	@Override
	public double getSecondPruneScore() {
		if (scoreIsPrimary) {
			return super.getPruneScore();
		} else {
			return calculateScore();
		}
	}
	
	private double calculateScore() {
		if (!calculatedScore) {
			score = getCategroy().getSem() == null ? 0.0 : scoringFunction
					.score(getCategroy().getSem());
		}
		return score;
	}
}
