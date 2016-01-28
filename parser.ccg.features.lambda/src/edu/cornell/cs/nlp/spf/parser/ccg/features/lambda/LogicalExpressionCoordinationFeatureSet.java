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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetHeadString;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSet;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.counter.Counter;

/**
 * Logical expression feature set.
 *
 * @author Yoav Artzi
 */
public class LogicalExpressionCoordinationFeatureSet<DI extends IDataItem<?>>
		implements IParseFeatureSet<DI, LogicalExpression> {
	private static final String	FEATURE_TAG			= "LOGEXP";

	private static final double	SCALE				= 1.0;

	private static final long	serialVersionUID	= -7190994850951239893L;

	private final boolean		cpapFeatures;

	private final boolean		cpp1Features;
	private final boolean		reptFeatures;

	public LogicalExpressionCoordinationFeatureSet(boolean cpp1Features,
			boolean reptFeatures, boolean cpapFeatures) {
		this.cpp1Features = cpp1Features;
		this.reptFeatures = reptFeatures;
		this.cpapFeatures = cpapFeatures;
	}

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return Collections.emptySet();
	}

	@Override
	public void setFeatures(IParseStep<LogicalExpression> parseStep,
			IHashVector feats, DI dataItem) {
		if (!parseStep.isFullParse()) {
			// Only generate logical expression features of the final logical
			// form.
			return;
		}

		final LogicalExpression sem = parseStep.getRoot().getSemantics();
		if (sem == null) {
			return;
		}

		// Generate deep semantic features
		final IHashVector features = ExtractFeatures.of(sem, cpp1Features,
				cpapFeatures, reptFeatures, SCALE);
		if (feats != null) {
			features.addTimesInto(1.0, feats);
		}
	}

	public static class Creator<DI extends IDataItem<?>> implements
			IResourceObjectCreator<LogicalExpressionCoordinationFeatureSet<DI>> {

		@Override
		public LogicalExpressionCoordinationFeatureSet<DI> create(
				Parameters params, IResourceRepository repo) {
			return new LogicalExpressionCoordinationFeatureSet<DI>(
					"true".equals(params.get("cpp1")), "true".equals(params
							.get("rept")), "true".equals(params.get("cpap")));
		}

		@Override
		public String type() {
			return "feat.logexp.coordination";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					LogicalExpressionCoordinationFeatureSet.class)
					.setDescription(
							"Complete logical expression coordination features")
					.addParam("cpp1", "boolean",
							"Activate CPP1 features (options: true, false) (default: false)")
					.addParam("rept", "boolean",
							"Activate REPT features (options: true, false) (default: false)")
					.addParam("cpap", "boolean",
							"Activate CPAP features (options: true, false) (default: false)")
					.build();
		}

	}

	/**
	 * Extracts the following *binary* features:
	 * <ul>
	 * <li>CPAP = Repeats in coordination of predicate with a certain argument
	 * in the same position, while the literals themselves are different. Aimed
	 * to control predicates such as to:t repeating with similar arguments.</li>
	 * <li>CPP1 = Pair of predicates in coordination that share the first
	 * argument. This is a binary feature. Fires for two identical literals as
	 * well, to control repetitions.</li>
	 * <li>REPT = Counting repeats of literals inside a coordination predicate.</li>
	 * </ul>
	 *
	 * @author Yoav Artzi
	 */
	private static class ExtractFeatures implements ILogicalExpressionVisitor {
		private final boolean		cpapFeatures;
		private final boolean		cpp1Features;
		/**
		 * Some of the features are binaries (e.g. CPAP), so must work on a
		 * local feature vector.
		 */
		private final IHashVector	features	= HashVectorFactory.create();
		private final boolean		reptFeatures;
		private final double		scale;

		/**
		 * Use only through 'of' method.
		 *
		 * @param reptFeatures
		 * @param cpapFeatures
		 * @param cpp1Features
		 * @param scale
		 */
		private ExtractFeatures(boolean cpp1Features, boolean cpapFeatures,
				boolean reptFeatures, double scale) {
			this.cpp1Features = cpp1Features;
			this.cpapFeatures = cpapFeatures;
			this.reptFeatures = reptFeatures;
			this.scale = scale;
		}

		public static IHashVector of(LogicalExpression exp,
				boolean cpp1Features, boolean cpapFeatures,
				boolean reptFeatures, double scale) {
			final ExtractFeatures visitor = new ExtractFeatures(cpp1Features,
					cpapFeatures, reptFeatures, scale);
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
			final boolean coordinationPredicate = LogicLanguageServices
					.isCoordinationPredicate(literal.getPredicate());

			literal.getPredicate().accept(this);
			final int numArgs = literal.numArgs();
			for (int i = 0; i < numArgs; ++i) {
				// Visit each argument
				literal.getArg(i).accept(this);
			}

			final String literalString = GetHeadString.of(literal);

			if (coordinationPredicate) {
				// first argument -> list of literals
				final Map<LogicalExpression, List<Literal>> pos1ArgToLiterals = new HashMap<LogicalExpression, List<Literal>>();
				// (argument, position) -> list of literals
				final Map<Pair<LogicalExpression, Integer>, List<Literal>> argPosToLiterals = new HashMap<Pair<LogicalExpression, Integer>, List<Literal>>();
				// literal -> count
				final Map<LogicalExpression, Counter> literalCounts = new HashMap<LogicalExpression, Counter>();

				// Collect data from the arguments
				for (int i = 0; i < numArgs; ++i) {
					final LogicalExpression arg = literal.getArg(i);
					if (arg instanceof Literal
							&& !LogicLanguageServices
									.isCoordinationPredicate(((Literal) arg)
											.getPredicate())) {
						final Literal argLiteral = (Literal) arg;

						// Counting unique literals
						if (!literalCounts.containsKey(argLiteral)) {
							literalCounts.put(argLiteral, new Counter(1));
						} else {
							literalCounts.get(argLiteral).inc();
						}

						final int argLiteralNumArgs = argLiteral.numArgs();
						if (argLiteralNumArgs >= 1) {
							final LogicalExpression pos1Arg = argLiteral
									.getArg(0);
							if (!pos1ArgToLiterals.containsKey(pos1Arg)) {
								pos1ArgToLiterals.put(pos1Arg,
										new LinkedList<Literal>());
							}
							pos1ArgToLiterals.get(pos1Arg).add(argLiteral);
						}
						int counter = 0;
						for (int j = 0; j < argLiteralNumArgs; ++j) {
							final LogicalExpression argArg = argLiteral
									.getArg(j);
							final Pair<LogicalExpression, Integer> argPosition = Pair
									.of(argArg, counter);
							if (!argPosToLiterals.containsKey(argPosition)) {
								argPosToLiterals.put(argPosition,
										new LinkedList<Literal>());
							}
							argPosToLiterals.get(argPosition).add(argLiteral);
							++counter;
						}
					}
				}

				// REPT features
				if (reptFeatures) {
					for (final Entry<LogicalExpression, Counter> literalCountEntry : literalCounts
							.entrySet()) {
						if (literalCountEntry.getValue().value() != 1) {
							features.set(
									FEATURE_TAG,
									"REPT",
									literalString,
									GetHeadString.of(literalCountEntry.getKey()),
									1.0 * scale);
						}
					}
				}

				// CPP1 features
				if (cpp1Features) {
					for (final List<Literal> literalsSharingArg1 : pos1ArgToLiterals
							.values()) {
						final int numLiterals = literalsSharingArg1.size();
						for (int i = 0; i < numLiterals; ++i) {
							final String iHeadString = GetHeadString
									.of(literalsSharingArg1.get(i));
							for (int j = i + 1; j < numLiterals; ++j) {
								final String jHeadString = GetHeadString
										.of(literalsSharingArg1.get(j));
								// Sort names lexicographically, because the
								// order
								// doesn't really matter
								if (!literalsSharingArg1.get(j).equals(
										literalsSharingArg1.get(i))) {
									if (iHeadString.compareTo(jHeadString) >= 0) {
										features.set(FEATURE_TAG, "CPP1",
												literalString, iHeadString,
												jHeadString, 1.0 * scale);
									} else {
										features.set(FEATURE_TAG, "CPP1",
												literalString, jHeadString,
												iHeadString, 1.0 * scale);
									}
								}
							}
						}
					}
				}

				// CPAP features
				if (cpapFeatures) {
					for (final Entry<Pair<LogicalExpression, Integer>, List<Literal>> argPosEntry : argPosToLiterals
							.entrySet()) {
						final int position = argPosEntry.getKey().second();
						final List<Literal> literals = argPosEntry.getValue();
						final int numLiterals = literals.size();
						for (int i = 0; i < numLiterals; ++i) {
							final Literal iLiteral = literals.get(i);
							for (int j = i + 1; j < numLiterals; ++j) {
								final Literal jLiteral = literals.get(j);
								if (iLiteral.getPredicate().equals(
										jLiteral.getPredicate())
										&& !iLiteral.equals(jLiteral)) {
									features.set(FEATURE_TAG, "CPAP",
											literalString,
											GetHeadString.of(iLiteral),
											String.valueOf(position),
											1.0 * scale);
								}

							}
						}
					}
				}
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
