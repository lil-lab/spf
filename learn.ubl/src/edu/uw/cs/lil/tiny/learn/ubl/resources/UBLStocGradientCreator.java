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
package edu.uw.cs.lil.tiny.learn.ubl.resources;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.CompositeDataCollection;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.learn.ubl.UBLStocGradient;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.IUBLSplitter;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.single.CKYParser;
import edu.uw.cs.lil.tiny.test.Tester;

public class UBLStocGradientCreator implements
		IResourceObjectCreator<UBLStocGradient> {
	
	@SuppressWarnings("unchecked")
	@Override
	public UBLStocGradient create(Parameters parameters,
			IResourceRepository resourceRepo) {
		
		// Get training sets
		final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> trainingSet;
		{
			final List<IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>> trainingSets = new LinkedList<IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>>();
			for (final String datasetId : parameters.getSplit("data")) {
				// [yoav] [17/10/2011] Store in Object to javac known bug
				final Object dataCollection = resourceRepo
						.getResource(datasetId);
				if (dataCollection == null
						|| !(dataCollection instanceof IDataCollection<?>)) {
					throw new RuntimeException("Invalid dataset: " + datasetId);
				} else {
					trainingSets
							.add((IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>) dataCollection);
				}
			}
			trainingSet = new CompositeDataCollection<ILabeledDataItem<Sentence, LogicalExpression>>(
					trainingSets);
		}
		
		final UBLStocGradient.Builder builder = new UBLStocGradient.Builder(
				trainingSet,
				(ICategoryServices<LogicalExpression>) resourceRepo
						.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
				(CKYParser<LogicalExpression>) resourceRepo
						.getResource(ParameterizedExperiment.PARSER_RESOURCE),
				(IUBLSplitter) resourceRepo.getResource(parameters
						.get("splitter")))
				.setEpochs(Integer.parseInt(parameters.get("iter")))
				.setC(Double.parseDouble(parameters.get("C")))
				.setAlpha0(Double.parseDouble(parameters.get("alpha")));
		
		if ("true".equals(parameters.get("conservative"))) {
			builder.setConservativeSplitting(true);
		}
		
		if (parameters.get("maxSentenceLength") != null) {
			builder.setMaxSentLen(Integer.valueOf(parameters
					.get("maxSentenceLen")));
		}
		
		if (parameters.get("tester") != null) {
			builder.setTester((Tester<Sentence, LogicalExpression>) resourceRepo
					.getResource(parameters.get("tester")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return "learner.ubl.stocgrad";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), UBLStocGradient.class)
				.setDescription(
						"Unification-based stochastic gradient descent learner (UBL)")
				.addParam("data", "id",
						"Training data. Sentences labeled with logical expressions.")
				.addParam("iter", "int",
						"Number of training iterations (epochs).")
				.addParam("splitter", "id", "Logical expression splitter")
				.addParam("C", "double", "C learning parameter")
				.addParam("alpha", "double", "Alpha learning parameter")
				.addParam(
						"conservative",
						"boolean",
						"Only add new lexical entries from splits when there's a single candidate split. Options: true, false. Default: false")
				.addParam("maxSentenceLength", "int",
						"Max sentence length (in tokens) to process")
				.addParam("tester", "id",
						"Tester to test the model after every training iteration")
				.build();
	}
}
