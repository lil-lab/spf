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
package edu.cornell.cs.nlp.spf.parser.ccg.cky;

import java.util.LinkedHashSet;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.parser.RuleUsageTriplet;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.IWeightedCKYStep;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphDerivation;

/**
 * A single CKY derivation, marginalizes over all parse trees for a complete
 * sentence category. The parse only gives access to the logical form, but
 * actually also contains the syntactic component. This is an existing issue.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class CKYDerivation<MR> implements IGraphDerivation<MR> {
	private IHashVectorImmutable	averageFeatureVector	= null;
	private final Cell<MR>			cell;

	public CKYDerivation(Cell<MR> cell) {
		this.cell = cell;
	}

	protected CKYDerivation(CKYDerivation<MR> parse) {
		this(parse.cell);
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
		@SuppressWarnings("rawtypes")
		final CKYDerivation other = (CKYDerivation) obj;
		if (cell == null) {
			if (other.cell != null) {
				return false;
			}
		} else if (!cell.equals(other.cell)) {
			return false;
		}
		return true;
	}

	@Override
	public LinkedHashSet<LexicalEntry<MR>> getAllLexicalEntries() {
		return cell.getAllLexicalEntriesRecursively();
	}

	@Override
	public Set<IWeightedCKYStep<MR>> getAllSteps() {
		return cell.getAllSteps();
	}

	@Override
	public IHashVectorImmutable getAverageMaxFeatureVector() {
		if (averageFeatureVector == null) {
			averageFeatureVector = cell.computeMaxAvgFeaturesRecursively();
		}
		return averageFeatureVector;
	}

	@Override
	public Category<MR> getCategory() {
		return cell.getCategory();
	}

	public Cell<MR> getCell() {
		return cell;
	}

	@Override
	public double getLogInsideScore() {
		return cell.getLogInsideScore();
	}

	@Override
	public LinkedHashSet<LexicalEntry<MR>> getMaxLexicalEntries() {
		return cell.getMaxLexicalEntriesRecursively();
	}

	@Override
	public LinkedHashSet<RuleUsageTriplet> getMaxRulesUsed() {
		return cell.getMaxRulesUsedRecursively();
	}

	@Override
	public LinkedHashSet<IWeightedCKYStep<MR>> getMaxSteps() {
		return cell.getMaxSteps();
	}

	@Override
	public double getScore() {
		return cell.getViterbiScore();
	}

	@Override
	public MR getSemantics() {
		return cell.getCategory().getSemantics();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (cell == null ? 0 : cell.hashCode());
		return result;
	}

	/**
	 * The number of parses packed into this derivation.
	 */
	@Override
	public long numParses() {
		return cell.getNumParses();
	}

	@Override
	public String toString() {
		return cell.getCategory().toString();
	}

}
