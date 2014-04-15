package edu.uw.cs.lil.tiny.data.singlesentence.lex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.exceptions.FileReadingException;
import edu.uw.cs.lil.tiny.base.string.IStringFilter;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentenceDataset;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpressionRuntimeException;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsTypeConsistent;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;

/**
 * Dataset of {@link SingleSentenceLex}.
 * 
 * @author Yoav Artzi
 */
public class SingleSentenceLexDataset implements
		IDataCollection<SingleSentenceLex> {
	
	private final List<SingleSentenceLex>	data;
	
	public SingleSentenceLexDataset(List<SingleSentenceLex> data) {
		this.data = Collections.unmodifiableList(data);
	}
	
	public static SingleSentenceLexDataset read(File f,
			IStringFilter textFilter,
			ICategoryServices<LogicalExpression> categoryServices,
			String entriesOrigin) {
		int readLineCounter = 0;
		try {
			// Open the file
			final BufferedReader in = new BufferedReader(new FileReader(f));
			final List<SingleSentenceLex> data = new LinkedList<SingleSentenceLex>();
			try {
				String line;
				String currentSentence = null;
				LogicalExpression currentExpression = null;
				Map<String, String> currentProperties = null;
				while ((line = in.readLine()) != null) {
					++readLineCounter;
					if (line.startsWith("//") || line.equals("")) {
						// Case comment or empty line, skip
						continue;
					}
					line = line.trim();
					if (currentSentence == null) {
						// Case we don't have a sentence, so we are supposed to
						// get a sentence.
						currentSentence = textFilter.filter(line);
					} else if (currentProperties == null
							&& SingleSentenceDataset.isPropertiesLine(line)) {
						currentProperties = SingleSentenceDataset
								.readProperties(line);
					} else if (currentExpression == null) {
						// Case we don't have a logical expression, so we are
						// supposed to get it and create the data item.
						
						// Get the logical expression string. Consume lines
						// until we have balanced parentheses. In case we have a
						// indented expression.
						final StringBuilder expString = new StringBuilder(line);
						int paranthesisCount = SingleSentenceDataset
								.countParanthesis(line);
						while (paranthesisCount > 0) {
							line = in.readLine();
							++readLineCounter;
							paranthesisCount += SingleSentenceDataset
									.countParanthesis(line);
							expString.append("\n").append(line);
						}
						
						try {
							currentExpression = Simplify.of(LogicalExpression
									.read(expString.toString()));
						} catch (final LogicalExpressionRuntimeException e) {
							// wrap with a dataset exception and throw
							in.close();
							throw new FileReadingException(e, readLineCounter,
									f.getName());
						}
						if (!IsTypeConsistent.of(currentExpression)) {
							// Throw exception
							throw new FileReadingException(
									"Expression not well-typed: "
											+ currentExpression,
									readLineCounter, f.getName());
						}
					} else {
						// Get the lexical entries and create the data item. The
						// list of entries is terminated by an empty line.
						final Set<LexicalEntry<LogicalExpression>> entries = new HashSet<LexicalEntry<LogicalExpression>>();
						while (!"".equals(line)) {
							if (!line.startsWith("//")) {
								// Skip comments. Empty lines not allowed.
								entries.add(LexicalEntry
										.<LogicalExpression> parse(line,
												textFilter, categoryServices,
												entriesOrigin));
							}
							line = in.readLine();
							++readLineCounter;
						}
						
						// Create the data item.
						final SingleSentenceLex dataItem;
						if (currentProperties != null) {
							dataItem = new SingleSentenceLex(new Sentence(
									currentSentence), currentExpression,
									currentProperties, entries);
						} else {
							dataItem = new SingleSentenceLex(new Sentence(
									currentSentence), currentExpression,
									entries);
						}
						data.add(dataItem);
						
						// Reset the accumulated data.
						currentSentence = null;
						currentProperties = null;
						currentExpression = null;
					}
				}
			} finally {
				in.close();
			}
			return new SingleSentenceLexDataset(data);
		} catch (final Exception e) {
			// Wrap with dataset exception and throw
			throw new FileReadingException(e, readLineCounter, f.getName());
		}
	}
	
	@Override
	public Iterator<SingleSentenceLex> iterator() {
		return data.iterator();
	}
	
	@Override
	public int size() {
		return data.size();
	}
	
	public static class Creator implements
			IResourceObjectCreator<SingleSentenceLexDataset> {
		
		@SuppressWarnings("unchecked")
		@Override
		public SingleSentenceLexDataset create(Parameters params,
				IResourceRepository repo) {
			return SingleSentenceLexDataset
					.read(params.getAsFile("file"),
							new StubStringFilter(),
							(ICategoryServices<LogicalExpression>) repo
									.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
							params.get("origin"));
		}
		
		@Override
		public String type() {
			return "data.single.lex";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					SingleSentenceLexDataset.class)
					.setDescription(
							"Dataset for pairs of sentences and logical forms")
					.addParam(
							"file",
							"file",
							"File with pairs of sentences and logical forms. The file will include a line with sentence, a line with a LF, empty line, a line with a sentence, and so on")
					.build();
		}
		
	}
	
}
