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
package edu.cornell.cs.nlp.spf.parser.ccg.factoredlex;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class FactoredLexiconTest {
	
	@Ignore
	@Test
	public void test() {
		final LexicalEntry<LogicalExpression> e1 = LexicalEntry
				.parse("turn :- S/NP : (lambda $0:e (lambda $1:e (and:<t*,t> (turn:<e,t> $1) (dir:<e,<e,t>> $1 $0))))",
						TestServices.getCategoryServices(),
						LexicalEntry.Origin.FIXED_DOMAIN);
		final LexicalEntry<LogicalExpression> e2 = LexicalEntry
				.parse("walk :- S/NP : (lambda $0:e (lambda $1:e (and:<t*,t> (move:<e,t> $1) (dir:<e,<e,t>> $1 $0))))",
						TestServices.getCategoryServices(),
						LexicalEntry.Origin.FIXED_DOMAIN);
		System.out.println(e1);
		System.out.println(e2);
		final LexicalTemplate t1 = FactoringServices.factor(e1).getTemplate();
		System.out.println(t1);
		final LexicalTemplate t2 = FactoringServices.factor(e2).getTemplate();
		System.out.println(t2);
		Assert.assertEquals(t1, t2);
	}
	
}
