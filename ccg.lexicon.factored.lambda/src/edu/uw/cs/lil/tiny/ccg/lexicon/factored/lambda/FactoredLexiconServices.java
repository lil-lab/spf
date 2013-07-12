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
package edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda;

import java.util.HashSet;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;

public class FactoredLexiconServices {
	private static FactoredLexiconServices	INSTANCE			= new FactoredLexiconServices();
	
	private final Set<LogicalConstant>		unfactoredConstants	= new HashSet<LogicalConstant>();
	
	private FactoredLexiconServices() {
	}
	
	public static boolean isFactorable(LogicalConstant constant) {
		return INSTANCE.doIsFactorable(constant);
	}
	
	public static void set(Set<LogicalConstant> unfactoredConstants) {
		INSTANCE = new FactoredLexiconServices();
		INSTANCE.addUnfactoredConstants(unfactoredConstants);
	}
	
	private void addUnfactoredConstants(Set<LogicalConstant> constants) {
		unfactoredConstants.addAll(constants);
	}
	
	private boolean doIsFactorable(LogicalConstant constant) {
		return !LogicLanguageServices.isCoordinationPredicate(constant)
				&& !LogicLanguageServices.isArrayIndexPredicate(constant)
				&& !LogicLanguageServices.isArraySubPredicate(constant)
				&& !LogicLanguageServices.getTypeRepository().getIndexType()
						.equals(constant.getType())
				&& !unfactoredConstants.contains(constant);
	}
	
}
