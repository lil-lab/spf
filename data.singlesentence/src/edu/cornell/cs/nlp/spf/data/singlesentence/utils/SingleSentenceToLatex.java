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
package edu.cornell.cs.nlp.spf.data.singlesentence.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentenceCollection;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToLatexString;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToLatexString.Printer;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.log.Log;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;

/**
 * Print a file with LATEX logical forms from a data set of sentences paired
 * with logical forms.
 *
 * @author Yoav Artzi
 */
public class SingleSentenceToLatex {

	public static void main(String[] args) {

		try {

			if (args.length == 0) {
				usage();
				return;
			}

			// //////////////////////////////////////////
			// Init logging
			// //////////////////////////////////////////
			Logger.DEFAULT_LOG = new Log(System.err);
			Logger.setSkipPrefix(true);
			LogLevel.setLogLevel(LogLevel.INFO);

			// //////////////////////////////////////////
			// Init lambda calculus system.
			// //////////////////////////////////////////

			try {
				// Init the logical expression type system
				LogicLanguageServices
						.setInstance(new LogicLanguageServices.Builder(
								new TypeRepository(new File(args[1])),
								new FlexibleTypeComparator()).build());
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

			// //////////////////////////////////////////
			// Init skolem terms services.
			// //////////////////////////////////////////

			SkolemServices.setInstance(new SkolemServices.Builder(
					LogicLanguageServices.getTypeRepository().getType("id"),
					LogicalConstant.read("na:id")).build());

			// //////////////////////////////////////////
			// Init latex printer.
			// //////////////////////////////////////////

			final LogicalExpressionToLatexString.Printer.Builder builder = new LogicalExpressionToLatexString.Printer.Builder();
			try (BufferedReader reader = new BufferedReader(new FileReader(
					new File(args[2])))) {
				String line;
				while ((line = reader.readLine()) != null) {
					final String[] split = line.split("\\s", 2);
					builder.addMapping(LogicalConstant.read(split[0]), split[1]);
				}
			}
			final Printer printer = builder.build();

			// //////////////////////////////////////////
			// Print output.
			// //////////////////////////////////////////

			// Read input data.
			final SingleSentenceCollection data = SingleSentenceCollection
					.read(new File(args[0]));
			for (final SingleSentence sentence : data) {
				System.out.println(sentence.getSample());
				System.out.println();
				System.out.println("$" + printer.toString(sentence.getLabel())
						+ "$");
				System.out.println();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static void usage() {
		System.out
				.println(SingleSentenceToLatex.class.getSimpleName()
						+ " input_file types_file latex_mapping_file [properties_flag]");
		System.out.println("\tinput_file");
		System.out.println("\t\tSingle sentence input file.");
		System.out.println("\ttypes_file");
		System.out.println("\t\tLogic system typing file.");
		System.out.println("\tlatex_mapping_file");
		System.out
				.println("\t\tMapping of logical constants to strings. Tab-separated file, each line includes a single mapping. Logical constant on first column, string on the second.");
		System.out.println("\tproperties_flag");
		System.out
				.println("\t\tSet to 'true' if the input file contains property dictionaries for each sample.");
	}
}
