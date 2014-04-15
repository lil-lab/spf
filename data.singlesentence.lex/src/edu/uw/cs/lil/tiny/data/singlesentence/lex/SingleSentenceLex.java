package edu.uw.cs.lil.tiny.data.singlesentence.lex;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

/**
 * {@link SingleSentence} with a set of lexical entries.
 * 
 * @author Yoav Artzi
 */
public class SingleSentenceLex extends SingleSentence {
	
	private final Set<LexicalEntry<LogicalExpression>>	entries;
	
	public SingleSentenceLex(Sentence sentence, LogicalExpression semantics,
			Map<String, String> properties,
			Set<LexicalEntry<LogicalExpression>> entries) {
		super(sentence, semantics, properties);
		this.entries = Collections.unmodifiableSet(entries);
	}
	
	public SingleSentenceLex(Sentence sentence, LogicalExpression semantics,
			Set<LexicalEntry<LogicalExpression>> entries) {
		super(sentence, semantics);
		this.entries = Collections.unmodifiableSet(entries);
	}
	
	public Set<LexicalEntry<LogicalExpression>> getEntries() {
		return entries;
	}
	
}
