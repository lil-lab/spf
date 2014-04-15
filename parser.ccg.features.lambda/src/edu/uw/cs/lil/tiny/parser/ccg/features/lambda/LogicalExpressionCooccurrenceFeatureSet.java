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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.base.PowerSet;
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
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetHeadString;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

public class LogicalExpressionCooccurrenceFeatureSet<DI extends IDataItem<?>>
		implements IParseFeatureSet<DI, LogicalExpression> {
	private static final String	FEATURE_TAG			= "LOGCOOC";
	private static final double	SCALE				= 1.0;
	
	private static final long	serialVersionUID	= 7387260474009084901L;
	
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
		ExtractFeatures.of(sem, SCALE).addTimesInto(1.0, feats);
		
		return feats;
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
			final String predicateString = GetHeadString.of(literal
					.getPredicate());
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					literal.getArguments());
			for (final List<LogicalExpression> subset : new PowerSet<LogicalExpression>(
					args)) {
				if (subset.size() == 2) {
					// Only observe pairs
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
			}
			
			for (final LogicalExpression arg : args) {
				arg.accept(this);
				
				final String argString = GetHeadString.of(arg);
				features.set(FEATURE_TAG, "PREDARG", predicateString,
						argString, 1.0 * scale);
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
