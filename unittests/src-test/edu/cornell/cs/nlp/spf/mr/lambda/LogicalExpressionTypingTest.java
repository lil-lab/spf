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
package edu.cornell.cs.nlp.spf.mr.lambda;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class LogicalExpressionTypingTest {

	public LogicalExpressionTypingTest() {
		TestServices.init();
	}

	@Test
	public void test1() {
		final LogicalExpression exp = TestServices.CATEGORY_SERVICES
				.readSemantics("(lambda $0:e (intersect:<<e,t>*,<e,t>> (lambda $1:e (chair:<e,t> $1)) (lambda $2:e (front:<<e,t>,<e,t>> (lambda $3:e (at:<e,t> $3)) $2)) $0))");
		assertEquals(
				LogicLanguageServices.getTypeRepository().getType("<e,t>"),
				exp.getType());

	}

}
