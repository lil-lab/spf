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
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;

/**
 * Generic simplifier for any array index predicate. This simplifier works for
 * any type of array.
 *
 * @author Yoav Artzi
 */
public class ArrayIndexAccessSimplifier implements IPredicateSimplifier {
	public static final ArrayIndexAccessSimplifier	INSTANCE	= new ArrayIndexAccessSimplifier();

	private ArrayIndexAccessSimplifier() {
	}

	@Override
	public LogicalExpression simplify(LogicalExpression exp) {
		if (exp instanceof Literal) {
			final Literal literal = (Literal) exp;
			if (literal.numArgs() == 2 && literal.getArg(0) instanceof Literal) {
				final Literal arg1 = (Literal) literal.getArg(0);
				if (LogicLanguageServices.isArraySubPredicate(arg1
						.getPredicate()) && arg1.numArgs() == 2) {
					// Case sub predicate with two arguments (a complete sub
					// predicate that we can simplify). We are going to shift
					// the ind argument of the index predicate to reflect the
					// shift in the array done by the sub predicate.
					final LogicalExpression[] newArgs = new LogicalExpression[2];
					newArgs[0] = arg1.getArg(0);
					LogicalExpression newArg2 = literal.getArg(1);
					for (int i = 0; i < LogicLanguageServices
							.indexConstantToInt((LogicalConstant) arg1
									.getArg(1)); ++i) {
						final LogicalExpression[] incArgument = new LogicalExpression[1];
						incArgument[0] = newArg2;
						newArg2 = new Literal(
								LogicLanguageServices
										.getIndexIncreasePredicate(),
								incArgument);
					}
					newArgs[1] = newArg2;
					return Simplify.of(new Literal(literal.getPredicate(),
							newArgs));
				}
			}
		}
		return exp;
	}
}
