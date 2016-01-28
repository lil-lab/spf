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
package edu.cornell.cs.nlp.spf.base.hashvector;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.base.hashvector.TroveHashVector;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class TroveHashVectorTest {
	
	@Test
	public void test() {
		final TroveHashVector vector = new TroveHashVector();
		
		vector.set("p1", 1.0);
		vector.set("p1", "p2", "p3", "p4", 2.0);
		
		assertTrue(vector.get("p1") == 1.0);
		assertTrue(vector.get("p1", "p2", "p3", "p4") == 2.0);
		assertTrue(vector.size() == 2);
		
		vector.set("p2", 3.0);
		
		final IHashVector p1 = vector.getAll("p1");
		assertTrue(p1.get("p1") == 1.0);
		assertTrue(p1.get("p1", "p2", "p3", "p4") == 2.0);
		assertTrue(p1.size() == 2);
		
		vector.set("p3", -2.5);
		Assert.assertEquals(2.5 + 3.0 + 1.0 + 2.0, vector.l1Norm(), 0.0);
		final TroveHashVector pairwise = vector.pairWiseProduct(vector);
		for (final Pair<KeyArgs, Double> entry : pairwise) {
			Assert.assertEquals(entry.second(),
					Math.pow(vector.get(entry.first()), 2), 0.0);
		}
		
	}
	
	@Test
	public void test2() {
		final TroveHashVector vector = new TroveHashVector();
		final int len = 50;
		for (int a1 = 0; a1 < len; ++a1) {
			for (int a2 = 0; a2 < len / 2; ++a2) {
				for (int a3 = 0; a3 < len / 4; ++a3) {
					for (int a4 = 0; a4 < len / 8; ++a4) {
						vector.set(String.valueOf(a1), String.valueOf(a2),
								String.valueOf(a3), String.valueOf(a4), a1 + a2
										+ a3 + a4);
					}
				}
			}
		}
		
		for (int a1 = 0; a1 < len; ++a1) {
			for (int a2 = 0; a2 < len / 2; ++a2) {
				for (int a3 = 0; a3 < len / 4; ++a3) {
					for (int a4 = 0; a4 < len / 8; ++a4) {
						assertTrue(vector.get(String.valueOf(a1),
								String.valueOf(a2), String.valueOf(a3),
								String.valueOf(a4)) == a1 + a2 + a3 + a4);
					}
				}
			}
		}
		
		assertTrue(vector.size() == len * (len / 2) * (len / 4) * (len / 8));
		
		for (int a1 = 0; a1 < len; ++a1) {
			final IHashVector a1v = vector.getAll(String.valueOf(a1));
			for (int a2 = 0; a2 < len / 2; ++a2) {
				final IHashVector a2v = a1v.getAll(String.valueOf(a1),
						String.valueOf(a2));
				for (int a3 = 0; a3 < len / 4; ++a3) {
					final IHashVector a3v = a2v.getAll(String.valueOf(a1),
							String.valueOf(a2), String.valueOf(a3));
					for (int a4 = 0; a4 < len / 8; ++a4) {
						final IHashVector a4v = a3v.getAll(String.valueOf(a1),
								String.valueOf(a2), String.valueOf(a3),
								String.valueOf(a4));
						assertTrue(a4v.get(String.valueOf(a1),
								String.valueOf(a2), String.valueOf(a3),
								String.valueOf(a4)) == a1 + a2 + a3 + a4);
					}
				}
			}
		}
	}
	
	@Test
	public void test3() {
		final List<Thread> threads = new LinkedList<Thread>();
		for (int i = 0; i < 10; ++i) {
			threads.add(new Thread(new Test3Task()));
		}
		for (final Thread t : threads) {
			t.start();
		}
		
		for (final Thread t : threads) {
			try {
				t.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testSerialization() {
		final TroveHashVector vector = new TroveHashVector();
		final Random random = new Random();
		for (int i = 0; i < 1000; ++i) {
			switch (random.nextInt() % 5) {
				case 0:
					vector.set(Integer.toString(random.nextInt()),
							random.nextDouble());
					break;
				case 1:
					vector.set(Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							random.nextDouble());
					break;
				case 2:
					vector.set(Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							random.nextDouble());
					break;
				case 3:
					vector.set(Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							random.nextDouble());
					break;
				case 4:
					vector.set(Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							Integer.toString(random.nextInt()),
							random.nextDouble());
					break;
			}
		}
		
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			new ObjectOutputStream(out).writeObject(vector);
			final TroveHashVector object = (TroveHashVector) new ObjectInputStream(
					new ByteArrayInputStream(out.toByteArray())).readObject();
			Assert.assertEquals(vector, object);
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
	private static class Test3Task implements Runnable {
		
		@Override
		public void run() {
			final TroveHashVector vector = new TroveHashVector();
			final int len = 50;
			for (int a1 = 0; a1 < len; ++a1) {
				for (int a2 = 0; a2 < len / 2; ++a2) {
					for (int a3 = 0; a3 < len / 4; ++a3) {
						for (int a4 = 0; a4 < len / 8; ++a4) {
							vector.set("j" + String.valueOf(a1),
									"j" + String.valueOf(a2),
									"j" + String.valueOf(a3),
									"j" + String.valueOf(a4), a1 + a2 + a3 + a4);
						}
					}
				}
			}
			
			for (int a1 = 0; a1 < len; ++a1) {
				final IHashVector a1v = vector.getAll("j" + String.valueOf(a1));
				for (int a2 = 0; a2 < len / 2; ++a2) {
					final IHashVector a2v = a1v.getAll(
							"j" + String.valueOf(a1), "j" + String.valueOf(a2));
					for (int a3 = 0; a3 < len / 4; ++a3) {
						final IHashVector a3v = a2v.getAll(
								"j" + String.valueOf(a1),
								"j" + String.valueOf(a2),
								"j" + String.valueOf(a3));
						for (int a4 = 0; a4 < len / 8; ++a4) {
							final IHashVector a4v = a3v.getAll(
									"j" + String.valueOf(a1),
									"j" + String.valueOf(a2),
									"j" + String.valueOf(a3),
									"j" + String.valueOf(a4));
							assertTrue(a4v.get("j" + String.valueOf(a1), "j"
									+ String.valueOf(a2),
									"j" + String.valueOf(a3),
									"j" + String.valueOf(a4)) == a1 + a2 + a3
									+ a4);
						}
					}
				}
			}
			
		}
		
	}
	
}
