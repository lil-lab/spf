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
package edu.cornell.cs.nlp.spf.test.ccg.lambda;

import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsExtendedConstant;
import edu.cornell.cs.nlp.spf.test.stats.ITestingStatistics;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

public class SingleSentencePartialCreditTestingStatistics implements
		ITestingStatistics<Sentence, LogicalExpression, SingleSentence> {
	public static final ILogger	LOG								= LoggerFactory
																		.create(SingleSentencePartialCreditTestingStatistics.class);

	private static final String	DEFAULT_METRIC_NAME				= "PARTIAL_CREDIT";

	private final String		metricName;
	private int					numGoldPartialPairs				= 0;

	private int					numLabelPartialPairs			= 0;

	private int					numMatchedPartialPairs			= 0;

	private final String		prefix;
	private int					skippingNumGoldPartialPairs		= 0;
	private int					skippingNumLabelPartialPairs	= 0;
	private int					skippingNumMatchedPartialPairs	= 0;

	public SingleSentencePartialCreditTestingStatistics() {
		this(DEFAULT_METRIC_NAME);
	}

	public SingleSentencePartialCreditTestingStatistics(String prefix) {
		this(prefix, DEFAULT_METRIC_NAME);
	}

	public SingleSentencePartialCreditTestingStatistics(String prefix,
			String metricName) {
		this.prefix = prefix;
		this.metricName = metricName;
	}

	private static PartialCreditTriplet partialCompare(LogicalExpression gold,
			LogicalExpression label) {
		final Multiset<Pair<? extends LogicalExpression, ? extends LogicalExpression>> goldPairs = GetPredConstPairs
				.of(gold);
		final Multiset<Pair<? extends LogicalExpression, ? extends LogicalExpression>> labelPairs;
		if (label == null) {
			labelPairs = HashMultiset.create();
		} else {
			labelPairs = GetPredConstPairs.of(label);
		}

		// The "intersection" of the gold and label pair sets = the number of
		// matches
		final Multiset<Pair<? extends LogicalExpression, ? extends LogicalExpression>> intersection = HashMultiset
				.create();

		for (final Entry<Pair<? extends LogicalExpression, ? extends LogicalExpression>> entry : goldPairs
				.entrySet()) {
			intersection.setCount(
					entry.getElement(),
					Math.min(entry.getCount(),
							labelPairs.count(entry.getElement())));
		}

		return new PartialCreditTriplet(goldPairs.size(), labelPairs.size(),
				intersection.size());
	}

	@Override
	public void recordNoParse(SingleSentence dataItem) {
		recordParse(dataItem, null);
	}

	@Override
	public void recordNoParseWithSkipping(SingleSentence dataItem) {
		recordParseWithSkipping(dataItem, null);
	}

	@Override
	public void recordParse(SingleSentence dataItem, LogicalExpression candidate) {
		final PartialCreditTriplet partialCreditTriplet = partialCompare(
				dataItem.getLabel(), candidate);

		LOG.info("Partial credit: %s", partialCreditTriplet);
		numGoldPartialPairs += partialCreditTriplet.getGoldPairs();
		numLabelPartialPairs += partialCreditTriplet.getLabelPairs();
		numMatchedPartialPairs += partialCreditTriplet.getMatchedPairs();
	}

	@Override
	public void recordParses(SingleSentence dataItem,
			List<LogicalExpression> candidates) {
		recordNoParse(dataItem);
	}

	@Override
	public void recordParsesWithSkipping(SingleSentence dataItem,
			List<LogicalExpression> candidates) {
		recordNoParseWithSkipping(dataItem);
	}

	@Override
	public void recordParseWithSkipping(SingleSentence dataItem,
			LogicalExpression candidate) {
		final PartialCreditTriplet partialCreditTriplet = partialCompare(
				dataItem.getLabel(), candidate);

		LOG.info("Empty partial credit: %s", partialCreditTriplet);
		skippingNumGoldPartialPairs += partialCreditTriplet.getGoldPairs();
		skippingNumLabelPartialPairs += partialCreditTriplet.getLabelPairs();
		skippingNumMatchedPartialPairs += partialCreditTriplet
				.getMatchedPairs();
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder("=== ").append(
				getMetricName()).append(" statistics:\n");
		ret.append("Recall: ").append(numMatchedPartialPairs).append('/')
				.append(numGoldPartialPairs).append(" = ").append(recall())
				.append('\n');
		ret.append("Precision: ").append(numMatchedPartialPairs).append('/')
				.append(numLabelPartialPairs).append(" = ").append(precision())
				.append('\n');
		ret.append("F1: ").append(f1()).append('\n');
		ret.append("SKIP Recall: ").append(skippingNumMatchedPartialPairs)
				.append('/').append(skippingNumGoldPartialPairs).append(" = ")
				.append(skippingRecall()).append('\n');
		ret.append("SKIP Precision: ").append(skippingNumMatchedPartialPairs)
				.append('/').append(skippingNumLabelPartialPairs).append(" = ")
				.append(skippingPrecision()).append('\n');
		ret.append("SKIP F1: ").append(skippingF1());
		return ret.toString();
	}

	@Override
	public String toTabDelimitedString() {
		final StringBuilder ret = new StringBuilder(getPrefix())
				.append("\tmetric=").append(getMetricName()).append("\t");
		ret.append("recall=").append(recall()).append('\t');
		ret.append("precision=").append(precision()).append('\t');
		ret.append("f1=").append(f1()).append('\t');
		ret.append("skippingRecall=").append(skippingRecall()).append('\t');
		ret.append("skippingPrecision=").append(skippingPrecision())
				.append('\t');
		ret.append("skippingF1=").append(skippingF1());
		return ret.toString();
	}

	private double f1() {
		return precision() + recall() == 0.0 ? 0.0 : 2 * precision() * recall()
				/ (precision() + recall());
	}

	private double precision() {
		return numLabelPartialPairs == 0.0 ? 0.0
				: (double) numMatchedPartialPairs / numLabelPartialPairs;
	}

	private double recall() {
		return numGoldPartialPairs == 0.0 ? 0.0
				: (double) numMatchedPartialPairs / numGoldPartialPairs;
	}

	private double skippingF1() {
		return skippingPrecision() + skippingRecall() == 0.0 ? 0.0 : 2
				* skippingPrecision() * skippingRecall()
				/ (skippingPrecision() + skippingRecall());
	}

	private double skippingPrecision() {
		return skippingNumLabelPartialPairs == 0.0 ? 0.0
				: (double) skippingNumMatchedPartialPairs
						/ skippingNumLabelPartialPairs;
	}

	private double skippingRecall() {
		return skippingNumGoldPartialPairs == 0.0 ? 0.0
				: (double) skippingNumMatchedPartialPairs
						/ skippingNumGoldPartialPairs;
	}

	protected String getMetricName() {
		return metricName;
	}

	protected String getPrefix() {
		return prefix == null ? "" : prefix;
	}

	private static class GetPredConstPairs implements ILogicalExpressionVisitor {
		private final Multiset<Pair<? extends LogicalExpression, ? extends LogicalExpression>>	predConstPairs	= HashMultiset
																														.create();

		/**
		 * Usage only through static 'of' method.
		 */
		private GetPredConstPairs() {
			// Nothing to do
		}

		public static Multiset<Pair<? extends LogicalExpression, ? extends LogicalExpression>> of(
				LogicalExpression exp) {
			final GetPredConstPairs visitor = new GetPredConstPairs();
			visitor.visit(exp);
			return visitor.getPredConstPairs();
		}

		public Multiset<Pair<? extends LogicalExpression, ? extends LogicalExpression>> getPredConstPairs() {
			return predConstPairs;
		}

		@Override
		public void visit(Lambda lambda) {
			lambda.getArgument().accept(this);
			lambda.getBody().accept(this);
		}

		@Override
		public void visit(Literal literal) {
			// Visit the predicate
			literal.getPredicate().accept(this);

			final LogicalExpression pred = literal.getPredicate();
			final int numArgs = literal.numArgs();
			if (!LogicLanguageServices.isCoordinationPredicate(pred)
					&& !LogicLanguageServices.isArrayIndexPredicate(pred)
					&& !LogicLanguageServices.isArraySubPredicate(pred)
					&& literal.getPredicate() instanceof LogicalConstant) {
				if (numArgs == 1
						&& !(literal.getArg(0) instanceof LogicalConstant)) {
					// Unary predicates
					predConstPairs.add(Pair.of(literal.getPredicate(),
							(LogicalExpression) null));
					return;
				} else if (numArgs == 2
						&& !(literal.getArg(0) instanceof LogicalConstant)
						&& IsExtendedConstant.of(literal.getArg(1))) {
					// Binary predicate
					predConstPairs.add(Pair.of(literal.getPredicate(),
							literal.getArg(1)));
					return;
				}
			}

			// Just visit the arguments and predicate
			for (int i = 0; i < numArgs; ++i) {
				literal.getArg(i).accept(this);
			}
		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			// Nothing to do
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			// Nothing to do
		}
	}

	private static class PartialCreditTriplet {
		private final int	goldPairs;
		private final int	labelPairs;
		private final int	matchedPairs;

		public PartialCreditTriplet(int goldPairs, int labelPairs,
				int matchedPairs) {
			this.goldPairs = goldPairs;
			this.labelPairs = labelPairs;
			this.matchedPairs = matchedPairs;
		}

		public int getGoldPairs() {
			return goldPairs;
		}

		public int getLabelPairs() {
			return labelPairs;
		}

		public int getMatchedPairs() {
			return matchedPairs;
		}

		@Override
		public String toString() {
			return "PartialCreditTriplet [goldPairs=" + goldPairs
					+ ", labelPairs=" + labelPairs + ", matchedPairs="
					+ matchedPairs + "]";
		}

	}
}
