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
package edu.cornell.cs.nlp.spf.utils.main;

import edu.cornell.cs.nlp.spf.geoquery.GeoMain;
import edu.cornell.cs.nlp.spf.utils.parse.ParseUtil;

/**
 * Main entry point to the jar.
 *
 * @author Yoav Artzi
 */
public class Main {

	private Main() {
		// Service class. No ctor.
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
		} else if ("parse".equals(args[0])) {
			final String[] parseArgs = new String[args.length - 1];
			System.arraycopy(args, 1, parseArgs, 0, parseArgs.length);
			ParseUtil.main(parseArgs);
		} else if (args.length == 1) {
			GeoMain.main(args);
		}
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println("... parse <exp_file> <data_1> <data_2> ... ");
		System.out
				.println("\tParse util. exp_file contains defintions and parser setup.");
		System.out.println("... <geoquery_exp_file>");
		System.out.println("\tGeoQuery example experiment.");
	}

}
