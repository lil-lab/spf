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
package edu.uw.cs.lil.tiny.data.singlesentence.utils;

import java.io.File;
import java.io.IOException;

import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentenceDataset;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetConstantsMultiSet;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.log.Log;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;

public class CreateSentenceConstantsFile {
	
	public static void main(String[] args) {
		
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
							new FlexibleTypeComparator()).closeOntology(false)
							.build());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		// //////////////////////////////////////////
		// Print output file.
		// //////////////////////////////////////////
		
		// Read input data.
		final SingleSentenceDataset data = SingleSentenceDataset.read(new File(
				args[0]), new StubStringFilter());
		
		for (final SingleSentence sentence : data) {
			System.out.println(sentence.getSample());
			System.out.println(ListUtils.join(
					GetConstantsMultiSet.of(sentence.getLabel()), " "));
			System.out.println();
		}
		
	}
	
}
