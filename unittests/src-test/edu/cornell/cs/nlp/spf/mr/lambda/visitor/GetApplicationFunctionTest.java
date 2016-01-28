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
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetApplicationFunction;
import edu.cornell.cs.nlp.utils.collections.ArrayUtils;

public class GetApplicationFunctionTest {

	public GetApplicationFunctionTest() {
		TestServices.init();
	}

	@Test
	public void test1() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(boo:<e,t> p:e)");
		final LogicalExpression arg = TestServices.getCategoryServices()
				.readSemantics("p:e");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (boo:<e,t> $0))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test10() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t>\n"
								+ "	(sign-02:<e,t> $0)\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
								+ "			(country:<e,t> $1)\n"
								+ "			(c_REL:<e,<e,t>> $1 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
								+ "					(ethnic-group:<e,t> $2)\n"
								+ "					(c_REL:<e,<e,t>> $2 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "							(name:<e,t> $3)\n"
								+ "							(c_op:<e,<e,t>> $3 Arab:e)))))))))))))\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t>\n"
								+ "			(agree-01:<e,t> $4)\n"
								+ "			(c_REL:<e,<e,t>> $4 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "					(counter-01:<e,t> $5)\n"
								+ "					(c_ARGX:<e,<e,t>> $5 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $6:e (terrorism:<e,t> $6))))\n"
								+ "					(c_ARGX-of:<e,<e,t>> $5 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t>\n"
								+ "							(bind-01:<e,t> $7)\n"
								+ "							(c_ARGX:<e,<e,t>> $7 \n"
								+ "								(ref:<id,e> na:id))\n"
								+ "							(c_ARGX:<e,<e,t>> $7 \n"
								+ "								(a:<id,<<e,t>,e>> na:id (lambda $8:e (and:<t*,t>\n"
								+ "									(coordinate-01:<e,t> $8)\n"
								+ "									(c_ARGX:<e,<e,t>> $8 \n"
								+ "										(ref:<id,e> na:id))\n"
								+ "									(c_REL:<e,<e,t>> $8 \n"
								+ "										(a:<id,<<e,t>,e>> na:id (lambda $9:e (and:<t*,t>\n"
								+ "											(fight-01:<e,t> $9)\n"
								+ "											(c_ARGX:<e,<e,t>> $9 \n"
								+ "												(ref:<id,e> na:id))\n"
								+ "											(c_ARGX:<e,<e,t>> $9 \n"
								+ "												(a:<id,<<e,t>,e>> na:id (lambda $10:e (terrorism:<e,t> $10))))))))))))))))))))))))\n"
								+ "	(c_REL:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $11:e (and:<t*,t>\n"
								+ "			(date-entity:<e,t> $11)\n"
								+ "			(c_year:<e,<i,t>> $11 1998:i)\n"
								+ "			(c_month:<e,<i,t>> $11 4:i))))))))\n"
								+ "");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (lambda $2:e (and:<t*,t>\n"
								+ "	(bind-01:<e,t> $2)\n"
								+ "	(c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "		(coordinate-01:<e,t> $3)\n"
								+ "		(c_ARGX:<e,<e,t>> $3 \n"
								+ "			(ref:<id,e> na:id))\n"
								+ "		(c_REL:<e,<e,t>> $3 $1)))))))))");
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test11() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t>\n"
								+ "	(sign-02:<e,t> $0)\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t>\n"
								+ "			(country:<e,t> $1)\n"
								+ "			(c_REL:<e,<e,t>> $1 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
								+ "					(ethnic-group:<e,t> $2)\n"
								+ "					(c_REL:<e,<e,t>> $2 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "							(name:<e,t> $3)\n"
								+ "							(c_op:<e,<e,t>> $3 Arab:e)))))))))))))\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t>\n"
								+ "			(agree-01:<e,t> $4)\n"
								+ "			(c_REL:<e,<e,t>> $4 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "					(counter-01:<e,t> $5)\n"
								+ "					(c_ARGX:<e,<e,t>> $5 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $6:e (terrorism:<e,t> $6))))\n"
								+ "					(c_ARGX-of:<e,<e,t>> $5 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t>\n"
								+ "							(bind-01:<e,t> $7)\n"
								+ "							(c_ARGX:<e,<e,t>> $7 \n"
								+ "								(ref:<id,e> na:id))\n"
								+ "							(c_ARGX:<e,<e,t>> $7 \n"
								+ "								(a:<id,<<e,t>,e>> na:id (lambda $8:e (and:<t*,t>\n"
								+ "									(coordinate-01:<e,t> $8)\n"
								+ "									(c_ARGX:<e,<e,t>> $8 \n"
								+ "										(ref:<id,e> na:id))\n"
								+ "									(c_REL:<e,<e,t>> $8 \n"
								+ "										(a:<id,<<e,t>,e>> na:id (lambda $9:e (and:<t*,t>\n"
								+ "											(fight-01:<e,t> $9)\n"
								+ "											(c_ARGX:<e,<e,t>> $9 \n"
								+ "												(ref:<id,e> na:id))\n"
								+ "											(c_ARGX:<e,<e,t>> $9 \n"
								+ "												(a:<id,<<e,t>,e>> na:id (lambda $10:e (terrorism:<e,t> $10))))))))))))))))))))))))\n"
								+ "	(c_REL:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $11:e (and:<t*,t>\n"
								+ "			(date-entity:<e,t> $11)\n"
								+ "			(c_year:<e,<i,t>> $11 1998:i)\n"
								+ "			(c_month:<e,<i,t>> $11 4:i))))))))\n"
								+ "");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t>\n"
								+ "	(bind-01:<e,t> $2)\n"
								+ "	(c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "		(coordinate-01:<e,t> $3)\n"
								+ "		(c_ARGX:<e,<e,t>> $3 \n"
								+ "			(ref:<id,e> na:id))\n"
								+ "		(c_REL:<e,<e,t>> $3 $1))))))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(
				GetApplicationFunction.of(result, arg, 3), arg));
	}

	@Test
	public void test12() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:<e,t> (a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
								+ "	($1 $2)\n"
								+ "	(c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "		(and:<e,t> $3)\n"
								+ "		(c_op1:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t>\n"
								+ "			(bias-01:<e,t> $4)\n"
								+ "			(c_ARGX:<e,<e,t>> $4 $0)\n"
								+ "			(c_REL:<e,<e,t>> $4 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $5:e (cultural:<e,t> $5))))))))\n"
								+ "		(c_op2:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t>\n"
								+ "			(insult-01:<e,t> $6)\n"
								+ "			(c_ARGX:<e,<e,t>> $6 $0)\n"
								+ "			(c_ARGX:<e,<e,t>> $6 $0))))))))))))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (lambda $2:e (c_ARGX:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "	(c_op1:<e,<e,t>> $3 $1)\n"
								+ "	(c_op2:<e,<e,t>> $3 $0)\n"
								+ "	(and:<e,t> $3))))))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(
				GetApplicationFunction.of(result, arg, 3), arg));
	}

	@Test
	public void test13() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (propose-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 $0))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $1 $2))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(
				GetApplicationFunction.of(result, arg, 3), arg));
	}

	@Test
	public void test14() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $4:e (c_ARGX:<e,<e,t>> $4\n"
								+ "                (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "                  (oppose-01:<e,t> $5)\n"
								+ "                  (c_ARGX:<e,<e,t>> $5\n"
								+ "                    (a:<id,<<e,t>,e>> na:id (lambda $6:e (terrorism:<e,t> $6)))))))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (terrorism:<e,t> $1)))))");
		final LogicalExpression func = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $4:e (c_ARGX:<e,<e,t>> $4\n"
								+ "                (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "                  (oppose-01:<e,t> $5)\n"
								+ "                  ($0 $5)))))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(func, arg));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 1));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 2));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 3));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 4));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 5));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 6));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 7));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 8));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 9));
	}

	@Test
	public void test15() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t>\n"
								+ "	(sponsor-01:<e,t> $0)\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $1:e (it:<e,t> $1))))\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
								+ "			(workshop:<e,t> $2)\n"
								+ "			(c_REL:<e,<e,t>> $2 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "					(person:<e,t> $3)\n"
								+ "					(c_REL:<e,<i,t>> $3 50:i)\n"
								+ "					(c_ARGX-of:<e,<e,t>> $3 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t>\n"
								+ "							(expert-41:<e,t> $4)\n"
								+ "							(c_ARGX:<e,<e,t>> $4 \n"
								+ "								(a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "									(oppose-01:<e,t> $5)\n"
								+ "									(c_ARGX:<e,<e,t>> $5 \n"
								+ "										(a:<id,<<e,t>,e>> na:id (lambda $6:e (terrorism:<e,t> $6))))))))))))))))\n"
								+ "			(c_REL:<e,<e,t>> $2 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t>\n"
								+ "					(temporal-quantity:<e,t> $7)\n"
								+ "					(c_REL:<e,<i,t>> $7 2:i)\n"
								+ "					(c_REL:<e,<e,t>> $7 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $8:e (week:<e,t> $8))))))))))))))");
		final LogicalExpression arg = TestServices.getCategoryServices()
				.readSemantics(
						"(a:<id,<<e,t>,e>> na:id (lambda $0:e (it:<e,t> $0)))");
		final LogicalExpression func = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $0:e (and:<t*,t>\n"
								+ "	(sponsor-01:<e,t> $0)\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		$1)\n"
								+ "	(c_ARGX:<e,<e,t>> $0 \n"
								+ "		(a:<id,<<e,t>,e>> na:id (lambda $2:e (and:<t*,t>\n"
								+ "			(workshop:<e,t> $2)\n"
								+ "			(c_REL:<e,<e,t>> $2 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t>\n"
								+ "					(person:<e,t> $3)\n"
								+ "					(c_REL:<e,<i,t>> $3 50:i)\n"
								+ "					(c_ARGX-of:<e,<e,t>> $3 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $4:e (and:<t*,t>\n"
								+ "							(expert-41:<e,t> $4)\n"
								+ "							(c_ARGX:<e,<e,t>> $4 \n"
								+ "								(a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t>\n"
								+ "									(oppose-01:<e,t> $5)\n"
								+ "									(c_ARGX:<e,<e,t>> $5 \n"
								+ "										(a:<id,<<e,t>,e>> na:id (lambda $6:e (terrorism:<e,t> $6))))))))))))))))\n"
								+ "			(c_REL:<e,<e,t>> $2 \n"
								+ "				(a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t>\n"
								+ "					(temporal-quantity:<e,t> $7)\n"
								+ "					(c_REL:<e,<i,t>> $7 2:i)\n"
								+ "					(c_REL:<e,<e,t>> $7 \n"
								+ "						(a:<id,<<e,t>,e>> na:id (lambda $8:e (week:<e,t> $8))))))))))))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(func, arg));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 1));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 2));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 3));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 4));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 5));
	}

	@Test
	public void test16() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (oppose-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (terrorism:<e,t> $2)))))))))");
		final LogicalExpression arg = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (terrorism:<e,t> $0))");
		final LogicalExpression func = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $4:<e,t> (lambda $0:e (c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (oppose-01:<e,t> $1) (c_ARGX:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id $4))))))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(func, arg));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 1));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 2));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 3));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 4));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 5));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 6));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 7));
		Assert.assertEquals(null, GetApplicationFunction.of(result, arg, 3, 8));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 9));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3, 10));
	}

	@Test
	public void test17() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t> (become-01:<e,t> $0) "
								+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (warfare:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (cyber:<e,t> $2)))))))) "
								+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (weapon:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (common:<e,t> $4)))))))) "
								+ "(c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (person:<e,t> $5) (c_REL:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (country:<e,t> $6) (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (name:<e,t> $7) (c_op:<e,<e,t>> $7 South++Korea:e)))))))))))))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t> (become-01:<e,t> $0) "
								+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (and:<t*,t> (warfare:<e,t> $1) (c_REL:<e,<e,t>> $1 (a:<id,<<e,t>,e>> na:id (lambda $2:e (cyber:<e,t> $2)))))))) "
								+ "(c_ARGX:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (common:<e,t> $4)))) (weapon:<e,t> $3)))))))");
		final LogicalExpression func = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:<e,t> (lambda $0:e (and:<t*,t>  "
								+ "($1 $0) "
								+ "(c_REL:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $5:e (and:<t*,t> (person:<e,t> $5) "
								+ "(c_REL:<e,<e,t>> $5 (a:<id,<<e,t>,e>> na:id (lambda $6:e (and:<t*,t> (country:<e,t> $6) (c_REL:<e,<e,t>> $6 (a:<id,<<e,t>,e>> na:id (lambda $7:e (and:<t*,t> (name:<e,t> $7) (c_op:<e,<e,t>> $7 South++Korea:e))))))))))))))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(func, arg));
		Assert.assertEquals(func, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test2() {
		final LogicalExpression result = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (boo:<e,<e,t>> p:e $0)");
		final LogicalExpression arg = TestServices.getCategoryServices()
				.readSemantics("p:e");
		final LogicalExpression expected = TestServices.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (boo:<e,<e,t>> $0 $1)))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test3() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t> (p:<e,t> E:e) (go:<e,t> p:e) (boo:<e,<e,t>> p:e $0)))");
		final LogicalExpression arg = TestServices.getCategoryServices()
				.readSemantics("p:e");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $0:e (and:<t*,t> (p:<e,t> E:e) (go:<e,t> $1) (boo:<e,<e,t>> $1 $0)))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test4() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (and:<t*,t> (n:<e,t> $1) (pred:<e,t> $1)))");
		final LogicalExpression arg = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (n:<e,t> $0))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pred:<e,t> $1))))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test5() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (b:<e,t> $1) (a:<e,<e,t>> $1 $2))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (a:<e,<e,t>> $1 $0) (b:<e,t> $1))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $2 $1))))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test6() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (lambda $2:e (and:<t*,t> (a:<e,<e,t>> $1 $2) (b:<e,t> $1))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (a:<e,<e,t>> $1 $0) (b:<e,t> $1))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $2 $1))))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test7() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (a:<e,<e,t>> $0 $1) (b:<e,t> $0) (a:<e,<e,t>> $1 $0) (b:<e,t> $1))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (a:<e,<e,t>> $1 $0) (b:<e,t> $1))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2 $1) ($0 $1 $2)))))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test8() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (a:<e,<e,t>> $0 $1) (b:<e,t> $0) (pred:<e,t> $1) (a:<e,<e,t>> $1 $0) (b:<e,t> $1))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (and:<t*,t> (a:<e,<e,t>> $1 $0) (b:<e,t> $1))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e (and:<t*,t> ($0 $2 $1) ($0 $1 $2) (pred:<e,t> $2)))))");
		Assert.assertEquals(expected, GetApplicationFunction.of(result, arg, 3));
	}

	@Test
	public void test9() {
		final LogicalExpression result = TestServices
				.getCategoryServices()
				.readSemantics(
						"(a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> "
								+ "(and:<e,t> $0) "
								+ "(c_op1:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (international:<e,t> $1)))) "
								+ "(c_op2:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $2:e (military:<e,t> $2)))) "
								+ "(c_op2:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (terrorism:<e,t> $3)))))))");
		final LogicalExpression arg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,<e,t>> (lambda $1:e (lambda $2:e ($0 $1 $2))))");
		Assert.assertEquals(result, ApplyAndSimplify.of(
				GetApplicationFunction.of(result, arg, 3), arg));
	}

	@Test
	public void testSubExp1() {
		final LogicalExpression appArg = TestServices.getCategoryServices()
				.readSemantics("boo:e");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("boo:e");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("e"));
		final LogicalExpression expected = replacementVariable;
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp10() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (a:<<e,t>,e> (lambda $0:e (and:<t*,t> (b:<e,t> A:e) (a:<e,t> P:e) (pred:<e,<e,t>> $1 $0)))))");
		final LogicalExpression subExp = TestServices
				.getCategoryServices()
				.readSemantics(
						"(a:<<e,t>,e> (lambda $0:e (and:<t*,t> (b:<e,t> A:e) (a:<e,t> P:e) (pred:<e,<e,t>> boo:e $0))))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,e>"));
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(TestServices.getCategoryServices()
						.readSemantics("boo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp11() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (a:<<e,t>,e> (lambda $0:e (and:<t*,t> (b:<e,t> A:e) (a:<e,t> P:e) (pred:<e,<e,t>> $1 $0)))))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,e>"));
		final LogicalExpression subExp = TestServices
				.getCategoryServices()
				.readSemantics(
						"(a:<<e,t>,e> (lambda $0:e (and:<t*,t> (b:<e,t> A:e) (pred:<e,<e,t>> boo:e $0) (a:<e,t> P:e))))");
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(TestServices.getCategoryServices()
						.readSemantics("boo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp12() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (b:<e,t> $2) (a:<e,t> P:e) (boo:<e,<e,t>> $0 $1)))))");
		final LogicalExpression subExp = TestServices
				.getCategoryServices()
				.readSemantics(
						"(and:<t*,t> (b:<e,t> doo:e) (a:<e,t> P:e) (boo:<e,<e,t>> koo:e loo:e))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,<e,<e,t>>>"));
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(
						TestServices.getCategoryServices().readSemantics(
								"koo:e"),
						TestServices.getCategoryServices().readSemantics(
								"loo:e"),
						TestServices.getCategoryServices().readSemantics(
								"doo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp13() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (b:<e,t> $2) (a:<e,t> P:e) (boo:<e,<e,t>> $0 $1)))))");
		final LogicalExpression subExp = TestServices
				.getCategoryServices()
				.readSemantics(
						"(and:<t*,t> (a:<e,t> P:e) (boo:<e,<e,t>> koo:e loo:e) (b:<e,t> doo:e))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,<e,<e,t>>>"));
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(
						TestServices.getCategoryServices().readSemantics(
								"koo:e"),
						TestServices.getCategoryServices().readSemantics(
								"loo:e"),
						TestServices.getCategoryServices().readSemantics(
								"doo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp14() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (b:<e,t> $2) (a:<e,t> P:e) (boo:<e,<e,t>> $0 $1)))))");
		final LogicalExpression subExp = TestServices
				.getCategoryServices()
				.readSemantics(
						"(and:<t*,t> (a:<e,t> P:e) (boo:<e,<e,t>> koo:e loo:e))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,<e,<e,t>>>"));
		final LogicalExpression expected = null;
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp2() {
		final LogicalExpression appArg = TestServices.getCategoryServices()
				.readSemantics("boo:e");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("foo:e");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("e"));
		final LogicalExpression expected = null;
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp3() {
		final LogicalExpression appArg = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (boo:<e,t> $0))");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("foo:e");
		final LogicalExpression expected = null;
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,t>"));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp4() {
		final LogicalExpression appArg = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (boo:<e,t> $0))");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("boo:<e,t>");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,t>"));
		final LogicalExpression expected = null;
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp5() {
		final LogicalExpression appArg = TestServices.getCategoryServices()
				.readSemantics("(lambda $0:e (boo:<e,t> $0))");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("(boo:<e,t> koo:e)");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,t>"));
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(TestServices.getCategoryServices()
						.readSemantics("koo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp6() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (lambda $2:e (boo:<e,<e,<e,t>>> $0 $1 $2))))");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("(boo:<e,<e,<e,t>>> koo:e)");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,<e,<e,t>>>"));
		final LogicalExpression expected = null;
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp7() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (lambda $1:e (lambda $2:e (boo:<e,<e,<e,t>>> $0 $1 $2))))");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("(boo:<e,<e,<e,t>>> koo:e loo:e doo:e)");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,<e,<e,t>>>"));
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(
						TestServices.getCategoryServices().readSemantics(
								"koo:e"),
						TestServices.getCategoryServices().readSemantics(
								"loo:e"),
						TestServices.getCategoryServices().readSemantics(
								"doo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp8() {
		final LogicalExpression appArg = TestServices.getCategoryServices()
				.readSemantics("(a:<<e,t>,e> (lambda $0:e (pred:<e,t> $0)))");
		final LogicalExpression subExp = TestServices.getCategoryServices()
				.readSemantics("(a:<<e,t>,e> (lambda $0:e (pred:<e,t> $0)))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,<e,<e,t>>>"));
		final LogicalExpression expected = replacementVariable;
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

	@Test
	public void testSubExp9() {
		final LogicalExpression appArg = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (a:<<e,t>,e> (lambda $0:e (pred:<e,<e,t>> $1 $0))))");
		final LogicalExpression subExp = TestServices
				.getCategoryServices()
				.readSemantics(
						"(a:<<e,t>,e> (lambda $0:e (pred:<e,<e,t>> boo:e $0))))");
		final Variable replacementVariable = new Variable(LogicLanguageServices
				.getTypeRepository().getType("<e,e>"));
		final LogicalExpression expected = new Literal(replacementVariable,
				ArrayUtils.create(TestServices.getCategoryServices()
						.readSemantics("boo:e")));
		Assert.assertEquals(expected, GetApplicationFunction.processSubExp(
				subExp, appArg, replacementVariable));
	}

}
