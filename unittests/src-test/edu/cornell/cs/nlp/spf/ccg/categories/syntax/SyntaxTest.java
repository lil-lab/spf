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
package edu.cornell.cs.nlp.spf.ccg.categories.syntax;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.Unification;

public class SyntaxTest {

	@Test
	public void test() {
		final Syntax s = Syntax.read("S");
		final Syntax sDcl = Syntax.read("S[dcl]");
		Assert.assertTrue(sDcl.unify(s).getUnifiedSyntax() == sDcl);
		Assert.assertTrue(s.unify(sDcl).getUnifiedSyntax() == sDcl);
	}

	@Test
	public void test10() {
		final Syntax s1 = Syntax.read("S[x]/S[x]");
		final Syntax s2 = Syntax.read("S[x]/S[x]");
		final Unification unify1 = s1.unify(s2);
		Assert.assertEquals(s2, unify1.getUnifiedSyntax());
		Assert.assertFalse(unify1.isVariableAssigned());
		final Unification unify2 = s2.unify(s1);
		Assert.assertEquals(s2, unify2.getUnifiedSyntax());
		Assert.assertFalse(unify2.isVariableAssigned());
	}

	@Test
	public void test11() {
		final Syntax s1 = Syntax.read("N[sg]\\(N[x]/N[x])");
		final Syntax s2 = Syntax.read("N[sg]\\(N/N)");
		final Unification unify1 = s1.unify(s2);
		Assert.assertNotNull(unify1);
	}

	@Test
	public void test2() {
		final Syntax s = Syntax.read("S[b]");
		final Syntax sDcl = Syntax.read("S[dcl]");
		Assert.assertTrue(s.unify(sDcl) == null);
	}

	@Test
	public void test3() {
		final Syntax s1 = Syntax.read("S[b]/S");
		final Syntax s2 = Syntax.read("S/S");
		Assert.assertTrue(s1.unify(s2).getUnifiedSyntax() == s1);
		Assert.assertEquals(s2.unify(s1).getUnifiedSyntax(), s1);
	}

	@Test
	public void test4() {
		final Syntax s1 = Syntax.read("S[b]/S");
		final Syntax s2 = Syntax.read("S\\S");
		Assert.assertNull(s1.unify(s2));
		Assert.assertNull(s2.unify(s1));
	}

	@Test
	public void test5() {
		final Syntax s1 = Syntax.read("S[x]/S[x]");
		final Syntax s2 = Syntax.read("S[b]/S[b]");
		final Unification unify1 = s1.unify(s2);
		Assert.assertEquals(s2, unify1.getUnifiedSyntax());
		Assert.assertEquals(Syntax.read("S[b]"), Syntax.read("S[x]")
				.setVariable(unify1.getVariableAssignment()));
		final Unification unify2 = s2.unify(s1);
		Assert.assertEquals(s2, unify2.getUnifiedSyntax());
		Assert.assertFalse(unify2.isVariableAssigned());
	}

	@Test
	public void test6() {
		final Syntax s1 = Syntax.read("S[x]/NP");
		final Syntax s2 = Syntax.read("S[dcl]/NP");
		final Unification unify1 = s1.unify(s2);
		Assert.assertEquals(s2, unify1.getUnifiedSyntax());
		Assert.assertEquals(Syntax.read("S[dcl]"), Syntax.read("S[x]")
				.setVariable(unify1.getVariableAssignment()));
	}

	@Test
	public void test7() {
		final Syntax s1 = Syntax.read("S[x]/S[x]/NP");
		final Syntax s2 = Syntax.read("S[x]/S[x]/NP");
		final Unification unify1 = s1.unify(s2);
		Assert.assertEquals(s2, unify1.getUnifiedSyntax());
		Assert.assertFalse(unify1.isVariableAssigned());
	}

	@Test
	public void test8() {
		final Syntax s1 = Syntax.read("S[x]/S[b]/S[x]/S[b]/NP");
		final Syntax s2 = Syntax.read("S[b]/S[x]/S[b]/S[x]/NP");
		final Unification unify1 = s1.unify(s2);
		Assert.assertEquals(Syntax.read("S[b]/S[b]/S[b]/S[b]/NP"),
				unify1.getUnifiedSyntax());
		Assert.assertEquals(Syntax.read("S[b]"), Syntax.read("S[x]")
				.setVariable(unify1.getVariableAssignment()));
	}

	@Test
	public void test9() {
		final Syntax s1 = Syntax.read("S[x]/S[c]/S[x]/S[b]/NP");
		final Syntax s2 = Syntax.read("S[b]/S[x]/S[b]/S[x]/NP");
		final Unification unify1 = s1.unify(s2);
		Assert.assertNull(unify1);
	}

}
