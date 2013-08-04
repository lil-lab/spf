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
package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.basic;

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.LambdaWrapped;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting.ITypeShiftingFunction;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * Abstract class for type shifting rules of the type PP -> N\N.
 * 
 * @author Yoav Artzi
 */
public abstract class AbstractTypeShiftingFunctionForThreading implements
		ITypeShiftingFunction<LogicalExpression> {
	
	/**
	 * (lambda $0:x (g $0)) ==> (lambda $0:<x,t> (lambda $1:x (and:<t*,t> ($0
	 * $1) (g $1))))
	 * 
	 * @param sem
	 * @return
	 */
	protected LogicalExpression typeRaiseSemantics(LogicalExpression sem) {
		final Type semType = sem.getType();
		final Type range = semType.getRange();
		
		if (semType.isComplex()
				&& range.equals(LogicLanguageServices.getTypeRepository()
						.getTruthValueType())) {
			
			// Make sure the expression is wrapped with lambda operators, since
			// the variables are required
			final Lambda lambda = (Lambda) LambdaWrapped.of(sem);
			
			// Variable for the new outer lambda
			final Variable outerVariable = new Variable(LogicLanguageServices
					.getTypeRepository().getTypeCreateIfNeeded(
							LogicLanguageServices.getTypeRepository()
									.getTruthValueType(),
							lambda.getArgument().getType()));
			
			// Create the literal applying the function to the original
			// argument
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					1);
			args.add(lambda.getArgument());
			final Literal newLiteral = new Literal(outerVariable, args);
			
			// Create the conjunction of newLitral and the original body
			final Literal conjunction = new Literal(
					LogicLanguageServices.getConjunctionPredicate(),
					ListUtils.createList(newLiteral, lambda.getBody()));
			
			// The new inner lambda
			final Lambda innerLambda = new Lambda(lambda.getArgument(),
					conjunction);
			
			// The new outer lambda
			final Lambda outerLambda = new Lambda(outerVariable, innerLambda);
			
			// Simplify the output and return it
			final LogicalExpression ret = Simplify.of(outerLambda);
			
			return ret;
		}
		
		return null;
	}
}
