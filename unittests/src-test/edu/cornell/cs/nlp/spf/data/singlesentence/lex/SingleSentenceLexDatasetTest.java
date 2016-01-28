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
package edu.cornell.cs.nlp.spf.data.singlesentence.lex;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.singlesentence.lex.SingleSentenceLex;
import edu.cornell.cs.nlp.spf.data.singlesentence.lex.SingleSentenceLexDataset;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class SingleSentenceLexDatasetTest {

	public SingleSentenceLexDatasetTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final SingleSentenceLexDataset dataset = SingleSentenceLexDataset.read(
				new File("resources-test/indent.with.props.lamlex"),
				TestServices.getCategoryServices(), "original");
		Assert.assertEquals(5, dataset.size());
		for (final SingleSentenceLex dataItem : dataset) {
			Assert.assertEquals(2, dataItem.getEntries().size());
			for (final LexicalEntry<LogicalExpression> entry : dataItem
					.getEntries()) {
				Assert.assertEquals("original", entry.getOrigin());
			}
		}
	}

}
