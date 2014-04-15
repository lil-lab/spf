package edu.uw.cs.lil.tiny.mr.lambda.printers;

import org.junit.Assert;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.printers.LogicalExpressionToLatexString.Printer;

public class LogicalExpressionToLatexStringTest {
	
	private final Printer	printer;
	
	public LogicalExpressionToLatexStringTest() {
		new TestServices();
		
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
				.parseSemantics("(lambda $0:e (pred:<e,t> $0))");
		Assert.assertEquals("\\lambda z. \\textit{pred}(z)",
				printer.toString(exp));
	}
	
}
