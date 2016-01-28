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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.coordination.lambda;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.coordination.lambda.LogicalExpressionCoordinationServices;

public class LogicalExpressionCoordinationServicesTest {
	
	private final LogicalExpressionCoordinationServices	services;
	
	public LogicalExpressionCoordinationServicesTest() {
		// Coordination services
		this.services = new LogicalExpressionCoordinationServices(
				(LogicalConstant) TestServices.getCategoryServices()
						.readSemantics("conj:c"),
				(LogicalConstant) TestServices.getCategoryServices()
						.readSemantics("disj:c"),
				TestServices.getCategoryServices());
	}
	
	@Ignore
	@Test
	public void test() {
		// cities or towns named springfield
		final LogicalExpression conj = TestServices.getCategoryServices()
				.readSemantics(
						"(disj:<<e,t>,<<e,t>,t>> city:<e,t> town:<e,t>)");
		final LogicalExpression func = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (named:<e,<e,t>> $1 springfield:e))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $1:e (and:<t*,t> (or:<t*,t> (city:<e,t> $1) (town:<e,t> $1)) (named:<e,<e,t>> $1 springfield:e)))");
		final LogicalExpression actual = services.applyCoordination(func, conj);
		Assert.assertEquals(expected, actual);
	}
	
	@Ignore
	@Test
	public void test2() {
		// states border colorado and border new mexico
		final LogicalExpression conj = TestServices
				.getCategoryServices()
				.readSemantics(
						"(conj:<<e,t>,<<e,t>,t>> (lambda $0:e (next_to:<e,<e,t>> $0 colorado:e)) (lambda $1:e (next_to:<e,<e,t>> $1 nm:e)))");
		final LogicalExpression func = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (named:<e,<e,t>> $1 springfield:e))))");
		final LogicalExpression expected = TestServices
				.getCategoryServices()
				.readSemantics(
						"(lambda $0:e (and:<t*,t> (next_to:<e,<e,t>> $0 colorado:e) (next_to:<e,<e,t>> $0 nm:e) (named:<e,<e,t>> $0 springfield:e)))");
		final LogicalExpression actual = services.applyCoordination(func, conj);
		Assert.assertEquals(expected, actual);
	}
	
}
