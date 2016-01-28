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
package edu.cornell.cs.nlp.spf.data.situated.labeled;

import java.util.Collections;
import java.util.Map;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;

/**
 * {@link SituatedSentence} paired with a label.
 *
 * @author Yoav Artzi
 * @param <STATE>
 *            Type of state/situation.
 * @param <LABEL>
 *            Final label for testing.
 */
public class LabeledSituatedSentence<STATE, LABEL> implements
		ILabeledDataItem<SituatedSentence<STATE>, LABEL> {

	private static final long				serialVersionUID	= -4413253762974669632L;
	private final LABEL						label;
	private final Map<String, String>		properties;
	private final SituatedSentence<STATE>	sentence;

	public LabeledSituatedSentence(SituatedSentence<STATE> sentence, LABEL label) {
		this(sentence, label, Collections.<String, String> emptyMap());
	}

	public LabeledSituatedSentence(SituatedSentence<STATE> sentence,
			LABEL label, Map<String, String> properties) {
		this.sentence = sentence;
		this.label = label;
		this.properties = Collections.unmodifiableMap(properties);
	}

	@Override
	public LABEL getLabel() {
		return label;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public SituatedSentence<STATE> getSample() {
		return sentence;
	}

	@Override
	public boolean isCorrect(LABEL candidate) {
		return label.equals(label);
	}

	@Override
	public String toString() {
		return sentence + "\n" + label;
	}

}
