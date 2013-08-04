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

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.TestServices;

public class LambdaWrappedTest {
	
	public LambdaWrappedTest() {
		new TestServices();
	}
	
	@Test
	public void test() {
		final LogicalExpression wrapped = LambdaWrapped.of(LogicalExpression
				.parse("boo:<e,t>", false));
		final LogicalExpression expected = LogicalExpression.parse(
				"(lambda $0:e (boo:<e,t> $0))", false);
		Assert.assertEquals(expected, wrapped);
	}
	
	@Test
	public void test2() {
		final LogicalExpression wrapped = LambdaWrapped.of(LogicalExpression
				.parse("boo:<e,<e,t>>", false));
		final LogicalExpression expected = LogicalExpression.parse(
				"(lambda $0:e (lambda $1:e (boo:<e,<e,t>> $0 $1))", false);
		Assert.assertEquals(expected, wrapped);
	}
	
	@Test
	public void test3() {
		final LogicalExpression wrapped = LambdaWrapped.of(LogicalExpression
				.parse("boo:e", false));
		final LogicalExpression expected = LogicalExpression.parse("boo:e",
				false);
		Assert.assertEquals(expected, wrapped);
	}
	
	@Test
	public void test4() {
		final LogicalExpression wrapped = LambdaWrapped.of(LogicalExpression
				.parse("(lambda $0:<e,t> $0)", false));
		final LogicalExpression expected = LogicalExpression.parse(
				"(lambda $0:<e,t> (lambda $1:e ($0 $1)))", false);
		Assert.assertEquals(expected, wrapped);
	}
	
	@Test
	public void test5() {
		final LogicalExpression wrapped = LambdaWrapped.of(LogicalExpression
				.parse("(lambda $0:e (boo:<e,<e,t>> $0))", false));
		final LogicalExpression expected = LogicalExpression.parse(
				"(lambda $0:e (lambda $1:e (boo:<e,<e,t>> $0 $1)))", false);
		Assert.assertEquals(expected, wrapped);
	}
	
	@Test
	public void test6() {
		final LogicalExpression wrapped = LambdaWrapped
				.of(LogicalExpression
						.parse("(lambda $0:e (intersect:<e,<e,t>> (orient:<e,<e,e>> x:e $0)))",
						
						false));
		final LogicalExpression expected = LogicalExpression
				.parse("(lambda $0:e (lambda $1:e (intersect:<e,<e,t>> (orient:<e,<e,e>> x:e $0) $1)))",
						false);
		Assert.assertEquals(expected, wrapped);
	}
	
}
