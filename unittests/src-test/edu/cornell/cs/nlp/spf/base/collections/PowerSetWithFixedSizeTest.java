package edu.cornell.cs.nlp.spf.base.collections;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.utils.collections.ListUtils;

public class PowerSetWithFixedSizeTest {

	@Test
	public void test0() {
		final List<Integer> list = ListUtils.createList(1, 2, 3, 4, 5);
		final Set<HashSet<Integer>> subsets = new HashSet<>();

		for (final List<Integer> subset : new PowerSetWithFixedSize<>(list,
				0)) {
			subsets.add(new HashSet<>(subset));
		}

		Assert.assertEquals(1, subsets.size());
		Assert.assertTrue(subsets.contains(new HashSet<>()));
	}

	@Test
	public void test1() {
		final List<Integer> list = ListUtils.createList(1, 2, 3, 4, 5);
		final Set<HashSet<Integer>> subsets = new HashSet<>();

		for (final List<Integer> subset : new PowerSetWithFixedSize<>(list,
				1)) {
			subsets.add(new HashSet<>(subset));
		}

		Assert.assertEquals(5, subsets.size());
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(1))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(2))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(3))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(4))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(5))));

	}

	@Test
	public void test2() {
		final List<Integer> list = ListUtils.createList(1, 2, 3, 4, 5);
		final Set<Set<Integer>> subsets = new HashSet<>();

		for (final List<Integer> subset : new PowerSetWithFixedSize<>(list,
				2)) {
			subsets.add(new HashSet<>(subset));
		}

		Assert.assertEquals(10, subsets.size());
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(1, 2))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(1, 3))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(1, 4))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(1, 5))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(2, 3))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(2, 4))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(2, 5))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(3, 4))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(3, 5))));
		Assert.assertTrue(
				subsets.contains(new HashSet<>(ListUtils.createList(4, 5))));
	}

}
