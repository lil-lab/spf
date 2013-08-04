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
package edu.uw.cs.lil.tiny.genlex.ccg.unification.resources;

import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.UnificationGenlex;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.split.IUnificationSplitter;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;

public class UnificationGenlexCreator implements
		IResourceObjectCreator<UnificationGenlex> {
	
	private final String	type;
	
	public UnificationGenlexCreator() {
		this("genlex.unification");
	}
	
	public UnificationGenlexCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public UnificationGenlex create(Parameters params, IResourceRepository repo) {
		return new UnificationGenlex(
				(AbstractCKYParser<LogicalExpression>) repo
						.getResource(ParameterizedExperiment.PARSER_RESOURCE),
				(IUnificationSplitter) repo.getResource(params.get("splitter")),
				"true".equals(params.get("conservative")));
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type, UnificationGenlex.class)
				.addParam("splitter", "IUnificationSplitter",
						"Category splitter")
				.addParam("convervative", "boolean",
						"If 'true' only use splits if there's a single best split.")
				.build();
	}
	
}
