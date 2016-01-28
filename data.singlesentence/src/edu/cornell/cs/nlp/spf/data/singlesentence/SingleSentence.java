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
package edu.cornell.cs.nlp.spf.data.singlesentence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.properties.Properties;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

/**
 * Represents a single sentence and its logical form for supervised learning.
 *
 * @author Yoav Artzi
 */
public class SingleSentence implements
		ILabeledDataItem<Sentence, LogicalExpression> {
	private static final long			serialVersionUID	= 5434665811874050978L;
	private final Map<String, String>	properties;

	private final LogicalExpression		semantics;
	private final Sentence				sentence;

	public SingleSentence(Sentence sentence, LogicalExpression semantics) {
		this(sentence, semantics, new HashMap<String, String>());
	}

	public SingleSentence(Sentence sentence, LogicalExpression semantics,
			Map<String, String> properties) {
		this.sentence = sentence;
		this.semantics = semantics;
		this.properties = Collections.unmodifiableMap(properties);
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
		final SingleSentence other = (SingleSentence) obj;
		if (properties == null) {
			if (other.properties != null) {
				return false;
			}
		} else if (!properties.equals(other.properties)) {
			return false;
		}
		if (semantics == null) {
			if (other.semantics != null) {
				return false;
			}
		} else if (!semantics.equals(other.semantics)) {
			return false;
		}
		if (sentence == null) {
			if (other.sentence != null) {
				return false;
			}
		} else if (!sentence.equals(other.sentence)) {
			return false;
		}
		return true;
	}

	@Override
	public LogicalExpression getLabel() {
		return semantics;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public Sentence getSample() {
		return sentence;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (properties == null ? 0 : properties.hashCode());
		result = prime * result
				+ (semantics == null ? 0 : semantics.hashCode());
		result = prime * result + (sentence == null ? 0 : sentence.hashCode());
		return result;
	}

	@Override
	public boolean isCorrect(LogicalExpression label) {
		return label.equals(semantics);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(sentence.toString())
				.append('\n');
		if (!properties.isEmpty()) {
			sb.append(Properties.toString(properties)).append('\n');
		}
		sb.append(semantics).toString();
		return sb.toString();
	}
}
