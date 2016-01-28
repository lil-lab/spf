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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToLatexString;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToLatexString.Printer;

public class LogicalExpressionToLatexStringTest {

	private final Printer	printer;

	public LogicalExpressionToLatexStringTest() {
		TestServices.init();

		this.printer = new LogicalExpressionToLatexString.Printer.Builder()
				.addMapping(LogicLanguageServices.getConjunctionPredicate(),
						"\\land")
				.addMapping(LogicLanguageServices.getDisjunctionPredicate(),
						"\\lor")
				.addMapping(LogicalConstant.read("io:<id,<id,<<e,t>,e>>>"),
						"\\iota")
				.addMapping(LogicalConstant.read("a:<id,<<e,t>,e>>"),
						"\\mathcal{A}").build();
	}

	@Test
	public void test() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (pred:<e,t> $0))");
		Assert.assertEquals("\\lambda z. \\textit{pred}(z)",
				printer.toString(exp));
	}

	@Test
	public void test2() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(a:<id,<<e,t>,e>> !1 (lambda $0:e (pred:<e,t> $0)))");
		Assert.assertEquals("\\mathcal{A}_{1}(\\lambda z. \\textit{pred}(z))",
				printer.toString(exp));
	}

	@Test
	public void test3() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(a:<id,<<e,t>,e>> !1 (lambda $0:e (and:<t*,t> (pred:<e,t> $0) (pred1:<e,t> $0) (pred2:<e,t> $0))))");
		Assert.assertEquals(
				"\\mathcal{A}_{1}(\\lambda z. \\textit{pred}(z) \\land \\textit{pred1}(z) \\land \\textit{pred2}(z))",
				printer.toString(exp));
	}

	@Test
	public void test4() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(a:<id,<<e,t>,e>> !1 (lambda $0:e (and:<t*,t> (pred:<e,t> $0) (pred1:<e,t> $0) (pred2:<e,t> $0) (pred3:<e,<e,t>> $0 (io:<id,<id,<<e,t>,e>>> !2 !1 (lambda $1:e true:t))))))");
		Assert.assertEquals(
				"\\mathcal{A}_{1}(\\lambda z. \\textit{pred}(z) \\land \\textit{pred1}(z) \\land \\textit{pred2}(z) \\land \\textit{pred3}(z, \\iota_{2}^{1}(\\lambda y. \\textit{TRUE})))",
				printer.toString(exp));
	}

}
