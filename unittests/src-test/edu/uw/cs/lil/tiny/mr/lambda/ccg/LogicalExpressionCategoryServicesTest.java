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
package edu.uw.cs.lil.tiny.mr.lambda.ccg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Assert;

import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

public class LogicalExpressionCategoryServicesTest {
	
	@Test
	public void apply1() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("S/(NP|(NP|NP)) : (lambda $0:<e,<e,e>> ($0 (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>)))))");
		final Category<LogicalExpression> a1 = TestServices
				.getCategoryServices().parse("NP|(NP|NP) : do_seq:<e+,e>");
		final Category<LogicalExpression> r1 = TestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.parse("S : (do_seq:<e+,e> (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>))))");
		assertTrue(String.format("%s != %s", r1, expected), expected.equals(r1));
	}
	
	@Test
	public void compose1() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.parseSemantics("f:<<e,t>,t>");
		final LogicalExpression g = TestServices.getCategoryServices()
				.parseSemantics("g:<<e,t>,<e,t>>");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.parseSemantics(
						"(lambda $0:<e,t> (f:<<e,t>,t> (g:<<e,t>,<e,t>> $0)))");
		final LogicalExpression result = TestServices.getCategoryServices()
				.compose(f, g, 0);
		expected.equals(result);
		assertEquals(expected, result);
	}
	
	@Test
	public void compose10() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $1:<e,t> (lambda $0:e (and:<t*,t> ($1 $0) (foo:<e,<e,t>> $0 a:e))))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $2:e (lambda $0:t (lambda $1:e (and:<t*,t> (boo:<e,t> $1) (goo:<e,<t,t>> $1 $0) (too:<e,<e,t>> $1 $2)))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $2:e (lambda $0:t (lambda $1:e (and:<t*,t> (boo:<e,t> $1) (goo:<e,<t,t>> $1 $0) (too:<e,<e,t>> $1 $2) (foo:<e,<e,t>> $1 a:e)))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose11() {
		final ComplexCategory<LogicalExpression> f = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("S/NP : (lambda $10:e (lambda $11:e (and:<t*,t> (boo:<e,t> $11) (goo:<e,<e,t>> $11 $10))))");
		final ComplexCategory<LogicalExpression> g = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("NP/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0)))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.parse("S/N/N/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:<e,t> "
						+ "(lambda $11:e (and:<t*,t> (boo:<e,t> $11) "
						+ "(goo:<e,<e,t>> $11 (io:<<e,t>,e> (lambda $3:e (and:<t*,t> ($2 $3) ($1 $3) (doo:<e,<e,t>> $3 $0))))))))))");
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 2);
			assertTrue(
					String.format("Expected: %s\nGot: %s", expected, result),
					expected.equals(result));
		}
		
		{
			final Category<LogicalExpression> result = TestServices.CATEGORY_SERVICES
					.compose(f, g, 1);
			Assert.assertNull(result);
		}
		
	}
	
	@Test
	public void compose12() {
		final ComplexCategory<LogicalExpression> shifting = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("(S\\NP)\\(S\\NP)/AP : (lambda $3:<e,t> (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) ($3 $2))))))");
		final Category<LogicalExpression> left = TestServices
				.getCategoryServices()
				.parse("AP : (lambda $0:e (c_location:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (budget:<e,t> $1)))))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices()
				.parse("AP : (lambda $0:e (c_time:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (date-entity:<e,t> $1) (c_year:<e,<i,t>> $1 2008:i) (c_month:<e,<i,t>> $1 5:i) (c_day:<e,<i,t>> $1 5:i))))))");
		
		final Category<LogicalExpression> shiftedRight = TestServices
				.getCategoryServices().apply(shifting, right);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.parse("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i)))))))))"),
				shiftedRight);
		
		final Category<LogicalExpression> shiftedLeft = TestServices
				.getCategoryServices().apply(shifting, left);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.parse("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3))))))))"),
				shiftedLeft);
		
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices().compose(
						(ComplexCategory<LogicalExpression>) shiftedLeft,
						(ComplexCategory<LogicalExpression>) shiftedRight, 0);
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.parse("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
						+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
		Assert.assertEquals(expected, result);
	}
	
	@Test
	public void compose13() {
		final ComplexCategory<LogicalExpression> shifting = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("S\\S/AP : (lambda $3:<e,t> (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) ($3 $1)))))");
		final Category<LogicalExpression> left = TestServices
				.getCategoryServices()
				.parse("AP : (lambda $0:e (c_location:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (budget:<e,t> $1)))))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices()
				.parse("AP : (lambda $0:e (c_time:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (date-entity:<e,t> $1) (c_year:<e,<i,t>> $1 2008:i) (c_month:<e,<i,t>> $1 5:i) (c_day:<e,<i,t>> $1 5:i))))))");
		
		final Category<LogicalExpression> shiftedRight = TestServices
				.getCategoryServices().apply(shifting, right);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.parse("S\\S : (lambda $0:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))))))"),
				shiftedRight);
		
		final Category<LogicalExpression> shiftedLeft = TestServices
				.getCategoryServices().apply(shifting, left);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.parse("S\\S : (lambda $0:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3)))))))"),
				shiftedLeft);
		
		final Category<LogicalExpression> adjuncts = TestServices
				.getCategoryServices().compose(
						(ComplexCategory<LogicalExpression>) shiftedLeft,
						(ComplexCategory<LogicalExpression>) shiftedRight, 0);
		final Category<LogicalExpression> adjunctsExpected = TestServices
				.getCategoryServices()
				.parse("S\\S : (lambda $0:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
						+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4)))))))");
		Assert.assertEquals(adjunctsExpected, adjuncts);
		
		final Category<LogicalExpression> transitiveVerb = TestServices
				.getCategoryServices()
				.parse("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0)))))");
		
		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true, true, true);
			final Category<LogicalExpression> result = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) transitiveVerb, 1);
			Assert.assertNull(result);
		}
		
		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true, true, false);
			final Category<LogicalExpression> actual = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) transitiveVerb, 1);
			final Category<LogicalExpression> expected = TestServices
					.getCategoryServices()
					.parse("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0)"
							+ "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
							+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
			Assert.assertEquals(expected, actual);
		}
		
		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true, true, false);
			final Category<LogicalExpression> actual = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) transitiveVerb, 0);
			Assert.assertNull(actual);
		}
		
		final Category<LogicalExpression> imperativeVerb = TestServices
				.getCategoryServices()
				.parse("S/NP : (lambda $0:e (lambda $1:e (and:<t*,t> (safeguard-01:<e,t> $1) (c_ARG1:<e,<e,t>> $1 $0))))");
		
		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true, true, true);
			final Category<LogicalExpression> result = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) imperativeVerb, 0);
			Assert.assertNull(result);
		}
		
		{
			final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices(
					true, true, false);
			final Category<LogicalExpression> actual = cs.compose(
					(ComplexCategory<LogicalExpression>) adjuncts,
					(ComplexCategory<LogicalExpression>) imperativeVerb, 0);
			final Category<LogicalExpression> expected = TestServices
					.getCategoryServices()
					.parse("S/NP : (lambda $0:e  (lambda $2:e (and:<t*,t>  (safeguard-01:<e,t> $2) (c_ARG1:<e,<e,t>> $2 $0)"
							+ "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> "
							+ "(date-entity:<e,t> $3) (c_year:<e,<i,t>> $3 2008:i) (c_month:<e,<i,t>> $3 5:i) (c_day:<e,<i,t>> $3 5:i))))) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (budget:<e,t> $4))))))))");
			Assert.assertEquals(expected, actual);
		}
		
	}
	
	@Test
	public void compose2() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.parseSemantics("f:<e,t>");
		final LogicalExpression g = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e $0)");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e (f:<e,t> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 0);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose3() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e (f:<e,t> $0))");
		final LogicalExpression g = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e $0)");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e (f:<e,t> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 0);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose4() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e $0)");
		final LogicalExpression g = TestServices.getCategoryServices()
				.parseSemantics("boo:<e,e>");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e (boo:<e,e> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 0);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose5() {
		final LogicalExpression f = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:t (and:<t*,t> true:t $0))");
		final LogicalExpression g = TestServices.getCategoryServices()
				.parseSemantics("g:<e,t>");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.parseSemantics("(lambda $0:e (g:<e,t> $0))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 0);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose6() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices.getCategoryServices()
				.parseSemantics("(do:<p,<m,s>> goal:p)");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) (do:<p,<m,s>> goal:p $0)))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 0);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose7() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:m (do:<p,<m,s>> goal:p $0))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) (do:<p,<m,s>> goal:p $0)))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 2);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose8() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:m (do:<p,<m,s>> goal:p $0))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 1);
		Assert.assertNull(result);
	}
	
	@Test
	public void compose9() {
		final LogicalExpression f = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = TestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $2:e (lambda $3:e (lambda $0:m (do:<p,<m,s>> goal:p $0))))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.compose(f, g, 8);
		Assert.assertNull(result);
	}
}
