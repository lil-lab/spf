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
package edu.cornell.cs.nlp.spf.parser.ccg.model;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;

public class DataItemModel<DI extends IDataItem<?>, MR> implements
		IDataItemModel<MR> {

	private final DI						dataItem;
	private final IModelImmutable<DI, MR>	model;

	public DataItemModel(IModelImmutable<DI, MR> model, DI dataItem) {
		this.model = model;
		this.dataItem = dataItem;
	}

	@Override
	public IHashVector computeFeatures(IParseStep<MR> parseStep) {
		return model.computeFeatures(parseStep, dataItem);
	}

	@Override
	public IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry) {
		return model.computeFeatures(lexicalEntry);
	}

	@Override
	public ILexiconImmutable<MR> getLexicon() {
		return model.getLexicon();
	}

	@Override
	public IHashVectorImmutable getTheta() {
		return model.getTheta();
	}

	@Override
	public double score(IHashVectorImmutable features) {
		return model.score(features);
	}

	@Override
	public double score(LexicalEntry<MR> entry) {
		return model.score(entry);
	}

}
