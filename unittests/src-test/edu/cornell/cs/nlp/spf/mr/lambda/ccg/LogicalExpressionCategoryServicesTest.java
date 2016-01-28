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
package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;

public class LogicalExpressionCategoryServicesTest {

	public LogicalExpressionCategoryServicesTest() {
		TestServices.init();
	}

	@Test
	public void apply1() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/(NP|(NP|NP)) : (lambda $0:<e,<e,e>> ($0 (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>)))))");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().read("NP|(NP|NP) : do_seq:<e+,e>");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S : (do_seq:<e+,e> (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>))))");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}

	@Test
	public void apply2() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("N[x]/N[x] : (lambda $0:<e,t> $0)");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().read("N : boo:<e,t>");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices().read("N : boo:<e,t>");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}

	@Test
	public void apply3() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("N[x]/N[x] : (lambda $0:<e,t> $0)");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().read("N[pl] : boo:<e,t>");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices().read("N[pl] : boo:<e,t>");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}

	@Test
	public void apply4() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("N[b]/N[b] : (lambda $0:<e,t> $0)");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().read("N[pl] : boo:<e,t>");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		Assert.assertNull(r1);
	}

	@Test
	public void apply5() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\(N[x]/N[x]) : (lambda $0:<<e,t>,<e,t>> (a:<id,<<e,t>,e>> na:id (lambda $1:e ($0 country:<e,t> $1))))");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices()
				.read("N[x]/N[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (c_op:<e,<e,t>> $2 Saudi++Arabia:e) (name:<e,t> $2))))))))");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S : (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (country:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (name:<e,t> $1) (c_op:<e,<e,t>> $1 Saudi++Arabia:e))))))))");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}

	@Test
	public void apply6() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/S[x]\\S[x] : (lambda $0:<e,t> (lambda $1:<e,t> (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (and:<e,t> $2) (c_op1:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e ($0 $3)))) (c_op2:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (military:<e,t> $4)))) (c_op2:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $5:e ($1 $5)))))))))");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().read(
						"S[dcl] : (lambda $0:e (international:<e,t> $0))");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S/S[dcl] : (lambda $0:<e,t> (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (and:<e,t> $1) (c_op1:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (international:<e,t> $2)))) (c_op2:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $3:e (military:<e,t> $3)))) (c_op2:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $4:e ($0 $4))))))))");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}

	@Test
	public void apply7() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("N[x]/N[x] : (lambda $0:<e,t> $0)");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().read("N : boo:<e,t>");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices().read("N : boo:<e,t>");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}

	@Test
	public void compose1() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.readSemantics("f:<<e,t>,t>");
		final LogicalExpression g = TestServices.getCategoryServices()
				.readSemantics("g:<<e,t>,<e,t>>");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (f:<<e,t>,t> (g:<<e,t>,<e,t>> $0)))");
		final LogicalExpression result = TestServices.getCategoryServices()
				.compose(f, g, 1);
		expected.equals(result);
		assertEquals(expected, result);
	}

	@Test
	public void compose10() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:<e,t> (lambda $0:e (and:<t*,t> ($1 $0) (foo:<e,<e,t>> $0 a:e))))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $2:e (lambda $0:t (lambda $1:e (and:<t*,t> (boo:<e,t> $1) (goo:<e,<t,t>> $1 $0) (too:<e,<e,t>> $1 $2)))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $2:e (lambda $0:t (lambda $1:e (and:<t*,t> (boo:<e,t> $1) (goo:<e,<t,t>> $1 $0) (too:<e,<e,t>> $1 $2) (foo:<e,<e,t>> $1 a:e)))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 2);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose11() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/NP : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> "
						+ "(lambda $11:e (and:<t*,t> (boo:<e,t> $11) "
						+ "(goo:<e,<e,t>> $11 (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0))))))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 3, false);
			assertTrue(
					String.format("Expected: %s\nGot: %s", expected, result),
					expected.equals(result));
		}

		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 1, false);
			Assert.assertNull(result);
		}

	}

	@Test
	public void compose12() {
		final ComplexCategory<LogicalExpression> shifting = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("(S\\NP)\\(S\\NP)/AP : (lambda $3:<e,t> (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) ($3 $2))))))");
		final Category<LogicalExpression> left = TestServices
				.getCategoryServices()
				.read("AP : (lambda $0:e (c_location:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (budget:<e,t> $1)))))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices()
				.read("AP : (lambda $0:e (c_time:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (date-entity:<e,t> $1) (c_year:<e,<i,t>> $1 2008:i) (c_month:<e,<i,t>> $1 5:i) (c_day:<e,<i,t>> $1 5:i))))))");

		final Category<LogicalExpression> shiftedRight = TestServices
				.getCategoryServices().apply(shifting, right);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.read("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i)))))))))"),
				shiftedRight);

		final Category<LogicalExpression> shiftedLeft = TestServices
				.getCategoryServices().apply(shifting, left);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.read("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3))))))))"),
				shiftedLeft);

		final Category<LogicalExpression> result = TestServices
				.getCategoryServices().compose(
						(ComplexCategory<LogicalExpression>) shiftedLeft,
						(ComplexCategory<LogicalExpression>) shiftedRight, 1,
						false);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
						+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
		Assert.assertEquals(expected, result);
	}

	@Test
	public void compose13() {
		final ComplexCategory<LogicalExpression> shifting = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\S/AP : (lambda $3:<e,t> (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) ($3 $1)))))");
		final Category<LogicalExpression> left = TestServices
				.getCategoryServices()
				.read("AP : (lambda $0:e (c_location:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (budget:<e,t> $1)))))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices()
				.read("AP : (lambda $0:e (c_time:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (date-entity:<e,t> $1) (c_year:<e,<i,t>> $1 2008:i) (c_month:<e,<i,t>> $1 5:i) (c_day:<e,<i,t>> $1 5:i))))))");

		final Category<LogicalExpression> shiftedRight = TestServices
				.getCategoryServices().apply(shifting, right);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.read("S\\S : (lambda $0:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))))))"),
				shiftedRight);

		final Category<LogicalExpression> shiftedLeft = TestServices
				.getCategoryServices().apply(shifting, left);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.read("S\\S : (lambda $0:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3)))))))"),
				shiftedLeft);

		final Category<LogicalExpression> adjuncts = TestServices
				.getCategoryServices().compose(
						(ComplexCategory<LogicalExpression>) shiftedLeft,
						(ComplexCategory<LogicalExpression>) shiftedRight, 1,
						false);
		final Category<LogicalExpression> adjunctsExpected = TestServices
				.getCategoryServices()
				.read("S\\S : (lambda $0:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
						+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4)))))))");
		Assert.assertEquals(adjunctsExpected, adjuncts);

		final Category<LogicalExpression> transitiveVerb = TestServices
				.getCategoryServices()
				.read("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0)))))");

		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true);
			final Category<LogicalExpression> actual = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) transitiveVerb, 2,
					false);
			final Category<LogicalExpression> expected = TestServices
					.getCategoryServices()
					.read("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0)"
							+ "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
							+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
			Assert.assertEquals(expected, actual);
		}

		// TODO
		// {
		// final LogicalExpressionCategoryServices cs = new
		// LogicalExpressionCategoryServices(
		// true);
		// final Category<LogicalExpression> actual = cs.compose(
		// (ComplexCategory<LogicalExpression>) adjuncts,
		// (ComplexCategory<LogicalExpression>) transitiveVerb, 2,
		// true);
		// final Category<LogicalExpression> expected = TestServices
		// .getCategoryServices()
		// .read("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0)"
		// +
		// "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
		// +
		// "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
		// Assert.assertEquals(expected, actual);
		// }

		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true);
			final Category<LogicalExpression> actual = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) transitiveVerb, 1,
					true);
			Assert.assertNull(actual);
		}

		final Category<LogicalExpression> imperativeVerb = TestServices
				.getCategoryServices()
				.read("S/NP : (lambda $0:e (lambda $1:e (and:<t*,t> (safeguard-01:<e,t> $1) (c_ARG1:<e,<e,t>> $1 $0))))");

		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true);
			final Category<LogicalExpression> result = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) imperativeVerb, 1,
					false);
			Assert.assertNull(result);
		}

		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					false);
			final Category<LogicalExpression> actual = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) imperativeVerb, 1,
					true);
			final Category<LogicalExpression> expected = TestServices
					.getCategoryServices()
					.read("S/NP : (lambda $0:e  (lambda $2:e (and:<t*,t>  (safeguard-01:<e,t> $2) (c_ARG1:<e,<e,t>> $2 $0)"
							+ "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
							+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
			Assert.assertEquals(expected, actual);
		}

	}

	@Test
	public void compose14() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("S\\S : (lambda $0:t $0)");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("S/S : (lambda $0:t $0)");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
				true);
		final Category<LogicalExpression> result = cs.compose(f, g, 8, false);
		Assert.assertNull(result);
	}

	@Test
	public void compose15() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/NP[d] : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP[d]/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> "
						+ "(lambda $11:e (and:<t*,t> (boo:<e,t> $11) "
						+ "(goo:<e,<e,t>> $11 (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0))))))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 3, false);
			assertTrue(
					String.format("Expected: %s\nGot: %s", expected, result),
					expected.equals(result));
		}

		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 2, false);
			Assert.assertNull(result);
		}

	}

	@Test
	public void compose16() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/NP[d] : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> "
						+ "(lambda $11:e (and:<t*,t> (boo:<e,t> $11) "
						+ "(goo:<e,<e,t>> $11 (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0))))))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 3, false);
			assertTrue(
					String.format("Expected: %s\nGot: %s", expected, result),
					expected.equals(result));
		}

		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 2, false);
			Assert.assertNull(result);
		}
	}

	@Test
	public void compose17() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/NP[d] : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP[k]/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 3, false);
			Assert.assertNull(result);
		}

		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 2, false);
			Assert.assertNull(result);
		}

	}

	@Test
	public void compose18() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[x]/NP[x] : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> "
						+ "(lambda $11:e (and:<t*,t> (boo:<e,t> $11) "
						+ "(goo:<e,<e,t>> $11 (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0))))))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 3, false);
			assertTrue(
					String.format("Expected: %s\nGot: %s", expected, result),
					expected.equals(result));
		}

		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 2, false);
			Assert.assertNull(result);
		}

	}

	@Test
	public void compose19() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[x]/NP[x] : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP[b]/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S[b]/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> "
						+ "(lambda $11:e (and:<t*,t> (boo:<e,t> $11) "
						+ "(goo:<e,<e,t>> $11 (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0))))))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 3, false);
			assertTrue(
					String.format("Expected: %s\nGot: %s", expected, result),
					expected.equals(result));
		}

		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 2, false);
			Assert.assertNull(result);
		}

	}

	@Test
	public void compose2() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.readSemantics("f:<e,t>");
		final LogicalExpression g = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e $0)");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (f:<e,t> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose3() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (f:<e,t> $0))");
		final LogicalExpression g = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e $0)");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (f:<e,t> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose4() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e $0)");
		final LogicalExpression g = TestServices.getCategoryServices()
				.readSemantics("boo:<e,e>");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (boo:<e,e> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose5() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:t (and:<t*,t> true:t $0))");
		final LogicalExpression g = TestServices.getCategoryServices()
				.readSemantics("g:<e,t>");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (g:<e,t> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose6() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices.getCategoryServices()
				.readSemantics("(do:<p,<m,s>> goal:p)");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) (do:<p,<m,s>> goal:p $0)))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose7() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:m (do:<p,<m,s>> goal:p $0))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) (do:<p,<m,s>> goal:p $0)))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 3);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}

	@Test
	public void compose8() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:m (do:<p,<m,s>> goal:p $0))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		Assert.assertNull(result);
	}

	@Test
	public void compose9() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:m (do:<p,<m,s>> goal:p $0))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 8);
		Assert.assertNull(result);
	}

	@Test
	public void hashTest() {
		final Category<LogicalExpression> c1 = TestServices
				.getCategoryServices()
				.read("N[x]\\N[x]/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:e (and:<t*,t>  (c_ARGX:<e,<e,t>> $2 $0) (c_ARGX-of:<e,<e,t>> $2 boo:e)))))");
		final Category<LogicalExpression> c2 = TestServices
				.getCategoryServices()
				.read("N[x]\\N[x]/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:e (and:<t*,t>  (c_ARGX-of:<e,<e,t>> $2 $0) (c_ARGX:<e,<e,t>> $2 boo:e)))))");
		Assert.assertNotEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	public void hashTest2() {
		final Category<LogicalExpression> c1 = TestServices
				.getCategoryServices()
				.read("N[x]\\N[x]/NP : (lambda $0:e (lambda $2:e (c_ARGX:<e,<e,t>> $2 $0)))");
		final Category<LogicalExpression> c2 = TestServices
				.getCategoryServices()
				.read("N[x]\\N[x]/NP : (lambda $0:e (lambda $2:e (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (draft-01:<e,t> $4))))))");
		Assert.assertNotEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	public void hashTest3() {
		final Category<LogicalExpression> c1 = TestServices
				.getCategoryServices()
				.read("S[pt]\\NP[x]/(S[to]\\NP[x]) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_ARGX-of:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (little:<e,t> $4)))) (limit-01:<e,t> $3) (c_ARGX:<e,<e,t>> $3 (ref:<id,e> na:id)) (c_ARGX:<e,<e,t>> $3 (ref:<id,e> na:id))))))))))");
		final Category<LogicalExpression> c2 = TestServices
				.getCategoryServices()
				.read("S[pt]\\NP[x]/(S[to]\\NP[x]) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_ARGX-of:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (little:<e,t> $4)))) (limit-01:<e,t> $3)))))))))");
		Assert.assertNotEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	public void hashTest4() {
		final Category<LogicalExpression> c1 = TestServices
				.getCategoryServices()
				.read("S[dcl] : (lambda $0:e (and:<t*,t> (state-01:<e,t> $0) (c_ARGX:<e,<e,t>> $0 (ref:<id,e> na:id)) (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (little:<e,t> $2)))) (limit-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (clamp-01:<e,t> $3) (c_ARGX:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (extremism:<e,t> $4)))) (c_ARGX:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (c_REL:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (c_op:<e,<e,t>> $6 Pervez++Musharraf:e) (name:<e,t> $6))))) (person:<e,t> $5))))))))) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (c_ARGX-of:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $8:e (ban-01:<e,t> $8)))) (c_REL:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $9:e (religious:<e,t> $9)))) (demand-01:<e,t> $7) (c_REL:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $10:e (extremism:<e,t> $10))))))))))))))");
		final Category<LogicalExpression> c2 = TestServices
				.getCategoryServices()
				.read("S[dcl] : (lambda $0:e (and:<t*,t> (state-01:<e,t> $0) (c_ARGX:<e,<e,t>> $0 (ref:<id,e> na:id)) (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (little:<e,t> $2)))) (limit-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (clamp-01:<e,t> $3) (c_ARGX:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (extremism:<e,t> $4)))) (c_ARGX:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (c_REL:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (c_op:<e,<e,t>> $6 Pervez++Musharraf:e) (name:<e,t> $6))))) (person:<e,t> $5))))))))) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (c_ARGX-of:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $8:e (ban-01:<e,t> $8)))) (c_REL:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $9:e (religious:<e,t> $9)))) (demand-01:<e,t> $7) (c_ARGX:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $10:e (extremism:<e,t> $10))))))))))))))");
		Assert.assertNotEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	public void hashTest5() {
		final Category<LogicalExpression> c1 = TestServices
				.getCategoryServices()
				.read("S[dcl]\\NP\\N[x]/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:e (lambda $3:e (and:<t*,t> (state-01:<e,t> $3) (c_ARGX:<e,<e,t>> $3 $2) (c_ARGX:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (c_REL:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $5:e (little:<e,t> $5)))) (limit-01:<e,t> $4) (c_ARGX:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (c_ARGX-of:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (ban-01:<e,t> $7)))) (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $8:e (religious:<e,t> $8)))) ($1 $6) (c_REL:<e,<e,t>> $6 $0) (c_ARGX-of:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $9:e (and:<t*,t> (clamp-01:<e,t> $9) (c_ARGX:<e,<e,t>> $9 (a:<id,<<e,t>,e>> na:id (lambda $10:e (extremism:<e,t> $10)))))))) (c_ARGX:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $11:e (and:<t*,t> (c_REL:<e,<e,t>> $11 (a:<id,<<e,t>,e>> na:id (lambda $12:e (and:<t*,t> (c_op:<e,<txt,t>> $12 Pervez++Musharraf:txt) (name:<e,t> $12))))) (person:<e,t> $11))))))))) (c_ARGX:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $13:e (demand-01:<e,t> $13)))))))))))))");
		final Category<LogicalExpression> c2 = TestServices
				.getCategoryServices()
				.read("S[dcl]\\NP\\N[x]/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:e (lambda $3:e (and:<t*,t> (state-01:<e,t> $3) (c_ARGX:<e,<e,t>> $3 $2) (c_ARGX:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (c_REL:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $5:e (little:<e,t> $5)))) (limit-01:<e,t> $4) (c_ARGX:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (c_ARGX-of:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (ban-01:<e,t> $7)))) (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $8:e (religious:<e,t> $8)))) ($1 $6) (c_REL:<e,<e,t>> $6 $0) (c_ARGX-of:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $9:e (and:<t*,t> (clamp-01:<e,t> $9) (c_REL:<e,<e,t>> $9 (a:<id,<<e,t>,e>> na:id (lambda $10:e (extremism:<e,t> $10)))))))) (c_ARGX:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $11:e (and:<t*,t> (c_REL:<e,<e,t>> $11 (a:<id,<<e,t>,e>> na:id (lambda $12:e (and:<t*,t> (c_op:<e,<txt,t>> $12 Pervez++Musharraf:txt) (name:<e,t> $12))))) (person:<e,t> $11))))))))) (c_ARGX:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $13:e (demand-01:<e,t> $13)))))))))))))");
		Assert.assertNotEquals(c1.hashCode(), c2.hashCode());
	}
}
