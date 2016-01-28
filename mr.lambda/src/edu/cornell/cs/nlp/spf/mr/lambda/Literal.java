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
package edu.cornell.cs.nlp.spf.mr.lambda;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.LispReader;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMappingOverlay;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType.Option;
import edu.cornell.cs.nlp.utils.collections.stackmap.IdentityFastStackMap;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Lambda calculus literal.
 *
 * @author Yoav Artzi
 */
public class Literal extends LogicalExpression {
	public static final ILogger			LOG					= LoggerFactory
																	.create(Literal.class);
	public static final String			PREFIX				= String.valueOf(LogicalExpression.PARENTHESIS_OPEN);

	private static final long			serialVersionUID	= -4209330309716600396L;

	/**
	 * The arguments are stored in an array which is never exposed publicly.
	 * This allows the class to guarantee its immutability, while providing high
	 * performance. This member doesn't provide an iterator, due to the cost of
	 * creating an iterator and superior performance of direct access (using
	 * {@link #getArg(int)}).
	 */
	private final LogicalExpression[]	arguments;

	private final Set<Variable>			freeVariables;

	private final LogicalExpression		predicate;

	private final Type[]				signature;

	private final Type					type;

	/**
	 * @param predicate
	 *            Predicate to use.
	 * @param argumentSource
	 *            Use the arguments from this literal
	 */
	public Literal(LogicalExpression predicate, Literal argumentSource) {
		this(predicate, argumentSource.arguments, LogicLanguageServices
				.getTypeComparator(), LogicLanguageServices.getTypeRepository());
	}

	public Literal(LogicalExpression predicate, LogicalExpression[] arguments) {
		this(predicate, arguments, LogicLanguageServices.getTypeComparator(),
				LogicLanguageServices.getTypeRepository());
	}

	@SuppressWarnings("unchecked")
	private Literal(LogicalExpression predicate, LogicalExpression[] arguments,
			ITypeComparator typeComparator, TypeRepository typeRepository) {
		// Assert that the predicate is not null and has a complex type.
		assert predicate != null : String.format("Null predicate");
		assert predicate.getType().isComplex() : String.format(
				"Predicate without a complex type: %s", predicate);
		this.predicate = predicate;
		this.arguments = arguments;

		// Compute the type. If the computed type is null, throw an exception.
		// Also check against null arguments.
		final Type[] argTypes = new Type[arguments.length];
		// Track if there's only one sub-expression with free variables. We do
		// this to optimize and and try to re-use the set of free variables from
		// the sub-expression, if possible.
		boolean singleSubExpWithFreeVariables = true;
		// Track the index of the sub-expression with free variables, if there
		// is only one. If none has been observed, set to -1. Otherwise, 0 for
		// predicate, and i for argument i-1.
		int subExpWithFreeVariables = predicate.numFreeVariables() == 0 ? -1
				: 0;
		for (int i = 0; i < arguments.length; ++i) {
			assert arguments[i] != null : "Null argument to literal";
			argTypes[i] = arguments[i].getType();
			if (singleSubExpWithFreeVariables
					&& arguments[i].numFreeVariables() > 0) {
				if (subExpWithFreeVariables < 0) {
					subExpWithFreeVariables = i + 1;
				} else {
					singleSubExpWithFreeVariables = false;
				}
			}
		}
		// Set the set of free variables.
		if (singleSubExpWithFreeVariables) {
			if (subExpWithFreeVariables == 0) {
				this.freeVariables = predicate.getFreeVariables();
			} else if (subExpWithFreeVariables > 0) {
				this.freeVariables = arguments[subExpWithFreeVariables - 1]
						.getFreeVariables();
			} else {
				this.freeVariables = ReferenceSets.EMPTY_SET;
			}
		} else {
			// Case we have multiple sub-expression with free variables. We need
			// to take their union. This is a relatively expensive process, so
			// we tried to avoid it so far.
			final ReferenceSet<Variable> variables = new ReferenceOpenHashSet<Variable>();
			if (predicate.numFreeVariables() > 0) {
				variables.addAll(predicate.getFreeVariables());
			}
			for (int i = 0; i < arguments.length; ++i) {
				if (arguments[i].numFreeVariables() > 0) {
					variables.addAll(arguments[i].getFreeVariables());
				}
			}
			this.freeVariables = ReferenceSets.unmodifiable(variables);
		}

		final Type[] impliedSignatureTypes = new Type[argTypes.length];
		final Type literalType = computeLiteralTyping(
				(ComplexType) predicate.getType(), argTypes, typeComparator,
				typeRepository, impliedSignatureTypes);
		assert literalType != null : String.format(
				"Failed to compute literal type. predicate=%s, arguments=%s",
				predicate, Arrays.toString(arguments));
		this.type = literalType;
		this.signature = impliedSignatureTypes;

	}

	public static Pair<Type, Type[]> computeLiteralTyping(
			ComplexType predicateType, Type[] argTypes,
			ITypeComparator typeComparator, TypeRepository typeRepository) {
		final Type[] impliedSignatureTypes = new Type[argTypes.length];
		final Type computedType = computeLiteralTyping(predicateType, argTypes,
				typeComparator, typeRepository, impliedSignatureTypes);
		if (computedType == null) {
			return null;
		} else {
			return Pair.of(computedType, impliedSignatureTypes);
		}
	}

	private static Type computeLiteralTyping(ComplexType predicateType,
			Type[] argTypes, ITypeComparator typeComparator,
			TypeRepository typeRepository, Type[] impliedSignatureTypes) {
		assert impliedSignatureTypes.length == argTypes.length : "Invalid length for array given to populate implied signature types";

		// Calculate the type of the literal and verify the types of the
		// arguments with regard to the signature.
		Type currentDomain;
		Type currentRange = predicateType;
		// Counts the number of arguments for the current sequence. A sequence
		// might change if we switch to a different recursive type.
		int currentNumArgs = 0;
		int i = 0;
		while (i < argTypes.length && currentRange.isComplex()) {
			final Type argType = argTypes[i];
			++i;
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
			impliedSignatureTypes[i - 1] = currentDomain;
			currentNumArgs++;

			// Return null if we have more arguments than the signature
			// supports.
			if (i < argTypes.length && !currentRange.isComplex()) {
				LOG.debug("Too many arguments for predicate of type %s: %s",
						predicateType, argType);
				return null;
			}
		}
		if (currentRange instanceof RecursiveComplexType) {
			// Case special predicate, such as "and"
			final RecursiveComplexType recursivePredicateType = (RecursiveComplexType) currentRange;
			if (currentNumArgs >= recursivePredicateType.getMinArgs()) {
				return recursivePredicateType.getFinalRange();
			} else {
				return typeRepository.getTypeCreateIfNeeded(
						recursivePredicateType.getDomain(),
						recursivePredicateType.getFinalRange(), new Option(
								recursivePredicateType.isOrderSensitive(),
								recursivePredicateType.getMinArgs()
										- currentNumArgs));
			}
		} else {
			// Case regular complex type
			return currentRange;
		}

	}

	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}

	public void addArgsToList(List<LogicalExpression> list) {
		for (int i = 0; i < arguments.length; ++i) {
			list.add(arguments[i]);
		}
	}

	public LogicalExpression[] argumentCopy() {
		return Arrays.copyOf(arguments, arguments.length);
	}

	public LogicalExpression[] argumentCopy(int fromIndex, int toIndex) {
		return Arrays.copyOfRange(arguments, fromIndex, toIndex);
	}

	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		if (getPredicateType().isOrderSensitive()) {
			// If the predicate is order sensitive, the internal hashing of a
			// list will make the hashcode order sensitive
			result = prime * result + Arrays.hashCode(arguments);
		} else {
			// Case the predicate is order insensitive, the hashcode should be
			// order insensitive, so can't use list's internal hashing method.
			// The code here is inspired by AbstractSet.hashCode()
			result = prime * result;
			final int len = arguments.length;
			// The hashcode is computed in a somewhat convoluted way here to
			// avoid collisions as much as possible.
			int xor = 1;
			int add = 1;
			for (int i = 0; i < len; ++i) {
				final int argHash = arguments[i].hashCode();
				xor ^= argHash;
				add += argHash;
			}
			result += xor;
			result ^= add;
		}
		result = prime * result + predicate.hashCode();
		result = prime * result + type.hashCode();
		return result;
	}

	@Override
	public boolean containsFreeVariable(Variable variable) {
		return freeVariables.contains(variable);
	}

	@Override
	public boolean containsFreeVariables(Set<Variable> variables) {
		if (freeVariables.isEmpty()) {
			return false;
		}

		final Set<Variable> bigSet;
		final Set<Variable> smallSet;
		if (freeVariables.size() >= variables.size()) {
			bigSet = freeVariables;
			smallSet = variables;
		} else {
			bigSet = variables;
			smallSet = freeVariables;
		}

		for (final Variable variable : smallSet) {
			if (bigSet.contains(variable)) {
				return true;
			}
		}
		return false;
	}

	public void copyArgsIntoArray(LogicalExpression[] array, int fromIndex,
			int destIndex, int length) {
		System.arraycopy(arguments, fromIndex, array, destIndex, length);
	}

	public LogicalExpression getArg(int index) {
		return arguments[index];
	}

	public Type getArgSignature(int index) {
		return signature[index];
	}

	@Override
	public Set<Variable> getFreeVariables() {
		return freeVariables;
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
		return arguments.length;
	}

	@Override
	public int numFreeVariables() {
		return freeVariables.size();
	}

	@Override
	protected boolean doEquals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		if (this == exp) {
			// Since skolem IDs from this literal may be used in other parts of
			// the logical form, we need to create a mapping of them. As the
			// instances are identical, we can just update the mapping by
			// creating a mapping from each SkolemId to itself.
			if (!freeVariables.isEmpty()) {
				for (final Variable freeVariable : freeVariables) {
					if (freeVariable instanceof SkolemId) {
						mapping.push(freeVariable, freeVariable);
					}
				}
			}
			return true;
		}
		if (getClass() != exp.getClass()) {
			return false;
		}
		final Literal other = (Literal) exp;
		if (!predicate.equals(other.predicate, mapping)) {
			return false;
		}
		if (arguments.length != other.arguments.length) {
			return false;
		}

		if (getPredicateType().isOrderSensitive()) {
			for (int i = 0; i < arguments.length; ++i) {
				if (!arguments[i].equals(other.arguments[i], mapping)) {
					return false;
				}
			}
		} else {
			final int length = arguments.length;
			final LogicalExpression[] otherArgsCopy = other.argumentCopy();
			for (int j = 0; j < arguments.length; ++j) {
				final LogicalExpression argThis = arguments[j];
				boolean found = false;
				for (int i = 0; i < length; ++i) {
					if (otherArgsCopy[i] != null) {
						// Use maps from the pool to avoid constantly allocating
						// objects.
						final ScopeMappingOverlay<Variable, Variable> overlayMapping = new ScopeMappingOverlay<Variable, Variable>(
								mapping,
								new IdentityFastStackMap<Variable, Variable>(),
								new IdentityFastStackMap<Variable, Variable>());
						if (argThis.equals(otherArgsCopy[i], overlayMapping)) {
							found = true;
							otherArgsCopy[i] = null;
							overlayMapping.applyToBase();
							break;
						}
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
		public Literal read(String string,
				ScopeMapping<String, LogicalExpression> mapping,
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
				return new Literal(predicate,
						arguments.toArray(new LogicalExpression[arguments
								.size()]), typeComparator, typeRepository);
			} catch (final RuntimeException e) {
				LOG.error("Literal syntax error: %s", string);
				throw e;
			}
		}

		@Override
		public boolean test(String string) {
			return string.startsWith(Literal.PREFIX)
					&& !string.startsWith(Lambda.PREFIX);
		}

	}

}
