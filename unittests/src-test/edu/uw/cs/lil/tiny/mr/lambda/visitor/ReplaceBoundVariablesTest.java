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
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import org.junit.Assert;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

public class ReplaceBoundVariablesTest {
	
	public ReplaceBoundVariablesTest() {
		// Make sure test services is initialized
		new TestServices();
	}
	
	@Test
	public void test() {
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e $0)");
		final LogicalExpression result = ReplaceBoundVariables.of(exp);
		Assert.assertEquals(exp, result);
		Assert.assertNotSame(((Lambda) exp).getArgument(),
				((Lambda) result).getArgument());
	}
	
}
