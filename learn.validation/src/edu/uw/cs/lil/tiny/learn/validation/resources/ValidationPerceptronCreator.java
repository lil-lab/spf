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

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.lexicalgen.ILexGenDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.learn.validation.perceptron.ValidationPerceptron;
import edu.uw.cs.lil.tiny.learn.validation.perceptron.ValidationPerceptron.Builder;
import edu.uw.cs.lil.tiny.parser.IOutputLogger;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.test.ITester;

public class ValidationPerceptronCreator<MR> implements
		IResourceObjectCreator<ValidationPerceptron<MR>> {
	
	private final String	type;
	
	public ValidationPerceptronCreator() {
		this("learner.validation.perceptron");
	}
	
	public ValidationPerceptronCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ValidationPerceptron<MR> create(Parameters params,
			IResourceRepository repo) {
		
		final IDataCollection<? extends ILexGenDataItem<Sentence, MR>> trainingData = repo
				.getResource(params.get("data"));
		
		final Builder<MR> builder = new ValidationPerceptron.Builder<MR>(
				trainingData,
				(IParser<Sentence, MR>) repo
						.getResource(ParameterizedExperiment.PARSER_RESOURCE),
				(IValidator<IDataItem<Sentence>, MR>) repo.getResource(params
						.get("validator")));
		
		if ("true".equals(params.get("hard"))) {
			builder.setHardUpdates(true);
		}
		
		if (params.contains("parseLogger")) {
			builder.setParserOutputLogger((IOutputLogger<MR>) repo
					.getResource(params.get("parseLogger")));
		}
		
		if ("false".equals(params.get("lexiconlearn"))) {
			builder.setLexiconLearning(false);
		}
		
		if (params.contains("genlexbeam")) {
			builder.setLexiconGenerationBeamSize(Integer.valueOf(params
					.get("genlexbeam")));
		}
		
		if (params.contains("conflateParses")) {
			builder.setConflateGenlexAndPrunedParses("true".equals(params
					.get("conflateParses")));
		}
		
		if (params.contains("margin")) {
			builder.setMargin(Double.valueOf(params.get("margin")));
		}
		
		if (params.contains("tester")) {
			builder.setTester((ITester<Sentence, MR>) repo.getResource(params
					.get("tester")));
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
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), ValidationPerceptron.class)
				.setDescription("Validation-based perceptron")
				.addParam("data", "id", "Training data (lexical generation)")
				.addParam(
						"hard",
						"boolean",
						"Use hard updates (i.e., only use max scoring valid parses/evaluation as positive samples). Options: true, false. Default: false")
				.addParam("parseLogger", "id",
						"Parse logger for debug detailed logging of parses")
				.addParam("lexiconlearn", "boolean",
						"Do lexicon learning. Options: true, false. Default: true")
				.addParam("tester", "ITester",
						"Intermediate tester to use between epochs")
				.addParam("genlexbeam", "int",
						"Beam to use for GENLEX inference (parsing).")
				.addParam("margin", "double",
						"Margin to use for updates. Updates will be done when this margin is violated.")
				.addParam("maxSentenceLength", "int",
						"Max sentence length to process")
				.addParam("iter", "int", "Number of training iterations")
				.addParam("validator", "IValidator", "Validation function")
				.addParam("conflateParses", "boolean",
						"Recyle lexical induction parsing output as pruned parsing output")
				.build();
	}
	
}
