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
package edu.uw.cs.lil.tiny;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;

public class TestServices {
	
	public static final LogicalExpressionCategoryServices	CATEGORY_SERVICES;
	
	public static final List<File>							DEFAULT_ONTOLOGY_FILES;
	public static final File								DEFAULT_TYPES_FILE;
	
	public TestServices() {
	}
	
	static {
		DEFAULT_TYPES_FILE = new File("resources-test/geo.types");
		DEFAULT_ONTOLOGY_FILES = new LinkedList<File>();
		DEFAULT_ONTOLOGY_FILES.add(new File("resources-test/geo.consts.ont"));
		DEFAULT_ONTOLOGY_FILES.add(new File("resources-test/geo.preds.ont"));
		
		// //////////////////////////////////////////
		// Init typing system.
		// //////////////////////////////////////////
		
		// Init the logical expression type system
		try {
			LogicLanguageServices
					.setInstance(new LogicLanguageServices.Builder(
							new TypeRepository(DEFAULT_TYPES_FILE),
							new FlexibleTypeComparator()).closeOntology(false)
							.addConstantsToOntology(DEFAULT_ONTOLOGY_FILES)
							.setNumeralTypeName("i").build());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		// //////////////////////////////////////////////////
		// Category services for logical expressions.
		// //////////////////////////////////////////////////
		
		// CCG LogicalExpression category services for handling categories
		// with LogicalExpression as semantics
		CATEGORY_SERVICES = new LogicalExpressionCategoryServices(true, true);
		
	}
	
	public static LogicalExpressionCategoryServices getCategoryServices() {
		return CATEGORY_SERVICES;
	}
}
