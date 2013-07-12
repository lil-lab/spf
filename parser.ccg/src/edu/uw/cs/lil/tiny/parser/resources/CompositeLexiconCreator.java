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

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.lexicon.CompositeLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;

public class CompositeLexiconCreator<Y> implements
		IResourceObjectCreator<CompositeLexicon<Y>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public CompositeLexicon<Y> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		
		final ILexicon<Y> master = (ILexicon<Y>) resourceRepo
				.getResource(parameters.get("masterLexicon"));
		
		final List<String> otherLexicons = parameters.getSplit("otherLexicons");
		final List<ILexicon<Y>> subLexicons = new ArrayList<ILexicon<Y>>();
		for (final String lexName : otherLexicons) {
			subLexicons.add((ILexicon<Y>) resourceRepo.getResource(lexName));
		}
		
		return new CompositeLexicon<Y>(master, subLexicons);
	}
	
	@Override
	public String type() {
		return "lexicon.composite";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), CompositeLexicon.class)
				.setDescription(
						"Lexicon that is made of a union of different lexicons")
				.addParam(
						"masterLexicon",
						"id",
						"The core lexicon. All entries added to the copmosite lexicon will be added to it")
				.addParam("otherLexicons", "[id]",
						"Non-master lexicons to use (e.g., 'lexicon1,lexicon2,lexicon3')")
				.build();
	}
}
