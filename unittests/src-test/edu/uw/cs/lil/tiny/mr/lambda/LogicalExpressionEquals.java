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
package edu.uw.cs.lil.tiny.mr.lambda;

import org.junit.Assert;

import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;

public class LogicalExpressionEquals {
	
	public LogicalExpressionEquals() {
		new TestServices();
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
		final LogicalExpression e1 = LogicalExpression
				.read("(boo:<e,t> $0:e)");
		final LogicalExpression e2 = LogicalExpression
				.read("(boo:<e,t> $0:e)");
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
	
}
