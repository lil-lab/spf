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

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

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
		boolean expChanged = false;
		if (exp instanceof Literal) {
			final Literal literal = (Literal) exp;
			// Consolidate arguments from all sub expressions that have the same
			// predicate
			final List<LogicalExpression> consolidatedArgs = new LinkedList<LogicalExpression>();
			for (final LogicalExpression arg : literal.getArguments()) {
				if (arg instanceof Literal
						&& ((Literal) arg).getPredicateType() == literal
								.getPredicateType()) {
					consolidatedArgs.addAll(((Literal) arg).getArguments());
					expChanged = true;
				} else {
					consolidatedArgs.add(arg);
				}
			}
			
			if (expChanged) {
				return new Literal(literal.getPredicate(), consolidatedArgs);
			} else {
				return exp;
			}
		} else {
			return exp;
		}
	}
	
}
