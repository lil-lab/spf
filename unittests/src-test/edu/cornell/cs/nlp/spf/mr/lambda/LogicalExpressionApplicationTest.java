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
package edu.cornell.cs.nlp.spf.mr.lambda;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.utils.collections.ArrayUtils;

public class LogicalExpressionApplicationTest {

	public LogicalExpressionApplicationTest() {
		TestServices.init();
	}

	@Test
	public void test1() {
		final Variable variable = new Variable(LogicLanguageServices
				.getTypeRepository().getEntityType());
		final LogicalExpression e1 = new Lambda(variable, new Literal(
				LogicalConstant.read("boo:<e,<<e,t>,t>>"), ArrayUtils.create(
						variable,
						new Lambda(variable,
								new Literal(LogicalConstant.read("goo:<e,t>"),
										ArrayUtils.create(variable))))));
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (boo:<e,<<e,t>,t>> $0 (lambda $1:e (goo:<e,t> $1))))");
		Assert.assertEquals(e2, e1);

		final LogicalExpression result1 = TestServices.getCategoryServices()
				.apply(e1, LogicalConstant.read("foo:e"));
		final LogicalExpression result2 = TestServices.getCategoryServices()
				.apply(e2, LogicalConstant.read("foo:e"));
		Assert.assertEquals(result1, result2);
		System.out.println(result1);
	}

}
