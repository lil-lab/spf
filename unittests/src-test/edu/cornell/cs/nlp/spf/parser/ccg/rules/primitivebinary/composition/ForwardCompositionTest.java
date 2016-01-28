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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.ForwardComposition;

public class ForwardCompositionTest {

	public ForwardCompositionTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final Category<LogicalExpression> primary = TestServices
				.getCategoryServices()
				.read("NP/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (oppose-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (terrorism:<e,t> $3)))))))))))");
		final Category<LogicalExpression> secondary = TestServices
				.getCategoryServices()
				.read("N[pl]\\(N[x]/N[x]) : (lambda $0:<<e,t>,<e,t>> (lambda $1:e (and:<t*,t> (person:<e,t> $1) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e ($0 (lambda $3:e (expert-41:<e,t> $3)) $2)))))))");
		final ForwardComposition<LogicalExpression> rule = new ForwardComposition<LogicalExpression>(
				TestServices.getCategoryServices(), 1, true);
		final ParseRuleResult<LogicalExpression> result = rule.apply(primary,
				secondary, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(
				">xcomp1->NP\\(N[x]/N[x]) : (lambda $0:<<e,t>,<e,t>> (lambda $1:e (and:<t*,t> (person:<e,t> $1) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e ($0 (lambda $3:e (expert-41:<e,t> $3)) $2)))) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (oppose-01:<e,t> $4) (c_ARGX:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $5:e (terrorism:<e,t> $5)))))))))))",
				result.toString());
	}

	@Test
	public void test2() {
		final Category<LogicalExpression> primary = TestServices
				.getCategoryServices()
				.read("N[x]/N[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (use-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (technology:<e,t> $3)))))))))))");
		final Category<LogicalExpression> secondary = TestServices
				.getCategoryServices()
				.read("N[x]/N[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (manufacture-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<e,t>> $4 KTX:e))))) (railway-line:<e,t> $3))))) (c_ARGX-of:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (c_ARGX:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (c_op:<e,<e,t>> $7 South++Korea:e) (name:<e,t> $7))))) (country:<e,t> $6))))) (cause-01:<e,t> $5))))))))))))");
		final ForwardComposition<LogicalExpression> rule = new ForwardComposition<LogicalExpression>(
				TestServices.getCategoryServices(), 1, false);
		final ParseRuleResult<LogicalExpression> result = rule.apply(primary,
				secondary, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(
				">comp1->N[x]/N[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (manufacture-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<e,t>> $4 KTX:e))))) (railway-line:<e,t> $3))))) (c_ARGX-of:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (c_ARGX:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (c_op:<e,<e,t>> $7 South++Korea:e) (name:<e,t> $7))))) (country:<e,t> $6))))) (cause-01:<e,t> $5))))))))) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $8:e (and:<t*,t> (use-01:<e,t> $8) (c_ARGX:<e,<e,t>> $8 (a:<id,<<e,t>,e>> na:id (lambda $9:e (technology:<e,t> $9)))))))))))",
				result.toString());
	}
}
