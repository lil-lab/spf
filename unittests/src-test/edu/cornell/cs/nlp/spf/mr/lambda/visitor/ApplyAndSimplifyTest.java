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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;

public class ApplyAndSimplifyTest {

	public ApplyAndSimplifyTest() {
		// Make sure test services is initialized
		TestServices.init();
	}

	@Test
	public void test1() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (boo:<e,t> $1) ($0 $1))))");
		final LogicalExpression a1 = LogicalExpression.read("doo:<e,t>");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (boo:<e,t> $0) (doo:<e,t> $0)))");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test10() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:t (or:<t*,t> (boo:<e,t> foo:e) $0))");
		final LogicalExpression a1 = LogicalExpression
				.read("(or:<t*,t> (goo:<e,t> foo:e) (koo:<e,t> foo:e))");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(or:<t*,t> (goo:<e,t> foo:e) (koo:<e,t> foo:e) (boo:<e,t> foo:e))");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test11() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:t (or:<t*,t> (boo:<e,t> foo:e) $0))");
		final LogicalExpression a1 = LogicalExpression
				.read("(and:<t*,t> (goo:<e,t> foo:e) (koo:<e,t> foo:e))");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(or:<t*,t> (and:<t*,t> (goo:<e,t> foo:e) (koo:<e,t> foo:e)) (boo:<e,t> foo:e))");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test2() {
		final LogicalExpression e1 = LogicalExpression.read("goo:<<e,t>,t>");
		final LogicalExpression a1 = LogicalExpression.read("doo:<e,t>");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(goo:<<e,t>,t> doo:<e,t>)");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test3() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (lambda $1:e (boo:<e,<e,t>> $0 $1)))");
		final LogicalExpression a1 = LogicalExpression.read("goo:e");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(lambda $0:e (boo:<e,<e,t>> goo:e $0))");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test4() {
		final LogicalExpression e1 = LogicalExpression
				.read("(boo:<e,<e,t>> go:e)");
		final LogicalExpression a1 = LogicalExpression.read("bo:e");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(boo:<e,<e,t>> go:e bo:e)");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test5() {
		final LogicalExpression e1 = LogicalExpression
				.read("(and:<t*,t> go:t)");
		final LogicalExpression a1 = LogicalExpression.read("do:t");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(and:<t*,t> go:t do:t)");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

	@Test
	public void test6() {
		final LogicalExpression e1 = LogicalExpression
				.read("(and:<t*,t> go:t do:t)");
		final LogicalExpression a1 = LogicalExpression.read("lo:t");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		assertTrue(String.format("%s != %s", r1, null), r1 == null);
	}

	@Test
	public void test7() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:<e,<e,e>> ($0 "
						+ "(do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) "
						+ "(do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>)))))");
		final LogicalExpression a1 = LogicalExpression.read("do_seq:<e+,e>");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected = LogicalExpression
				.read("(do_seq:<e+,e> (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>))))");
		assertTrue(String.format("%s != %s", r1, expected), r1.equals(expected));
	}

	@Test
	public void test8() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:e (and:<t*,t> (p:<e,t> $0) (q:<e,t> $0)))");
		final LogicalExpression a1 = LogicalExpression
				.read("(a:<<e,t>,e> (lambda $0:e (r:<e,t> $0)))");
		final LogicalExpression expected = LogicalExpression
				.read("(and:<t*,t> (p:<e,t> (a:<<e,t>,e> (lambda $0:e (r:<e,t> $0)))) (q:<e,t> (a:<<e,t>,e> (lambda $1:e (r:<e,t> $1)))))");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		assertTrue(String.format("%s != %s", r1, expected), r1.equals(expected));
	}

	@Test
	public void test9() {
		final LogicalExpression e1 = LogicalExpression
				.read("(lambda $0:t (and:<t*,t> (boo:<e,t> foo:e) $0))");
		final LogicalExpression a1 = LogicalExpression
				.read("(and:<t*,t> (goo:<e,t> foo:e) (koo:<e,t> foo:e))");
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression
				.read("(and:<t*,t> (goo:<e,t> foo:e) (koo:<e,t> foo:e) (boo:<e,t> foo:e))");
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}

}
