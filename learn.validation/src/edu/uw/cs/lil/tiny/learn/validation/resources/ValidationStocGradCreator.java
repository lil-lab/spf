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
package edu.uw.cs.lil.tiny.learn.validation.resources;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.validation.perceptron.ValidationPerceptron;
import edu.uw.cs.lil.tiny.learn.validation.stocgrad.ValidationStocGrad;
import edu.uw.cs.lil.tiny.learn.validation.stocgrad.ValidationStocGrad.Builder;
import edu.uw.cs.lil.tiny.parser.IOutputLogger;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParser;
import edu.uw.cs.lil.tiny.test.ITester;
import edu.uw.cs.utils.filter.IFilter;

public class ValidationStocGradCreator<SAMPLE, DI extends IDataItem<SAMPLE>, MR>
		implements IResourceObjectCreator<ValidationStocGrad<SAMPLE, DI, MR>> {
	
	private final String	type;
	
	public ValidationStocGradCreator() {
		this("learner.validation.stocgrad");
	}
	
	public ValidationStocGradCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ValidationStocGrad<SAMPLE, DI, MR> create(Parameters params,
			IResourceRepository repo) {
		
		final IDataCollection<DI> trainingData = repo.getResource(params
				.get("data"));
		
		final Builder<SAMPLE, DI, MR> builder = new ValidationStocGrad.Builder<SAMPLE, DI, MR>(
				trainingData,
				(IGraphParser<SAMPLE, MR>) repo
						.getResource(ParameterizedExperiment.PARSER_RESOURCE),
				(IValidator<DI, MR>) repo.getResource(params.get("validator")));
		
		if (params.contains("genlex")) {
			builder.setGenlex(
					(ILexiconGenerator<DI, MR, IModelImmutable<IDataItem<SAMPLE>, MR>>) repo
							.getResource(params.get("genlex")),
					(ICategoryServices<MR>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}
		
		if (params.contains("parseLogger")) {
			builder.setParserOutputLogger((IOutputLogger<MR>) repo
					.getResource(params.get("parseLogger")));
		}
		
		if (params.contains("genlexbeam")) {
			builder.setLexiconGenerationBeamSize(Integer.valueOf(params
					.get("genlexbeam")));
		}
		
		if (params.contains("tester")) {
			builder.setTester((ITester<SAMPLE, MR>) repo.getResource(params
					.get("tester")));
		}
		
		if (params.contains("iter")) {
			builder.setNumIterations(Integer.valueOf(params.get("iter")));
		}
		
		if (params.contains("filter")) {
			builder.setProcessingFilter((IFilter<DI>) repo.getResource(params
					.get("filter")));
		}
		
		if (params.contains("errorDriven")) {
			builder.setErrorDriven("true".equals(params.get("errorDriven")));
		}
		
		if (params.contains("c")) {
			builder.setC(Double.valueOf(params.get("c")));
		}
		
		if (params.contains("alpha0")) {
			builder.setAlpha0(Double.valueOf(params.get("alpha0")));
		}
		
		if (params.contains("conflateParses")) {
			builder.setConflateGenlexAndPrunedParses("true".equals(params
					.get("conflateParses")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), ValidationPerceptron.class)
				.setDescription("Validation-based stochastic gradient learner")
				.addParam("data", "id", "Training data")
				.addParam("genlex", "ILexiconGenerator", "GENLEX procedure")
				.addParam("conflateParses", "boolean",
						"Recyle lexical induction parsing output as pruned parsing output")
				.addParam("parseLogger", "id",
						"Parse logger for debug detailed logging of parses")
				.addParam("genlexbeam", "int",
						"Beam to use for GENLEX inference (parsing).")
				.addParam("filter", "IFilter", "Processing filter")
				.addParam("iter", "int", "Number of training iterations")
				.addParam("validator", "IValidator", "Validation function")
				.addParam("tester", "ITester",
						"Intermediate tester to use between epochs")
				.addParam(
						"c",
						"double",
						"Learing rate c parameter, temperature=alpha_0/(1+c*tot_number_of_training_instances)")
				.addParam(
						"alpha0",
						"double",
						"Learing rate alpha0 parameter, temperature=alpha_0/(1+c*tot_number_of_training_instances)")
				.addParam(
						"errorDriven",
						"boolean",
						"Error driven lexical generation, if the can generate a valid parse, skip lexical induction")
				.build();
	}
	
}
