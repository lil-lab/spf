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
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;

public class GetApplicationArgumentTest {

	public GetApplicationArgumentTest() {
		TestServices.init();
	}

	@Test
	public void test1() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(pred:<e,t> boo:e)");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (pred:<e,t> $0))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("boo:e");
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test10() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (q:<e,<e,t>> $1 $2) (p:<e,t> $1))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2 $1) (p:<e,t> $1)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (q:<e,<e,t>> $1 $0)))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test11() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (q:<e,<e,<e,t>>> $2 P:e K:e) (l:<e,<e,<e,t>>> $1 J:e L:e) (p:<e,t> $1))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2 $1) (p:<e,t> $1)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (q:<e,<e,<e,t>>> $0 P:e K:e) (l:<e,<e,<e,t>>> $1 J:e L:e))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test12() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (q:<e,<e,t>> $2 $1) (l:<e,<e,t>> $1 $2) (p:<e,t> $1))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2 $1) (p:<e,t> $1)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (q:<e,<e,t>> $0 $1) (l:<e,<e,t>> $1 $0))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test13() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (q:<e,<e,t>> $2 B:e) (l:<e,<e,t>> B:e $2) (p:<e,<e,t>> $1 $2))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 B:e $2) (p:<e,<e,t>> $1 $2)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (q:<e,<e,t>> $1 $0) (l:<e,<e,t>> $0 $1))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test14() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (pred:<e,<e,<e,t>>> $0 $0 $0))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e ($0 $1 $1)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (pred:<e,<e,<e,t>>> $0 $1 $1)))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(
				"Expected failure: re-use of arguments in the same literal is not used in function",
				null, GetApplicationArgument.of(function, result));
	}

	@Test
	public void test15() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (pred:<e,<e,<e,t>>> $0 B:e B:e))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e ($0 $1 B:e)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (pred:<e,<e,<e,t>>> $0 $1 $1)))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test16() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (pred:<e,<e,t>> $0 B:e))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e ($0 $1 B:e)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (pred:<e,<e,t>> $0 $1)))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test17() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (and:<t*,t> (p:<e,t> $1) (q:<e,<e,t>> $1 A:e) (p:<e,<e,t>> $1 A:e) (q:<e,<e,t>> $1 B:e) (p:<e,<e,t>> $1 B:e)))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (and:<t*,t> (p:<e,t> $1) ($0 $1 A:e) ($0 $1 B:e))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (q:<e,<e,t>> $0 $1) (p:<e,<e,t>> $0 $1))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(
				"An expected failure due to the greedy nature of the operation",
				null, GetApplicationArgument.of(function, result));
	}

	@Test
	public void test18() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (country:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> ($0 $2) (c_op:<e,<e,t>> $2 Saudi++Arabia:e))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $1 $2))))");
		Assert.assertEquals("Should fail", null,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test19() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<<e,t>,<e,t>> (lambda $1:e (and:<t*,t> ($0 earmark-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (invest-01:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (capital:<e,t> $3)))))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<<<e,t>,<e,t>>,<e,t>>> (lambda $1:<<e,t>,<e,t>> (lambda $2:e ($0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (invest-01:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (capital:<e,t> $4))))))) $1 $2))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:<<e,t>,<e,t>> (lambda $2:e (and:<t*,t> ($1 earmark-01:<e,t> $2) (c_ARGX:<e,<e,t>> $2 $0)))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test2() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (pred:<e,<e,t>> $0 boo:e))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $0:e (pred:<e,<e,t>> $0 $1)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("boo:e");
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test20() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t> (sponsor-01:<e,t> $0) (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (it:<e,t> $1)))) (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (workshop:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (person:<e,t> $3) (c_REL:<e,<i,t>> $3 50:i) (c_ARGX-of:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t> (expert-41:<e,t> $4) (c_ARGX:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (oppose-01:<e,t> $5) (c_ARGX:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (terrorism:<e,t> $6)))))))))))))))) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (temporal-quantity:<e,t> $7) (c_REL:<e,<i,t>> $7 2:i) (c_REL:<e,<e,t>> $7 (a:<id,<<e,t>,e>> na:id (lambda $8:e (week:<e,t> $8))))))))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:<e,<e,t>> (lambda $1:e ($0 $1)))");
		Assert.assertEquals(null, GetApplicationArgument.of(function, result));
	}

	@Test
	public void test21() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (conference:<e,t> $1) (c_REL:<e,<e,t>> $1 $0)))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:<e,t> (lambda $1:e ($0 $1)))");
		Assert.assertEquals(null, GetApplicationArgument.of(function, result));
	}

	@Test
	public void test22() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<<e,t>,<e,<e,t>>> (lambda $1:e (and:<t*,t>\n"
								+ "	(suspect-01:<e,t> $1)\n"
								+ "	(c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e ($0 (lambda $3:e (build-01:<e,t> $3)) \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t>\n"
								+ "			(person:<e,t> $4)\n"
								+ "			(c_ARGX-of:<e,<e,t>> $4 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "					(have-org-role-91:<e,t> $5)\n"
								+ "					(c_ARGX:<e,<e,t>> $5 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t>\n"
								+ "							(country:<e,t> $6)\n"
								+ "							(c_REL:<e,<e,t>> $6 \n"
								+ "								(a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t>\n"
								+ "									(name:<e,t> $7)\n"
								+ "									(c_op:<e,<e,t>> $7 North++Korea:e)))))))))\n"
								+ "					(c_ARGX:<e,<e,t>> $5 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $8:e (official:<e,t> $8))))))))))) $2)))))))\n"
								+ "");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<<e,t>,<e,t>> (lambda $1:<<e,t>,<e,<e,t>>> (lambda $2:e (and:<t*,t>\n"
								+ "	(suspect-01:<e,t> $2)\n"
								+ "	(c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "		($1 (lambda $4:e (have-org-role-91:<e,t> $4)) (a:<id,<<e,t>,e>> na:id (lambda $5:e ($0 (lambda $6:e (person:<e,t> $6)) $5))) $3)\n"
								+ "		(c_ARGX:<e,<e,t>> $3 \n"
								+ "			(a:<id,<<e,t>,e>> na:id (lambda $7:e (official:<e,t> $7))))))))))))\n"
								+ "");
		Assert.assertEquals(null, GetApplicationArgument.of(function, result));
	}

	@Test
	public void test23() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (and:<t*,t>\n"
						+ "	(workshop:<e,t> $0)\n" + "	(c_REL:<e,<e,t>> $0 \n"
						+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
						+ "			(person:<e,t> $1)\n"
						+ "			(c_REL:<e,<i,t>> $1 50:i)\n"
						+ "			(c_ARGX-of:<e,<e,t>> $1 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "					(expert-41:<e,t> $2)\n"
						+ "					(c_ARGX:<e,<e,t>> $2 \n"
						+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
						+ "							(oppose-01:<e,t> $3)\n"
						+ "							(c_ARGX:<e,<e,t>> $3 \n"
						+ "								(a:<id,<<e,t>,e>> na:id (lambda $4:e (terrorism:<e,t> $4))))))))))))))))\n"
						+ "	(c_REL:<e,<e,t>> $0 \n"
						+ "		(a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
						+ "			(temporal-quantity:<e,t> $5)\n"
						+ "			(c_REL:<e,<i,t>> $5 2:i)\n"
						+ "			(c_REL:<e,<e,t>> $5 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $6:e (week:<e,t> $6))))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:<e,t> (lambda $1:e (and:<t*,t>\n"
						+ "	($0 $1)\n" + "	(c_REL:<e,<e,t>> $1 \n"
						+ "		(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "			(c_REL:<e,<e,t>> $2 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $3:e (week:<e,t> $3))))\n"
						+ "			(temporal-quantity:<e,t> $2)\n"
						+ "			(c_REL:<e,<i,t>> $2 2:i))))))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices()
				.readSemantics("(lambda $0:e (and:<t*,t>\n"
						+ "	(workshop:<e,t> $0)\n" + "	(c_REL:<e,<e,t>> $0 \n"
						+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
						+ "			(person:<e,t> $1)\n"
						+ "			(c_REL:<e,<i,t>> $1 50:i)\n"
						+ "			(c_ARGX-of:<e,<e,t>> $1 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "					(expert-41:<e,t> $2)\n"
						+ "					(c_ARGX:<e,<e,t>> $2 \n"
						+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
						+ "							(oppose-01:<e,t> $3)\n"
						+ "							(c_ARGX:<e,<e,t>> $3 \n"
						+ "								(a:<id,<<e,t>,e>> na:id (lambda $4:e (terrorism:<e,t> $4))))))))))))))))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test24() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (c_ARGX:<e,<e,t>> $0 \n"
						+ "	(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
						+ "		(person:<e,t> $1)\n"
						+ "		(c_REL:<e,<i,t>> $1 50:i)\n"
						+ "		(c_ARGX-of:<e,<e,t>> $1 \n"
						+ "			(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "				(expert-41:<e,t> $2)\n"
						+ "				(c_ARGX:<e,<e,t>> $2 \n"
						+ "					(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
						+ "						(oppose-01:<e,t> $3)\n"
						+ "						(c_ARGX:<e,<e,t>> $3 \n"
						+ "							(a:<id,<<e,t>,e>> na:id (lambda $4:e (terrorism:<e,t> $4)))))))))))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (c_ARGX:<e,<e,t>> $1 $0)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
								+ "		(person:<e,t> $1)\n"
								+ "		(c_REL:<e,<i,t>> $1 50:i)\n"
								+ "		(c_ARGX-of:<e,<e,t>> $1 \n"
								+ "			(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
								+ "				(expert-41:<e,t> $2)\n"
								+ "				(c_ARGX:<e,<e,t>> $2 \n"
								+ "					(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "						(oppose-01:<e,t> $3)\n"
								+ "						(c_ARGX:<e,<e,t>> $3 \n"
								+ "							(a:<id,<<e,t>,e>> na:id (lambda $4:e (terrorism:<e,t> $4)))))))))))))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test25() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (and:<t*,t>\n"
						+ "	(person:<e,t> $0)\n"
						+ "	(c_ARGX-of:<e,<e,t>> $0 \n"
						+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
						+ "			(expert-41:<e,t> $1)\n"
						+ "			(c_ARGX:<e,<e,t>> $1 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "					(oppose-01:<e,t> $2)\n"
						+ "					(c_ARGX:<e,<e,t>> $2 \n"
						+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (terrorism:<e,t> $3))))))))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (person:<e,t> $1) (c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t> (expert-41:<e,t> $2) ($0 $2))))))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices()
				.readSemantics("(lambda $1:e (c_ARGX:<e,<e,t>> $1 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "					(oppose-01:<e,t> $2)\n"
						+ "					(c_ARGX:<e,<e,t>> $2 \n"
						+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (terrorism:<e,t> $3)))))))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test26() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (and:<t*,t>\n"
						+ "	(person:<e,t> $0)\n"
						+ "	(c_ARGX-of:<e,<e,t>> $0 \n"
						+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
						+ "			(expert-41:<e,t> $1)\n"
						+ "			(c_ARG2:<e,<e,t>> $1 \n"
						+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
						+ "					(oppose-01:<e,t> $2)\n"
						+ "					(c_ARG1:<e,<e,t>> $2 \n"
						+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (terrorism:<e,t> $3))))))))))))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<<e,t>,<e,t>> (lambda $1:e (and:<t*,t>\n"
								+ "	(person:<e,t> $1)\n"
								+ "	(c_ARGX-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e ($0 (lambda $3:e (expert-41:<e,t> $3)) $2)))))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $2:<e,t> (lambda $1:e (and:<t*,t> ($2 $1) (c_ARG2:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $8:e (and:<t*,t> (oppose-01:<e,t> $8) (c_ARG1:<e,<e,t>> $8 (a:<id,<<e,t>,e>> na:id (lambda $10:e (terrorism:<e,t> $10)))))))))))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test27() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (c_ARG0:<e,<e,t>> $1 $0) (c_ARG1:<e,<e,t>> $1 $0))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2) (c_ARG0:<e,<e,t>> $2 $1)))))");
		Assert.assertNull(GetApplicationArgument.of(function, result));
	}

	@Test
	public void test28() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (c_ARG0:<e,<e,t>> $1 $0) (c_ARG1:<e,t> $1))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2) (c_ARG0:<e,<e,t>> $2 $1)))))");
		final LogicalExpression expectedArg = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (c_ARG1:<e,t> $0))");
		Assert.assertEquals(expectedArg,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test3() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $2:<e,e> (pred:<e,<e,t>> $0 ($2 boo:e))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $0:e (lambda $2:<e,e> (pred:<e,<e,t>> $0 ($2 $1)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("boo:e");
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test4() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (a:<t,e> (boo:<e,<e,t>> $2 $1))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (a:<t,e> ($0 $1 $2)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics(
						"(lambda $0:e (lambda $1:e (boo:<e,<e,t>> $1 $0)))");
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test5() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(and:<t*,t> (p:<e,t> boo:e) (q:<e,t> boo:e))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t> (p:<e,t> $0) (q:<e,t> $0)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("boo:e");
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test6() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(and:<t*,t> (koko:<e,t> boo:e) (koko:<e,t> goo:e))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (and:<t*,t> ($0 boo:e) ($0 goo:e)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("koko:<e,t>");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test7() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (and:<t*,t> (sofa:<e,t> $1) (pred:<e,<e,t>> $1 B:e))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pred:<e,<e,t>> $1 B:e))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("sofa:<e,t>");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test8() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(and:<t*,t> (sofa:<e,<e,t>> A:e B:e) (pred:<e,t> C:e))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (and:<t*,t> ($0 B:e) (pred:<e,t> C:e)))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices()
				.readSemantics("(lambda $0:e (sofa:<e,<e,t>> A:e $0))");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void test9() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (q:<e,<e,t>> $2 $1) (p:<e,t> $1))))");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2 $1) (p:<e,t> $1)))))");
		final LogicalExpression expectedArgument = TestServices
				.getCategoryServices().readSemantics("q:<e,<e,t>>");
		Assert.assertEquals(result,
				ApplyAndSimplify.of(function, expectedArgument));
		Assert.assertEquals(expectedArgument,
				GetApplicationArgument.of(function, result));
	}

	@Test
	public void testCreateArg1() {
		final LogicalExpression resultSubExp = TestServices
				.getCategoryServices().readSemantics("boo:e");
		final Variable applicationArg = new Variable(
				LogicLanguageServices.getTypeRepository().getEntityType());
		Assert.assertEquals(resultSubExp,
				GetApplicationArgument.createArgument(resultSubExp,
						applicationArg, applicationArg,
						new ScopeMapping<Variable, Variable>()));

	}

	@Test
	public void testCreateArg2() {
		final LogicalExpression resultSubExp = TestServices
				.getCategoryServices().readSemantics("boo:t");
		final Variable applicationArg = new Variable(
				LogicLanguageServices.getTypeRepository().getEntityType());
		Assert.assertEquals(null,
				GetApplicationArgument.createArgument(resultSubExp,
						applicationArg, applicationArg,
						new ScopeMapping<Variable, Variable>()));

	}

	@Test
	public void testCreateArg3() {
		final LogicalExpression resultSubExp = TestServices
				.getCategoryServices().readSemantics("(pred:<e,t> boo:e)");
		final LogicalExpression function = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:<e,t> ($0 boo:e))");
		final Variable applicationArg = ((Lambda) function).getArgument();
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("pred:<e,t>");
		Assert.assertEquals(expected,
				GetApplicationArgument.createArgument(resultSubExp,
						((Lambda) function).getBody(), applicationArg,
						new ScopeMapping<Variable, Variable>()));

	}

	@Test
	public void testCreateArg4() {
		final LogicalExpression resultSubExp = TestServices
				.getCategoryServices().readSemantics("(pred:<e,t> $0:e)");
		final LogicalExpression functionSubExp = TestServices
				.getCategoryServices().readSemantics("($1:<e,t> $0:e)");
		final Variable applicationArg = (Variable) ((Literal) functionSubExp)
				.getPredicate();
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("pred:<e,t>");
		final ScopeMapping<Variable, Variable> scope = new ScopeMapping<Variable, Variable>();
		scope.push((Variable) ((Literal) functionSubExp).getArg(0),
				(Variable) ((Literal) resultSubExp).getArg(0));
		Assert.assertEquals(expected, GetApplicationArgument.createArgument(
				resultSubExp, functionSubExp, applicationArg, scope));

	}

}
