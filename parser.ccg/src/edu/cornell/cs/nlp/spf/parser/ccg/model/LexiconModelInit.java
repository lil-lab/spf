/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.parser.ccg.model;

import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

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
					(ILexicon<MR>) repo.get(params.get("lexicon")));
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
