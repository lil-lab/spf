package edu.uw.cs.lil.tiny.data.singlesentence.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentenceDataset;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.printers.LogicalExpressionToLatexString;
import edu.uw.cs.lil.tiny.mr.lambda.printers.LogicalExpressionToLatexString.Printer;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.log.Log;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;

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
								new FlexibleTypeComparator()).closeOntology(
								false).build());
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			
			// //////////////////////////////////////////
			// Init latex printer.
			// //////////////////////////////////////////
			
			final LogicalExpressionToLatexString.Printer.Builder builder = new LogicalExpressionToLatexString.Printer.Builder();
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(new File(args[2])));
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] split = line.split("\\s", 2);
				builder.addMapping(LogicalConstant.read(split[0]), split[1]);
			}
			reader.close();
			final Printer printer = builder.build();
			
			// //////////////////////////////////////////
			// Print output.
			// //////////////////////////////////////////
			
			// Read input data.
			final SingleSentenceDataset data = SingleSentenceDataset.read(
					new File(args[0]), new StubStringFilter());
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
