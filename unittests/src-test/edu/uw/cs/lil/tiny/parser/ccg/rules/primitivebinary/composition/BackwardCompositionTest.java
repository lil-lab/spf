package edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class BackwardCompositionTest {
	
	public BackwardCompositionTest() {
		new TestServices();
	}
	
	@Test
	public void test() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("NP/N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");
		
		final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
				new LogicalExpressionCategoryServices(true, true, true), 0);
		final Collection<ParseRuleResult<LogicalExpression>> actual = rule
				.apply(secondary, primary);
		Assert.assertTrue(actual.isEmpty());
	}
	
	@Test
	public void test2() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("NP\\N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.parse("N\\N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");
		
		final BackwardComposition<LogicalExpression> rule = new BackwardComposition<LogicalExpression>(
				new LogicalExpressionCategoryServices(true, true, true), 0);
		final Collection<ParseRuleResult<LogicalExpression>> actual = rule
				.apply(secondary, primary);
		Assert.assertEquals(1, actual.size());
		Assert.assertEquals(
				TestServices
						.getCategoryServices()
						.parse("NP\\N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1)))))"),
				actual.iterator().next().getResultCategory());
	}
	
}
