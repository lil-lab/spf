package edu.uw.cs.lil.tiny.mr.lambda;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;

public class LogicalExpressionTypingTest {
	
	public LogicalExpressionTypingTest() {
		new TestServices();
	}
	
	@Test
	public void test1() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.parseSemantics("(lambda $0:e (intersect:<<e,t>*,<e,t>> (lambda $1:e (chair:<e,t> $1)) (lambda $2:e (front:<<e,t>,<e,t>> (lambda $3:e (at:<e,t> $3)) $2)) $0))");
		assertEquals(
				LogicLanguageServices.getTypeRepository().getType("<e,t>"),
				exp.getType());
		
	}
	
}
