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

import edu.cornell.cs.nlp.spf.parser.ParsingOp;

/**
 * Utils for {@link IJointInferenceFilter}.
 *
 * @author Yoav Artzi
 */
public class JointInferenceFilterUtils {

	public static <MR, ESTEP, ERESULT> IJointInferenceFilter<MR, ESTEP, ERESULT> stubFalse() {
		return new IJointInferenceFilter<MR, ESTEP, ERESULT>() {

			@Override
			public boolean test(ParsingOp<MR> op) {
				return false;
			}

			@Override
			public boolean testResult(ERESULT result) {
				return false;
			}

			@Override
			public boolean testStep(ESTEP step) {
				return false;
			}
		};
	}

	public static <MR, ESTEP, ERESULT> IJointInferenceFilter<MR, ESTEP, ERESULT> stubTrue() {
		return new IJointInferenceFilter<MR, ESTEP, ERESULT>() {

			@Override
			public boolean test(ParsingOp<MR> op) {
				return true;
			}

			@Override
			public boolean testResult(ERESULT result) {
				return true;
			}

			@Override
			public boolean testStep(ESTEP step) {
				return true;
			}
		};
	}

}
