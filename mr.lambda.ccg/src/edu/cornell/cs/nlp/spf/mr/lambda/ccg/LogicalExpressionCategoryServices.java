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
package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.AbstractCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.SimpleCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsTypeConsistent;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

public class LogicalExpressionCategoryServices extends
		AbstractCategoryServices<LogicalExpression> {
	public static final ILogger					LOG					= LoggerFactory
																			.create(LogicalExpressionCategoryServices.class);

	private static final long					serialVersionUID	= -3386234351724055685L;

	private final boolean						doTypeChecking;
	private final Category<LogicalExpression>	EMP					= new SimpleCategory<LogicalExpression>(
																			Syntax.EMPTY,
																			null);

	public LogicalExpressionCategoryServices() {
		this(false);
	}

	public LogicalExpressionCategoryServices(boolean doTypeChecking) {
		this.doTypeChecking = doTypeChecking;
		LOG.info("Init :: %s: doTypeChecking=%s",
				LogicalExpressionCategoryServices.class.getSimpleName(),
				doTypeChecking);
	}

	@Override
	public LogicalExpression apply(LogicalExpression function,
			LogicalExpression argument) {
		final LogicalExpression result;

		// Combined application and simplification
		final LogicalExpression applicationResult = ApplyAndSimplify.of(
				function, argument);
		// Verify application result is well typed, only if verification is
		// turned on
		if (applicationResult != null && doTypeChecking
				&& !IsTypeConsistent.of(applicationResult)) {
			result = null;
		} else {
			result = applicationResult;
		}

		return result;
	}

	/**
	 * Generalized function composition given a specific order.
	 */
	@Override
	public LogicalExpression compose(LogicalExpression f, LogicalExpression g,
			int order) {
		assert order != 0 : "Order must be at least 1. Zero-order composition is identical to application and should use the apply() method.";

		final Variable[] gVariableStack = new Variable[order - 1];
		LogicalExpression currentG = g;
		for (int i = 0; i < order - 1; ++i) {
			if (currentG instanceof Lambda) {
				gVariableStack[i] = ((Lambda) currentG).getArgument();
				currentG = ((Lambda) currentG).getBody();
			} else {
				return null;
			}
		}

		// Quick type checking.
		if (!(f.getType().isComplex() && currentG.getType().isComplex())) {
			return null;
		}

		// Function composition.
		final ComplexType fType = (ComplexType) f.getType();
		final ComplexType gType = (ComplexType) currentG.getType();

		// Validate the types of the composed expressions.
		if (!LogicLanguageServices.getTypeComparator().verifyArgType(
				fType.getDomain(), gType.getRange())) {
			return null;
		}

		// Make a new variable x. Generalization is required in the case g is
		// not a lambda expression, so its type was not generalized.
		final Variable x = new Variable(LogicLanguageServices
				.getTypeRepository().generalizeType(gType.getDomain()));

		final LogicalExpression gBodyWithNewVar = ApplyAndSimplify.of(currentG,
				x);
		if (gBodyWithNewVar != null) {
			final LogicalExpression newbody = ApplyAndSimplify.of(f,
					gBodyWithNewVar);
			if (newbody != null) {
				final LogicalExpression newComposedExp = new Lambda(x, newbody);
				// Do type checking, if verification is turned on
				if (doTypeChecking && !IsTypeConsistent.of(newComposedExp)) {
					return null;
				} else {
					// If gBodyWithNewVar is a variable (such as will happen
					// when g is the identity function), it is possible that we
					// need to simplify, since the simplify code can fold and
					// drop Lambda operators under certain conditions. The same
					// is true for the cases where newbody is identical to
					// gBodyWithNewVar (such as the case when f is the identity
					// function).
					// See AbstractSimplify.visit(Lambda).

					final LogicalExpression result = gBodyWithNewVar instanceof Variable
							|| gBodyWithNewVar == newbody ? Simplify
							.of(newComposedExp) : newComposedExp;

					// Wrap the result with all the variables previously
					// stripped from G.
					LogicalExpression wrappedResult = result;
					for (int i = order - 2; i >= 0; --i) {
						wrappedResult = new Lambda(gVariableStack[i],
								wrappedResult);
					}

					return wrappedResult;
				}
			}
		}

		// Case composition failed
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final LogicalExpressionCategoryServices other = (LogicalExpressionCategoryServices) obj;
		if (doTypeChecking != other.doTypeChecking) {
			return false;
		}
		return true;
	}

	@Override
	public Category<LogicalExpression> getEmptyCategory() {
		return EMP;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (doTypeChecking ? 1231 : 1237);
		return result;
	}

	@Override
	public LogicalExpression readSemantics(String string, boolean checkType) {
		final LogicalExpression exp = LogicalExpression.read(string);
		if (checkType) {

			final Pair<Boolean, String> typeChecking = IsTypeConsistent
					.ofVerbose(exp);
			if (!typeChecking.first()) {
				throw new IllegalStateException("Semantics not well typed ["
						+ typeChecking.second() + "]: " + string);
			}
		}
		return Simplify.of(exp);
	}

	@Override
	public String toString() {
		return LogicalExpressionCategoryServices.class.getName();
	}
}
