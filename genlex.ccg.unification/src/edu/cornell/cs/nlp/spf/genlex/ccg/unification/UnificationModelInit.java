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
package edu.cornell.cs.nlp.spf.genlex.ccg.unification;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelInit;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.utils.collections.MapUtils;

public class UnificationModelInit implements
		IModelInit<Sentence, LogicalExpression> {

	private static final String																UNIFICATION_INIT	= "unification_init";

	private final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>	data;

	private final Syntax																	sentenceSyntax;

	public UnificationModelInit(
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> data,
			Syntax sentenceSyntax) {
		this.data = data;
		this.sentenceSyntax = sentenceSyntax;
	}

	@Override
	public void init(Model<Sentence, LogicalExpression> model) {
		for (final ILabeledDataItem<Sentence, LogicalExpression> dataItem : data) {
			model.addLexEntry(FactoringServices
					.factor(new LexicalEntry<LogicalExpression>(dataItem
							.getSample().getTokens(), Category.create(
							sentenceSyntax, dataItem.getLabel()), false,
							MapUtils.createSingletonMap(
									LexicalEntry.ORIGIN_PROPERTY,
									UNIFICATION_INIT))));
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
							.get(params.get("data")), Syntax.read(params
							.get("sentenceSyntax")));
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
