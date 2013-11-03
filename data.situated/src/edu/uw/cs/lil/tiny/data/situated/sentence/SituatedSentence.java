package edu.uw.cs.lil.tiny.data.situated.sentence;

import java.util.List;

import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.utils.composites.Pair;

/**
 * A sentence situated in some kind of state.
 * 
 * @author Yoav Artzi
 * @param <STATE>
 *            Type of state/situation.
 */
public class SituatedSentence<STATE> implements
		ISituatedDataItem<Sentence, STATE> {
	
	private final Pair<Sentence, STATE>	sample;
	private final Sentence				sentence;
	private final STATE					state;
	
	public SituatedSentence(Sentence sentence, STATE state) {
		this.sentence = sentence;
		this.state = state;
		this.sample = Pair.of(sentence, state);
	}
	
	@Override
	public Pair<Sentence, STATE> getSample() {
		return sample;
	}
	
	public List<String> getTokens() {
		return sentence.getTokens();
	}
	
	@Override
	public String toString() {
		return sentence.toString() + " :: " + state.toString();
	}
	
}
