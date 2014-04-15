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
package edu.uw.cs.lil.tiny.parser.ccg.features.lambda;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetHeadString;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;
import edu.uw.cs.utils.counter.Counter;

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
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(
			IHashVector theta) {
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		for (final Pair<KeyArgs, Double> feature : theta.getAll(FEATURE_TAG)) {
			weights.add(Triplet.of(feature.first(), feature.second(),
					(String) null));
		}
		return weights;
	}
	
	@Override
	public boolean isValidWeightVector(IHashVectorImmutable update) {
		// No protected features
		return true;
	}
	
	@Override
	public double score(IParseStep<LogicalExpression> parseStep,
			IHashVector theta, DI dataItem) {
		if (!parseStep.isFullParse()) {
			// Only score logical expression features of the final logical
			// form
			return 0.0;
		}
		
		return setFeats(parseStep.getRoot().getSem(),
				HashVectorFactory.create()).vectorMultiply(theta);
	}
	
	@Override
	public void setFeats(IParseStep<LogicalExpression> parseStep,
			IHashVector feats, DI dataItem) {
		if (!parseStep.isFullParse()) {
			// Only generate logical expression features of the final logical
			// form
			return;
		}
		setFeats(parseStep.getRoot().getSem(), feats);
	}
	
	private IHashVector setFeats(LogicalExpression sem, IHashVector feats) {
		
		// Generate deep semantic features
		ExtractFeatures
				.of(sem, cpp1Features, cpapFeatures, reptFeatures, SCALE)
				.addTimesInto(1.0, feats);
		
		return feats;
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
			for (final LogicalExpression arg : literal.getArguments()) {
				// Visit each argument
				arg.accept(this);
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
				for (final LogicalExpression arg : literal.getArguments()) {
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
						
						if (argLiteral.getArguments().size() >= 1) {
							final LogicalExpression pos1Arg = argLiteral
									.getArguments().get(0);
							if (!pos1ArgToLiterals.containsKey(pos1Arg)) {
								pos1ArgToLiterals.put(pos1Arg,
										new LinkedList<Literal>());
							}
							pos1ArgToLiterals.get(pos1Arg).add(argLiteral);
						}
						int counter = 0;
						for (final LogicalExpression argArg : argLiteral
								.getArguments()) {
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
