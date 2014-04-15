package edu.uw.cs.lil.tiny.mr.lambda.printers;

import org.junit.Assert;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

public class LogicalExpressionToIndentedStringTest {
	
	public LogicalExpressionToIndentedStringTest() {
		new TestServices();
	}
	
	@Test
	public void test() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.parseSemantics("(lambda $0:e (and:<t*,t> (p:<e,t> $0) (q:<e,<e,t>> $0 (io:<<e,t>,e> (lambda $1:e (and:<t*,t> (boo:<e,t> $1) (foo:<e,<e,t>> $1 foo:e)))))))");
		final String expected = "(lambda $0:e (and:<t*,t>\n\t(p:<e,t> $0)\n\t(q:<e,<e,t>> $0 \n\t\t(io:<<e,t>,e> (lambda $1:e (and:<t*,t>\n\t\t\t(boo:<e,t> $1)\n\t\t\t(foo:<e,<e,t>> $1 foo:e)))))))";
		Assert.assertEquals(expected,
				LogicalExpressionToIndentedString.of(exp, "\t"));
	}
	
	@Test
	public void test3() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.parseSemantics("(lambda $0:e (p:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (and:<t*,t> (boo:<e,t> foo:e) (goo:<e,t> doo:e))))))");
		final String expected = "(lambda $0:e (p:<e,<e,t>> $0 \n\t(a:<<e,t>,e> (lambda $1:e (and:<t*,t>\n\t\t\t(boo:<e,t> foo:e)\n\t\t\t(goo:<e,t> doo:e))))))";
		Assert.assertEquals(expected,
				LogicalExpressionToIndentedString.of(exp, "\t"));
	}
	
}
