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
import org.junit.Ignore;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class SkolemIDTest {

	public SkolemIDTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (p:<e,<e,t>> $0 (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t))))");
		final LogicalExpression exp2 = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (p:<e,<e,t>> $0 (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t))))");
		Assert.assertEquals(exp, exp2);
	}

	@Test
	public void test2() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (p:<e*,t> (ref:<id,e> !5) (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t))))");
		final LogicalExpression exp2 = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (p:<e*,t> (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t)) (ref:<id,e> !5)))");
		Assert.assertEquals(exp, exp2);
	}

	@Test
	public void test3() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(boo:<id,<<e,t>,t>> !5 (lambda $0:e (p:<e*,t> (io:<id,<<e,t>,e>> !6 (lambda $1:e true:t)) (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t)))))");
		final LogicalExpression exp2 = TestServices.CATEGORY_SERVICES
				.readSemantics("(boo:<id,<<e,t>,t>> !6 (lambda $0:e (p:<e*,t> (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t)) (io:<id,<<e,t>,e>> !6 (lambda $1:e true:t)))))");
		Assert.assertEquals(exp, exp2);

	}

	@Test
	public void test4() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(boo:<id,<<e,t>,t>> !5 (lambda $0:e (p:<e*,t> (io:<id,<<e,t>,e>> !6 (lambda $1:e false:t)) (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t)))))");
		final LogicalExpression exp2 = TestServices.CATEGORY_SERVICES
				.readSemantics("(boo:<id,<<e,t>,t>> !6 (lambda $0:e (p:<e*,t> (io:<id,<<e,t>,e>> !5 (lambda $1:e true:t)) (io:<id,<<e,t>,e>> !6 (lambda $1:e false:t)))))");
		Assert.assertNotSame(exp, exp2);
	}

	@Ignore
	@Test
	public void test5() {
		// This failure is expected due the way in which we compare skolem IDs.
		// The case is ignored, but left here as a note.
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(p:<<e,t>,<e,t>> (lambda $0:e (and:<t*,t> (p:<e,<e,t>> (ref:<id,e> !1) $0) (p:<e,<e,t>> (ref:<id,e> !2) $0))) (ref:<id,e> !2))");
		final LogicalExpression exp2 = TestServices.CATEGORY_SERVICES
				.readSemantics("(p:<<e,t>,<e,t>> (lambda $0:e (and:<t*,t> (p:<e,<e,t>> (ref:<id,e> !2) $0) (p:<e,<e,t>> (ref:<id,e> !1) $0))) (ref:<id,e> !2))");
		Assert.assertEquals(exp, exp2);
	}

}
