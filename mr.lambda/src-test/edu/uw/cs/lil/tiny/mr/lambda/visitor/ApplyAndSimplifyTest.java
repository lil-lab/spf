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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.TestServices;

public class ApplyAndSimplifyTest {
	
	public ApplyAndSimplifyTest() {
		// Make sure test services is initialized
		new TestServices();
	}
	
	@Test
	public void test1() {
		final LogicalExpression e1 = LogicalExpression
				.parse("(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (boo:<e,t> $1) ($0 $1))))",
						false);
		final LogicalExpression a1 = LogicalExpression
				.parse("doo:<e,t>", false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression.parse(
				"(lambda $0:e (and:<t*,t> (boo:<e,t> $0) (doo:<e,t> $0)))",
				false);
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}
	
	@Test
	public void test2() {
		final LogicalExpression e1 = LogicalExpression.parse("goo:<<e,t>,t>",
				false);
		final LogicalExpression a1 = LogicalExpression
				.parse("doo:<e,t>", false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression.parse(
				"(goo:<<e,t>,t> doo:<e,t>)", false);
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}
	
	@Test
	public void test3() {
		final LogicalExpression e1 = LogicalExpression.parse(
				"(lambda $0:e (lambda $1:e (boo:<e,<e,t>> $0 $1)))", false);
		final LogicalExpression a1 = LogicalExpression.parse("goo:e", false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression.parse(
				"(boo:<e,<e,t>> goo:e)", false);
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}
	
	@Test
	public void test4() {
		final LogicalExpression e1 = LogicalExpression.parse(
				"(boo:<e,<e,t>> go:e)", false);
		final LogicalExpression a1 = LogicalExpression.parse("bo:e", false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression.parse(
				"(boo:<e,<e,t>> go:e bo:e)", false);
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}
	
	@Test
	public void test5() {
		final LogicalExpression e1 = LogicalExpression.parse(
				"(and:<t*,t> go:t)", false);
		final LogicalExpression a1 = LogicalExpression.parse("do:t", false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected1 = LogicalExpression.parse(
				"(and:<t*,t> go:t do:t)", false);
		assertTrue(String.format("%s != %s", r1, expected1),
				expected1.equals(r1));
	}
	
	@Test
	public void test6() {
		final LogicalExpression e1 = LogicalExpression.parse(
				"(and:<t*,t> go:t do:t)", false);
		final LogicalExpression a1 = LogicalExpression.parse("lo:t", false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		assertTrue(String.format("%s != %s", r1, null), r1 == null);
	}
	
	@Test
	public void test7() {
		final LogicalExpression e1 = LogicalExpression
				.parse("(lambda $0:<e+,e> ($0 (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>)))))",
						false);
		final LogicalExpression a1 = LogicalExpression.parse("do_seq:<e+,e>",
				false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		final LogicalExpression expected = LogicalExpression
				.parse("(do_seq:<e+,e> (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>))))",
						false);
		assertTrue(String.format("%s != %s", r1, expected), r1.equals(expected));
	}
	
	@Test
	public void test8() {
		final LogicalExpression e1 = LogicalExpression.parse(
				"(lambda $0:e (and:<t*,t> (p:<e,t> $0) (q:<e,t> $0)))", false);
		final LogicalExpression a1 = LogicalExpression.parse(
				"(a:<<e,t>,e> (lambda $0:e (r:<e,t> $0)))", false);
		final LogicalExpression expected = LogicalExpression
				.parse("(and:<t*,t> (p:<e,t> (a:<<e,t>,e> (lambda $0:e (r:<e,t> $0)))) (q:<e,t> (a:<<e,t>,e> (lambda $1:e (r:<e,t> $1)))))",
						false);
		final LogicalExpression r1 = ApplyAndSimplify.of(e1, a1);
		assertTrue(String.format("%s != %s", r1, expected), r1.equals(expected));
	}
}
