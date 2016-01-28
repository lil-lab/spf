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
package edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes;

import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class IncSimplifier implements IPredicateSimplifier {
	static public final IncSimplifier	INSTANCE	= new IncSimplifier();

	private IncSimplifier() {
	}

	@Override
	public LogicalExpression simplify(LogicalExpression exp) {
		if (exp instanceof Literal) {
			final Literal literal = (Literal) exp;
			if (literal.numArgs() == 1
					&& literal.getArg(0) instanceof LogicalConstant
					&& literal.getArg(0).getType() == LogicLanguageServices
							.getTypeRepository().getIndexType()) {
				// If we have a single argument and it's a constant of type
				// index, replace it with a new constant
				final int i = LogicLanguageServices
						.indexConstantToInt((LogicalConstant) literal.getArg(0));
				return LogicLanguageServices.intToIndexConstant(i + 1);
			}
		}
		return exp;
	}

}
