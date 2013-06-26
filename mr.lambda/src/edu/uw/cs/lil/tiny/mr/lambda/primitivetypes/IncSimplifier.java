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
package edu.uw.cs.lil.tiny.mr.lambda.primitivetypes;

import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

public class IncSimplifier implements IPredicateSimplifier {
	static public final IncSimplifier	INSTANCE	= new IncSimplifier();
	
	private IncSimplifier() {
	}
	
	@Override
	public LogicalExpression simplify(LogicalExpression exp) {
		if (exp instanceof Literal) {
			final Literal literal = (Literal) exp;
			if (literal.getArguments().size() == 1
					&& literal.getArguments().get(0) instanceof LogicalConstant
					&& literal.getArguments().get(0).getType() == LogicLanguageServices.getTypeRepository()
							.getIndexType()) {
				// If we have a single argument and it's a constant of type
				// index, replace it with a new constant
				final int i = LogicLanguageServices
						.indexConstantToInt((LogicalConstant) literal
								.getArguments().get(0));
				return LogicLanguageServices.intToIndexConstant(i + 1);
			}
		}
		return exp;
	}
	
}
