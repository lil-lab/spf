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
package edu.cornell.cs.nlp.spf.genlex.ccg;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;

/**
 * Lexical entries generator. The generator may over-generate by large margins.
 * It relies on the user pruning the set of generated entries to select the ones
 * to use.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Type of sample data item for generation.
 * @param <MR>
 *            Type of meaning representation.
 * @param <MODEL>
 *            Inference model.
 */
public interface ILexiconGenerator<DI extends IDataItem<?>, MR, MODEL extends IModelImmutable<?, ?>>
		extends Serializable {
	public static final String	GENLEX_LEXICAL_ORIGIN	= "genlex";

	public static final String	GENLEX_MARKING_PROPERTY	= "MARK";

	ILexiconImmutable<MR> generate(DI dataItem, MODEL model,
			ICategoryServices<MR> categoryServices);

	void init(MODEL model);

	boolean isGenerated(LexicalEntry<MR> entry);
}
