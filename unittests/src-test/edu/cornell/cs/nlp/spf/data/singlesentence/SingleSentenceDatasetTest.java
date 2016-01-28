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
package edu.cornell.cs.nlp.spf.data.singlesentence;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentenceCollection;

public class SingleSentenceDatasetTest {

	public SingleSentenceDatasetTest() {
		TestServices.init();
	}

	@Test
	public void test() {
		final SingleSentenceCollection dataset = SingleSentenceCollection
				.read(new File("resources-test/geo.lam"));
		Assert.assertEquals(60, dataset.size());
	}

	@Test
	public void test2() {
		final SingleSentenceCollection dataset = SingleSentenceCollection
				.read(new File("resources-test/indent.with.props.lam"));
		Assert.assertEquals(5, dataset.size());
	}

}
