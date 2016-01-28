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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.ToDoubleFunction;

public class HashVectorFactory {
	public static Type	DEFAULT	= Type.TREE;

	private HashVectorFactory() {
		// Service class.
	}

	public static IHashVector create() {
		switch (DEFAULT) {
			case FAST_TREE:
				return createFastTree();
			case TREE:
				return createTree();
			case TROVE:
				return createTrove();
			default:
				throw new IllegalStateException("unhandled type");
		}
	}

	public static IHashVector create(IHashVectorImmutable vector) {
		switch (DEFAULT) {
			case FAST_TREE:
				return createFastTree(vector);
			case TREE:
				return createTree(vector);
			case TROVE:
				return createTrove(vector);
			default:
				throw new IllegalStateException("unhandled type");
		}
	}

	public static IHashVector createFastTree() {
		return new FastTreeHashVector();
	}

	public static IHashVector createFastTree(IHashVectorImmutable vector) {
		return new FastTreeHashVector(vector);
	}

	public static IHashVector createTree() {
		return new TreeHashVector();
	}

	public static IHashVector createTree(IHashVectorImmutable vector) {
		return new TreeHashVector(vector);
	}

	public static IHashVector createTreeWithHashInit(double scalingFactor) {
		return new TreeHashVectorWithInit(
				new TreeHashVectorWithInit.HashInitFunction(scalingFactor));
	}

	public static IHashVector createTreeWithInit(IHashVectorImmutable vector,
			ToDoubleFunction<KeyArgs> initFunction) {
		return new TreeHashVectorWithInit(vector, initFunction);
	}

	public static IHashVector createTreeWithInit(
			ToDoubleFunction<KeyArgs> initFunction) {
		return new TreeHashVectorWithInit(initFunction);
	}

	public static IHashVector createTrove() {
		return new TroveHashVector();
	}

	public static IHashVector createTrove(IHashVectorImmutable vector) {
		return new TroveHashVector(vector);
	}

	public static IHashVectorImmutable empty() {
		switch (DEFAULT) {
			case FAST_TREE:
				return FastTreeHashVector.EMPTY;
			case TREE:
				return TreeHashVector.EMPTY;
			case TROVE:
				return TroveHashVector.EMPTY;
			default:
				throw new IllegalStateException("unhandled type");
		}
	}

	public static IHashVector read(File file) throws IOException {
		try (final BufferedReader reader = new BufferedReader(new FileReader(
				file))) {
			final IHashVector vector = create();
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("//")) {
					final int splitIndex = line.lastIndexOf("=");
					vector.set(KeyArgs.read(line.substring(0, splitIndex)),
							Double.valueOf(line.substring(splitIndex + 1)));
				}
			}
			return vector;
		}
	}

	public static enum Type {
		// Only general-purpose hash vectors are enumerated here. For example,
		// vectors with special initialization are not, since they are
		// specifically designed to store parameters.
		FAST_TREE, TREE, TROVE;
	}

}
