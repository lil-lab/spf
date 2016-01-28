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
package edu.cornell.cs.nlp.spf.base.numeral;

import java.io.File;

import normalization.BiuNormalizer;

/**
 * Numeral parser wrapping the BIU Normalizer.
 * 
 * @author Yoav Artzi
 */
public class NumberParser {
	final private BiuNormalizer	biuNormalizer;
	
	public NumberParser(File rulesDirectory) throws Exception {
		biuNormalizer = new BiuNormalizer(rulesDirectory);
	}
	
	public static void main(String[] args) {
		try {
			final NumberParser parser = new NumberParser(new File(
					"resources/biu-normalizer-rules"));
			System.out.println(parser.parse("nineteenth"));
			System.out.println(parser.parse("eighth"));
			System.out.println(parser.parse("twenty eighth"));
			System.out.println(parser.parse("twenty twenty two"));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String parse(String numString) throws Exception {
		return biuNormalizer.normalize(numString);
	}
}
