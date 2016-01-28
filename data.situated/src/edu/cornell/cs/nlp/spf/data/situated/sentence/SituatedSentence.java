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
package edu.cornell.cs.nlp.spf.data.situated.sentence;

import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.situated.ISituatedDataItem;

/**
 * A sentence situated in some kind of state.
 *
 * @author Yoav Artzi
 * @param <STATE>
 *            Type of state/situation.
 */
public class SituatedSentence<STATE> extends Sentence implements
		ISituatedDataItem<Sentence, STATE> {

	private static final long	serialVersionUID	= -2438466488521841887L;
	private final STATE			state;

	public SituatedSentence(Sentence sentence, STATE state) {
		super(sentence);
		this.state = state;
	}

	@Override
	public STATE getState() {
		return state;
	}

	@Override
	public String toString() {
		return super.toString() + "\n" + state.toString();
	}

}
