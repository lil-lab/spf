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
package edu.uw.cs.lil.tiny.genlex.ccg.unification;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelInit;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;

public class UnificationModelInit implements
		IModelInit<Sentence, LogicalExpression> {
	
	private static final String																UNIFICATION_INIT	= "unification_init";
	
	private final ICategoryServices<LogicalExpression>										categoryServices;
	private final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>	data;
	
	public UnificationModelInit(
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> data,
			ICategoryServices<LogicalExpression> categoryServices) {
		this.data = data;
		this.categoryServices = categoryServices;
	}
	
	@Override
	public void init(Model<Sentence, LogicalExpression> model) {
		for (final ILabeledDataItem<Sentence, LogicalExpression> dataItem : data) {
			model.addLexEntry(FactoredLexicon
					.factor(new LexicalEntry<LogicalExpression>(dataItem
							.getSample().getTokens(), categoryServices
							.getSentenceCategory().cloneWithNewSemantics(
									dataItem.getLabel()), UNIFICATION_INIT)));
		}
	}
	
	public static class Creator implements
			IResourceObjectCreator<UnificationModelInit> {
		
		private final String	type;
		
		public Creator() {
			this("init.lex.unification");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public UnificationModelInit create(Parameters params,
				IResourceRepository repo) {
			return new UnificationModelInit(
					(IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>) repo
							.getResource(params.get("data")),
					(ICategoryServices<LogicalExpression>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, UnificationModelInit.class)
					.addParam("data", "Labeled data collection",
							"Training data").build();
		}
		
	}
}
