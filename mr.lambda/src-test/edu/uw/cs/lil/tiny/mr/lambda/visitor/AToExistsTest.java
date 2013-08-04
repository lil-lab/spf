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
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.TestServices;
import edu.uw.cs.lil.tiny.mr.language.type.Type;

public class AToExistsTest {
	
	private final LogicalExpression				aPredicate;
	private final Map<Type, LogicalConstant>	equalsPredicates;
	private final LogicalExpression				existsPredicate;
	
	public AToExistsTest() {
		// Make sure test services is initialized
		new TestServices();
		this.existsPredicate = LogicalExpression.parse("exists:<<e,t>,t>",
				false);
		this.aPredicate = LogicalExpression.parse("a:<<e,t>,e>", false);
		this.equalsPredicates = new HashMap<Type, LogicalConstant>();
		this.equalsPredicates.put(LogicLanguageServices.getTypeRepository()
				.getEntityType(), (LogicalConstant) LogicalExpression.parse(
				"eq:<e,<e,t>>", false));
		
	}
	
	@Test
	public void test() {
		final LogicalExpression exp = LogicalExpression
				.parse("(lambda $0:e (pred1:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (pred2:<e,t> $1)))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> (pred1:<e,<e,t>> $0 $1) (pred2:<e,t> $1)))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(result, out);
	}
	
	@Test
	public void test10() {
		// (lambda $0:e (a:<<e,t>,e> (lambda $1:e (and:<t*,t> (eq:<e,<e,t>> $1
		// (argmax:<<e,t>,<<e,n>,e>> (lambda $2:e (front:<e,<e,t>> $2 $0))
		// dist:<e,n>)) (blue:<e,t> $1)))))
		final LogicalExpression exp = LogicalExpression
				.parse("(lambda $0:e (fun:<e,e> (a:<<e,t>,e> (lambda $1:e (boo:<e,<e,t>> $1 $0)))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (boo:<e,<e,t>> $1 $0))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(Simplify.of(result), out);
	}
	
	@Test
	public void test11() {
		final LogicalExpression exp = LogicalExpression
				.parse("(lambda $0:e (a:<<e,t>,e> (lambda $1:e (boo:<e,<e,t>> $1 $0))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (boo:<e,<e,t>> $1 $0))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(Simplify.of(result), out);
	}
	
	@Test
	public void test2() {
		final LogicalExpression exp = LogicalExpression
				.parse("(lambda $0:e (to:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (and:<t*,t> (chair:<e,t> $1) (intersect:<e,<e,t>> $1 (a:<<e,t>,e> corner:<e,t>))))))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> (to:<e,<e,t>> $0 $1) (chair:<e,t> $1) (exists:<<e,t>,t> (lambda $2:e (and:<t*,t> (intersect:<e,<e,t>> $1 $2) (corner:<e,t> $2))))))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(result, out);
	}
	
	@Test
	public void test3() {
		final LogicalExpression exp = LogicalExpression
				.parse("(intersect:<e,<e,t>> (a:<<e,t>,e> chair:<e,t>) (a:<<e,t>,e> corner:<e,t>))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(exists:<<e,t>,t> (lambda $0:e (and:<t*,t> (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> (intersect:<e,<e,t>> $0 $1) (corner:<e,t> $1)))) (chair:<e,t> $0))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(result, out);
	}
	
	@Test
	public void test4() {
		final LogicalExpression exp = LogicalExpression
				.parse("(a:<<e,t>,e> (lambda $0:e (and:<t*,t> (blue:<e,t> $0) (hall:<e,t> $0))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(exists:<<e,t>,t> (lambda $0:e (and:<t*,t> (blue:<e,t> $0) (hall:<e,t> $0))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(result, out);
	}
	
	@Test
	public void test5() {
		final LogicalExpression exp = LogicalExpression.parse(
				"(a:<<e,t>,e> intersection:<e,t>)", false);
		final LogicalExpression result = LogicalExpression.parse(
				"(exists:<<e,t>,t> intersection:<e,t>)", false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(result, out);
	}
	
	@Test
	public void test6() {
		final LogicalExpression exp = LogicalExpression
				.parse("(front:<e,<e,t>> (a:<<e,t>,e> (lambda $0:e (end:<e,<e,t>> $0 (io:<<e,t>,e> blue:<e,t>)))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> (end:<e,<e,t>> $1 (io:<<e,t>,e> blue:<e,t>)) (front:<e,<e,t>> $1 $0)))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(result, out);
	}
	
	@Test
	public void test7() {
		final LogicalExpression exp = LogicalExpression.parse(
				"(fun:<e,e> (a:<<e,t>,e> (lambda $0:e true:t)))", false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (and:<t*,t> true:t (eq:<e,<e,t>> $0 (fun:<e,e> $1))))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(Simplify.of(result), out);
	}
	
	@Test
	public void test8() {
		final LogicalExpression exp = LogicalExpression.parse(
				"(fun:<e,e> (a:<<e,t>,e> (lambda $0:e (pred:<e,t> $0))))",
				false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $1:e (exists:<<e,t>,t> (lambda $0:e (and:<t*,t> (pred:<e,t> $0) (eq:<e,<e,t>> $1 (fun:<e,e> $0)))))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(Simplify.of(result), out);
	}
	
	@Test
	public void test9() {
		final LogicalExpression exp = LogicalExpression
				.parse("(lambda $0:e (a:<<e,t>,e> (lambda $1:e (boo:<e,<e,t>> $1 $0))))",
						false);
		final LogicalExpression result = LogicalExpression
				.parse("(lambda $0:e (exists:<<e,t>,t> (lambda $1:e (boo:<e,<e,t>> $1 $0))))",
						false);
		final LogicalExpression out = AToExists.of(exp, existsPredicate,
				aPredicate, equalsPredicates);
		Assert.assertEquals(Simplify.of(result), out);
	}
}
