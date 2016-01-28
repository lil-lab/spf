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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeraising;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;

public class ForwardTypeRaisedCompositionTest {

	public ForwardTypeRaisedCompositionTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final Category<LogicalExpression> primary = TestServices
				.getCategoryServices().read("N : (lambda $1:e (boo:<e,t> $1))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read(
						"(S\\N)/NP : (lambda $0:e (lambda $1:<e,t> ($1 $0)))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices().read(
						"S/NP : (lambda $0:e (boo:<e,t> $0))");

		final ForwardTypeRaisedComposition rule = new ForwardTypeRaisedComposition(
				TestServices.getCategoryServices());
		final ParseRuleResult<LogicalExpression> actual = rule.apply(primary,
				secondary, null);
		Assert.assertEquals(expected, actual.getResultCategory());
	}

	@Test
	public void test2() {
		final Category<LogicalExpression> primary = TestServices
				.getCategoryServices()
				.read("N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (boo:<e,t> $1))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\(N/N)/NP : (lambda $0:e (lambda $1:<<e,t>,<e,t>> ($1 foo:<e,t> $0)))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S/NP : (lambda $0:e (and:<t*,t> (boo:<e,t> $0) (foo:<e,t> $0)))");

		final ForwardTypeRaisedComposition rule = new ForwardTypeRaisedComposition(
				TestServices.getCategoryServices());
		final ParseRuleResult<LogicalExpression> actual = rule.apply(primary,
				secondary, null);
		Assert.assertEquals(expected, actual.getResultCategory());
	}

}
