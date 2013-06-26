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
import edu.uw.cs.lil.tiny.learn.ubl.UBLMarginConstrainedPerceptron;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.IUBLSplitter;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.single.CKYParser;
import edu.uw.cs.lil.tiny.test.Tester;

public class UBLMarginConstrainedPerceptronCreator implements
		IResourceObjectCreator<UBLMarginConstrainedPerceptron> {
	
	@SuppressWarnings("unchecked")
	@Override
	public UBLMarginConstrainedPerceptron create(Parameters parameters,
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
					throw new RuntimeException(
							"Unknown or not a labeled dataset: " + datasetId);
				} else {
					trainingSets
							.add((IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>) dataCollection);
				}
			}
			trainingSet = new CompositeDataCollection<ILabeledDataItem<Sentence, LogicalExpression>>(
					trainingSets);
		}
		
		final UBLMarginConstrainedPerceptron.Builder builder = new UBLMarginConstrainedPerceptron.Builder(
				(ICategoryServices<LogicalExpression>) resourceRepo
						.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
				trainingSet, Double.parseDouble(parameters.get("margin")),
				Integer.parseInt(parameters.get("iter")),
				(IUBLSplitter) resourceRepo.getResource(parameters
						.get("splitter")),
				(CKYParser<LogicalExpression>) resourceRepo
						.getResource(ParameterizedExperiment.PARSER_RESOURCE));
		
		if ("true".equals(parameters.get("conservative"))) {
			builder.setConservativeSplitting(true);
		}
		
		if (parameters.get("maxSentenceLength") != null) {
			builder.setMaxSentenceLength(Integer.valueOf(parameters
					.get("maxSentenceLen")));
		}
		
		if (parameters.get("tester") != null) {
			builder.setTester((Tester<Sentence, LogicalExpression>) resourceRepo
					.getResource(parameters.get("tester")));
		}
		
		if ("true".equals(parameters.get("prune"))) {
			builder.setPruneLexicon(true);
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return "learner.ubl.marginperceptron";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(),
				UBLMarginConstrainedPerceptron.class)
				.setDescription(
						"Unification-based margin perceptron learner (UBL)")
				.addParam("data", "id",
						"Training data. Sentences labeled with logical expressions.")
				.addParam("iter", "int",
						"Number of training iterations (epochs).")
				.addParam("splitter", "id", "Logical expression splitter")
				.addParam(
						"conservative",
						"boolean",
						"Only add new lexical entries from splits when there's a single candidate split. Options: true, false. Default: false")
				.addParam("maxSentenceLength", "int",
						"Max sentence length (in tokens) to process")
				.addParam("tester", "id",
						"Tester to test the model after every training iteration")
				.addParam(
						"prune",
						"boolean",
						"Prune the learned lexicon after learning. Options: true, false. Default: false.")
				.build();
	}
}
