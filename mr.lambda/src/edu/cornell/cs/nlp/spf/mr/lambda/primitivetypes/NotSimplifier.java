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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

/**
 * Simplifier for 'not:t' predicate.
 *
 * @author Yoav Artzi
 */
public class NotSimplifier implements IPredicateSimplifier {
	static public final NotSimplifier	INSTANCE	= new NotSimplifier();

	private NotSimplifier() {
	}

	@Override
	public LogicalExpression simplify(LogicalExpression exp) {
		if (exp instanceof Literal) {
			// If the argument is a literal with 'not:t' predicate, return the
			// argument for the inner literal
			final Literal literal = (Literal) exp;

			// If we have more than one argument, don't do anything, this
			// expression is one bad apple
			if (literal.numArgs() == 1) {
				if (literal.getArg(0) instanceof Literal
						&& ((Literal) literal.getArg(0)).getPredicate().equals(
								literal.getPredicate())) {
					// Case the only argument is a 'not:t' literal, so return
					// its
					// single argument
					final Literal subNot = (Literal) literal.getArg(0);
					if (subNot.numArgs() == 1) {
						return subNot.getArg(0);
					}
				} else if (literal.getArg(0) == LogicLanguageServices.getTrue()) {
					// If the single argument is 'true:t' constant, return
					// 'false:t'
					return LogicLanguageServices.getFalse();
				} else if (literal.getArg(0) == LogicLanguageServices
						.getFalse()) {
					// If the single argument is 'false:t' constant, return
					// 'true:t'
					return LogicLanguageServices.getTrue();
				}

			}
			// Case didn't change anything
			return exp;
		} else {
			return exp;
		}
	}
}
