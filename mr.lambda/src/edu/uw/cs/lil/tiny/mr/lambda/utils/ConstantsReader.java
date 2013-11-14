package edu.uw.cs.lil.tiny.mr.lambda.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.utils.LispReader;

public class ConstantsReader {
	private ConstantsReader() {
		// Nothing to do. Private ctor.
	}
	
	public static List<LogicalConstant> readConstantsFile(File file)
			throws IOException {
		// First, strip the comments and prepare a clean LISP string to
		// parse
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		final StringBuilder strippedFile = new StringBuilder();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				line = line.split("\\s*//")[0];
				if (!line.equals("")) {
					strippedFile.append(line).append(" ");
				}
			}
		} finally {
			reader.close();
		}
		
		// Read the constants
		final List<LogicalConstant> constants = new LinkedList<LogicalConstant>();
		final LispReader lispReader = new LispReader(new StringReader(
				strippedFile.toString()));
		while (lispReader.hasNext()) {
			final LogicalConstant exp = LogicalConstant
					.parse(lispReader.next());
			constants.add(exp);
		}
		
		return constants;
		
	}
	
}
