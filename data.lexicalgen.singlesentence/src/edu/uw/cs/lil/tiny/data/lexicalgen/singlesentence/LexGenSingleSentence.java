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

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.lexicalgen.ILexGenDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

/**
 * Single sentence paired with a logical form and with the ability to generate
 * new lexical entries.
 * 
 * @author Yoav Artzi
 */
public class LexGenSingleSentence extends SingleSentence implements
		ILexGenDataItem<Sentence, LogicalExpression> {
	
	private final ILexiconGenerator<SingleSentence, LogicalExpression>	genlex;
	
	public LexGenSingleSentence(Sentence sentence, LogicalExpression semantics,
			ILexiconGenerator<SingleSentence, LogicalExpression> genlex) {
		super(sentence, semantics);
		this.genlex = genlex;
	}
	
	@Override
	public ILexicon<LogicalExpression> generateLexicon() {
		return genlex.generate(this);
	}
	
}
