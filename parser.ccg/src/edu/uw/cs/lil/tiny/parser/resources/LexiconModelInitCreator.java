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
package edu.uw.cs.lil.tiny.parser.resources;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.model.init.LexiconModelInit;

public class LexiconModelInitCreator<DI extends IDataItem<?>, MR> implements
		IResourceObjectCreator<LexiconModelInit<DI, MR>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public LexiconModelInit<DI, MR> create(Parameters params,
			IResourceRepository repo) {
		return new LexiconModelInit<DI, MR>((ILexicon<MR>) repo.getResource(params
				.get("lexicon")), "true".equals(params.get("fixed")));
	}
	
	@Override
	public String type() {
		return "init.lex";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), LexiconModelInit.class)
				.setDescription("Intializer to add lexical entries to a model")
				.addParam("lexicon", "id",
						"Lexicon of entries to add to initialized models")
				.addParam(
						"fixed",
						"boolean",
						"Add lexical entries as fixed entries or not. Options: false, true. Default: false")
				.build();
	}
	
}
