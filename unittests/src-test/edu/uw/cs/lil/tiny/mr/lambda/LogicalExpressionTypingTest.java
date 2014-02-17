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
