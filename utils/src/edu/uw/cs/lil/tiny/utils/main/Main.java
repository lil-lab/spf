package edu.uw.cs.lil.tiny.utils.main;

import edu.uw.cs.lil.tiny.geoquery.GeoMain;
import edu.uw.cs.lil.tiny.utils.parse.ParseUtil;

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
		}
		if ("parse".equals(args[0])) {
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
