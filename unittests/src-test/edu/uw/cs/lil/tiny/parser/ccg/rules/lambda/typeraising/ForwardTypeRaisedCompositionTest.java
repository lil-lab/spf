package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class ForwardTypeRaisedCompositionTest {
	
	public ForwardTypeRaisedCompositionTest() {
		new TestServices();
	}
	
	@Test
	public void test() {
		final Category<LogicalExpression> primary = TestServices
				.getCategoryServices()
				.parse("N : (lambda $1:e (boo:<e,t> $1))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices().parse(
						"(S\\N)/NP : (lambda $0:e (lambda $1:<e,t> ($1 $0)))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices().parse(
						"S/NP : (lambda $0:e (boo:<e,t> $0))");
		
		final ForwardTypeRaisedComposition rule = new ForwardTypeRaisedComposition(
				TestServices.getCategoryServices());
		final Collection<ParseRuleResult<LogicalExpression>> actual = rule
				.apply(primary, secondary);
		Assert.assertEquals(1, actual.size());
		Assert.assertEquals(expected, actual.iterator().next()
				.getResultCategory());
	}
	
	@Test
	public void test2() {
		final Category<LogicalExpression> primary = TestServices
				.getCategoryServices()
				.parse("N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (boo:<e,t> $1))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("S\\(N/N)/NP : (lambda $0:e (lambda $1:<<e,t>,<e,t>> ($1 foo:<e,t> $0)))");
		final Category<LogicalExpression> expected = TestServices
				.getCategoryServices()
				.parse("S/NP : (lambda $0:e (and:<t*,t> (boo:<e,t> $0) (foo:<e,t> $0)))");
		
		final ForwardTypeRaisedComposition rule = new ForwardTypeRaisedComposition(
				TestServices.getCategoryServices());
		final Collection<ParseRuleResult<LogicalExpression>> actual = rule
				.apply(primary, secondary);
		Assert.assertEquals(1, actual.size());
		Assert.assertEquals(expected, actual.iterator().next()
				.getResultCategory());
	}
	
}
