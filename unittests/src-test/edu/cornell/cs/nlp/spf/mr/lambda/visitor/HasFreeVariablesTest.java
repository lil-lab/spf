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
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.HasFreeVariables;

public class HasFreeVariablesTest {

	public HasFreeVariablesTest() {
		// Make sure test services is initialized
		TestServices.init();
	}

	@Test
	public void test() {
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e (pred1:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (pred2:<e,t> $1)))))");
		Assert.assertFalse(HasFreeVariables.of(exp));
		Assert.assertTrue(HasFreeVariables.of(((Lambda) exp).getBody()));
	}

}
