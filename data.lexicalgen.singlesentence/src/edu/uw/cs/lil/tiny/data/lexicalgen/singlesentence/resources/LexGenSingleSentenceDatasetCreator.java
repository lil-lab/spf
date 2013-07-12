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
package edu.uw.cs.lil.tiny.data.lexicalgen.singlesentence.resources;

import edu.uw.cs.lil.tiny.data.lexicalgen.singlesentence.LexGenSingleSentenceDataset;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentenceDataset;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.utils.string.StubStringFilter;

/**
 * Creator for {@link LexGenSingleSentenceDataset}.
 * 
 * @author Yoav Artzi
 */
public class LexGenSingleSentenceDatasetCreator implements
		IResourceObjectCreator<LexGenSingleSentenceDataset> {
	
	@SuppressWarnings("unchecked")
	@Override
	public LexGenSingleSentenceDataset create(Parameters parameters,
			IResourceRepository resourceRepo) {
		return LexGenSingleSentenceDataset
				.read(parameters.getAsFile("file"),
						new StubStringFilter(),
						((ILexiconGenerator<SingleSentence, LogicalExpression>) resourceRepo
								.getResource(parameters.get("genlex"))), true);
	}
	
	@Override
	public String type() {
		return "data.single.genlex";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), SingleSentenceDataset.class)
				.setDescription(
						"Dataset for pairs of sentences and logical forms, with the ability to generate lexical entries")
				.addParam(
						"file",
						"file",
						"File with pairs of sentences and logical forms. The file will include a line with sentence, a line with a LF, empty line, a line with a sentence, and so on")
				.addParam("genlex", "ILexiconGenerator", "Lexicon generator")
				.build();
	}
	
}
