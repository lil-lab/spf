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
package edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.scorers;

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;

public class LexicalEntryLexicalTemplateBasedScorer implements
		ISerializableScorer<LexicalEntry<LogicalExpression>> {
	
	private static final long				serialVersionUID	= 5970648805173831923L;
	private final IScorer<LexicalTemplate>	templateScorer;
	
	public LexicalEntryLexicalTemplateBasedScorer(
			IScorer<LexicalTemplate> templateScorer) {
		this.templateScorer = templateScorer;
	}
	
	@Override
	public double score(LexicalEntry<LogicalExpression> e) {
		return templateScorer.score(FactoringServices.factor(e).getTemplate());
	}
	
}
