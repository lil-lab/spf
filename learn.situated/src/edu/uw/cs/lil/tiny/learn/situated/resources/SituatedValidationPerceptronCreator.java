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
package edu.uw.cs.lil.tiny.learn.situated.resources;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.situated.perceptron.SituatedValidationPerceptron;
import edu.uw.cs.lil.tiny.learn.situated.perceptron.SituatedValidationPerceptron.Builder;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.IJointParser;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointModelImmutable;
import edu.uw.cs.utils.composites.Pair;

public class SituatedValidationPerceptronCreator<STATE, MR, ESTEP, ERESULT, DI extends IDataItem<Pair<Sentence, STATE>>>
		implements
		IResourceObjectCreator<SituatedValidationPerceptron<STATE, MR, ESTEP, ERESULT, DI>> {
	
	private final String	name;
	
	public SituatedValidationPerceptronCreator() {
		this("learner.weakp.valid.situated");
	}
	
	public SituatedValidationPerceptronCreator(String name) {
		this.name = name;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SituatedValidationPerceptron<STATE, MR, ESTEP, ERESULT, DI> create(
			Parameters params, IResourceRepository repo) {
		
		final IDataCollection<DI> trainingData = repo.getResource(params
				.get("data"));
		
		final Builder<STATE, MR, ESTEP, ERESULT, DI> builder = new SituatedValidationPerceptron.Builder<STATE, MR, ESTEP, ERESULT, DI>(
				trainingData,
				(IJointParser<Sentence, STATE, MR, ESTEP, ERESULT>) repo
						.getResource(ParameterizedExperiment.PARSER_RESOURCE),
				(IValidator<DI, Pair<MR, ERESULT>>) repo.getResource(params
						.get("validator")));
		
		if ("true".equals(params.get("hard"))) {
			builder.setHardUpdates(true);
		}
		
		if (params.contains("parseLogger")) {
			builder.setParserOutputLogger((IJointOutputLogger<MR, ESTEP, ERESULT>) repo
					.getResource(params.get("parseLogger")));
		}
		
		if (params.contains("genlex")) {
			builder.setGenlex(
					(ILexiconGenerator<DI, MR, IJointModelImmutable<IDataItem<Pair<Sentence, STATE>>, STATE, MR, ESTEP>>) repo
							.getResource(params.get("genlex")),
					(ICategoryServices<MR>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}
		
		if (params.contains("genlexbeam")) {
			builder.setLexiconGenerationBeamSize(Integer.valueOf(params
					.get("genlexbeam")));
		}
		
		if (params.contains("margin")) {
			builder.setMargin(Double.valueOf(params.get("margin")));
		}
		
		if (params.contains("maxSentenceLength")) {
			builder.setMaxSentenceLength(Integer.valueOf(params
					.get("maxSentenceLength")));
		}
		
		if (params.contains("iter")) {
			builder.setNumTrainingIterations(Integer.valueOf(params.get("iter")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return name;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(),
				SituatedValidationPerceptron.class)
				.setDescription(
						"Validation senstive perceptron for situated learning of models with situated inference (cite: Artzi and Zettlemoyer 2013)")
				.addParam("data", "id", "Training data")
				.addParam(
						"hard",
						"boolean",
						"Use hard updates (i.e., only use max scoring valid parses/evaluation as positive samples). Options: true, false. Default: false")
				.addParam("parseLogger", "id",
						"Parse logger for debug detailed logging of parses")
				.addParam("genlex", "ILexiconGenerator", "GENLEX procedure")
				.addParam("genlexbeam", "int",
						"Beam to use for GENLEX inference (parsing).")
				.addParam("margin", "double",
						"Margin to use for updates. Updates will be done when this margin is violated.")
				.addParam("maxSentenceLength", "int",
						"Max sentence length to process")
				.addParam("iter", "int", "Number of training iterations")
				.addParam("validator", "IValidator", "Validation function")
				.build();
	}
	
}
