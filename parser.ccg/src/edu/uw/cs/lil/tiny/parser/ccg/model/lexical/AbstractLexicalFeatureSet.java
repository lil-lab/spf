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
package edu.uw.cs.lil.tiny.parser.ccg.model.lexical;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;

public abstract class AbstractLexicalFeatureSet<DI extends IDataItem<?>, MR>
		implements IIndependentLexicalFeatureSet<DI, MR> {
	
	private static final long	serialVersionUID	= 8723491320895862185L;
	
	@Override
	public final double score(IParseStep<MR> obj, IHashVector theta, DI dataItem) {
		if (obj instanceof ILexicalParseStep) {
			return score(((ILexicalParseStep<MR>) obj).getLexicalEntry(), theta);
		} else {
			return 0;
		}
	}
	
	@Override
	public final void setFeats(IParseStep<MR> obj, IHashVector features,
			DI dataItem) {
		if (obj instanceof ILexicalParseStep) {
			setFeats(((ILexicalParseStep<MR>) obj).getLexicalEntry(), features);
		}
	}
}
