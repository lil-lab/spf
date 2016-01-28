package edu.cornell.cs.nlp.spf.data.situated.sentence;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;

/**
 * Collection of {@link SituatedSentence}.
 *
 * @author Yoav Artzi
 * @param <STATE>
 *            Type of state/situation.
 */
public class SituatedSentenceCollection<STATE>
		implements IDataCollection<SituatedSentence<STATE>> {

	private static final long					serialVersionUID	= -3259824918810436454L;
	private final List<SituatedSentence<STATE>>	entries;

	public SituatedSentenceCollection(List<SituatedSentence<STATE>> entries) {
		this.entries = Collections.unmodifiableList(entries);
	}

	@Override
	public Iterator<SituatedSentence<STATE>> iterator() {
		return entries.iterator();
	}

	@Override
	public int size() {
		return entries.size();
	}

}
