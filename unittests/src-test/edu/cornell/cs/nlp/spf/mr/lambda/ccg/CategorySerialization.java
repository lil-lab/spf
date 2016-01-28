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
package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class CategorySerialization {

	private static void doTest(Category<LogicalExpression> category) {
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			new ObjectOutputStream(out).writeObject(category);
			@SuppressWarnings("unchecked")
			final Category<LogicalExpression> object = (Category<LogicalExpression>) new ObjectInputStream(
					new ByteArrayInputStream(out.toByteArray())).readObject();
			Assert.assertEquals(category, object);
			Assert.assertFalse(category == object);
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test1() {
		final ComplexCategory<LogicalExpression> c = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/(NP|(NP|NP)) : (lambda $0:<e+,e> ($0 (do_until:<e,<t,e>> (do:<e,e> turn:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) (do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>)))))");
		doTest(c);
	}

	@Test
	public void test2() {
		final Category<LogicalExpression> c = TestServices
				.getCategoryServices().read("NP|(NP|NP) : do_seq:<e+,e>");
		doTest(c);
	}

	@Test
	public void test3() {
		final Category<LogicalExpression> c = TestServices
				.getCategoryServices()
				.read("S : (do_seq:<e+,e> (do_until:<e,<t,e>> (do:<e,e> turn:e) "
						+ "(notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> (front:<<e,t>,<e,t>> at:<e,t>)))) "
						+ "(do_until:<e,<t,e>> (do:<e,e> travel:e) (notempty:<<e,t>,t> (intersect:<<e,t>*,<e,t>> chair:<e,t> at:<e,t>))))");
		doTest(c);
	}

}
