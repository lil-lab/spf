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
package edu.uw.cs.lil.tiny.data.lexicalgen.singlesentence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.lexicalgen.ILexGenLabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.IEvidenceLexicalGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.Lexicon;

public class LexGenSingleSentence extends SingleSentence
		implements
		ILexGenLabeledDataItem<Sentence, LogicalExpression, LogicalExpression> {
	
	private final IEvidenceLexicalGenerator<Sentence, LogicalExpression, LogicalExpression>	semanticsLexicalGeneration;
	private final List<ISentenceLexiconGenerator<LogicalExpression>>						textLexicalGenerators;
	
	public LexGenSingleSentence(
			Sentence sentence,
			LogicalExpression semantics,
			IEvidenceLexicalGenerator<Sentence, LogicalExpression, LogicalExpression> semanticsLexicalGeneration,
			List<ISentenceLexiconGenerator<LogicalExpression>> textLexicalGenerators) {
		super(sentence, semantics);
		this.semanticsLexicalGeneration = semanticsLexicalGeneration;
		this.textLexicalGenerators = textLexicalGenerators;
	}
	
	@Override
	public ILexicon<LogicalExpression> generateLexicon() {
		final Set<LexicalEntry<LogicalExpression>> generatedEntries = new HashSet<LexicalEntry<LogicalExpression>>(
				semanticsLexicalGeneration.generateLexicon(getSample(),
						getLabel()));
		for (final IEvidenceLexicalGenerator<Sentence, LogicalExpression, Sentence> generator : textLexicalGenerators) {
			generatedEntries.addAll(generator.generateLexicon(getSample(),
					getSample()));
		}
		return new Lexicon<LogicalExpression>(generatedEntries);
	}
	
}
