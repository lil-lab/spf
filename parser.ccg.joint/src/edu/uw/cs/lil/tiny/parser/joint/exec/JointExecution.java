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
package edu.uw.cs.lil.tiny.parser.joint.exec;

import edu.uw.cs.lil.tiny.parser.joint.IJointDerivation;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;

/**
 * Single execution wrapper for joint inference. The final execution output is
 * simply the evaluation result. This class provides no access to the underlying
 * semantics (for that see {@link MaxSemanticsJointExecution}).
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic formal meaning representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 */
public class JointExecution<MR, ERESULT> extends
		AbstractJointExecution<MR, ERESULT, ERESULT> {
	
	public JointExecution(IJointDerivation<MR, ERESULT> jointDerivation,
			IJointDataItemModel<MR, ERESULT> dataItemModel) {
		super(jointDerivation, dataItemModel);
	}
	
	@Override
	public ERESULT getResult() {
		return jointDerivation.getResult();
	}
	
}
