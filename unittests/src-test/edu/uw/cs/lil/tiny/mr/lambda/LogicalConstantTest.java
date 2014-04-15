/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
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
 ******************************************************************************/
package edu.uw.cs.lil.tiny.mr.lambda;

import java.io.IOException;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.utils.log.LogLevel;

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
		LogLevel.setLogLevel(LogLevel.NO_LOG);
		TestServices.CATEGORY_SERVICES.parseSemantics("boo:e");
		LogLevel.setLogLevel(LogLevel.INFO);
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
	
	@Test
	public void testBaseName1() {
		Assert.assertEquals("boo",
				((LogicalConstant) TestServices.CATEGORY_SERVICES
						.parseSemantics("@boo:e")).getBaseName());
	}
	
	@Test
	public void testBaseName2() {
		Assert.assertEquals("capital", LogicalConstant.read("capital:<c,t>")
				.getBaseName());
	}
	
}
