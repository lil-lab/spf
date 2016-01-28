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

/**
 * Simplifier for coordination predicates
 * {@link LogicLanguageServices#isCoordinationPredicate(LogicalExpression)}
 * (i.e., and:<t*,t> and or:<t*,t>).
 *
 * @author Yoav Artzi
 */
public class AbstractCoordinationSimplifier implements IPredicateSimplifier {
	private final LogicalConstant	dominatingValue;
	private final LogicalConstant	insignificantValue;

	protected AbstractCoordinationSimplifier(LogicalConstant dominatingValue,
			LogicalConstant insignificantValue) {
		this.dominatingValue = dominatingValue;
		this.insignificantValue = insignificantValue;
	}

	@Override
	public LogicalExpression simplify(LogicalExpression exp) {
		if (exp instanceof Literal) {
			final Literal literal = (Literal) exp;
			final LogicalExpression predicate = literal.getPredicate();
			final int len = literal.numArgs();

			// Before allocating and copying any data, quickly iterate to check
			// if we can collapse anything. Also, collect the number of
			// arguments of each collapsed literal. If we find a dominating
			// value, just return it.
			int numArgs = 0;
			boolean collapsible = false;
			boolean nonDeterminedArgExists = false;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(i);
				if (arg instanceof Literal
						&& ((Literal) arg).getPredicate().equals(predicate)) {
					collapsible = true;
					final Literal argLiteral = (Literal) arg;
					final int argLiteralLen = argLiteral.numArgs();
					for (int j = 0; j < argLiteralLen; ++j) {
						final LogicalExpression argArg = argLiteral.getArg(j);
						if (argArg.equals(dominatingValue)) {
							return dominatingValue;
						} else if (!argArg.equals(insignificantValue)) {
							nonDeterminedArgExists = true;
							++numArgs;
						}
					}
				} else {
					if (arg.equals(insignificantValue)) {
						collapsible = true;
					} else if (arg.equals(dominatingValue)) {
						return dominatingValue;
					} else {
						nonDeterminedArgExists = true;
						++numArgs;
					}
				}
			}

			if (!nonDeterminedArgExists) {
				// Case no dominating value was found and no non determined
				// argument found, just return the default insignificant value
				// for this predicate.
				return insignificantValue;
			}

			// Collapse the arguments into the literal, if possible, and remove
			// all arguments that equal to the insignificant value. If we only
			// have one argument left, return it.
			if (collapsible) {
				final LogicalExpression[] consolidatedArgs = new LogicalExpression[numArgs];
				int j = 0;
				for (int i = 0; i < len; ++i) {
					final LogicalExpression arg = literal.getArg(i);
					if (arg instanceof Literal
							&& ((Literal) arg).getPredicate().equals(predicate)) {
						for (int k = 0; k < ((Literal) arg).numArgs(); ++k) {
							final LogicalExpression literalArg = ((Literal) arg)
									.getArg(k);
							if (!literalArg.equals(insignificantValue)) {
								if (numArgs == 1) {
									return literalArg;
								}
								consolidatedArgs[j++] = literalArg;
							}
						}
					} else if (!arg.equals(insignificantValue)) {
						if (numArgs == 1) {
							return arg;
						}
						consolidatedArgs[j++] = arg;
					}
				}
				return new Literal(predicate, consolidatedArgs);
			}
		}
		return exp;
	}

}
