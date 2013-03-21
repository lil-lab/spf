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
package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.LinkedHashSet;

import edu.uw.cs.lil.tiny.parser.IParseResult;
import edu.uw.cs.lil.tiny.parser.RuleUsageTriplet;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;

public class CKYParse<Y> implements IParseResult<Y> {
	private IHashVector				averageFeatureVector	= null;
	private final Cell<Y>			cell;
	private final IDataItemModel<Y>	model;
	private final Y					semantics;
	
	public CKYParse(Cell<Y> cell, IDataItemModel<Y> model) {
		this.cell = cell;
		this.model = model;
		this.semantics = cell.getCategroy().getSem();
	}
	
	protected CKYParse(CKYParse<Y> parse) {
		this(parse.cell, parse.model);
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
		final CKYParse other = (CKYParse) obj;
		if (cell == null) {
			if (other.cell != null) {
				return false;
			}
		} else if (!cell.equals(other.cell)) {
			return false;
		}
		if (semantics == null) {
			if (other.semantics != null) {
				return false;
			}
		} else if (!semantics.equals(other.semantics)) {
			return false;
		}
		return true;
	}
	
	@Override
	public LinkedHashSet<LexicalEntry<Y>> getAllLexicalEntries() {
		return cell.getAllLexicalEntriesRecursively();
	}
	
	@Override
	public IHashVector getAverageMaxFeatureVector() {
		if (averageFeatureVector == null) {
			averageFeatureVector = cell.computeMaxAvgFeaturesRecursively(model);
		}
		return averageFeatureVector;
	}
	
	public LinkedHashSet<LexicalEntry<Y>> getMaxLexicalEntries() {
		return cell.getMaxLexicalEntriesRecursively();
	}
	
	@Override
	public LinkedHashSet<RuleUsageTriplet> getMaxRulesUsed() {
		return cell.getMaxRulesUsedRecursively();
	}
	
	public double getScore() {
		return cell.getViterbiScore();
	}
	
	public Y getY() {
		return semantics;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cell == null) ? 0 : cell.hashCode());
		result = prime * result
				+ ((semantics == null) ? 0 : semantics.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return semantics.toString();
	}
}
