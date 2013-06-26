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
package edu.uw.cs.lil.tiny.paser.ccg.rules.lambda.typeshifting.basic;

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;

/**
 * N : f:<e,t> => NP (a:<<e,t>,e> f)
 * 
 * @author Luke Zettlemoyer
 */
public class PluralTypeShifting extends
		AbstractTypeShiftingFunctionForThreading {
	
	private final LogicalConstant	aPred;
	private final Type				ET;
	
	public PluralTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices) {
		ET = LogicLanguageServices.getTypeRepository().getTypeCreateIfNeeded(
				"<e,t>");
		aPred = (LogicalConstant) categoryServices
				.parseSemantics("a:<<e,t>,e>");
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof PluralTypeShifting;
	}
	
	@Override
	public int hashCode() {
		return PluralTypeShifting.class.getName().hashCode();
	}
	
	@Override
	public Category<LogicalExpression> typeRaise(
			Category<LogicalExpression> category) {
		if (category.getSyntax().equals(Syntax.N)
				&& category.getSem().getType().isExtendingOrExtendedBy(ET)) {
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					1);
			args.add(category.getSem());
			final LogicalExpression aLit = new Literal(aPred, args,
					LogicLanguageServices.getTypeComparator(),
					LogicLanguageServices.getTypeRepository());
			return Category.create(Syntax.NP, aLit);
		}
		return null;
	}
}
