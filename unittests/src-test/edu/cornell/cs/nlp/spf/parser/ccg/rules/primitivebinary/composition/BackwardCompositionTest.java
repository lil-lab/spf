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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.BackwardComposition;

public class BackwardCompositionTest {

	public BackwardCompositionTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP/N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");

		final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
				new LogicalExpressionCategoryServices(true), 1, false);
		final ParseRuleResult<LogicalExpression> actual = rule.apply(secondary,
				primary, null);
		Assert.assertTrue(actual == null);
	}

	@Test
	public void test2() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP\\N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("N\\N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");

		final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
				new LogicalExpressionCategoryServices(true), 1, false);
		final ParseRuleResult<LogicalExpression> actual = rule.apply(secondary,
				primary, null);
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.read("NP\\N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1)))))"),
				actual.getResultCategory());
	}

	@Test
	public void test3() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3)))) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (date-entity:<e,t> $4) (c_year:<e,<i,t>> $4 2008:i) (c_month:<e,<i,t>> $4 5:i) (c_day:<e,<i,t>> $4 5:i)))))))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\NP/PP/NP : (lambda $0:e (lambda $10:<e,t> (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0) ($10 $2))))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S\\NP/PP/NP : (lambda $0:e (lambda $10:<e,t> (lambda $1:e (lambda $2:e (and:<t*,t> "
						+ "(reveal-01:<e,t> $2) "
						+ "(c_ARG0:<e,<e,t>> $2 $1) "
						+ "(c_ARG1:<e,<e,t>> $2 $0) "
						+ " ($10 $2) "
						+ "(c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3)))) "
						+ "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (date-entity:<e,t> $4) (c_year:<e,<i,t>> $4 2008:i) (c_month:<e,<i,t>> $4 5:i) (c_day:<e,<i,t>> $4 5:i))))))))))");

		{
			final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
					TestServices.CATEGORY_SERVICES, 2, false);

			final ParseRuleResult<LogicalExpression> result = rule.apply(
					secondary, primary, null);
			Assert.assertEquals(expected, result.getResultCategory());
		}

		{
			final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
					TestServices.CATEGORY_SERVICES, 1, true);

			final ParseRuleResult<LogicalExpression> result = rule.apply(
					secondary, primary, null);
			Assert.assertEquals(null, result);
		}

		{
			final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
					TestServices.CATEGORY_SERVICES, 3, false);

			final ParseRuleResult<LogicalExpression> result = rule.apply(
					secondary, primary, null);
			Assert.assertEquals(null, result);
		}

	}

	@Test
	public void test4() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\NP\\(S\\NP) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $1 $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3)))) (c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (date-entity:<e,t> $4) (c_year:<e,<i,t>> $4 2008:i) (c_month:<e,<i,t>> $4 5:i) (c_day:<e,<i,t>> $4 5:i)))))))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (reveal-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $1) (c_ARG1:<e,<e,t>> $2 $0)))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> "
						+ "(reveal-01:<e,t> $2) "
						+ "(c_ARG0:<e,<e,t>> $2 $1) "
						+ "(c_ARG1:<e,<e,t>> $2 $0) "
						+ "(c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (budget:<e,t> $3)))) "
						+ "(c_time:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (date-entity:<e,t> $4) (c_year:<e,<i,t>> $4 2008:i) (c_month:<e,<i,t>> $4 5:i) (c_day:<e,<i,t>> $4 5:i)))))))))");

		{
			final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
					TestServices.CATEGORY_SERVICES, 1, true);

			final ParseRuleResult<LogicalExpression> result = rule.apply(
					secondary, primary, null);
			Assert.assertEquals(expected, result.getResultCategory());
		}
	}
}
