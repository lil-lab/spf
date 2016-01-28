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

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;

/**
 * {@link LexicalEntry} generator. Generators that implement this interface,
 * unlike generators that only implement {@link ILexiconGenerator}, promise to
 * return a concise and accurate set of {@link LexicalEntry}s. The user of the
 * generator is not required to do further pruning.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Type of sample data item for generation.
 * @param <MR>
 *            Type of meaning representation.
 * @param <MODEL>
 *            Inference model.
 */
public interface ILexiconGeneratorPrecise<DI extends IDataItem<?>, MR, MODEL extends IModelImmutable<?, ?>>
		extends ILexiconGenerator<DI, MR, MODEL> {
	// Interface provides no extra functionality.
}
