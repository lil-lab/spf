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
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class SerializationTest {

	public SerializationTest() {
		// Make sure test services is initialized
		TestServices.init();
	}

	private static void doTest(LogicalExpression exp) {
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			new ObjectOutputStream(out).writeObject(exp);
			final LogicalExpression object = (LogicalExpression) new ObjectInputStream(
					new ByteArrayInputStream(out.toByteArray())).readObject();
			Assert.assertEquals(exp, object);
			Assert.assertFalse(exp == object);
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test() {
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e (pred1:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (pred2:<e,t> $1)))))");
		doTest(exp);
	}

	@Test
	public void test10() {
		// (lambda $0:e (a:<<e,t>,e> (lambda $1:e (and:<t*,t> (eq:<e,<e,t>> $1
		// (argmax:<<e,t>,<<e,n>,e>> (lambda $2:e (front:<e,<e,t>> $2 $0))
		// dist:<e,n>)) (blue:<e,t> $1)))))
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e (fun:<e,e> (a:<<e,t>,e> (lambda $1:e (boo:<e,<e,t>> $1 $0)))))");
		doTest(exp);
	}

	@Test
	public void test11() {
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e (a:<<e,t>,e> (lambda $1:e (boo:<e,<e,t>> $1 $0))))");
		doTest(exp);
	}

	@Test
	public void test2() {
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e (to:<e,<e,t>> $0 (a:<<e,t>,e> (lambda $1:e (and:<t*,t> (chair:<e,t> $1) (intersect:<e,<e,t>> $1 (a:<<e,t>,e> corner:<e,t>))))))))");
		doTest(exp);
	}

	@Test
	public void test3() {
		final LogicalExpression exp = LogicalExpression
				.read("(intersect:<e,<e,t>> (a:<<e,t>,e> chair:<e,t>) (a:<<e,t>,e> corner:<e,t>))");
		doTest(exp);
	}

	@Test
	public void test4() {
		final LogicalExpression exp = LogicalExpression
				.read("(a:<<e,t>,e> (lambda $0:e (and:<t*,t> (blue:<e,t> $0) (hall:<e,t> $0))))");
		doTest(exp);
	}

	@Test
	public void test5() {
		final LogicalExpression exp = LogicalExpression
				.read("(a:<<e,t>,e> intersection:<e,t>)");
		doTest(exp);
	}

	@Test
	public void test6() {
		final LogicalExpression exp = LogicalExpression
				.read("(front:<e,<e,t>> (a:<<e,t>,e> (lambda $0:e (end:<e,<e,t>> $0 (io:<<e,t>,e> blue:<e,t>)))))");
		doTest(exp);
	}

	@Test
	public void test7() {
		final LogicalExpression exp = LogicalExpression
				.read("(fun:<e,e> (a:<<e,t>,e> (lambda $0:e true:t)))");
		doTest(exp);
	}

	@Test
	public void test8() {
		final LogicalExpression exp = LogicalExpression
				.read("(fun:<e,e> (a:<<e,t>,e> (lambda $0:e (pred:<e,t> $0))))");
		doTest(exp);
	}

	@Test
	public void test9() {
		final LogicalExpression exp = LogicalExpression
				.read("(lambda $0:e (a:<<e,t>,e> (lambda $1:e (boo:<e,<e,t>> $1 $0))))");
		doTest(exp);
	}

}
