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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

/**
 * Generic simplifier for literals with recursive predicates that support
 * folding. Maintains order, so can be used for both order-sensitive and
 * order-insensitive predicates.
 *
 * @author Yoav Artzi
 */
public class GenericRecursiveSimplifier implements IPredicateSimplifier {

	static public final GenericRecursiveSimplifier	INSTANCE	= new GenericRecursiveSimplifier();

	private GenericRecursiveSimplifier() {
		// Access through static INSTANCE
	}

	@Override
	public LogicalExpression simplify(LogicalExpression exp) {
		boolean consolidated = false;
		if (exp instanceof Literal) {
			final Literal literal = (Literal) exp;
			final int len = literal.numArgs();
			final LogicalExpression predicate = literal.getPredicate();
			// Consolidate arguments from all sub expressions that have the same
			// predicate.

			// Before allocating and copying any data, quickly iterate to check
			// if we can collapse anything. Also, collect the number of
			// arguments of each collapsed literal.
			int numArgs = 0;
			boolean collapsible = false;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(0);
				if (arg instanceof Literal
						&& ((Literal) arg).getPredicate().equals(predicate)) {
					numArgs += ((Literal) arg).numArgs();
					collapsible = true;
				} else {
					++numArgs;
				}
			}

			if (!collapsible) {
				return exp;
			}

			final LogicalExpression[] consolidatedArgs = new LogicalExpression[numArgs];
			int j = 0;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(0);
				if (arg instanceof Literal
						&& ((Literal) arg).getPredicate().equals(predicate)) {
					((Literal) arg).copyArgsIntoArray(consolidatedArgs, 0, j,
							((Literal) arg).numArgs());
					j += ((Literal) arg).numArgs();
					consolidated = true;
				} else {
					consolidatedArgs[j] = arg;
					++j;
				}
			}

			if (consolidated) {
				return new Literal(predicate, consolidatedArgs);
			} else {
				return exp;
			}
		} else {
			return exp;
		}
	}

}
