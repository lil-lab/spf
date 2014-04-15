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
package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IUnaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.lil.tiny.parser.ccg.rules.UnaryRuleName;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * Abstract class for type shifting rules of the type PP -> N\N.
 * 
 * @author Yoav Artzi
 */
public abstract class AbstractUnaryRuleForThreading implements
		IUnaryParseRule<LogicalExpression> {
	private final Syntax			argumentSyntax;
	protected final UnaryRuleName	name;
	
	public AbstractUnaryRuleForThreading(String name, Syntax argumentSyntax) {
		this.argumentSyntax = argumentSyntax;
		this.name = UnaryRuleName.create(name);
	}
	
	@Override
	public Collection<ParseRuleResult<LogicalExpression>> apply(
			Category<LogicalExpression> category) {
		if (category.getSyntax().equals(getSourceSyntax())) {
			final LogicalExpression raisedSemantics = typeShiftSemantics(category
					.getSem());
			if (raisedSemantics != null) {
				return ListUtils
						.createSingletonList(new ParseRuleResult<LogicalExpression>(
								name, Category.create(getTargetSyntax(),
										raisedSemantics)));
			}
		}
		
		return Collections.emptyList();
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
		final AbstractUnaryRuleForThreading other = (AbstractUnaryRuleForThreading) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
	
	@Override
	public RuleName getName() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public boolean isValidArgument(Category<LogicalExpression> category) {
		return argumentSyntax.equals(category.getSyntax());
	}
	
	@Override
	public String toString() {
		return name.toString();
	}
	
	protected abstract Syntax getSourceSyntax();
	
	protected abstract Syntax getTargetSyntax();
	
	/**
	 * (lambda $0:x (g $0)) ==> (lambda $0:<x,t> (lambda $1:x (and:<t*,t> ($0
	 * $1) (g $1))))
	 * 
	 * @param sem
	 * @return
	 */
	protected LogicalExpression typeShiftSemantics(LogicalExpression sem) {
		final Type semType = sem.getType();
		final Type range = semType.getRange();
		
		if (semType.isComplex()
				&& range.equals(LogicLanguageServices.getTypeRepository()
						.getTruthValueType())) {
			
			// Make sure the expression is wrapped with lambda operators, since
			// the variables are required
			final Lambda lambda = (Lambda) sem;
			
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
