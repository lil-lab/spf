package edu.uw.cs.lil.tiny.data.singlesentence;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import edu.uw.cs.lil.tiny.TestServices;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;

public class SingleSentenceDatasetTest {
	
	public SingleSentenceDatasetTest() {
		new TestServices();
	}
	
	@Test
	public void test() {
		final SingleSentenceDataset dataset = SingleSentenceDataset.read(
				new File("resources-test/geo.lam"), new StubStringFilter());
		Assert.assertEquals(60, dataset.size());
	}
	
}
