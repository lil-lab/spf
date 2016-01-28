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
package edu.cornell.cs.nlp.spf.parser.ccg.lambda.pruning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import edu.cornell.cs.nlp.spf.base.collections.AllPairs;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.filter.IParsingFilterFactory;
import edu.cornell.cs.nlp.utils.counter.Counter;
import edu.cornell.cs.nlp.utils.function.PredicateUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Parsing filter factory that assumes access to a gold label logical form. The
 * created filters use count co-occurrence statistics to prune categories during
 * parsing. These filters require access to the annotated logical form and are
 * used only during learning.
 *
 * @author Yoav Artzi
 */
public class SupervisedFilterFactory<DI extends ILabeledDataItem<?, LogicalExpression>>
		implements IParsingFilterFactory<DI, LogicalExpression> {
	public static final ILogger						LOG					= LoggerFactory
			.create(SupervisedFilterFactory.class);
	private static final long						serialVersionUID	= -5582633357265257674L;
	private final Predicate<LogicalExpression>		argumentFilter;
	/**
	 * Filter to ignore certain constants. Should return 'true' for each
	 * constant that should be counted.
	 */
	private final Predicate<LogicalConstant>		constantFilter;

	private final UnaryOperator<LogicalConstant>	constantPreprocessor;

	public SupervisedFilterFactory(
			final Predicate<LogicalConstant> constantFilter) {
		this(constantFilter, PredicateUtils.alwaysTrue(),
				UnaryOperator.identity());
	}

	public SupervisedFilterFactory(
			final Predicate<LogicalConstant> constantFilter,
			Predicate<LogicalExpression> argumentFilter,
			UnaryOperator<LogicalConstant> constantPreprocessor) {
		this.constantPreprocessor = constantPreprocessor;
		this.constantFilter = new ConstantFilter(constantFilter);
		this.argumentFilter = argumentFilter;
	}

	@Override
	public Predicate<ParsingOp<LogicalExpression>> create(DI dataItem) {
		return create(dataItem.getLabel());
	}

	public Predicate<ParsingOp<LogicalExpression>> create(
			LogicalExpression semantics) {
		// Prepare pruning data.
		final CollectStats stats = new CollectStats(constantFilter,
				argumentFilter, constantPreprocessor);
		stats.visit(semantics);

		LOG.debug("Creating filter for: %s", semantics);
		LOG.debug("Constant counts: %s", stats.constants);
		LOG.debug("Predicate-argument counts: %s", stats.predicateArg);
		LOG.debug("Coordination co-occurrence counts: %s",
				stats.coordinationCooc);

		return new SupervisedFilter(stats.constants, stats.predicateArg,
				stats.coordinationCooc, constantFilter, argumentFilter,
				constantPreprocessor);
	}

	public static class ArgArgTriplet {
		private final int				hashCode;
		private final LogicalConstant	head1;
		private final LogicalConstant	head2;
		private final LogicalExpression	predicate;

		public ArgArgTriplet(LogicalConstant head1, LogicalConstant head2,
				LogicalExpression predicate) {
			this.head1 = head1;
			this.head2 = head2;
			this.predicate = predicate;
			this.hashCode = calcHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			// if (this == obj) {
			// return true;
			// }
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ArgArgTriplet other = (ArgArgTriplet) obj;

			// First compare 1 to 1 and 2 to 2, if that fails, try 1 to 2 and 2
			// to 1.
			if ((!head1.equals(other.head1) || !head2.equals(other.head2))
					&& (!head1.equals(other.head2)
							|| !head2.equals(other.head1))) {
				return false;
			}

			if (predicate == null) {
				if (other.predicate != null) {
					return false;
				}
			} else if (!predicate.equals(other.predicate)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return "ArgArgTriplet [head1=" + head1 + ", head2=" + head2
					+ ", predicate=" + predicate + "]";
		}

		private int calcHashCode() {
			final int prime = 31;
			int result = 1;
			// Modified this hash code computation so objects that flip head1
			// and head2 still compute the same code.
			result = prime * 1 + (head1 == null ? 0 : head1.hashCode())
					+ (head2 == null ? 0 : head2.hashCode());
			result = prime * result
					+ (predicate == null ? 0 : predicate.hashCode());
			return result;
		}

	}

	/**
	 * Collect various statistics from a logical form:
	 * <ul>
	 * <li>Counts of triplets of (1) predicate head constant, (2) argument head
	 * constant and (3) position of argument (if order sensitive).</li>
	 * <li>{@link LogicalConstant} counts.</li>
	 * <li>Counts of triplets of (1) coordination predicate (disjunction or
	 * conjunction), (2) argument head and (3) another argument head.
	 * </ul>
	 *
	 * @author Yoav Artzi
	 * @see SupervisedFilterFactory#getHeadConst(LogicalExpression)
	 */
	public static class CollectStats implements ILogicalExpressionVisitor {
		private final Predicate<LogicalExpression>		argumentFilter;
		private final UnaryOperator<LogicalConstant>	constantPreprocessor;
		private final Map<LogicalConstant, Counter>		constants			= new HashMap<LogicalConstant, Counter>();
		private final Map<LogicalConstant, Counter>		constantsRef;
		private final Map<ArgArgTriplet, Counter>		coordinationCooc	= new HashMap<ArgArgTriplet, Counter>();
		private final Map<ArgArgTriplet, Counter>		coordinationCoocRef;
		/**
		 * Filter to ignore certain constants. Should return 'true' for each
		 * constant that should be counted.
		 */
		private final Predicate<LogicalConstant>		filter;
		private boolean									isValid				= true;
		private final Map<PredArgTriplet, Counter>		predicateArg		= new HashMap<PredArgTriplet, Counter>();
		private final Map<PredArgTriplet, Counter>		predicateArgRef;

		public CollectStats(Predicate<LogicalConstant> filter,
				Predicate<LogicalExpression> argumentFilter,
				Map<LogicalConstant, Counter> constantsRef,
				Map<ArgArgTriplet, Counter> coordinationCoocRef,
				Map<PredArgTriplet, Counter> predicateArgRef,
				UnaryOperator<LogicalConstant> constantPreprocessor) {
			this.filter = filter;
			this.argumentFilter = argumentFilter;
			this.constantsRef = constantsRef;
			this.coordinationCoocRef = coordinationCoocRef;
			this.predicateArgRef = predicateArgRef;
			this.constantPreprocessor = constantPreprocessor;
		}

		public CollectStats(Predicate<LogicalConstant> filter,
				Predicate<LogicalExpression> argumentFilter,
				UnaryOperator<LogicalConstant> constantPreprocessor) {
			this(filter, argumentFilter, null, null, null,
					constantPreprocessor);
		}

		@Override
		public void visit(Lambda lambda) {
			lambda.getArgument().accept(this);
			if (isValid) {
				lambda.getBody().accept(this);
			}
		}

		@Override
		public void visit(Literal literal) {
			literal.getPredicate().accept(this);
			if (!isValid) {
				return;
			}
			final LogicalExpression predicate = literal
					.getPredicate() instanceof LogicalConstant
							? constantPreprocessor.apply(
									(LogicalConstant) literal.getPredicate())
							: literal.getPredicate();
			int i = 0;
			final LogicalConstant predicateHead = getHeadConst(predicate);
			// Don't count for predicates that may disappear later by
			// simplifying the logical expression.
			final boolean collectPredicateArgStats = predicate instanceof LogicalConstant
					&& filter.test((LogicalConstant) predicate);
			final int numArgs = literal.numArgs();
			for (int j = 0; j < numArgs; ++j) {
				final LogicalExpression arg = literal.getArg(j);
				if (collectPredicateArgStats && argumentFilter.test(arg)) {
					final LogicalConstant argHead = getHeadConst(arg);
					// Skip arguments that might disappear later by simplifying
					// the logical expression, but do visit them to check their
					// sub-expressions.
					if (argHead != null && filter.test(argHead)) {
						final PredArgTriplet triplet = new PredArgTriplet(
								predicateHead, argHead, i);
						count(triplet, predicateArg, predicateArgRef);
						if (!isValid) {
							return;
						}
					}
				}
				arg.accept(this);
				if (!isValid) {
					return;
				}
				if (literal.getPredicateType().isOrderSensitive()) {
					// Increase the index for the next argument.
					++i;
				}
			}
			// Collect coordination co-occurrence counts.
			if (LogicLanguageServices.isCoordinationPredicate(predicate)) {

				for (final List<LogicalExpression> subset : new AllPairs<LogicalExpression>(
						literal.argumentCopy())) {
					assert subset
							.size() == 2 : "Subset must be a pair -- probably a bug in PowerSetWithFixedSize";
					if (argumentFilter.test(subset.get(0))
							&& argumentFilter.test(subset.get(1))) {
						final LogicalConstant head1 = getHeadConst(
								subset.get(0));
						final LogicalConstant head2 = getHeadConst(
								subset.get(1));
						if (head1 != null && head2 != null && filter.test(head1)
								&& filter.test(head2)) {
							final ArgArgTriplet triplet = new ArgArgTriplet(
									head1, head2, predicateHead);
							count(triplet, coordinationCooc,
									coordinationCoocRef);
							if (!isValid) {
								return;
							}
						}
					}
				}
			}
		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			final LogicalConstant processed = constantPreprocessor
					.apply(logicalConstant);
			if (filter.test(processed)) {
				count(processed, constants, constantsRef);
			}
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			// Nothing to do
		}

		private <S> void count(S object, Map<S, Counter> counters,
				Map<S, Counter> reference) {
			final Counter counter = counters.get(object);
			Counter refCounter = null;
			if (counter == null) {
				// Case new object.
				if (reference == null || reference.containsKey(object)) {
					// Case the object is known to the reference map, so it must
					// have a value of at least 1. Add it to the current
					// counters map.
					counters.put(object, new Counter(1));
				} else {
					// Case the object is not known to the reference map, so
					// invalidate the expression.
					LOG.debug("Invalid - unexpected %s", object);
					isValid = false;
				}
			} else if (reference == null || reference.containsKey(object)
					&& (refCounter = reference.get(object)).value() > counter
							.value()) {
				// Case the reference is null, or the reference counter is
				// bigger than the current counter.
				counter.inc();
			} else {
				// Case object which is not included in the reference, so it
				// invalidates the expression. At this point, it's guaranteed
				// that the reference counter exists.
				LOG.debug(
						"Invalid - %s expected %d times, but observed %d times",
						object, refCounter == null ? -1 : refCounter.value(),
						counter.value() + 1);
				isValid = false;
			}
		}

		/**
		 * Returns the pre-processed (see constantPreprocessor) head constant.
		 */
		private LogicalConstant getHeadConst(LogicalExpression exp) {
			if (exp instanceof LogicalConstant) {
				return constantPreprocessor.apply((LogicalConstant) exp);
			} else if (exp instanceof Literal) {
				return getHeadConst(((Literal) exp).getPredicate());
			} else if (exp instanceof Lambda) {
				return getHeadConst(((Lambda) exp).getBody());
			} else if (exp instanceof Variable) {
				return null;
			} else {
				throw new IllegalStateException(
						"unknown logical expression class");
			}
		}
	}

	public static class Creator<DI extends ILabeledDataItem<Sentence, LogicalExpression>>
			implements IResourceObjectCreator<SupervisedFilterFactory<DI>> {

		private String type;

		public Creator() {
			this("parser.filter.supervised");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public SupervisedFilterFactory<DI> create(Parameters params,
				IResourceRepository repo) {
			return new SupervisedFilterFactory<DI>(
					params.contains("ignoreFilter")
							? repo.get(params.get("ignoreFilter"))
							: PredicateUtils.alwaysTrue(),
					PredicateUtils.alwaysTrue(),
					params.contains("constantPreprocessor")
							? repo.get(params.get("constantPreprocessor"))
							: UnaryOperator.identity());
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					SupervisedFilterFactory.class)
							.setDescription(
									"Filter factory for filters used for pruning using a labeled logical form")
							.addParam("ignoreFilter", Predicate.class,
									"Filter used to ignore logical constant when collecting statistics (default: allow all)")
							.addParam("constantPreprocessor",
									UnaryOperator.class,
									"Pre-processor for logical constants. Applied before statistics collection (default: identity function)")
							.build();
		}

	}

	public static class PredArgTriplet {
		private final LogicalConstant	argPredicate;
		private final int				hashCode;
		private final LogicalConstant	headPredicate;
		private final int				position;

		private PredArgTriplet(LogicalConstant headPredicate,
				LogicalConstant argPredicate, int position) {
			this.headPredicate = headPredicate;
			this.argPredicate = argPredicate;
			this.position = position;
			this.hashCode = calcHashCode();
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
			final PredArgTriplet other = (PredArgTriplet) obj;
			if (position != other.position) {
				return false;
			}
			if (argPredicate == null) {
				if (other.argPredicate != null) {
					return false;
				}
			} else if (!argPredicate.equals(other.argPredicate)) {
				return false;
			}
			if (headPredicate == null) {
				if (other.headPredicate != null) {
					return false;
				}
			} else if (!headPredicate.equals(other.headPredicate)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return "PredArgTriplet [argPredicate=" + argPredicate
					+ ", headPredicate=" + headPredicate + ", position="
					+ position + "]";
		}

		private int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (argPredicate == null ? 0 : argPredicate.hashCode());
			result = prime * result
					+ (headPredicate == null ? 0 : headPredicate.hashCode());
			result = prime * result + position;
			return result;
		}

	}

	public static class SupervisedFilter
			implements Predicate<ParsingOp<LogicalExpression>> {

		private final Predicate<LogicalExpression>		argumentFilter;
		private final Predicate<LogicalConstant>		constantFilter;
		private final UnaryOperator<LogicalConstant>	constantPreprocessor;
		private final Map<LogicalConstant, Counter>		constants;
		private final Map<ArgArgTriplet, Counter>		coordinationCooc;
		private final Map<PredArgTriplet, Counter>		predicateArg;

		private SupervisedFilter(Map<LogicalConstant, Counter> constants,
				Map<PredArgTriplet, Counter> predicateArg,
				Map<ArgArgTriplet, Counter> coordinationCooc,
				Predicate<LogicalConstant> constantFilter,
				Predicate<LogicalExpression> argumentFilter,
				UnaryOperator<LogicalConstant> constantPreprocessor) {
			this.constants = constants;
			this.predicateArg = predicateArg;
			this.coordinationCooc = coordinationCooc;
			this.constantFilter = constantFilter;
			this.argumentFilter = argumentFilter;
			this.constantPreprocessor = constantPreprocessor;
		}

		@Override
		public boolean test(ParsingOp<LogicalExpression> op) {
			LOG.debug("Validating %s", op);
			if (op.getCategory().getSemantics() != null) {
				final CollectStats stats = new CollectStats(constantFilter,
						argumentFilter, constants, coordinationCooc,
						predicateArg, constantPreprocessor);
				stats.visit(op.getCategory().getSemantics());
				return stats.isValid;
			} else {
				return true;
			}
		}

	}

	/**
	 * Use a separate class to keep the filter {@link Serializable}.
	 *
	 * @author Yoav Artzi
	 */
	private static class ConstantFilter
			implements Predicate<LogicalConstant>, Serializable {

		private static final long					serialVersionUID	= 7549147645985747290L;
		private final Predicate<LogicalConstant>	baseFilter;

		public ConstantFilter(Predicate<LogicalConstant> baseFilter) {
			this.baseFilter = baseFilter;
		}

		@Override
		public boolean test(LogicalConstant constant) {
			return !(LogicLanguageServices.isCollpasibleConstant(constant)
					|| LogicLanguageServices.isArrayIndexPredicate(constant)
					|| constant.getType().equals(SkolemServices.getIDType()))
					&& baseFilter.test(constant);
		}

	}
}
