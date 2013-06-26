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
package edu.uw.cs.lil.tiny.mr.lambda.ccg;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Ontology;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;

public class LogicalExpressionTestServices {
	private static final ICategoryServices<LogicalExpression>	CATEGORY_SERVICES;
	
	private static final List<File>								DEFAULT_ONTOLOGY_FILES;
	private static final File									DEFAULT_TYPES_FILE;
	
	private LogicalExpressionTestServices() {
	}
	
	static {
		DEFAULT_TYPES_FILE = new File("resources-test/geo-lambda.types");
		DEFAULT_ONTOLOGY_FILES = new LinkedList<File>();
		DEFAULT_ONTOLOGY_FILES.add(new File(
				"resources-test/geo-lambda.consts.ont"));
		DEFAULT_ONTOLOGY_FILES.add(new File(
				"resources-test/geo-lambda.preds.ont"));
		
		// //////////////////////////////////////////
		// Init typing system
		// //////////////////////////////////////////
		
		// Init the logical expression type system
		LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
				new TypeRepository(DEFAULT_TYPES_FILE)).setNumeralTypeName("n")
				.setTypeComparator(new FlexibleTypeComparator()).build());
		
		// //////////////////////////////////////////////////
		// Category services for logical expressions
		// //////////////////////////////////////////////////
		
		// CCG LogicalExpression category services for handling categories
		// with LogicalExpression as semantics
		CATEGORY_SERVICES = new LogicalExpressionCategoryServices(true, false,
				true);
		
		// //////////////////////////////////////////////////
		// Read ontology (loads all constants)
		// //////////////////////////////////////////////////
		
		try {
			// Ontology is currently not used, so we are just reading it, not
			// storing
			new Ontology(DEFAULT_ONTOLOGY_FILES);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public static ICategoryServices<LogicalExpression> getCategoryServices() {
		return CATEGORY_SERVICES;
	}
	
}
