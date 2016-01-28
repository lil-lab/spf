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
package edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features;

import java.util.Collections;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSet;
import edu.cornell.cs.nlp.utils.collections.ListUtils;

/**
 * Features that pair syntactic templates (syntax with abstracted over
 * attributes) and a sequence of attributes.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item.
 */
public class SyntaxAttributeFeatureSet<DI extends IDataItem<?>>
		implements IParseFeatureSet<DI, LogicalExpression> {

	private static final String	FEATURE_TAG			= "SYNTAX";

	private static final long	serialVersionUID	= 2565045254292156250L;

	private final double		scale;

	public SyntaxAttributeFeatureSet(double scale) {
		this.scale = scale;
	}

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return Collections.emptySet();
	}

	@Override
	public void setFeatures(IParseStep<LogicalExpression> parseStep,
			IHashVector features, DI dataItem) {
		if (parseStep instanceof ILexicalParseStep) {
			final LexicalEntry<LogicalExpression> lexicalEntry = ((ILexicalParseStep<LogicalExpression>) parseStep)
					.getLexicalEntry();
			if (lexicalEntry instanceof FactoredLexicalEntry) {
				final FactoredLexicalEntry entry = (FactoredLexicalEntry) lexicalEntry;
				features.add(FEATURE_TAG,
						entry.getTemplate().getTemplateCategory().getSyntax()
								.toString(),
						ListUtils.join(entry.getLexeme().getAttributes(), "+"),
						scale * 1.0);
			}
		}
	}

	public static class Creator<DI extends IDataItem<?>>
			implements IResourceObjectCreator<SyntaxAttributeFeatureSet<DI>> {

		private String type;

		public Creator() {
			this("feat.syntax.attributes");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public SyntaxAttributeFeatureSet<DI> create(Parameters params,
				IResourceRepository repo) {
			return new SyntaxAttributeFeatureSet<DI>(
					params.getAsDouble("scale", 1.0));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, SyntaxAttributeFeatureSet.class)
					.setDescription(
							"Features that pair syntactic templates (syntax with abstracted over attributes) and a sequence of attributes.")
					.build();
		}

	}

}
