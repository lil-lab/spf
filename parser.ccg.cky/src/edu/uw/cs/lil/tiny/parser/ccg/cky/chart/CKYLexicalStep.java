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
package edu.uw.cs.lil.tiny.parser.ccg.cky.chart;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;

public class CKYLexicalStep<MR> extends AbstractCKYParseStep<MR> implements
		ILexicalParseStep<MR> {
	
	private final LexicalEntry<MR>	lexicalEntry;
	private final IHashVector		localFeatures;
	private final double			localScore;
	
	public CKYLexicalStep(LexicalEntry<MR> lexicalEntry, boolean isFullParse,
			IDataItemModel<MR> model) {
		super(lexicalEntry.getCategory(), LEXICAL_DERIVATION_STEP_RULENAME,
				isFullParse);
		this.lexicalEntry = lexicalEntry;
		this.localFeatures = model.computeFeatures(this);
		this.localScore = model.score(this);
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
		@SuppressWarnings("unchecked")
		final CKYLexicalStep<MR> other = (CKYLexicalStep<MR>) obj;
		if (lexicalEntry == null) {
			if (other.lexicalEntry != null) {
				return false;
			}
		} else if (!lexicalEntry.equals(other.lexicalEntry)) {
			return false;
		}
		return true;
	}
	
	@Override
	public LexicalEntry<MR> getLexicalEntry() {
		return lexicalEntry;
	}
	
	@Override
	public IHashVector getLocalFeatures() {
		return localFeatures;
	}
	
	@Override
	public double getLocalScore() {
		return localScore;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((lexicalEntry == null) ? 0 : lexicalEntry.hashCode());
		return result;
	}
	
}
