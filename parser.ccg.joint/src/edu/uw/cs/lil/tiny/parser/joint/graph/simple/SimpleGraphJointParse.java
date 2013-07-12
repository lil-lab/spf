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
package edu.uw.cs.lil.tiny.parser.joint.graph.simple;

import edu.uw.cs.lil.tiny.parser.graph.IGraphParse;
import edu.uw.cs.lil.tiny.parser.joint.JointParse;

public class SimpleGraphJointParse<MR, ERESULT> extends JointParse<MR, ERESULT> {
	
	private final IGraphParse<MR>	baseParse;
	
	public SimpleGraphJointParse(IGraphParse<MR> baseParse,
			DeterministicExecResultWrapper<?, ERESULT> execResult) {
		super(baseParse, execResult);
		this.baseParse = baseParse;
	}
	
	public IGraphParse<MR> getBaseParse() {
		return baseParse;
	}
	
}
