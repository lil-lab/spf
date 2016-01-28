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
package edu.cornell.cs.nlp.spf.data.singlesentence.lex;

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

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.base.properties.Properties;
import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.ITokenizer;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentenceCollection;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionRuntimeException;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsTypeConsistent;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Dataset of {@link SingleSentenceLex}.
 *
 * @author Yoav Artzi
 */
public class SingleSentenceLexDataset
		implements IDataCollection<SingleSentenceLex> {

	private static final long				serialVersionUID	= 2696871460450666311L;
	private final List<SingleSentenceLex>	data;

	public SingleSentenceLexDataset(List<SingleSentenceLex> data) {
		this.data = Collections.unmodifiableList(data);
	}

	public static SingleSentenceLexDataset read(File f,
			ICategoryServices<LogicalExpression> categoryServices,
			String entriesOrigin) {
		return read(f, new StubStringFilter(), categoryServices, entriesOrigin,
				null);
	}

	public static SingleSentenceLexDataset read(File f,
			ICategoryServices<LogicalExpression> categoryServices,
			String entriesOrigin, ITokenizer tokenizer) {
		return read(f, new StubStringFilter(), categoryServices, entriesOrigin,
				tokenizer);
	}

	public static SingleSentenceLexDataset read(File f,
			IStringFilter textFilter,
			ICategoryServices<LogicalExpression> categoryServices,
			String entriesOrigin, ITokenizer tokenizer) {
		int readLineCounter = 0;
		try {
			// Open the file
			final List<SingleSentenceLex> data = new LinkedList<SingleSentenceLex>();
			try (final BufferedReader in = new BufferedReader(
					new FileReader(f))) {
				String line;
				String currentSentence = null;
				LogicalExpression currentExpression = null;
				Map<String, String> currentProperties = null;
				while ((line = in.readLine()) != null) {
					++readLineCounter;
					if (line.startsWith("//")
							|| line.equals("") && currentSentence == null) {
						// Case comment or empty line between examples, skip
						continue;
					}
					line = line.trim();
					if (currentSentence == null) {
						// Case we don't have a sentence, so we are supposed to
						// get a sentence.
						currentSentence = textFilter.filter(line);
					} else if (currentProperties == null
							&& Properties.isPropertiesLine(line)) {
						currentProperties = Properties.readProperties(line);
					} else if (currentExpression == null) {
						// Case we don't have a logical expression, so we are
						// supposed to get it and create the data item.

						// Get the logical expression string. Consume lines
						// until we have balanced parentheses. In case we have a
						// indented expression.
						final StringBuilder expString = new StringBuilder(line);
						int paranthesisCount = SingleSentenceCollection
								.countParanthesis(line);
						while (paranthesisCount > 0) {
							line = in.readLine();
							++readLineCounter;
							paranthesisCount += SingleSentenceCollection
									.countParanthesis(line);
							expString.append("\n").append(line);
						}

						try {
							currentExpression = Simplify.of(LogicalExpression
									.read(expString.toString()));
						} catch (final LogicalExpressionRuntimeException e) {
							// Wrap with a dataset exception and throw.
							throw new FileReadingException(e, readLineCounter,
									f.getName());
						}
						final Pair<Boolean, String> typeChecking = IsTypeConsistent
								.ofVerbose(currentExpression);
						if (!typeChecking.first()) {
							// Throw exception
							throw new FileReadingException(
									"Expression not well-typed ["
											+ typeChecking.second() + "]:"
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
										.<LogicalExpression> read(line,
												textFilter, categoryServices,
												entriesOrigin));
							}
							line = in.readLine();
							++readLineCounter;
						}

						// Create the data item.
						final SingleSentenceLex dataItem;
						final Sentence sentence = tokenizer == null
								? new Sentence(currentSentence)
								: new Sentence(currentSentence, tokenizer);
						if (currentProperties != null) {
							dataItem = new SingleSentenceLex(sentence,
									currentExpression, entries,
									currentProperties);
						} else {
							dataItem = new SingleSentenceLex(sentence,
									currentExpression, entries);
						}
						data.add(dataItem);

						// Reset the accumulated data.
						currentSentence = null;
						currentProperties = null;
						currentExpression = null;
					}
				}
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

	public static class Creator
			implements IResourceObjectCreator<SingleSentenceLexDataset> {

		@SuppressWarnings("unchecked")
		@Override
		public SingleSentenceLexDataset create(Parameters params,
				IResourceRepository repo) {
			return SingleSentenceLexDataset
					.read(params.getAsFile("file"), new StubStringFilter(),
							(ICategoryServices<LogicalExpression>) repo
									.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
							params.get("origin"),
							(ITokenizer) (params.contains("tokenizer")
									? repo.get(params.get("tokenizer"))
									: null));
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
									"Dataset for pairs of sentences and logical forms with sentence-specific lexical entries")
							.addParam("origin", String.class,
									"The origin of the lexical entries")
							.addParam("tokenizer", ITokenizer.class,
									"Tokenizer to process the sentence string (default: default tokenizer)")
							.addParam("file", "file",
									"File with pairs of sentences and logical forms. The file will include a line with sentence, a line with a LF, a few lines with lexical entries, a empty line, a line with a sentence, and so on")
							.build();
		}

	}

}
