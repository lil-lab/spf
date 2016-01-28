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

public class LogicalExpressionEqualsTest {

	public LogicalExpressionEqualsTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (boo:<e,t> $0))");
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (boo:<e,t> $0))");
		Assert.assertTrue(e1.equals(e2));
	}

	@Test
	public void test2() {
		final LogicalExpression e1 = LogicalExpression.read("(boo:<e,t> $0:e)");
		final LogicalExpression e2 = LogicalExpression.read("(boo:<e,t> $0:e)");
		Assert.assertFalse(e1.equals(e2));
	}

	@Test
	public void test3() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (boo:<e,t> $0))");
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $1:e (boo:<e,t> $0:e))");
		Assert.assertFalse(e1.equals(e2));
	}

	@Test
	public void test4() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $0) (foo:<e,t> $1:e)))");
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $0) (foo:<e,t> $0)))");
		Assert.assertFalse(e1.equals(e2));
	}

	@Test
	public void test5() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $0) (foo:<e,t> $0)))");
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $0) (foo:<e,t> $1:e)))");
		Assert.assertFalse(e1.equals(e2));
	}

	@Test
	public void test6() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $1:e) (foo:<e,t> $1)))");
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $1:e) (foo:<e,t> $0)))");
		Assert.assertFalse(e1.equals(e2));
	}

	@Test
	public void test7() {
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
	}

	@Test
	public void test8() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> (place:<p,t> $1) (exists:<<e,t>,t> (lambda $2:e (and:<t*,t> (state:<s,t> $2) (loc:<lo,<lo,t>> $2 $2)))) (equals:<e,<e,t>> $0 $1)))))");
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> (place:<p,t> $1) (exists:<<e,t>,t> (lambda $2:e (and:<t*,t> (state:<s,t> $2) (loc:<lo,<lo,t>> $2 $1)))) (equals:<e,<e,t>> $0 $1)))))");
		Assert.assertFalse(e1.equals(e2));
	}

	@Test
	public void test9() {
		final Variable variable = new Variable(LogicLanguageServices
				.getTypeRepository().getEntityType());
		final LogicalExpression e1 = new Lambda(variable, new Lambda(variable,
				new Literal(LogicalConstant.read("pred:<e,<e,t>>"),
						ArrayUtils.create(variable, variable))));
		final LogicalExpression e2 = LogicalExpression
				.read("(lambda $0:e (lambda $1:e (pred:<e,<e,t>> $0 $1)))");
		Assert.assertNotEquals(e2, e1);
		Assert.assertNotEquals(e1, e2);
	}

}
