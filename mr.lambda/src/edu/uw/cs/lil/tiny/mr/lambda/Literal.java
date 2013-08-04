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
package edu.uw.cs.lil.tiny.mr.lambda;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType.Option;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.tiny.utils.LispReader;
import edu.uw.cs.utils.assertion.Assert;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lambda calculus literal.
 * 
 * @author Yoav Artzi
 */
public class Literal extends LogicalExpression {
	public static final String				PREFIX				= String.valueOf(LogicalExpression.PARENTHESIS_OPEN);
	
	private static final ILogger			LOG					= LoggerFactory
																		.create(Literal.class);
	
	private static final long				serialVersionUID	= -4209330309716600396L;
	
	private final List<LogicalExpression>	arguments;
	
	private final LogicalExpression			predicate;
	
	private final Type						type;
	
	public Literal(LogicalExpression predicate,
			List<LogicalExpression> arguments) {
		this(predicate, arguments, LogicLanguageServices.getTypeComparator(),
				LogicLanguageServices.getTypeRepository());
	}
	
	/**
	 * The given arguments list will be wrapped as an unmodifiable list.
	 * 
	 * @param predicate
	 * @param arguments
	 * @param typeComparator
	 * @param typeRepository
	 */
	private Literal(LogicalExpression predicate,
			List<LogicalExpression> arguments, ITypeComparator typeComparator,
			TypeRepository typeRepository) {
		// Verify predicate and all arguments are not null -- safety measure
		Assert.ifNull(predicate);
		for (final LogicalExpression arg : arguments) {
			Assert.ifNull(arg);
		}
		
		// Verify that the predicate has a complex type, so it will be able to
		// take arguments
		if (!(predicate.getType().isComplex())) {
			throw new LogicalExpressionRuntimeException(String.format(
					"Predicate must have a complex type, not %s",
					predicate.getType()));
		}
		final ComplexType predicateType = (ComplexType) predicate.getType();
		
		// Calculate the type of the literal and verify the types of the
		// arguments with regard to the signature
		Type currentDomain = predicateType.getDomain();
		Type currentRange = predicateType.getRange();
		Type currentType = predicateType;
		final Iterator<? extends LogicalExpression> argIterator = arguments
				.iterator();
		while (argIterator.hasNext() && currentDomain != null) {
			final LogicalExpression arg = argIterator.next();
			if (!typeComparator.verifyArgType(currentDomain, arg.getType())) {
				throw new LogicalExpressionRuntimeException(String.format(
						"Invalid argument type (%s) for signature type (%s)",
						arg.getType(), currentDomain));
			}
			
			currentType = currentType.getRange();
			if (currentRange.isComplex()) {
				currentDomain = currentRange.getDomain();
				currentRange = currentRange.getRange();
			} else {
				currentDomain = null;
				if (argIterator.hasNext()) {
					throw new LogicalExpressionRuntimeException(String.format(
							"Too many arguments for predicate %s: %s",
							predicate, arguments));
				}
			}
		}
		if (predicateType instanceof RecursiveComplexType) {
			// Case special predicate, such as "and"
			final RecursiveComplexType recursivePredicateType = (RecursiveComplexType) predicateType;
			if (arguments.size() >= recursivePredicateType.getMinArgs()) {
				this.type = recursivePredicateType.getFinalRange();
			} else {
				this.type = typeRepository.getTypeCreateIfNeeded(
						predicateType.getDomain(),
						recursivePredicateType.getFinalRange(),
						new Option(recursivePredicateType.isOrderSensitive(),
								recursivePredicateType.getMinArgs()
										- arguments.size()));
			}
		} else {
			// Case regular complex type
			this.type = currentType;
		}
		
		this.predicate = predicate;
		this.arguments = Collections.unmodifiableList(arguments);
	}
	
	protected static Literal doParse(String string,
			Map<String, Variable> variables, TypeRepository typeRepository,
			ITypeComparator typeComparator, boolean lockOntology) {
		try {
			final LispReader lispReader = new LispReader(new StringReader(
					string));
			
			// First is the literal predicate. Get its signature and verify it
			// exists
			final String predicateString = lispReader.next();
			final LogicalExpression predicate = LogicalExpression.doParse(
					predicateString, variables, typeRepository, typeComparator,
					lockOntology);
			
			// The rest of the elements are the arguments
			final List<LogicalExpression> arguments = new ArrayList<LogicalExpression>();
			while (lispReader.hasNext()) {
				final String stringElement = lispReader.next();
				final LogicalExpression argument = LogicalExpression.doParse(
						stringElement, variables, typeRepository,
						typeComparator, lockOntology);
				arguments.add(argument);
			}
			
			// Create the literal, all checks are done within the constructor
			return new Literal(predicate, arguments, typeComparator,
					typeRepository);
		} catch (final RuntimeException e) {
			LOG.error("Literal syntax error: %s", string);
			throw e;
		}
	}
	
	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		if (getPredicateType().isOrderSensitive()) {
			// If the predicate is order sensitive, the internal hashing of a
			// list will make the hashcode order sensitive
			result = prime * result
					+ ((arguments == null) ? 0 : arguments.hashCode());
		} else {
			// Case the predicate is order insensitive, the hashcode should be
			// order insensitive, so can't use list's internal hashing method.
			// The code here is inspired by AbstractSet.hashCode()
			result = prime * result;
			if (arguments != null) {
				for (final LogicalExpression arg : arguments) {
					result += arg.hashCode();
				}
			}
		}
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		// The parent object will create the variable mapping and use it for
		// equal. Shortcut by checking that obj is a Literal
		return obj instanceof Literal && super.equals(obj);
	}
	
	public List<LogicalExpression> getArguments() {
		return arguments;
	}
	
	public LogicalExpression getPredicate() {
		return predicate;
	}
	
	public ComplexType getPredicateType() {
		return (ComplexType) predicate.getType();
	}
	
	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	protected boolean doEquals(Object obj,
			Map<Variable, Variable> variablesMapping) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Literal other = (Literal) obj;
		if (predicate == null) {
			if (other.predicate != null) {
				return false;
			}
		} else if (!predicate.equals(other.predicate, variablesMapping)) {
			return false;
		}
		if (arguments == null) {
			if (other.arguments != null) {
				return false;
			}
		} else if (arguments.size() != other.arguments.size()) {
			return false;
		}
		
		if (getPredicateType().isOrderSensitive()) {
			final Iterator<? extends LogicalExpression> thisIterator = arguments
					.iterator();
			final Iterator<? extends LogicalExpression> otherIterator = other.arguments
					.iterator();
			while (thisIterator.hasNext()) {
				if (!thisIterator.next().equals(otherIterator.next(),
						variablesMapping)) {
					return false;
				}
			}
		} else {
			final ArrayList<LogicalExpression> otherArgsCopy = new ArrayList<LogicalExpression>(
					other.arguments);
			for (final LogicalExpression argThis : arguments) {
				final int length = otherArgsCopy.size();
				boolean found = false;
				for (int i = 0; i < length; ++i) {
					if (argThis.equals(otherArgsCopy.get(i), variablesMapping)) {
						found = true;
						otherArgsCopy.remove(i);
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
			// No need to check if otherArgCopy is empty, since we already know
			// the number of arguments is identical
		}
		
		return true;
	}
	
}
