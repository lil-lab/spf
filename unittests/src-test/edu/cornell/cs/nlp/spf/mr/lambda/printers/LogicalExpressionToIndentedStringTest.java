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
package edu.cornell.cs.nlp.spf.mr.lambda.printers;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToIndentedString;

public class LogicalExpressionToIndentedStringTest {

	public LogicalExpressionToIndentedStringTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (and:<t*,t> (p:<e,t> $0) (q:<e,<e,t>> $0 (io:<<e,t>,e> (lambda $1:e (and:<t*,t> (boo:<e,t> $1) (foo:<e,<e,t>> $1 foo:e)))))))");
		final String expected = "(lambda $0:e (and:<t*,t>\n\t(p:<e,t> $0)\n\t(q:<e,<e,t>> $0 \n\t\t(io:<<e,t>,e> (lambda $1:e (and:<t*,t>\n\t\t\t(boo:<e,t> $1)\n\t\t\t(foo:<e,<e,t>> $1 foo:e)))))))";
		Assert.assertEquals(expected,
				LogicalExpressionToIndentedString.of(exp, "\t"));
	}

	@Test
	public void test2() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(a:<id,<<e,t>,e>> !1 (lambda $0:e (and:<t*,t> (say-01:<e,t> $0) (c_ARG0:<e,<e,t>> $0 (a:<id,<<e,t>,e>> !2 (lambda $1:e (and:<t*,t> (university:<e,t> $1) (c_name:<e,<e,t>> $1 Naif++Arab++Academy++for++Security++Sciences:e) "
						+ "(c_ARG1-of:<e,<e,t>> $1 (a:<id,<<e,t>,e>> !3 (lambda $2:e (and:<t*,t> (base-01:<e,t> $2) (c_location:<e,<e,t>> $2 (a:<id,<<e,t>,e>> !4 (lambda $3:e (and:<t*,t> (city:<e,t> $3) (c_name:<e,<e,t>> $3 Riyadh:e))))))))))))) "
						+ "(c_ARG1:<e,<e,t>> $0 (a:<id,<<e,t>,p>> !5 (lambda $4:e (and:<t*,t> (run-01:<e,t> $4) (c_ARG0:<e,<e,t>> $4 (ref:<id,e> !2)) (c_ARG1:<e,<e,t>> $4 (a:<id,<<e,t>,e>> !6 (lambda $5:e (and:<t*,t> (workshop:<e,t> $5) (c_beneficiary:<e,<e,t>> $5 "
						+ "(a:<id,<<e,t>,e>> !7 (lambda $6:e (and:<t*,t> (person:<e,t> $6) (c_quant:<e,<i,t>> $6 50:i) (c_ARG1-of:<e,<e,t>> $6 (a:<id,<<e,t>,e>> !8 (lambda $7:e (and:<t*,t> (expert-41:<e,t> $7) (c_ARG2:<e,<e,t>> $7 (a:<id,<<e,t>,e>> !9 (lambda $8:e (and:<t*,t> (oppose-01:<e,t> $8) "
						+ "(c_ARG1:<e,<e,t>> $8 (a:<id,<<e,t>,e>> !10 (lambda $9:e (terrorism:<e,t> $9)))))))))))))))) (c_duration:<e,<e,t>> $5 (a:<id,<<e,t>,e>> !11 (lambda $10:e (and:<t*,t> (week:<e,t> $10) (c_quant:<e,<i,t>> $10 2:i))))))))))))) "
						+ "(c_manner:<e,<e,t>> $0 (a:<id,<<e,t>,e>> !12 (lambda $11:e (and:<t,<t,t>> (thing:<e,t> $11) (c_ARG1-of:<e,<p,t>> $11 (a:<id,<<p,t>,p>> !13 (lambda $12:e (and:<t,<t,t>> (state-01:<e,t> $12) (c_ARG0:<p,<e,t>> $12 (ref:<id,e> !2)))))))))))))");
		final String expected = "(a:<id,<<e,t>,e>> !1 (lambda $0:e (and:<t*,t>\n\t(say-01:<e,t> $0)\n\t(c_ARG0:<e,<e,t>> $0 \n\t\t(a:<id,<<e,t>,e>> !2 (lambda $1:e (and:<t*,t>\n\t\t\t(university:<e,t> $1)\n\t\t\t(c_name:<e,<e,t>> $1 Naif++Arab++Academy++for++Security++Sciences:e)\n\t\t\t(c_ARG1-of:<e,<e,t>> $1 \n\t\t\t\t(a:<id,<<e,t>,e>> !3 (lambda $2:e (and:<t*,t>\n\t\t\t\t\t(base-01:<e,t> $2)\n\t\t\t\t\t(c_location:<e,<e,t>> $2 \n\t\t\t\t\t\t(a:<id,<<e,t>,e>> !4 (lambda $3:e (and:<t*,t>\n\t\t\t\t\t\t\t(city:<e,t> $3)\n\t\t\t\t\t\t\t(c_name:<e,<e,t>> $3 Riyadh:e)))))))))))))\n\t(c_ARG1:<e,<e,t>> $0 \n\t\t(a:<id,<<e,t>,p>> !5 (lambda $4:e (and:<t*,t>\n\t\t\t(run-01:<e,t> $4)\n\t\t\t(c_ARG0:<e,<e,t>> $4 \n\t\t\t\t(ref:<id,e> !2))\n\t\t\t(c_ARG1:<e,<e,t>> $4 \n\t\t\t\t(a:<id,<<e,t>,e>> !6 (lambda $5:e (and:<t*,t>\n\t\t\t\t\t(workshop:<e,t> $5)\n\t\t\t\t\t(c_beneficiary:<e,<e,t>> $5 \n\t\t\t\t\t\t(a:<id,<<e,t>,e>> !7 (lambda $6:e (and:<t*,t>\n\t\t\t\t\t\t\t(person:<e,t> $6)\n\t\t\t\t\t\t\t(c_quant:<e,<i,t>> $6 50:i)\n\t\t\t\t\t\t\t(c_ARG1-of:<e,<e,t>> $6 \n\t\t\t\t\t\t\t\t(a:<id,<<e,t>,e>> !8 (lambda $7:e (and:<t*,t>\n\t\t\t\t\t\t\t\t\t(expert-41:<e,t> $7)\n\t\t\t\t\t\t\t\t\t(c_ARG2:<e,<e,t>> $7 \n\t\t\t\t\t\t\t\t\t\t(a:<id,<<e,t>,e>> !9 (lambda $8:e (and:<t*,t>\n\t\t\t\t\t\t\t\t\t\t\t(oppose-01:<e,t> $8)\n\t\t\t\t\t\t\t\t\t\t\t(c_ARG1:<e,<e,t>> $8 \n\t\t\t\t\t\t\t\t\t\t\t\t(a:<id,<<e,t>,e>> !10 (lambda $9:e (terrorism:<e,t> $9))))))))))))))))\n\t\t\t\t\t(c_duration:<e,<e,t>> $5 \n\t\t\t\t\t\t(a:<id,<<e,t>,e>> !11 (lambda $10:e (and:<t*,t>\n\t\t\t\t\t\t\t(week:<e,t> $10)\n\t\t\t\t\t\t\t(c_quant:<e,<i,t>> $10 2:i)))))))))))))\n\t(c_manner:<e,<e,t>> $0 \n\t\t(a:<id,<<e,t>,e>> !12 (lambda $11:e (and:<t,<t,t>> (thing:<e,t> $11) (c_ARG1-of:<e,<p,t>> $11 \n\t\t\t(a:<id,<<p,t>,p>> !13 (lambda $12:e (and:<t,<t,t>> (state-01:<e,t> $12) (c_ARG0:<p,<e,t>> $12 \n\t\t\t\t(ref:<id,e> !2)))))))))))))";
		Assert.assertEquals(expected,
				LogicalExpressionToIndentedString.of(exp, "\t"));
	}

	@Test
	public void test3() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (p:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (and:<t*,t> (boo:<e,t> foo:e) (goo:<e,t> doo:e))))))");
		final String expected = "(lambda $0:e (p:<e,<e,t>> $0 \n\t(a:<<e,t>,e> (lambda $1:e (and:<t*,t>\n\t\t\t(boo:<e,t> foo:e)\n\t\t\t(goo:<e,t> doo:e))))))";
		Assert.assertEquals(expected,
				LogicalExpressionToIndentedString.of(exp, "\t"));
	}

}
