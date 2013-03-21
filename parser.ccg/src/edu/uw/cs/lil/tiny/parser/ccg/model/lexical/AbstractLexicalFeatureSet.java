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
package edu.uw.cs.lil.tiny.parser.ccg.model.lexical;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.parser.ccg.ILexicalParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;

public abstract class AbstractLexicalFeatureSet<X, Y> implements
		IIndependentLexicalFeatureSet<X, Y> {
	
	@Override
	public final double score(IParseStep<Y> obj, IHashVector theta,
			IDataItem<X> dataItem) {
		if (obj instanceof ILexicalParseStep) {
			return score(((ILexicalParseStep<Y>) obj).getLexicalEntry(), theta);
		} else {
			return 0;
		}
	}
	
	@Override
	public final void setFeats(IParseStep<Y> obj, IHashVector features,
			IDataItem<X> dataItem) {
		if (obj instanceof ILexicalParseStep) {
			setFeats(((ILexicalParseStep<Y>) obj).getLexicalEntry(), features);
		}
	}
}
