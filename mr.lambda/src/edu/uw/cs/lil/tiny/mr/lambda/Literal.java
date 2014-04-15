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

import edu.uw.cs.lil.tiny.base.LispReader;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpressionReader.IReader;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType.Option;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.assertion.Assert;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.collections.MapOverlay;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lambda calculus literal.
 * 
 * @author Yoav Artzi
 */
public class Literal extends LogicalExpression {
	public static final ILogger									LOG					= LoggerFactory
																							.create(Literal.class);
	
	public static final String									PREFIX				= String.valueOf(LogicalExpression.PARENTHESIS_OPEN);
	
	/** Mapper used in constructor. */
	private static ListUtils.Mapper<LogicalExpression, Type>	ARG_TO_TYPE_MAPPER	= new ListUtils.Mapper<LogicalExpression, Type>() {
																						
																						@Override
																						public Type process(
																								LogicalExpression obj) {
																							return Assert
																									.ifNull(obj,
																											"Null argument to literal.")
																									.getType();
																						}
																					};
	
	private static final long									serialVersionUID	= -4209330309716600396L;
	
	private final List<LogicalExpression>						arguments;
	
	private final LogicalExpression								predicate;
	
	private final Type											type;
	
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
		this.predicate = Assert.ifNull(predicate);
		this.arguments = Collections.unmodifiableList(arguments);
		
		// Verify that the predicate has a complex type, so it will be able to
		// take arguments
		if (!predicate.getType().isComplex()) {
			throw new LogicalExpressionRuntimeException(String.format(
					"Predicate must have a complex type, not %s",
					predicate.getType()));
		}
		
		// Compute the type. If the computed type is null, throw an exception.
		// Also check against null arguments.
		final Pair<Type, List<Type>> literalTyping = computeLiteralTyping(
				(ComplexType) predicate.getType(),
				ListUtils.map(arguments, ARG_TO_TYPE_MAPPER), typeComparator,
				typeRepository);
		this.type = Assert.ifNull(
				literalTyping == null ? null : literalTyping.first(),
				"Failed to compute literal type.");
	}
	
	public static Pair<Type, List<Type>> computeLiteralTyping(
			ComplexType predicateType, List<Type> argTypes,
			ITypeComparator typeComparator, TypeRepository typeRepository) {
		// Calculate the type of the literal and verify the types of the
		// arguments with regard to the signature
		Type currentDomain;
		Type currentRange = predicateType;
		// Counts the number of arguments for the current sequence. A sequence
		// might change if we switch to a different recursive type.
		int currentNumArgs = 0;
		final Iterator<Type> argIterator = argTypes.iterator();
		final List<Type> impliedSignatureTypes = new ArrayList<Type>(
				argTypes.size());
		while (argIterator.hasNext() && currentRange.isComplex()) {
			final Type argType = argIterator.next();
			currentDomain = currentRange.getDomain();
			currentRange = currentRange.getRange();
			
			// If we have a recursive complex type, and the final return
			// type is complex, we might be seeing a switch to the argument
			// of the final return type. For example: (pred:<e*,<t,t>> boo:e
			// foo:e too:t). So try to switch to the type of the final range
			// before the validity check.
			if (!typeComparator.verifyArgType(currentDomain, argType)
					&& currentRange instanceof RecursiveComplexType
					&& ((RecursiveComplexType) currentRange).getFinalRange()
							.isComplex()) {
				// Verify that the current recursive range was given at least
				// the minimal number of arguments.
				if (currentNumArgs < ((RecursiveComplexType) currentRange)
						.getMinArgs()) {
					LOG.debug(
							"Recursive type %s requires a minimum of %d arguments, %d were provided.",
							currentRange,
							((RecursiveComplexType) currentRange).getMinArgs(),
							currentNumArgs);
					return null;
				}
				
				// Need to update both the current domain and range, basically
				// switching to work with the complex final return type.
				currentDomain = ((ComplexType) ((RecursiveComplexType) currentRange)
						.getFinalRange()).getDomain();
				currentRange = ((ComplexType) ((RecursiveComplexType) currentRange)
						.getFinalRange()).getRange();
				currentNumArgs = 0;
			}
			
			// Validate the type of the argument against the current position in
			// the signature.
			if (!typeComparator.verifyArgType(currentDomain, argType)) {
				LOG.debug("Invalid argument type (%s) for signature type (%s)",
						argType, currentDomain);
				return null;
			}
			impliedSignatureTypes.add(currentDomain);
			currentNumArgs++;
			
			// Return null if we have more arguments than the signature
			// supports.
			if (argIterator.hasNext() && !currentRange.isComplex()) {
				LOG.debug("Too many arguments for predicate of type %s: %s",
						predicateType, argType);
				return null;
			}
		}
		if (currentRange instanceof RecursiveComplexType) {
			// Case special predicate, such as "and"
			final RecursiveComplexType recursivePredicateType = (RecursiveComplexType) currentRange;
			if (currentNumArgs >= recursivePredicateType.getMinArgs()) {
				return Pair.of(recursivePredicateType.getFinalRange(),
						impliedSignatureTypes);
			} else {
				return Pair.of((Type) typeRepository.getTypeCreateIfNeeded(
						recursivePredicateType.getDomain(),
						recursivePredicateType.getFinalRange(), new Option(
								recursivePredicateType.isOrderSensitive(),
								recursivePredicateType.getMinArgs()
										- currentNumArgs)),
						impliedSignatureTypes);
			}
		} else {
			// Case regular complex type
			return Pair.of(currentRange, impliedSignatureTypes);
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
	
	public int numArgs() {
		return arguments.size();
	}
	
	@Override
	protected boolean doEquals(LogicalExpression exp,
			Map<LogicalExpression, LogicalExpression> mapping) {
		if (this == exp) {
			return true;
		}
		if (getClass() != exp.getClass()) {
			return false;
		}
		final Literal other = (Literal) exp;
		if (predicate == null) {
			if (other.predicate != null) {
				return false;
			}
		} else if (!predicate.equals(other.predicate, mapping)) {
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
				if (!thisIterator.next().equals(otherIterator.next(), mapping)) {
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
					final MapOverlay<LogicalExpression, LogicalExpression> tempMapping = new MapOverlay<LogicalExpression, LogicalExpression>(
							mapping);
					if (argThis.equals(otherArgsCopy.get(i), tempMapping)) {
						found = true;
						otherArgsCopy.remove(i);
						mapping.putAll(tempMapping.getOverlayMap());
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
	
	public static class Reader implements IReader<Literal> {
		
		@Override
		public boolean isValid(String string) {
			return string.startsWith(Literal.PREFIX)
					&& !string.startsWith(Lambda.PREFIX);
		}
		
		@Override
		public Literal read(String string,
				Map<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader) {
			try {
				final LispReader lispReader = new LispReader(new StringReader(
						string));
				
				// First is the literal predicate. Get its signature and verify
				// it
				// exists
				final String predicateString = lispReader.next();
				final LogicalExpression predicate = reader.read(
						predicateString, mapping, typeRepository,
						typeComparator);
				
				// The rest of the elements are the arguments
				final List<LogicalExpression> arguments = new ArrayList<LogicalExpression>();
				while (lispReader.hasNext()) {
					final String stringElement = lispReader.next();
					final LogicalExpression argument = reader.read(
							stringElement, mapping, typeRepository,
							typeComparator);
					arguments.add(argument);
				}
				
				// Create the literal, all checks are done within the
				// constructor
				return new Literal(predicate, arguments, typeComparator,
						typeRepository);
			} catch (final RuntimeException e) {
				LOG.error("Literal syntax error: %s", string);
				throw e;
			}
		}
		
	}
	
}
