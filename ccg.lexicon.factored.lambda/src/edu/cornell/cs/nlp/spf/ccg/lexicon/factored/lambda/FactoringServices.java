package edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.google.common.base.Function;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.counter.Counter;

public class FactoringServices {
	public static final String									SYNTAX_PLACEHOLDER_PREFIX	= "PLHD";

	private static FactoringServices							INSTANCE					= null;

	private final Function<LogicalConstant, Boolean>			filter;

	private final boolean										hashCodeOrdering;

	private final UnaryOperator<Category<LogicalExpression>>	preprocessor;

	private final Set<LogicalConstant>							unfactoredConstants;

	private FactoringServices(Function<LogicalConstant, Boolean> filter,
			Set<LogicalConstant> unfactoredConstants,
			UnaryOperator<Category<LogicalExpression>> preprocessor,
			boolean hashCodeOrdering) {
		this.filter = filter;
		this.preprocessor = preprocessor;
		this.hashCodeOrdering = hashCodeOrdering;
		this.unfactoredConstants = Collections
				.unmodifiableSet(unfactoredConstants);
	}

	public static FactoredLexicalEntry factor(
			LexicalEntry<LogicalExpression> entry) {
		if (entry instanceof FactoredLexicalEntry) {
			// Case already a factored lexical entry, cast and return
			return (FactoredLexicalEntry) entry;
		} else {
			// we need to compute the maximal factoring and return it
			return FactoringServices.factor(entry, true, false, 0).get(0);
		}
	}

	public static List<FactoredLexicalEntry> factor(
			final LexicalEntry<LogicalExpression> entry, boolean doMaximal,
			boolean doPartial, int maxConstantsInPartial) {

		// Abstract syntactic attributes.
		final Pair<List<String>, Syntax> syntaxAbstractionPair = abstractSyntaxAttributes(
				entry.getCategory().getSyntax());
		final List<String> indexedAttributes = syntaxAbstractionPair.first();
		final Category<LogicalExpression> category;
		if (syntaxAbstractionPair.second() == entry.getCategory().getSyntax()) {
			category = INSTANCE.preprocessor.apply(entry.getCategory());
		} else {
			category = INSTANCE.preprocessor
					.apply(Category.create(syntaxAbstractionPair.second(),
							entry.getCategory().getSemantics()));
		}

		final List<Pair<List<LogicalConstant>, LexicalTemplate>> factoring = FactoringServices
				.doFactoring(category, doMaximal, doPartial,
						maxConstantsInPartial, entry.getProperties(),
						indexedAttributes.size());

		return ListUtils.map(factoring,
				obj -> new FactoredLexicalEntry(entry.getTokens(),
						entry.getCategory(),
						new Lexeme(entry.getTokens(), obj.first(),
								indexedAttributes, entry.getProperties()),
				obj.second(), entry.isDynamic(), entry.getProperties()));
	}

	public static boolean isFactorable(LogicalConstant constant) {
		return !LogicLanguageServices.isCoordinationPredicate(constant)
				&& !LogicLanguageServices.isArrayIndexPredicate(constant)
				&& !LogicLanguageServices.isArraySubPredicate(constant)
				&& !LogicLanguageServices.getTypeRepository().getIndexType()
						.equals(constant.getType())
				&& INSTANCE.filter.apply(constant)
				&& !INSTANCE.unfactoredConstants.contains(constant);
	}

	public static void set(FactoringServices services) {
		INSTANCE = services;
	}

	private static Pair<List<String>, Syntax> abstractSyntaxAttributes(
			Syntax syntax) {
		final List<String> indexedAttributes = new LinkedList<String>();
		return Pair.of(indexedAttributes,
				abstractSyntaxAttributes(syntax, indexedAttributes));
	}

	private static Syntax abstractSyntaxAttributes(Syntax syntax,
			List<String> indexedAttributes) {
		if (syntax instanceof ComplexSyntax) {
			final ComplexSyntax complex = (ComplexSyntax) syntax;
			final Syntax left = abstractSyntaxAttributes(complex.getLeft(),
					indexedAttributes);
			final Syntax right = abstractSyntaxAttributes(complex.getRight(),
					indexedAttributes);
			// Both sides can't be null -- otherwise it's a bug.
			assert left != null;
			assert right != null;
			if (left == complex.getLeft() && right == complex.getRight()) {
				return syntax;
			} else {
				return new ComplexSyntax(left, right, complex.getSlash());
			}
		} else if (syntax instanceof SimpleSyntax) {
			final SimpleSyntax simple = (SimpleSyntax) syntax;
			final String attribute = simple.getAttribute();
			if (attribute != null && !simple.hasAttributeVariable()) {
				final int index = indexedAttributes.indexOf(attribute);
				if (index < 0) {
					indexedAttributes.add(attribute);
					return simple
							.cloneWithAttribute(indexToAttributePlaceholder(
									indexedAttributes.size() - 1));
				} else {
					return simple.cloneWithAttribute(
							indexToAttributePlaceholder(index));
				}
			} else {
				return syntax;
			}
		} else {
			throw new RuntimeException("unexpected syntax class");
		}
	}

	private static List<Pair<List<LogicalConstant>, LexicalTemplate>> doFactoring(
			final Category<LogicalExpression> inputCategory, boolean doMaximal,
			boolean doPartial, int maxConstantsInPartial,
			final Map<String, String> properties, final int numAttributes) {
		if (inputCategory.getSemantics() == null) {
			return ListUtils.createSingletonList(
					Pair.of(Collections.<LogicalConstant> emptyList(),
							new LexicalTemplate(
									Collections.<LogicalConstant> emptyList(),
									numAttributes, inputCategory, properties)));
		}

		final Set<Pair<AbstractConstants.Placeholders, ? extends LogicalExpression>> factoring = AbstractConstants
				.of(inputCategory.getSemantics(), doMaximal, doPartial,
						maxConstantsInPartial);
		return factoring.stream()
				.map(obj -> Pair
						.of(Collections.unmodifiableList(obj.first().originals),
								new LexicalTemplate(
										Collections.unmodifiableList(
												obj.first().placeholders),
								numAttributes,
								inputCategory.cloneWithNewSemantics(
										obj.second()), properties)))
				.collect(Collectors.toList());
	}

	private static String indexToAttributePlaceholder(int index) {
		return SYNTAX_PLACEHOLDER_PREFIX + (char) ('a' + index);
	}

	static int attributePlacehoderToIndex(String placeholder) {
		assert placeholder
				.startsWith(FactoringServices.SYNTAX_PLACEHOLDER_PREFIX)
				&& placeholder
						.length() == FactoringServices.SYNTAX_PLACEHOLDER_PREFIX
								.length() + 1 : String.format(
										"Expected syntactic attribute placeholder, instead: %s",
										placeholder);
		return placeholder.charAt(SYNTAX_PLACEHOLDER_PREFIX.length()) - 'a';
	}

	public static class AbstractConstants implements ILogicalExpressionVisitor {
		private final Map<Type, Counter>								counters	= new HashMap<Type, Counter>();
		private final boolean											doMaximal;
		private final boolean											doPartial;
		private final int												partialMaxConstants;
		private List<Pair<Placeholders, ? extends LogicalExpression>>	tempReturn	= null;

		private AbstractConstants(boolean doMaximal, boolean doPartial,
				int partialMaxConstants) {
			// Usage only through static 'of' method
			this.doMaximal = doMaximal;
			this.doPartial = doPartial;
			this.partialMaxConstants = partialMaxConstants;
		}

		public static Set<Pair<Placeholders, ? extends LogicalExpression>> of(
				LogicalExpression exp, boolean getMaximal, boolean getPartial,
				int partialMaxConstants) {
			final AbstractConstants visitor = new AbstractConstants(getMaximal,
					getPartial, partialMaxConstants);
			visitor.visit(exp);

			// Remove any empty factoring, unless it's a maximal one
			final Iterator<Pair<Placeholders, ? extends LogicalExpression>> iterator = visitor.tempReturn
					.iterator();
			while (iterator.hasNext()) {
				final Pair<Placeholders, ? extends LogicalExpression> pair = iterator
						.next();
				if (!pair.first().isMaximal() && pair.first().size() == 0) {
					iterator.remove();
				}
			}

			return new HashSet<Pair<Placeholders, ? extends LogicalExpression>>(
					visitor.tempReturn);
		}

		private static Pair<Placeholders, ? extends LogicalExpression> getAndRemoveMaximal(
				List<Pair<Placeholders, ? extends LogicalExpression>> pairs) {
			Pair<Placeholders, ? extends LogicalExpression> maximal = null;
			final Iterator<Pair<Placeholders, ? extends LogicalExpression>> iterator = pairs
					.iterator();
			while (iterator.hasNext()) {
				final Pair<Placeholders, ? extends LogicalExpression> pair = iterator
						.next();
				if (pair.first().isMaximal()) {
					if (maximal == null) {
						maximal = pair;
						iterator.remove();
					} else {
						throw new IllegalStateException(
								"found more than one maximal");
					}
				}
			}

			if (maximal == null) {
				throw new IllegalStateException(
						"expected a maximal pair, not found");
			}

			return maximal;
		}

		@Override
		public void visit(Lambda lambda) {
			// not visiting argument, since we are only abstracting constants.
			lambda.getBody().accept(this);
			final ListIterator<Pair<Placeholders, ? extends LogicalExpression>> iterator = tempReturn
					.listIterator();
			while (iterator.hasNext()) {
				final Pair<Placeholders, ? extends LogicalExpression> pair = iterator
						.next();
				if (pair.second() != null) {
					final LogicalExpression newBody = pair.second();
					if (newBody == lambda.getBody()) {
						iterator.set(Pair.of(pair.first(), lambda));
					} else {
						iterator.set(Pair.of(pair.first(),
								new Lambda(lambda.getArgument(), newBody)));
					}

				}
			}
		}

		@Override
		public void visit(Literal literal) {
			// Visit the predicate
			literal.getPredicate().accept(this);
			final List<Pair<Placeholders, ? extends LogicalExpression>> predicateReturn = tempReturn;

			final LogicalExpression[] args = literal.argumentCopy();

			final List<List<Pair<Placeholders, ? extends LogicalExpression>>> argReturns = new ArrayList<List<Pair<Placeholders, ? extends LogicalExpression>>>(
					literal.numArgs());

			// In case of an order insensitive, sort the arguments by hashcode,
			// so the abstraction of constants will be insensitive to order,
			// when that order doesn't matter. TODO [yoav] [limitations] this
			// solution is still not perfect and might cause duplicate
			// templates/lexemes where such shouldn't exist. To fix it, we need
			// to change lexemes to hold a set of constants and not a list
			// (this, in turn, will cause difficulties in init templates).
			// However, it does allow for better stability, which is beneficial
			// in some cases.
			if (INSTANCE.hashCodeOrdering) {
				if (!literal.getPredicateType().isOrderSensitive()) {
					Arrays.sort(args,
							(l1, l2) -> l1.hashCode() - l2.hashCode());
				}
			}

			for (final LogicalExpression arg : args) {
				arg.accept(this);
				argReturns.add(tempReturn);
			}

			tempReturn = new LinkedList<Pair<Placeholders, ? extends LogicalExpression>>();

			if (doMaximal) {
				// Do the maximal combination by getting all the maximals.
				// Each returned list should have a single maximal, no more, no
				// less. The maximal is also removed to make it simpler to do
				// the partial ones later on.
				final Pair<Placeholders, ? extends LogicalExpression> predPair = getAndRemoveMaximal(
						predicateReturn);
				final List<Pair<Placeholders, ? extends LogicalExpression>> argPairs = ListUtils
						.map(argReturns, obj -> getAndRemoveMaximal(obj));
				final Placeholders placeholder = predPair.first();
				int i = 0;
				boolean argsChanged = false;
				final LogicalExpression[] newArgs = new LogicalExpression[args.length];
				for (final Pair<Placeholders, ? extends LogicalExpression> argPair : argPairs) {
					placeholder.concat(argPair.first());
					newArgs[i] = argPair.second();
					if (args[i] != argPair.second()) {
						argsChanged = true;
					}
					++i;
				}
				if (argsChanged
						|| predPair.second() != literal.getPredicate()) {
					tempReturn.add(Pair.of(placeholder,
							new Literal(
									predPair.second() == literal.getPredicate()
											? literal.getPredicate()
											: predPair.second(),
									newArgs)));
				} else {
					tempReturn.add(Pair.of(placeholder, literal));
				}

			}

			if (doPartial) {
				// At this point, if maximal pairs were present, they were
				// removed
				for (final Pair<Placeholders, ? extends LogicalExpression> predPair : predicateReturn) {
					for (final List<Pair<Placeholders, ? extends LogicalExpression>> argPairs : CollectionUtils
							.cartesianProduct(argReturns)) {
						final Placeholders placeholder = new Placeholders();
						placeholder.concat(predPair.first());
						int i = 0;
						boolean argsChanged = false;
						final LogicalExpression[] newArgs = new LogicalExpression[args.length];
						boolean fail = false;
						for (final Pair<Placeholders, ? extends LogicalExpression> argPair : argPairs) {
							if (placeholder.size() + argPair.first()
									.size() <= partialMaxConstants) {
								placeholder.concat(argPair.first());
								newArgs[i] = argPair.second();
								if (args[i] != argPair.second()) {
									argsChanged = true;
								}
								++i;
							} else {
								fail = true;
								break;
							}
						}
						if (!fail) {
							if (argsChanged || predPair.second() != literal
									.getPredicate()) {
								tempReturn.add(Pair.of(placeholder, new Literal(
										predPair.second() == literal
												.getPredicate()
														? literal.getPredicate()
														: predPair.second(),
										newArgs)));
							} else {
								tempReturn.add(Pair.of(placeholder, literal));
							}
						}
					}
				}
			}
		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			if (isFactorable(logicalConstant)) {
				tempReturn = new ArrayList<Pair<Placeholders, ? extends LogicalExpression>>(
						3);

				if (doPartial) {
					// No factoring (empty) placeholder
					final Pair<Placeholders, ? extends LogicalExpression> noFactoringPair = Pair
							.of(new Placeholders(), logicalConstant);
					tempReturn.add(noFactoringPair);
					// Partial factoring placeholder
					final Placeholders factoringPlaceholder = new Placeholders();
					final Pair<Placeholders, ? extends LogicalExpression> factoringPair = Pair
							.of(factoringPlaceholder,
									factoringPlaceholder.add(logicalConstant));
					tempReturn.add(factoringPair);
				}

				if (doMaximal) {
					// Maximal factoring placeholder
					final Placeholders factoringPlaceholder = new Placeholders(
							true);
					final Pair<Placeholders, ? extends LogicalExpression> factoringPair = Pair
							.of(factoringPlaceholder,
									factoringPlaceholder.add(logicalConstant));
					tempReturn.add(factoringPair);
				}
				final Type genType = LogicLanguageServices.getTypeRepository()
						.generalizeType(logicalConstant.getType());
				if (counters.containsKey(genType)) {
					counters.get(genType).inc();
				} else {
					counters.put(genType, new Counter(1));
				}
			} else {
				// No factoring, only empty placeholders

				tempReturn = new ArrayList<Pair<Placeholders, ? extends LogicalExpression>>(
						2);

				if (doPartial) {
					// No factoring (empty) placeholder
					final Pair<Placeholders, ? extends LogicalExpression> noFactoringPair = Pair
							.of(new Placeholders(), logicalConstant);
					tempReturn.add(noFactoringPair);
				}

				if (doMaximal) {
					// Maximal factoring (empty) placeholder
					final Pair<Placeholders, ? extends LogicalExpression> factoringPair = Pair
							.of(new Placeholders(true), logicalConstant);
					tempReturn.add(factoringPair);
				}

			}
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			tempReturn = new ArrayList<Pair<Placeholders, ? extends LogicalExpression>>(
					2);

			// No factoring (empty) placeholder: maximal
			if (doMaximal) {
				final Pair<Placeholders, ? extends LogicalExpression> p = Pair
						.of(new Placeholders(true), variable);
				tempReturn.add(p);
			}

			// No factoring (empty) placeholder: partial
			if (doMaximal) {
				final Pair<Placeholders, ? extends LogicalExpression> p = Pair
						.of(new Placeholders(), variable);
				tempReturn.add(p);
			}

		}

		public class Placeholders {
			private final boolean		maximal;
			final List<LogicalConstant>	originals		= new LinkedList<LogicalConstant>();
			final List<LogicalConstant>	placeholders	= new LinkedList<LogicalConstant>();

			public Placeholders() {
				this(false);
			}

			public Placeholders(boolean maximal) {
				this.maximal = maximal;
			}

			public LogicalConstant add(LogicalConstant original) {
				assert original != null;
				originals.add(original);
				final LogicalConstant placeholder = makeConstant(
						original.getType());
				assert placeholder != null;
				placeholders.add(placeholder);
				return placeholder;
			}

			public void concat(Placeholders other) {
				this.originals.addAll(other.originals);
				this.placeholders.addAll(other.placeholders);
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
				final Placeholders other = (Placeholders) obj;
				if (!getOuterType().equals(other.getOuterType())) {
					return false;
				}
				if (originals == null) {
					if (other.originals != null) {
						return false;
					}
				} else if (!originals.equals(other.originals)) {
					return false;
				}
				if (placeholders == null) {
					if (other.placeholders != null) {
						return false;
					}
				} else if (!placeholders.equals(other.placeholders)) {
					return false;
				}
				return true;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result
						+ (originals == null ? 0 : originals.hashCode());
				result = prime * result
						+ (placeholders == null ? 0 : placeholders.hashCode());
				return result;
			}

			public boolean isMaximal() {
				return maximal;
			}

			public int size() {
				return originals.size();
			}

			@Override
			public String toString() {
				return originals + (maximal ? " M-> " : " -> ") + placeholders;
			}

			private AbstractConstants getOuterType() {
				return AbstractConstants.this;
			}

			private LogicalConstant makeConstant(Type type) {
				final Type generalType = LogicLanguageServices
						.getTypeRepository().generalizeType(type);

				return LogicalConstant.createDynamic("#"
						+ (counters.containsKey(generalType)
								? counters.get(generalType).value() : 0)
						+ generalType, generalType, true);
			}
		}

	}

	public static class Builder {

		private Function<LogicalConstant, Boolean>			filter				= input -> true;
		/**
		 * An ugly fix to try and make the same category always factor in the
		 * same way. However, it does unpleasant side effects (as in the case of
		 * AMR, where it's disabled).
		 */
		private boolean										hashCodeOrdering	= true;
		private UnaryOperator<Category<LogicalExpression>>	preprocessor		= UnaryOperator
				.identity();

		private final Set<LogicalConstant>					unfactoredConstants	= new HashSet<LogicalConstant>();

		public Builder addConstant(LogicalConstant unfactoredConstant) {
			unfactoredConstants.add(unfactoredConstant);
			return this;
		}

		public FactoringServices build() {
			return new FactoringServices(filter, unfactoredConstants,
					preprocessor, hashCodeOrdering);
		}

		public Builder setFilter(Function<LogicalConstant, Boolean> filter) {
			this.filter = filter;
			return this;
		}

		public Builder setHashCodeOrdering(boolean hashCodeOrdering) {
			this.hashCodeOrdering = hashCodeOrdering;
			return this;
		}

		public Builder setPreprocessor(
				UnaryOperator<Category<LogicalExpression>> preprocessor) {
			this.preprocessor = preprocessor;
			return this;
		}

	}

}
