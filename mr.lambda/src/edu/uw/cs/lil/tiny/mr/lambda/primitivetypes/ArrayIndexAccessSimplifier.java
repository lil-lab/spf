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

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;

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
			final List<? extends LogicalExpression> args = literal
					.getArguments();
			
			if (args.size() == 2 && args.get(0) instanceof Literal) {
				final Literal arg1 = (Literal) args.get(0);
				final List<? extends LogicalExpression> arg1Arguments = arg1
						.getArguments();
				if (LogicLanguageServices.isArraySubPredicate(arg1
						.getPredicate()) && arg1Arguments.size() == 2) {
					// Case sub predicate with two arguments (a complete sub
					// predicate that we can simplify). We are going to shift
					// the ind argument of the index predicate to reflect the
					// shift in the array done by the sub predicate.
					final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
							2);
					newArgs.add(arg1Arguments.get(0));
					LogicalExpression newArg2 = args.get(1);
					for (int i = 0; i < LogicLanguageServices
							.indexConstantToInt((LogicalConstant) arg1Arguments
									.get(1)); ++i) {
						final List<LogicalExpression> incArgument = new ArrayList<LogicalExpression>(
								1);
						incArgument.add(newArg2);
						newArg2 = new Literal(
								LogicLanguageServices
										.getIndexIncreasePredicate(),
								incArgument);
					}
					newArgs.add(newArg2);
					return Simplify.of(new Literal(literal.getPredicate(),
							newArgs));
				}
			}
		}
		return exp;
	}
}
