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
package edu.cornell.cs.nlp.spf.parser.ccg.features.lambda;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.collections.AllPairs;
import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetHeadString;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSet;

public class LogicalExpressionCooccurrenceFeatureSet<DI extends IDataItem<?>>
		implements IParseFeatureSet<DI, LogicalExpression> {
	private static final String	FEATURE_TAG			= "LOGCOOC";

	private static final long	serialVersionUID	= 7387260474009084901L;

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return Collections.emptySet();
	}

	@Override
	public void setFeatures(IParseStep<LogicalExpression> parseStep,
			IHashVector feats, DI dataItem) {
		if (!parseStep.isFullParse()) {
			// Only generate logical expression features of the final logical
			// format
			return;
		}

		// Generate deep semantic features
		final IHashVector features = ExtractFeatures
				.of(parseStep.getRoot().getSemantics(), 1.0);
		if (feats != null) {
			features.addTimesInto(1.0, feats);
		}
	}

	public static class Creator<DI extends IDataItem<?>> implements
			IResourceObjectCreator<LogicalExpressionCooccurrenceFeatureSet<DI>> {

		@Override
		public LogicalExpressionCooccurrenceFeatureSet<DI> create(
				Parameters parameters, IResourceRepository resourceRepo) {
			return new LogicalExpressionCooccurrenceFeatureSet<DI>();
		}

		@Override
		public String type() {
			return "feat.logexp.cooc";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					LogicalExpressionCooccurrenceFeatureSet.class)
							.setDescription(
									"Co-occurrence features for constants in complete logical expressions")
							.build();
		}

	}

	/**
	 * Accumulates the following features:
	 * <p>
	 * <li>PREDARG = Cooccurrence of predicate at the head string of its
	 * arguments.</li>
	 * <li>ARGARG = Cooccurrence of a pair of arguments in a literal.
	 * <li>
	 * </p>
	 *
	 * @author Yoav Artzi
	 */
	private static class ExtractFeatures implements ILogicalExpressionVisitor {

		private final IHashVector	features	= HashVectorFactory.create();
		private final double		scale;

		private ExtractFeatures(double scale) {
			this.scale = scale;

		}

		public static IHashVector of(LogicalExpression exp, double scale) {

			final ExtractFeatures visitor = new ExtractFeatures(scale);
			visitor.visit(exp);
			return visitor.features;
		}

		@Override
		public void visit(Lambda lambda) {
			lambda.getArgument().accept(this);
			lambda.getBody().accept(this);
		}

		@Override
		public void visit(Literal literal) {
			final String predicateString = GetHeadString
					.of(literal.getPredicate());
			// Only observe pairs.
			for (final List<LogicalExpression> subset : new AllPairs<LogicalExpression>(
					literal.argumentCopy())) {
				assert subset
						.size() == 2 : "Subset must be a pair -- probably a bug in PowerSetWithFixedSize";
				final String first = GetHeadString.of(subset.get(0));
				final String second = GetHeadString.of(subset.get(1));
				if (first.compareTo(second) >= 0) {
					features.set(FEATURE_TAG, "ARGARG", first, second,
							1.0 * scale);
				} else {
					features.set(FEATURE_TAG, "ARGARG", second, first,
							1.0 * scale);
				}
			}

			final int len = literal.numArgs();
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(i);
				arg.accept(this);

				final String argString = GetHeadString.of(arg);
				features.set(FEATURE_TAG, "PREDARG", predicateString, argString,
						1.0 * scale);
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

}
