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
package edu.uw.cs.lil.tiny.parser.ccg.model;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Init a lexicon with a set lexical entries.
 * 
 * @author Yoav Artzi
 * @param <DI>
 * @param <MR>
 */
public class LexiconModelInit<DI extends IDataItem<?>, MR> implements
		IModelInit<DI, MR> {
	
	public static final ILogger	LOG	= LoggerFactory
											.create(LexiconModelInit.class);
	private final ILexicon<MR>	lexicon;
	
	public LexiconModelInit(ILexicon<MR> lexicon) {
		this.lexicon = lexicon;
	}
	
	@Override
	public void init(Model<DI, MR> model) {
		LOG.info(new Runnable() {
			
			@Override
			public void run() {
				for (final LexicalEntry<MR> entry : lexicon.toCollection()) {
					LOG.info("Adding entry: %s", entry);
				}
			}
		});
		model.addLexEntries(lexicon.toCollection());
	}
	
	public static class Creator<DI extends IDataItem<?>, MR> implements
			IResourceObjectCreator<LexiconModelInit<DI, MR>> {
		
		@SuppressWarnings("unchecked")
		@Override
		public LexiconModelInit<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			return new LexiconModelInit<DI, MR>(
					(ILexicon<MR>) repo.getResource(params.get("lexicon")));
		}
		
		@Override
		public String type() {
			return "init.lex";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), LexiconModelInit.class)
					.setDescription(
							"Intializer to add lexical entries to a model")
					.addParam("lexicon", "id",
							"Lexicon of entries to add to initialized models")
					.build();
		}
		
	}
}
