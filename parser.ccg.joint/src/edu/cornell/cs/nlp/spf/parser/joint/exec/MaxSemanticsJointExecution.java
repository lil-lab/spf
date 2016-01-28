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
package edu.cornell.cs.nlp.spf.parser.joint.exec;

import edu.cornell.cs.nlp.spf.parser.joint.IJointDerivation;
import edu.cornell.cs.nlp.spf.parser.joint.model.IJointDataItemModel;
import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Single execution wrapper for joint inference. The final execution output is a
 * pair of the max-scoring logical form (if a unique one exists) and the
 * evaluation result.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic formal meaning representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 */
public class MaxSemanticsJointExecution<MR, ERESULT> extends
		AbstractJointExecution<MR, ERESULT, Pair<MR, ERESULT>> {
	
	public MaxSemanticsJointExecution(
			IJointDerivation<MR, ERESULT> jointDerivation,
			IJointDataItemModel<MR, ERESULT> dataItemModel) {
		super(jointDerivation, dataItemModel);
	}
	
	@Override
	public Pair<MR, ERESULT> getResult() {
		return Pair.of(
				jointDerivation.getMaxSemantics().size() == 1 ? jointDerivation
						.getMaxSemantics().get(0) : null, jointDerivation
						.getResult());
	}
	
}
