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
package edu.cornell.cs.nlp.spf.parser.ccg.lambda.pruning;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;

public class SupervisedFilterFactoryTest {

	public SupervisedFilterFactoryTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final LogicalExpression exp = TestServices.getCategoryServices()
				.readSemantics(
						"(a:<id,<<e,t>,e>> !1 (lambda $0:e (and:<t*,t> (state-01:<e,t> $0) (c_ARG0:<e,<e,t>> $0 (a:<id,<<e,t>,e>> !2 (lambda $1:e (and:<t*,t> (person:<e,t> $1) (c_name:<e,<e,t>> $1 Megawati:e))))))))");
		final LogicalExpression partialExp = TestServices.getCategoryServices()
				.readSemantics(
						"(a:<id,<<e,t>,e>> !2 (lambda $1:e (and:<t*,t> (person:<e,t> $1) (c_name:<e,<e,t>> $1 Megawati:e))))");
		final SupervisedFilterFactory<SingleSentence> factory = new SupervisedFilterFactory<SingleSentence>(
				e -> !LogicLanguageServices.isCoordinationPredicate(e));
		Assert.assertTrue(factory.create(exp)
				.test(new ParsingOp<LogicalExpression>(
						Category.create(Syntax.N, partialExp),
						new SentenceSpan(1, 1, 2),
						RuleName.create("dummy", Direction.FORWARD))));
	}
}
