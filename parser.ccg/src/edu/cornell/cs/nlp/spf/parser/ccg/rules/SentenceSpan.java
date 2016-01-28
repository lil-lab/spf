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
package edu.cornell.cs.nlp.spf.parser.ccg.rules;

/**
 * Packs various information about a span of tokens in a sentence.
 *
 * @author Yoav Artzi
 */
public class SentenceSpan extends Span {

	private final int	sentenceLength;

	public SentenceSpan(int start, int end, int sentenceLength) {
		super(start, end);
		assert end < sentenceLength;
		this.sentenceLength = sentenceLength;
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
		final SentenceSpan other = (SentenceSpan) obj;
		if (sentenceLength != other.sentenceLength) {
			return false;
		}
		return true;
	}

	public int getSentenceLength() {
		return sentenceLength;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + sentenceLength;
		return result;
	}

	/**
	 * @return 'true' iff the span represented spans the complete sentence.
	 */
	public boolean isCompleteSentence() {
		return getStart() == 0 && getEnd() == sentenceLength - 1;

	}

	/**
	 * @return 'true' iff the span represented ends at the sentence end.
	 */
	public boolean isEnd() {
		return getEnd() == sentenceLength - 1;
	}

	/**
	 * @return 'true' iff the span represented starts at the sentence end.
	 */
	public boolean isStart() {
		return getStart() == 0;
	}

	public int length() {
		return getEnd() - getStart() + 1;
	}

	@Override
	public String toString() {
		return new StringBuilder(super.toString()).append("{")
				.append(sentenceLength).append("}").toString();
	}

}
