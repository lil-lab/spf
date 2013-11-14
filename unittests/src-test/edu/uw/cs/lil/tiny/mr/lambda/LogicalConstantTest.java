package edu.uw.cs.lil.tiny.mr.lambda;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;

public class LogicalConstantTest {
	
	private LogicLanguageServices	originalLLS;
	
	public LogicalConstantTest() {
		new TestServices();
	}
	
	@After
	public void after() {
		LogicLanguageServices.setInstance(originalLLS);
	}
	
	@Before
	public void before() {
		originalLLS = LogicLanguageServices.instance();
		try {
			LogicLanguageServices
					.setInstance(new LogicLanguageServices.Builder(
							LogicLanguageServices.getTypeRepository(),
							LogicLanguageServices.getTypeComparator())
							.addConstantsToOntology(
									TestServices.DEFAULT_ONTOLOGY_FILES)
							.setNumeralTypeName("n").closeOntology(true)
							.build());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test(expected = LogicalExpressionRuntimeException.class)
	public void test1() {
		TestServices.CATEGORY_SERVICES.parseSemantics("boo:e");
	}
	
	@Test
	public void test2() {
		TestServices.CATEGORY_SERVICES.parseSemantics("@foo:e");
	}
	
	@Test
	public void test3() {
		final LogicalExpression constant = TestServices.CATEGORY_SERVICES
				.parseSemantics("@123456789:n");
		Assert.assertEquals(123456789, LogicLanguageServices
				.logicalExpressionToInteger(constant).intValue());
		final LogicalExpression constant2 = TestServices.CATEGORY_SERVICES
				.parseSemantics("123456789:n");
		Assert.assertEquals(123456789, LogicLanguageServices
				.logicalExpressionToInteger(constant2).intValue());
	}
	
}
