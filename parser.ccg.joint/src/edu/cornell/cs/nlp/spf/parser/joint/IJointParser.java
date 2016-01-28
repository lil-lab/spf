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
package edu.cornell.cs.nlp.spf.parser.joint;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.data.situated.ISituatedDataItem;
import edu.cornell.cs.nlp.spf.parser.joint.model.IJointDataItemModel;

/**
 * Joint inference procedure.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Situated inference data item.
 * @param <MR>
 *            Semantics formal meaning representation (e.g., LogicalExpression).
 * @param <ESTEP>
 *            Semantics evaluation step.
 * @param <ERESULT>
 *            Semantics evaluation results.
 */
public interface IJointParser<DI extends ISituatedDataItem<?, ?>, MR, ESTEP, ERESULT>
		extends Serializable {

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model, boolean allowWordSkipping);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model, boolean allowWordSkipping,
			ILexiconImmutable<MR> tempLexicon);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model, boolean allowWordSkipping,
			ILexiconImmutable<MR> tempLexicon, Integer beamSize);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model,
			IJointInferenceFilter<MR, ESTEP, ERESULT> filter);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model,
			IJointInferenceFilter<MR, ESTEP, ERESULT> filter,
			boolean allowWordSkipping);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model,
			IJointInferenceFilter<MR, ESTEP, ERESULT> filter,
			boolean allowWordSkipping, ILexiconImmutable<MR> tempLexicon);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model,
			IJointInferenceFilter<MR, ESTEP, ERESULT> filter,
			boolean allowWordSkipping, ILexiconImmutable<MR> tempLexicon,
			Integer beamSize);

	IJointOutput<MR, ERESULT> parse(DI dataItem,
			IJointDataItemModel<MR, ESTEP> model,
			IJointInferenceFilter<MR, ESTEP, ERESULT> filter, Integer beamSize);

}
