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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

public class LogicalExpressionCategoryServicesTest {
	
	@Test
	public void apply1() {
		final ComplexCategory<LogicalExpression> e1 = (ComplexCategory<LogicalExpression>) LogicalExpressionTestServices
				.getCategoryServices()
				.parse("S/(NP|(NP|NP)) : (lambda $0:<e+,e> ($0 (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>)))))");
		final Category<LogicalExpression> a1 = LogicalExpressionTestServices
				.getCategoryServices().parse("NP|(NP|NP) : do_seq:<e+,e>");
		final Category<LogicalExpression> r1 = LogicalExpressionTestServices
				.getCategoryServices().apply(e1, a1);
		final Category<LogicalExpression> expected = LogicalExpressionTestServices
				.getCategoryServices()
				.parse("S : (do_seq:<e+,e> (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>))))");
		assertTrue(String.format("%s != %s", r1, expected), r1.equals(expected));
	}
	
	@Test
	public void compose1() {
		final LogicalExpression f = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("f:<<e,t>,t>");
		final LogicalExpression g = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("g:<<e,t>,<e,t>>");
		final LogicalExpression expected = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics(
						"(lambda $0:<e,t> (f:<<e,t>,t> (g:<<e,t>,<e,t>> $0)))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.doSemanticComposition(f, g);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose2() {
		final LogicalExpression f = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("f:<e,t>");
		final LogicalExpression g = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("(lambda $0:e $0)");
		final LogicalExpression expected = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("f:<e,t>");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.doSemanticComposition(f, g);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose3() {
		final LogicalExpression f = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics(
						"(lambda $0:e (f:<e,t> $0))");
		final LogicalExpression g = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("(lambda $0:e $0)");
		final LogicalExpression expected = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("f:<e,t>");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.doSemanticComposition(f, g);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose4() {
		final LogicalExpression f = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("(lambda $0:e $0)");
		final LogicalExpression g = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("boo:<e,e>");
		final LogicalExpression expected = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("boo:<e,e>");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.doSemanticComposition(f, g);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose5() {
		final LogicalExpression f = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics(
						"(lambda $0:t (and:<t*,t> true:t $0))");
		final LogicalExpression g = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("g:<e,t>");
		final LogicalExpression expected = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("g:<e,t>");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.doSemanticComposition(f, g);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
	@Test
	public void compose6() {
		final LogicalExpression f = LogicalExpressionTestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) $0))");
		final LogicalExpression g = LogicalExpressionTestServices
				.getCategoryServices().parseSemantics("(do:<p,<m,s>> goal:p)");
		final LogicalExpression expected = LogicalExpressionTestServices
				.getCategoryServices()
				.parseSemantics(
						"(lambda $0:e (do_seq:<s+,s> (do_until:<s,<t,s>> (do:<p,s> travel:p) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> deadend:<e,t> at:<e,t>))) (do:<p,<m,s>> goal:p $0)))");
		final LogicalExpressionCategoryServices cs = new LogicalExpressionCategoryServices();
		final LogicalExpression result = cs.doSemanticComposition(f, g);
		assertTrue(String.format("Expected: %s\nGot: %s", expected, result),
				expected.equals(result));
	}
	
}
