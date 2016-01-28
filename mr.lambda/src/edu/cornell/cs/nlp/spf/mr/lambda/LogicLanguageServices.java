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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.cornell.cs.nlp.spf.base.LispReader;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.AndSimplifier;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.ArrayIndexAccessSimplifier;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.IPredicateSimplifier;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.IncSimplifier;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.NotSimplifier;
import edu.cornell.cs.nlp.spf.mr.lambda.primitivetypes.OrSimplifier;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.ILogicalExpressionPrinter;
import edu.cornell.cs.nlp.spf.mr.lambda.printers.LogicalExpressionToString;
import edu.cornell.cs.nlp.spf.mr.language.type.ArrayType;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Logical expression meaning representation services.
 *
 * @author Yoav Artzi
 */
public class LogicLanguageServices {
	public static final ILogger									LOG									= LoggerFactory
																											.create(LogicLanguageServices.class);

	private static final String									ARRAY_INDEX_ACCESS_PREDICATE_NAME	= "i";

	private static final String									ARRAY_SUB_PREDICATE_NAME			= "sub";
	private static LogicLanguageServices						INSTANCE							= null;

	/**
	 * Generic simplifier for index access predicates for any type of array.
	 */
	private final IPredicateSimplifier							arrayIndexPredicateSimplifier		= ArrayIndexAccessSimplifier.INSTANCE;

	/**
	 * Logical constant that might be removed from the logical form during
	 * simplification without changing its meaning. Several dynamic constants
	 * are not covered by this set, see
	 * {@link #isCollpasibleConstant(LogicalConstant)}.
	 */
	private final Set<LogicalConstant>							collapsibleConstants				= new HashSet<LogicalConstant>();

	private final ILogicalExpressionComparator					comparator;
	private final LogicalConstant								conjunctionPredicate;
	private final LogicalConstant								disjunctionPredicate;
	private final LogicalConstant								falseConstant;

	private final LogicalConstant								indexIncreasePredicate;

	private final LogicalConstant								negationPredicate;

	/**
	 * If the system supports numeral types, this should be set to the base
	 * type.
	 */
	private final Type											numeralType;

	private final Ontology										ontology;

	private final ILogicalExpressionPrinter						printer;

	private final Map<LogicalConstant, IPredicateSimplifier>	simplifiers							= new ConcurrentHashMap<LogicalConstant, IPredicateSimplifier>();

	private final LogicalConstant								trueConstant;

	/**
	 * A special comparator for types that allows comparing types for various
	 * cases, such as comparing the type of an argument to the signature type.
	 */
	private final ITypeComparator								typeComparator;

	private final TypeRepository								typeRepository;

	private LogicLanguageServices(TypeRepository typeRepository,
			String numeralTypeName, ITypeComparator typeComparator,
			Ontology ontology, LogicalConstant conjunctionPredicate,
			LogicalConstant disjunctionPredicate,
			LogicalConstant negationPredicate,
			LogicalConstant indexIncreasePredicate,
			LogicalConstant trueConstant, LogicalConstant falseConstant,
			ILogicalExpressionPrinter printer,
			ILogicalExpressionComparator comparator) {
		this.typeRepository = typeRepository;
		this.ontology = ontology;
		this.printer = printer;
		this.comparator = comparator;
		this.numeralType = numeralTypeName == null ? null : typeRepository
				.getType(numeralTypeName);
		this.typeComparator = typeComparator;

		// Basic predicates
		this.conjunctionPredicate = conjunctionPredicate;
		this.disjunctionPredicate = disjunctionPredicate;
		this.negationPredicate = negationPredicate;
		this.indexIncreasePredicate = indexIncreasePredicate;

		// Predicate specific simplifiers
		this.setSimplifier(conjunctionPredicate, new AndSimplifier(
				trueConstant, falseConstant), true);
		this.setSimplifier(disjunctionPredicate, new OrSimplifier(trueConstant,
				falseConstant), true);
		this.setSimplifier(negationPredicate, NotSimplifier.INSTANCE, true);
		this.setSimplifier(indexIncreasePredicate, IncSimplifier.INSTANCE,
				false);

		// Special constants
		this.trueConstant = trueConstant;
		this.falseConstant = falseConstant;
		this.collapsibleConstants.add(trueConstant);
		this.collapsibleConstants.add(falseConstant);
	}

	public static ILogicalExpressionComparator getComparator() {
		return INSTANCE.comparator;
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

	public static LogicalConstant getIndexIncreasePredicate() {
		return INSTANCE.indexIncreasePredicate;
	}

	static public LogicalConstant getIndexPredicateForArray(ArrayType arrayType) {
		final ComplexType predicateType = INSTANCE.typeRepository
				.getIndexPredicateTypeForArray(arrayType);
		final String name = LogicalConstant.makeFullName(
				ARRAY_INDEX_ACCESS_PREDICATE_NAME, predicateType);
		return LogicalConstant.createDynamic(name, predicateType, false);
	}

	public static LogicalConstant getNegationPredicate() {
		return INSTANCE.negationPredicate;
	}

	public static Type getNumeralType() {
		return INSTANCE.numeralType;
	}

	public static Ontology getOntology() {
		return INSTANCE == null ? null : INSTANCE.ontology;
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
		final String name = LogicalConstant.makeFullName(
				ARRAY_SUB_PREDICATE_NAME, predicateType);
		return LogicalConstant.createDynamic(name, predicateType, false);
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
			return Integer.parseInt(constant.getBaseName());
		} else {
			throw new LogicalExpressionRuntimeException(
					"Constant must be of index type: " + constant);
		}
	}

	public static LogicLanguageServices instance() {
		return INSTANCE;
	}

	static public LogicalConstant intToIndexConstant(int i) {
		final String name = LogicalConstant.makeFullName(String.valueOf(i),
				INSTANCE.typeRepository.getIndexType());
		return LogicalConstant.createDynamic(name,
				INSTANCE.typeRepository.getIndexType(), false);
	}

	static public LogicalConstant intToLogicalExpression(long num) {
		final String name = LogicalConstant.makeFullName(String.valueOf(num),
				INSTANCE.numeralType);
		return LogicalConstant.createDynamic(name, INSTANCE.numeralType, false);
	}

	public static boolean isArrayIndexPredicate(LogicalExpression pred) {
		return pred instanceof LogicalConstant
				&& ((LogicalConstant) pred).getName()
						.startsWith(
								ARRAY_INDEX_ACCESS_PREDICATE_NAME
										+ Term.TYPE_SEPARATOR);
	}

	public static boolean isArraySubPredicate(LogicalExpression pred) {
		return pred instanceof LogicalConstant
				&& ((LogicalConstant) pred).getName().startsWith(
						ARRAY_SUB_PREDICATE_NAME + Term.TYPE_SEPARATOR);
	}

	/**
	 * Returns 'true' iff the constant may disappear form the logical form
	 * during simplification (without modifying the meaning of the logical
	 * form).
	 */
	public static boolean isCollpasibleConstant(LogicalExpression exp) {
		return INSTANCE.collapsibleConstants.contains(exp)
				|| isArraySubPredicate(exp);
	}

	/**
	 * Return 'true' iff the type is a predicate of 'and:t' or 'or:t'.
	 *
	 * @param type
	 * @return
	 */
	static public boolean isCoordinationPredicate(LogicalExpression pred) {
		return pred.equals(INSTANCE.conjunctionPredicate)
				|| pred.equals(INSTANCE.disjunctionPredicate);
	}

	public static boolean isEqual(LogicalExpression e1, LogicalExpression e2) {
		if (e1 == e2) {
			return true;
		}

		// Logical expression equality is deterministic but approximate,
		// meaning: it can fail for equal expressions. If it returns 'true',
		// it's always correct. However, if it returns 'false', it can be
		// incorrect. This beahvior is not symmetric, so we order the
		// expressions using their identity hash code (usually their memory
		// address) so given two objects, we will always get the same result.
		if (System.identityHashCode(e2) > System.identityHashCode(e1)) {
			return INSTANCE.comparator.compare(e1, e2);
		} else {
			return INSTANCE.comparator.compare(e2, e1);
		}

	}

	public static boolean isOntologyClosed() {
		return INSTANCE.ontology != null && INSTANCE.ontology.isClosed();
	}

	static public Long logicalExpressionToInteger(LogicalExpression exp) {
		if (exp instanceof LogicalConstant
				&& exp.getType().isExtending(INSTANCE.numeralType)) {
			final LogicalConstant constant = (LogicalConstant) exp;
			try {
				return Long.valueOf(constant.getBaseName());
			} catch (final NumberFormatException e) {
				// Ignore, just return null
			}
		}
		return null;
	}

	public static void setInstance(LogicLanguageServices logicLanguageServices) {
		INSTANCE = logicLanguageServices;
	}

	public static String toString(LogicalExpression logicalExpression) {
		return INSTANCE.printer.toString(logicalExpression);
	}

	public void setSimplifier(LogicalConstant predicate,
			IPredicateSimplifier simplifier, boolean collapsable) {
		if (collapsable) {
			synchronized (collapsibleConstants) {
				collapsibleConstants.add(predicate);
			}
		}
		simplifiers.put(predicate, simplifier);
	}

	public static class Builder {

		private ILogicalExpressionComparator	comparator		= new LogicalExpressionComparator();
		private final List<File>				constantsFiles	= new LinkedList<File>();
		private String							numeralTypeName	= null;
		private boolean							ontologyClosed	= false;
		private ILogicalExpressionPrinter		printer			= new LogicalExpressionToString.Printer();

		private final ITypeComparator			typeComparator;

		private final TypeRepository			typeRepository;

		private boolean							useOntology		= false;

		/**
		 * @param typeRepository
		 *            Type repository to be used by the system.
		 */
		public Builder(TypeRepository typeRepository) {
			this(typeRepository, new StrictTypeComparator());
		}

		/**
		 * @param typeRepository
		 *            Type repository to be used by the system.
		 * @param typeComparator
		 *            Type comparator to be used to compare types through the
		 *            system. Setting this accordingly allows to ignore certain
		 *            distinctions between finer types.
		 */
		public Builder(TypeRepository typeRepository,
				ITypeComparator typeComparator) {
			this.typeRepository = typeRepository;
			this.typeComparator = typeComparator;
		}

		private static Set<LogicalConstant> readConstantsFromFile(File file,
				TypeRepository typeRepository) throws IOException {
			// First, strip the comments and prepare a clean LISP string to
			// parse
			final StringBuilder strippedFile = new StringBuilder();
			try (final BufferedReader reader = new BufferedReader(
					new FileReader(file))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					line = line.split("\\s*//")[0];
					if (!line.equals("")) {
						strippedFile.append(line).append(" ");
					}
				}
			}

			// Read the constants
			final Set<LogicalConstant> constants = new HashSet<LogicalConstant>();
			final LispReader lispReader = new LispReader(new StringReader(
					strippedFile.toString()));
			while (lispReader.hasNext()) {
				final LogicalConstant exp = LogicalConstant.read(
						lispReader.next(), typeRepository);
				constants.add(exp);
			}

			return constants;
		}

		private static Set<LogicalConstant> readConstantsFromFiles(
				List<File> files, TypeRepository typeRepository)
				throws IOException {
			final Set<LogicalConstant> constants = new HashSet<LogicalConstant>();
			for (final File file : files) {
				constants.addAll(readConstantsFromFile(file, typeRepository));
			}
			return constants;
		}

		/**
		 * Use an ontology. This allows to re-use {@link LogicalConstant}
		 * objects. If not ontology is defined, {@link LogicalConstant} objects
		 * are created just like any other objects.
		 */
		public Builder addConstantsToOntology(File constantsFile) {
			this.constantsFiles.add(constantsFile);
			return this;
		}

		/**
		 * Shortcut for {@link #addConstantsToOntology(File)}.
		 */
		public Builder addConstantsToOntology(
				@SuppressWarnings("hiding") List<File> constantsFiles) {
			this.constantsFiles.addAll(constantsFiles);
			return this;
		}

		public LogicLanguageServices build() throws IOException {
			// Basic predicates
			final LogicalConstant conjunctionPredicate = LogicalConstant.read(
					"and:<t*,t>", typeRepository);
			final LogicalConstant disjunctionPredicate = LogicalConstant.read(
					"or:<t*,t>", typeRepository);
			final LogicalConstant negationPredicate = LogicalConstant.read(
					"not:<t,t>", typeRepository);
			final LogicalConstant indexIncreasePredicate = LogicalConstant
					.read("inc:<" + typeRepository.getIndexType().getName()
							+ "," + typeRepository.getIndexType().getName()
							+ ">", typeRepository);

			// Special constants
			final LogicalConstant trueConstant = LogicalConstant.create(
					"true:t", typeRepository.getTruthValueType(), false);
			final LogicalConstant falseConstant = LogicalConstant.create(
					"false:t", typeRepository.getTruthValueType(), false);

			// Create the ontology if using one
			final Ontology ontology;
			if (useOntology) {
				final Set<LogicalConstant> constants = readConstantsFromFiles(
						constantsFiles, typeRepository);
				// Add all the above mentioned constants.
				constants.add(conjunctionPredicate);
				constants.add(disjunctionPredicate);
				constants.add(negationPredicate);
				constants.add(indexIncreasePredicate);
				constants.add(trueConstant);
				constants.add(falseConstant);
				ontology = new Ontology(constants, ontologyClosed);
			} else {
				ontology = null;
				if (ontologyClosed || !constantsFiles.isEmpty()) {
					throw new IllegalArgumentException(
							"Requested close ontology or constant files provided, but no ontology is used.");
				}
			}

			return new LogicLanguageServices(typeRepository, numeralTypeName,
					typeComparator, ontology, conjunctionPredicate,
					disjunctionPredicate, negationPredicate,
					indexIncreasePredicate, trueConstant, falseConstant,
					printer, comparator);
		}

		/**
		 * Create a closed ontology. {@link LogicalConstant} objects will be
		 * re-used and the set of available constants is closed once the LLS is
		 * built.
		 */
		public Builder closeOntology(boolean isClosed) {
			this.ontologyClosed = isClosed;
			return this;
		}

		public Builder setComparator(ILogicalExpressionComparator comparator) {
			this.comparator = comparator;
			return this;
		}

		/**
		 * Set the type used for numerical objects in the logical system. This
		 * type is used to convert such objects to numbers using
		 * {@link LogicLanguageServices#intToLogicalExpression(int)} and
		 * {@link LogicLanguageServices#logicalExpressionToInteger(LogicalExpression)}
		 * .
		 */
		public Builder setNumeralTypeName(String numeralTypeName) {
			this.numeralTypeName = numeralTypeName;
			return this;
		}

		public Builder setPrinter(ILogicalExpressionPrinter printer) {
			this.printer = printer;
			return this;
		}

		public Builder setUseOntology(boolean useOntology) {
			this.useOntology = useOntology;
			return this;
		}
	}

}
