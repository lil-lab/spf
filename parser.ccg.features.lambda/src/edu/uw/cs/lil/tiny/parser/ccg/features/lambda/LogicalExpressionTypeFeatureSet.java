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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.InferSubexpressionTypes;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.storage.IDecoder;
import edu.uw.cs.lil.tiny.utils.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.utils.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.utils.hashvector.KeyArgs;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

/**
 * Logical expression feature set.
 * 
 * @author Luke Zettlemoyer
 * @author Yoav Artzi
 */
public class LogicalExpressionTypeFeatureSet<DI extends IDataItem<?>>
		implements IParseFeatureSet<DI, LogicalExpression> {
	private static final String	FEATURE_TAG			= "LIT_TYPE";
	private static final double	SCALE				= 1.0;
	private static final long	serialVersionUID	= 4606128349923926260L;
	
	public LogicalExpressionTypeFeatureSet() {
	}
	
	public static <DI extends IDataItem<?>> IDecoder<LogicalExpressionTypeFeatureSet<DI>> getDecoder() {
		return new Decoder<DI>();
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
		ExtractLiteralTypeFeatures.of(sem).addTimesInto(1.0 * SCALE, feats);
		
		return feats;
	}
	
	private static class Decoder<DI extends IDataItem<?>> extends
			AbstractDecoderIntoFile<LogicalExpressionTypeFeatureSet<DI>> {
		
		private static final int	VERSION	= 1;
		
		public Decoder() {
			super(LogicalExpressionTypeFeatureSet.class);
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LogicalExpressionTypeFeatureSet<DI> object) {
			return new HashMap<String, String>();
		}
		
		@Override
		protected LogicalExpressionTypeFeatureSet<DI> doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			return new LogicalExpressionTypeFeatureSet<DI>();
		}
		
		@Override
		protected void doEncode(LogicalExpressionTypeFeatureSet<DI> object,
				BufferedWriter writer) throws IOException {
			// Nothing to do here
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LogicalExpressionTypeFeatureSet<DI> object, File directory,
				File parentFile) throws IOException {
			// No dependent files
			return new HashMap<String, File>();
		}
		
	}
	
	/**
	 * @author Luke Zettlemoyer
	 */
	private static class ExtractLiteralTypeFeatures implements
			ILogicalExpressionVisitor {
		
		private final IHashVector					features	= HashVectorFactory
																		.create();
		private final Map<LogicalExpression, Type>	inferredTypes;
		
		/**
		 * Use only through 'of' method.
		 * 
		 * @param exp
		 */
		private ExtractLiteralTypeFeatures(LogicalExpression exp) {
			this.inferredTypes = InferSubexpressionTypes.of(exp,
					LogicLanguageServices.getTypeRepository());
		}
		
		public static IHashVector of(LogicalExpression exp) {
			final ExtractLiteralTypeFeatures visitor = new ExtractLiteralTypeFeatures(
					exp);
			visitor.visit(exp);
			return visitor.getFeatures();
		}
		
		public IHashVector getFeatures() {
			return features;
		}
		
		@Override
		public void visit(Lambda lambda) {
			// lambda.getArgument().accept(this);
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
			
			if (!coordinationPredicate
					&& (literal.getPredicate() instanceof LogicalConstant)) {
				final StringBuilder signature = new StringBuilder();
				signature.append(literal.getPredicate().toString());
				for (final LogicalExpression arg : literal.getArguments()) {
					signature.append(":");
					final Type t = inferredTypes.get(arg);
					if (t == null) {
						signature.append("nil");
					} else {
						signature.append(t.getName());
					}
				}
				final String signatureString = signature.toString();
				features.set(FEATURE_TAG, signatureString, 1.0 * SCALE);
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
