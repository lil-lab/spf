package edu.cornell.cs.nlp.spf.data.sentence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;

/**
 * Collection of {@link Sentence} objects.
 *
 * @author Yoav Artzi
 */
public class SentenceCollection implements IDataCollection<Sentence> {

	private static final long		serialVersionUID	= 8635476739352566108L;
	private final List<Sentence>	entries;

	public SentenceCollection(List<Sentence> entries) {
		this.entries = Collections.unmodifiableList(entries);
	}

	public static SentenceCollection read(File f) {
		return read(f, new StubStringFilter(), null);
	}

	public static SentenceCollection read(File f, IStringFilter textFilter,
			ITokenizer tokenizer) {
		int readLineCounter = 0;
		try {
			// Open the file
			final List<Sentence> data = new LinkedList<Sentence>();
			try (final BufferedReader in = new BufferedReader(
					new FileReader(f))) {
				String line;
				while ((line = in.readLine()) != null) {
					++readLineCounter;
					if (line.startsWith("//") || line.equals("")) {
						// Case comment or empty line, skip
						continue;
					}
					line = line.trim();

					// Read a sentence from the line. Each line contains a
					// sentence (if not empty or a comment).
					final String currentSentence = textFilter.filter(line);
					final Sentence sentence = tokenizer == null
							? new Sentence(currentSentence)
							: new Sentence(currentSentence, tokenizer);
					final Sentence dataItem;
					dataItem = new Sentence(sentence);
					data.add(dataItem);
				}
			}
			return new SentenceCollection(data);
		} catch (final Exception e) {
			// Wrap with dataset exception and throw
			throw new FileReadingException(e, readLineCounter, f.getName());
		}
	}

	public static SentenceCollection read(File f, ITokenizer tokenizer) {
		return read(f, new StubStringFilter(), tokenizer);
	}

	@Override
	public Iterator<Sentence> iterator() {
		return entries.iterator();
	}

	@Override
	public int size() {
		return entries.size();
	}

	public static class Creator
			implements IResourceObjectCreator<SentenceCollection> {

		private final String type;

		public Creator() {
			this("data.sent");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public SentenceCollection create(Parameters params,
				IResourceRepository repo) {
			return SentenceCollection.read(params.getAsFile("file"),
					(IStringFilter) (params.contains("filter")
							? repo.get(params.get("filter"))
							: new StubStringFilter()),
					(ITokenizer) (params.contains("tokenizer")
							? repo.get(params.get("tokenizer")) : null));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), SentenceCollection.class)
					.setDescription("Collection of sentences")
					.addParam("tokenizer", ITokenizer.class,
							"Tokenizer to process the sentence string (default: default tokenizer)")
					.addParam("filter", IStringFilter.class,
							"Filter to process input strings (default: identify filter)")
					.addParam("file", "file",
							"File with sentences. Each line includes a sentence. Empty and comment lines are ignored.")
					.build();
		}

	}

}
