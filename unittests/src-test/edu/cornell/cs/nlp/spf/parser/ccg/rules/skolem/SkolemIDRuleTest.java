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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.skolem;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.skolem.SkolemIDRule;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

public class SkolemIDRuleTest {

	private final SkolemIDRule	rule;

	public SkolemIDRuleTest() {
		TestServices.init();
		this.rule = new SkolemIDRule(
				SetUtils.createSet(LogicalConstant.create(
						"io:<id,<id,<<e,t>,e>>>",
						LogicLanguageServices.getTypeRepository()
								.getTypeCreateIfNeeded("<id,<id,<<e,t>,e>>>"),
						false),
						LogicalConstant
								.create("a:<id,<<e,t>,e>>",
										LogicLanguageServices
												.getTypeRepository()
												.getTypeCreateIfNeeded(
														"<id,<<e,t>,e>>"),
										false)));
	}

	@Test
	public void test() {
		final Category<LogicalExpression> category = TestServices.CATEGORY_SERVICES
				.read("NP : (io:<id,<id,<<e,t>,e>>> na:id na:id (lambda $0:e (apple:<e,t> $0)))");
		final Category<LogicalExpression> expected = TestServices.CATEGORY_SERVICES
				.read("NP : (io:<id,<id,<<e,t>,e>>> !1 na:id (lambda $0:e (apple:<e,t> $0)))");
		Assert.assertEquals(expected, rule.apply(category, null)
				.getResultCategory());
	}

	@Test
	public void test2() {
		final Category<LogicalExpression> category = TestServices.CATEGORY_SERVICES
				.read("NP : (a:<id,<<e,t>,e>> na:id (lambda $0:e (apple:<e,t> $0)))");
		final Category<LogicalExpression> expected = TestServices.CATEGORY_SERVICES
				.read("NP : (a:<id,<<e,t>,e>> !1 (lambda $0:e (apple:<e,t> $0)))");
		Assert.assertEquals(expected, rule.apply(category, null)
				.getResultCategory());
	}

	@Test
	public void test3() {
		final Category<LogicalExpression> category = TestServices.CATEGORY_SERVICES
				.read("NP : (io:<id,<id,<<e,t>,e>>> na:id na:id (lambda $1:e (eq:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $0:e (apple:<e,t> $0))))))");
		final Category<LogicalExpression> expected = TestServices.CATEGORY_SERVICES
				.read("NP : (io:<id,<id,<<e,t>,e>>> !1 na:id (lambda $1:e (eq:<e,<e,t>> $1 (a:<id,<<e,t>,e>> !2 (lambda $0:e (apple:<e,t> $0))))))");
		Assert.assertEquals(expected, rule.apply(category, null)
				.getResultCategory());
	}

	@Test
	public void test4() {
		final Category<LogicalExpression> category = TestServices.CATEGORY_SERVICES
				.read("NP : (io:<id,<id,<<e,t>,e>>> na:id na:id (lambda $1:e (eq:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (p:<e,<e,t>> $0 $1) (apple:<e,t> $0)))))))");
		final Category<LogicalExpression> expected = TestServices.CATEGORY_SERVICES
				.read("NP : (io:<id,<id,<<e,t>,e>>> !1 na:id (lambda $1:e (eq:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (p:<e,<e,t>> $0 $1) (apple:<e,t> $0)))))))");
		Assert.assertEquals(expected, rule.apply(category, null)
				.getResultCategory());
	}

	@Test
	public void test5() {
		final Category<LogicalExpression> category = TestServices.CATEGORY_SERVICES
				.read("NP : (a:<id,<<e,t>,e>> na:id (lambda $12:e (and:<t*,t> (reach-01:<e,t> $12) (c_extent:<e,<e,t>> $12 (a:<id,<<e,t>,e>> !7 (lambda $13:e (far:<e,t> $13)))))))");
		final Category<LogicalExpression> expected = TestServices.CATEGORY_SERVICES
				.read("NP : (a:<id,<<e,t>,e>> !1 (lambda $12:e (and:<t*,t> (reach-01:<e,t> $12) (c_extent:<e,<e,t>> $12 (a:<id,<<e,t>,e>> !7 (lambda $13:e (far:<e,t> $13)))))))");
		Assert.assertEquals(expected, rule.apply(category, null)
				.getResultCategory());
	}

}
