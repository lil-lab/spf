/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
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
 ******************************************************************************/
package edu.uw.cs.lil.tiny.base.hashvector;

public class HashVectorFactory {
	public static Type	DEFAULT	= Type.TREE;
	
	public static IHashVector create() {
		switch (DEFAULT) {
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
			case TREE:
				return createTree(vector);
			case TROVE:
				return createTrove(vector);
			default:
				throw new IllegalStateException("unhandled type");
		}
	}
	
	public static IHashVector createTree() {
		return new TreeHashVector();
	}
	
	public static IHashVector createTree(IHashVectorImmutable vector) {
		return new TreeHashVector(vector);
	}
	
	public static IHashVector createTrove() {
		return new TroveHashVector();
	}
	
	public static IHashVector createTrove(IHashVectorImmutable vector) {
		return new TroveHashVector(vector);
	}
	
	public static enum Type {
		TREE, TROVE;
	}
	
}
