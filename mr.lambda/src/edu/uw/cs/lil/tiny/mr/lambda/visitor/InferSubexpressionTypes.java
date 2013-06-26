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
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;

/**
 * Infers types for all of the subexpressions. The inferred types are the most
 * specific type that could hold for each expression (variable, etc). Note: the
 * types can be null if there is no valid type for a subexpression.
 * 
 * @author Luke Zettlemoyer
 */
public class InferSubexpressionTypes implements ILogicalExpressionVisitor {
	/**
	 * Usually we don't see many subexpressions, so initializing this map to be
	 * relatively small.
	 */
	private final Map<LogicalExpression, Type>	expressionTypes	= new HashMap<LogicalExpression, Type>(
																		10);
	private final TypeRepository				typeRepository;
	
	private InferSubexpressionTypes(TypeRepository typeRepository) {
		// Usage only through static 'of' method.
		this.typeRepository = typeRepository;
	}
	
	public static Map<LogicalExpression, Type> of(LogicalExpression exp,
			TypeRepository typeRepository) {
		final InferSubexpressionTypes visitor = new InferSubexpressionTypes(
				typeRepository);
		visitor.visit(exp);
		return visitor.getTypes();
	}
	
	public Map<LogicalExpression, Type> getTypes() {
		return expressionTypes;
	}
	
	@Override
	public void visit(Lambda lambda) {
		expressionTypes.put(lambda.getArgument(), lambda.getArgument()
				.getType());
		lambda.getBody().accept(this);
		
		final Type bodyType = expressionTypes.get(lambda.getBody());
		final Type argType = expressionTypes.get(lambda.getArgument());
		if (bodyType == null || argType == null) {
			expressionTypes.put(lambda, null);
		} else {
			final Type newType = typeRepository.getTypeCreateIfNeeded(bodyType,
					argType);
			expressionTypes.put(lambda, newType);
		}
	}
	
	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		
		// Check the arguments match the type of the function
		final Set<Variable> firstArgVars = new HashSet<Variable>();
		
		// Case we have a composite list of arguments
		Type currentDomain = literal.getPredicateType().getDomain();
		Type currentRange = literal.getPredicateType().getRange();
		for (final LogicalExpression arg : literal.getArguments()) {
			// Visit the argument, to get its type into the expressionTypes map
			arg.accept(this);
			
			// Match the type of the argument and that of the signature
			final Type newType = findCommonSubType(expressionTypes.get(arg),
					currentDomain);
			currentDomain = currentRange.getDomain();
			currentRange = currentRange.getRange();
			// Update the type of the argument with what was found when the
			// signature type was taken into account
			expressionTypes.put(arg, newType);
			
			// special case for argmax, etc.
			if (arg instanceof Lambda) {
				firstArgVars.add(((Lambda) arg).getArgument());
			}
		}
		
		// Add the current literal to typing list
		expressionTypes.put(literal, literal.getType().getRange());
		
		// TODO [yoav] [withluke] [posttyping] Let's fix this
		// this is a hack for argmax, etc. should think it through later...
		if (firstArgVars.size() > 0) {
			Type t = firstArgVars.iterator().next().getType();
			for (final Variable v : firstArgVars) {
				t = findCommonSubType(t, expressionTypes.get(v));
			}
			for (final Variable v : firstArgVars) {
				expressionTypes.put(v, t);
			}
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		expressionTypes.put(logicalConstant, logicalConstant.getType());
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		// Nothing to do
	}
	
	private Type findCommonSubType(Type t1, Type t2) {
		if (t1 == null || t2 == null) {
			return null;
		}
		if (t2.isExtending(t1)) {
			return t2;
		}
		if (t1.isExtending(t2)) {
			return t1;
		}
		return null;
	}
	
}
