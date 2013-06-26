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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.AndSimplifier;
import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.ArrayIndexAccessSimplifier;
import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.IPredicateSimplifier;
import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.IncSimplifier;
import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.NotSimplifier;
import edu.uw.cs.lil.tiny.mr.lambda.primitivetypes.OrSimplifier;
import edu.uw.cs.lil.tiny.mr.language.type.ArrayType;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;

/**
 * Logical expression meaning representation services.
 * 
 * @author Yoav Artzi
 */
public class LogicLanguageServices {
	
	private static final String									ARRAY_INDEX_ACCESS_PREDICATE_NAME	= "i";
	
	private static final String									ARRAY_SUB_PREDICATE_NAME			= "sub";
	private static LogicLanguageServices						INSTANCE							= null;
	private static final String									PREDICATE_RETURN_TYPE_SEPARATOR		= ":";
	/**
	 * Generic simplifier for index access predicates for any type of array.
	 */
	private final IPredicateSimplifier							arrayIndexPredicateSimplifier		= ArrayIndexAccessSimplifier.INSTANCE;
	private final LogicalConstant								conjunctionPredicate;
	private final LogicalConstant								disjunctionPredicate;
	private final LogicalConstant								falseConstant;
	private final LogicalConstant								impliesPredicate;
	private final LogicalConstant								indexIncreasePredicate;
	
	private final LogicalConstant								negationPredicate;
	
	/**
	 * If the system supports numeral types, this should be set to the base
	 * type.
	 */
	private final Type											numeralType;
	private final Set<LogicalConstant>							predicateCollapsible				= new HashSet<LogicalConstant>();
	
	private final Map<LogicalConstant, IPredicateSimplifier>	simplifiers							= new ConcurrentHashMap<LogicalConstant, IPredicateSimplifier>();
	
	private final LogicalConstant								trueConstant;
	
	/**
	 * A special comparator for types that allows comparing types for various
	 * cases, such as comparing the type of an argument to the signature type.
	 */
	private final ITypeComparator								typeComparator;
	
	private final TypeRepository								typeRepository;
	
	private LogicLanguageServices(TypeRepository typeRepository,
			String numeralTypeName, ITypeComparator typeComparator) {
		this.typeRepository = typeRepository;
		this.numeralType = numeralTypeName == null ? null : typeRepository
				.getType(numeralTypeName);
		this.typeComparator = typeComparator;
		
		// Basic predicates
		conjunctionPredicate = (LogicalConstant) LogicalExpression.parse(
				"and:<t*,t>", typeRepository, typeComparator, false);
		disjunctionPredicate = (LogicalConstant) LogicalExpression.parse(
				"or:<t*,t>", typeRepository, typeComparator, false);
		impliesPredicate = (LogicalConstant) LogicalExpression.parse(
				"implies:<e,<e,t>>", typeRepository, typeComparator, false);
		negationPredicate = (LogicalConstant) LogicalExpression.parse(
				"not:<t,t>", typeRepository, typeComparator, false);
		indexIncreasePredicate = (LogicalConstant) LogicalExpression.parse(
				"inc:<" + typeRepository.getIndexType().getName() + ","
						+ typeRepository.getIndexType().getName() + ">",
				typeRepository, typeComparator, false);
		
		// Predicate specific simplifiers
		this.setSimplifier(conjunctionPredicate, AndSimplifier.INSTANCE, true);
		this.setSimplifier(disjunctionPredicate, OrSimplifier.INSTANCE, true);
		this.setSimplifier(negationPredicate, NotSimplifier.INSTANCE, true);
		this.setSimplifier(indexIncreasePredicate, IncSimplifier.INSTANCE,
				false);
		
		// Special constants
		trueConstant = LogicalConstant.create("true:t",
				typeRepository.getTruthValueType(), false);
		falseConstant = LogicalConstant.create("false:t",
				typeRepository.getTruthValueType(), false);
	}
	
	public static LogicalConstant getConjunctionPredicate() {
		return INSTANCE.conjunctionPredicate;
	}
	
	public static LogicalConstant getDisjunctionPredicate() {
		return INSTANCE.disjunctionPredicate;
	}
	
	public static LogicalConstant getFalse() {
		return INSTANCE.falseConstant;
	}
	
	public static LogicalConstant getImpliesPredicate() {
		return INSTANCE.impliesPredicate;
	}
	
	public static LogicalConstant getIndexIncreasePredicate() {
		return INSTANCE.indexIncreasePredicate;
	}
	
	static public LogicalConstant getIndexPredicateForArray(ArrayType arrayType) {
		final ComplexType predicateType = INSTANCE.typeRepository
				.getIndexPredicateTypeForArray(arrayType);
		return LogicalConstant.create(
				composePredicateName(ARRAY_INDEX_ACCESS_PREDICATE_NAME,
						predicateType), predicateType);
	}
	
	public static LogicalConstant getNegationPredicate() {
		return INSTANCE.negationPredicate;
	}
	
	static public IPredicateSimplifier getSimplifier(LogicalExpression pred) {
		// First check if the given type is a index predicate, if so return the
		// generic index predicate simplifier.
		if (isArrayIndexPredicate(pred)) {
			// If this is a index function for an array, return the
			// generic
			// simplifier for index functions
			return INSTANCE.arrayIndexPredicateSimplifier;
		}
		
		return INSTANCE.simplifiers.get(pred);
	}
	
	static public LogicalConstant getSubPredicateForArray(ArrayType arrayType) {
		final ComplexType predicateType = INSTANCE.typeRepository
				.getSubPredicateTypeForArray(arrayType);
		return LogicalConstant.create(
				composePredicateName(ARRAY_SUB_PREDICATE_NAME, predicateType),
				predicateType);
	}
	
	public static LogicalConstant getTrue() {
		return INSTANCE.trueConstant;
	}
	
	public static ITypeComparator getTypeComparator() {
		return INSTANCE.typeComparator;
	}
	
	public static TypeRepository getTypeRepository() {
		return INSTANCE.typeRepository;
	}
	
	/**
	 * @param constant
	 *            Assumes the constant is of type 'ind'
	 * @return
	 */
	static public int indexConstantToInt(LogicalConstant constant) {
		if (constant.getType() == LogicLanguageServices.getTypeRepository()
				.getIndexType()) {
			return Integer.valueOf(LogicalConstant.splitName(constant).first());
		} else {
			throw new LogicalExpressionRuntimeException(
					"Constant must be of index type: " + constant);
		}
	}
	
	public static LogicLanguageServices instance() {
		return INSTANCE;
	}
	
	static public LogicalConstant intToIndexConstant(int i) {
		return LogicalConstant.create(i + Term.TYPE_SEPARATOR
				+ INSTANCE.typeRepository.getIndexType().getName(),
				INSTANCE.typeRepository.getIndexType());
	}
	
	static public LogicalConstant intToLogicalExpression(int num) {
		return LogicalConstant.create(String.valueOf(num) + ":"
				+ INSTANCE.numeralType.getName(), INSTANCE.numeralType);
	}
	
	public static boolean isArrayIndexPredicate(LogicalExpression pred) {
		return pred instanceof LogicalConstant
				&& ((LogicalConstant) pred).getName().startsWith(
						ARRAY_INDEX_ACCESS_PREDICATE_NAME
								+ PREDICATE_RETURN_TYPE_SEPARATOR);
	}
	
	public static boolean isArraySubPredicate(LogicalExpression pred) {
		return pred instanceof LogicalConstant
				&& ((LogicalConstant) pred).getName().startsWith(
						ARRAY_SUB_PREDICATE_NAME
								+ PREDICATE_RETURN_TYPE_SEPARATOR);
	}
	
	public static boolean isCollpasiblePredicate(LogicalExpression predicate) {
		return INSTANCE.predicateCollapsible.contains(predicate);
	}
	
	/**
	 * Return 'true' iff the type is a predicate of 'and:t' or 'or:t'.
	 * 
	 * @param type
	 * @return
	 */
	static public boolean isCoordinationPredicate(LogicalExpression pred) {
		return pred == INSTANCE.conjunctionPredicate
				|| pred == INSTANCE.disjunctionPredicate;
	}
	
	static public Integer logicalExpressionToInteger(LogicalExpression exp) {
		if (exp instanceof LogicalConstant
				&& exp.getType().isExtending(INSTANCE.numeralType)) {
			final LogicalConstant constant = (LogicalConstant) exp;
			try {
				return Integer.valueOf(constant.getName().split(
						Term.TYPE_SEPARATOR)[0]);
			} catch (final NumberFormatException e) {
				// Ignore, just return null
			}
		}
		return null;
	}
	
	public static void setInstance(LogicLanguageServices logicLanguageServices) {
		INSTANCE = logicLanguageServices;
	}
	
	private static String composePredicateName(String name, ComplexType type) {
		return name + PREDICATE_RETURN_TYPE_SEPARATOR + type.getName();
	}
	
	public void setSimplifier(LogicalConstant predicate,
			IPredicateSimplifier simplifier, boolean collapsable) {
		if (collapsable) {
			synchronized (predicateCollapsible) {
				predicateCollapsible.add(predicate);
			}
		}
		simplifiers.put(predicate, simplifier);
	}
	
	public static class Builder {
		
		private String					numeralTypeName	= null;
		private ITypeComparator			typeComparator	= new StrictTypeComparator();
		private final TypeRepository	typeRepository;
		
		public Builder(TypeRepository typeRepositor) {
			this.typeRepository = typeRepositor;
		}
		
		public LogicLanguageServices build() {
			return new LogicLanguageServices(typeRepository, numeralTypeName,
					typeComparator);
		}
		
		public Builder setNumeralTypeName(String numeralTypeName) {
			this.numeralTypeName = numeralTypeName;
			return this;
		}
		
		public Builder setTypeComparator(ITypeComparator typeComparator) {
			this.typeComparator = typeComparator;
			return this;
		}
	}
	
}
