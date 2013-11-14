package edu.uw.cs.lil.tiny.data.situated.sentence;

import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;

/**
 * A sentence situated in some kind of state.
 * 
 * @author Yoav Artzi
 * @param <STATE>
 *            Type of state/situation.
 */
public class SituatedSentence<STATE> extends Sentence implements
		ISituatedDataItem<Sentence, STATE> {
	
	private final STATE	state;
	
	public SituatedSentence(Sentence sentence, STATE state) {
		super(sentence.getTokens());
		this.state = state;
	}
	
	@Override
	public STATE getState() {
		return state;
	}
	
	@Override
	public String toString() {
		return super.toString() + " :: " + state.toString();
	}
	
}
