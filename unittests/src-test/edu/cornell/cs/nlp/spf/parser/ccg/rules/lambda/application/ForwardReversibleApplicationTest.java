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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

public class ForwardReversibleApplicationTest {

	private final ForwardReversibleApplication	nfRule;
	private final ForwardReversibleApplication	rule;

	public ForwardReversibleApplicationTest() {
		TestServices.init();
		this.rule = new ForwardReversibleApplication(
				TestServices.getCategoryServices(), 3, 9, false,
				SetUtils.createSet("sg", "pl"));
		this.nfRule = new ForwardReversibleApplication(
				TestServices.getCategoryServices(), 3, 9, true,
				SetUtils.createSet("sg", "pl"));
	}

	@Test
	public void test() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("S[x]\\S[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (person:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<e,t>> $4 South++Korea:e))))))))))))))))");
		final Category<LogicalExpression> left = TestServices
				.getCategoryServices()
				.read("S[x]\\S[x]/NP : (lambda $0:e (lambda $1:<e,t> (lambda $2:e (and:<t*,t> ($1 $2) (c_REL:<e,<e,t>> $2 $0)))))");
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(3, actual.size());
		Assert.assertTrue(actual.contains(TestServices.getCategoryServices()
				.read("NP : (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (person:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (name:<e,t> $2) (c_op:<e,<e,t>> $2 South++Korea:e))))))))))))")));
		Assert.assertTrue(actual.contains(TestServices.getCategoryServices()
				.read("NP[sg] : (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (person:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (name:<e,t> $2) (c_op:<e,<e,t>> $2 South++Korea:e))))))))))))")));
		Assert.assertTrue(actual.contains(TestServices.getCategoryServices()
				.read("NP[pl] : (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (person:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (name:<e,t> $2) (c_op:<e,<e,t>> $2 South++Korea:e))))))))))))")));

	}

	@Test
	public void test10() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("S\\NP[sg] : (lambda $0:e (lambda $1:e (and:<t*,t> (begin-01:<e,t> $1) (c_ARG0:<e,<e,t>> $1 $0) (c_ARG1:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (talk-01:<e,t> $2) (c_ARG0:<e,<e,t>> $2 $0) (c_ARG1:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (sell-01:<e,t> $3) (c_ARG1:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (facility:<e,t> $4) (c_REL-of:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (process-01:<e,t> $5) (c_ARG1:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (plutonium:<e,t> $6)))))))))))))))) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (party:<e,t> $7) (c_REL:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $8:e (and:<t*,t> (country:<e,t> $8) (c_REL:<e,<e,t>> $8 (a:<id,<<e,t>,e>> na:id (lambda $9:e (and:<t*,t> (name:<e,t> $9) (c_op:<e,<txt,t>> $9 China:txt))))))))) (c_REL-of:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $10:e (interest-01:<e,t> $10)))))))))))))))");
		final Category<LogicalExpression> left = TestServices
				.getCategoryServices()
				.read("S\\NP[sg]/N[sg] : (lambda $0:<e,t> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2) (c_ARG0:<e,<e,t>> $2 $1)))))");
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		System.out.println(actual);
		Assert.assertTrue(actual.isEmpty());
	}

	@Test
	public void test2() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("S[dcl] : (lambda $0:e (and:<t*,t> (become-01:<e,t> $0) "
						+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (warfare:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (cyber:<e,t> $2)))))))) "
						+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (weapon:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (common:<e,t> $4)))))))) "
						+ "(c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (person:<e,t> $5) (c_REL:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (country:<e,t> $6) (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (name:<e,t> $7) (c_op:<e,<e,t>> $7 South++Korea:e)))))))))))))))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices()
				.read("S[dcl] : (lambda $0:e (and:<t*,t> (become-01:<e,t> $0) "
						+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (warfare:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (cyber:<e,t> $2)))))))) "
						+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (common:<e,t> $4)))) (weapon:<e,t> $3)))))))");
		final ComplexCategory<LogicalExpression> expected1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[x]/S[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) "
						+ "(c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (person:<e,t> $2) "
						+ "(c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) "
						+ "(c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<e,t>> $4 South++Korea:e))))))))))))))))");
		final ComplexCategory<LogicalExpression> expected2 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[dcl]/S[dcl] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (person:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<e,t>> $4 South++Korea:e))))))))))))))))");
		final ComplexCategory<LogicalExpression> expected3 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[dcl]/S : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (person:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<e,t>> $4 South++Korea:e))))))))))))))))");

		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(expected1, right));
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(expected2, right));
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(expected3, right));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyRight(right, result, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(3, actual.size());
		Assert.assertTrue(actual.contains(expected1));
		Assert.assertTrue(actual.contains(expected2));
		Assert.assertTrue(actual.contains(expected3));
	}

	@Test
	public void test3() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("N : (lambda $0:e (and:<t*,t> (p:<e,t> $0) (boo:<e,<e,t>> $0 koo:e)))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices()
				.read("N\\NP : (lambda $0:e (lambda $1:e (and:<t*,t> (boo:<e,<e,t>> $1 $0) (p:<e,t> $1))))");
		final ComplexCategory<LogicalExpression> expected = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("N/(N\\NP) : (lambda $0:<e,<e,t>> ($0 koo:e))");
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(expected, right));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyRight(right, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(actual.size() == 1);
		Assert.assertTrue(actual.contains(expected));

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyRight(right, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.isEmpty());
	}

	@Test
	public void test4() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("S[dcl] : (lambda $0:e (and:<t*,t> (nation:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (name:<e,t> $2) (c_op:<e,<txt,t>> $2 Japan:txt))))))))) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (pacifism:<e,t> $3)))) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $4:e (official:<e,t> $4))))))");
		final ComplexCategory<LogicalExpression> left = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[x]/S[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (official:<e,t> $2)))) ($0 $1))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S[dcl] : (lambda $0:e (and:<t*,t> (nation:<e,t> $0) "
						+ "(c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (name:<e,t> $2) (c_op:<e,<txt,t>> $2 Japan:txt))))))))) "
						+ "(c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (pacifism:<e,t> $3))))))");
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(left, expected));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(actual.size() == 1);
		Assert.assertTrue(actual.contains(expected));

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.equals(actual));
	}

	@Test
	public void test5() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("S[pt]\\NP[sg] : (lambda $0:e (lambda $1:e (and:<t*,t> (push-02:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (negotiate-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<txt,t>> $4 United++States:txt))))))))))))))))");
		final ComplexCategory<LogicalExpression> left = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[pt]\\NP[x]/(S[ng]\\NP[x]) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $1 $2))))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.read("S[ng]\\NP[sg] : (lambda $0:e (lambda $1:e (and:<t*,t> (push-02:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (negotiate-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<txt,t>> $4 United++States:txt))))))))))))))))");
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(left, expected));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(actual.size() == 1);
		Assert.assertTrue(actual.contains(expected));

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.equals(actual));
	}

	@Test
	public void test6() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("S[dcl]\\NP[sg] : (lambda $0:e (lambda $1:e (and:<t*,t> (push-02:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (negotiate-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<txt,t>> $4 United++States:txt))))))))))))))))");
		final ComplexCategory<LogicalExpression> left = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S[dcl]\\NP[sg]/(S[pt]\\NP[sg]) : (lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $1 $2))))");
		final Category<LogicalExpression> expected1 = TestServices
				.getCategoryServices()
				.read("S[pt]\\NP[sg] : (lambda $0:e (lambda $1:e (and:<t*,t> (push-02:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (negotiate-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<txt,t>> $4 United++States:txt))))))))))))))))");
		final Category<LogicalExpression> expected2 = TestServices
				.getCategoryServices()
				.read("S[pt]\\NP : (lambda $0:e (lambda $1:e (and:<t*,t> (push-02:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (negotiate-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (country:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (name:<e,t> $4) (c_op:<e,<txt,t>> $4 United++States:txt))))))))))))))))");
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(left, expected1));
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(left, expected2));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(2, actual.size());
		Assert.assertTrue(actual.contains(expected1));
		Assert.assertTrue(actual.contains(expected2));

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.equals(actual));
	}

	@Test
	public void test7() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("N[x]\\N[x] : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pred:<e,<e,t>> $1 boo:e))))");
		final Category<LogicalExpression> right = TestServices
				.getCategoryServices().read("NP[sg] : boo:e");
		final ComplexCategory<LogicalExpression> expected1 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("N[x]\\N[x]/NP : (lambda $2:e (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pred:<e,<e,t>> $1 $2)))))");
		final ComplexCategory<LogicalExpression> expected2 = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("N[x]\\N[x]/NP[sg] : (lambda $2:e (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pred:<e,<e,t>> $1 $2)))))");
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(expected1, right));
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(expected2, right));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyRight(right, result, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(2, actual.size());
		Assert.assertTrue(actual.contains(expected1));
		Assert.assertTrue(actual.contains(expected2));

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyRight(right, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.equals(actual));
	}

	@Test
	public void test8() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("NP : (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (tradition:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<i,t>> $1 1:i))))))))");
		final ComplexCategory<LogicalExpression> left = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().read("NP[x]/NP[x] : (lambda $0:e $0)");
		final Category<LogicalExpression> expected1 = TestServices
				.getCategoryServices()
				.read("NP : (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (tradition:<e,t> $0) (c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<i,t>> $1 1:i))))))))");
		Assert.assertEquals(result,
				TestServices.getCategoryServices().apply(left, expected1));
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(1, actual.size());
		Assert.assertTrue(actual.contains(expected1));

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.equals(actual));
	}

	@Test
	public void test9() {
		final Category<LogicalExpression> result = TestServices
				.getCategoryServices()
				.read("NP[pl]\\NP[pl] : (lambda $0:e (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (control-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0) (c_REL:<e,<e,t>> $1 -:e)))))");
		final ComplexCategory<LogicalExpression> left = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("NP\\NP[x]/(S[dcl]\\NP[x]) : (lambda $0:<e,<e,t>> (lambda $1:e (a:<id,<<e,t>,e>> na:id (lambda $2:e ($0 $1 $2)))))");
		final Set<Category<LogicalExpression>> actual = rule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertEquals(0, actual.size());

		// With NF constraint.
		final Set<Category<LogicalExpression>> nfActual = nfRule
				.reverseApplyLeft(left, result, new SentenceSpan(0, 1, 2));
		Assert.assertTrue(nfActual.equals(actual));
	}

}
