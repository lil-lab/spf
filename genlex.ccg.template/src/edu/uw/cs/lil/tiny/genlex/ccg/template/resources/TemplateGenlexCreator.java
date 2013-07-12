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
package edu.uw.cs.lil.tiny.genlex.ccg.template.resources;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.template.TemplateGenlex;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;

public class TemplateGenlexCreator implements
		IResourceObjectCreator<TemplateGenlex> {
	
	private final String	type;
	
	public TemplateGenlexCreator() {
		this("genlex.template");
	}
	
	public TemplateGenlexCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public TemplateGenlex create(Parameters params, IResourceRepository repo) {
		final TemplateGenlex.Builder builder = new TemplateGenlex.Builder(
				Integer.valueOf(params.get("maxTokens")));
		
		if (params.contains("templatesModel")) {
			builder.addTemplatesFromModel((IModelImmutable<?, LogicalExpression>) repo
					.getResource(params.get("model")));
		} else if (params.contains("lexicon")) {
			builder.addTemplatesFromLexicon((ILexicon<LogicalExpression>) repo
					.getResource(params.get("lexicon")));
		} else {
			throw new IllegalStateException("no templates source specified");
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), TemplateGenlex.class)
				.addParam("model", "Model",
						"Model object to get templates from")
				.addParam("lexicon", "ILexicon",
						"Lexicon to get templates from")
				.addParam("maxTokens", "int",
						"Max number of tokens to consider for new lexical entries")
				.build();
	}
	
}
