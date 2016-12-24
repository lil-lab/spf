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

public class BackwardCrossingCompositionTest {

	public BackwardCrossingCompositionTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final Category<LogicalExpression> secondary = TestServices
				.getCategoryServices()
				.read("NP[pl] : (a:<<e,t>,e> (lambda $0:e (and:<t*,t> (network:<e,t> $0) (rel:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (some:<e,t> $1)))))))");
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S\\NP[pl]/(N[pl]/N[pl]) : (lambda $0:<<e,t>,<e,t>> (lambda $1:e ($0 (lambda $2:e (and:<t*,t> (remain:<e,t> $2) (arg1:<e,<e,t>> $2 $1))))))");
		final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
				new LogicalExpressionCategoryServices(true), 1, true);
		final ParseRuleResult<LogicalExpression> actual = rule.apply(secondary,
				primary, null);
		Assert.assertTrue(actual == null);
	}

}
