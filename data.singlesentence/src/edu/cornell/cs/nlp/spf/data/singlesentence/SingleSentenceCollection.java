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
package edu.cornell.cs.nlp.spf.data.singlesentence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.base.properties.Properties;
import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.ITokenizer;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionRuntimeException;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsTypeConsistent;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.cornell.cs.nlp.utils.composites.Pair;

/**
 * Collection of {@link SingleSentence}.
 *
 * @author Yoav Artzi
 */
public class SingleSentenceCollection
		implements IDataCollection<SingleSentence> {

	private static final long			serialVersionUID	= 8478343465195617940L;
	private final List<SingleSentence>	data;

	public SingleSentenceCollection(List<SingleSentence> data) {
		this.data = Collections.unmodifiableList(data);
	}

	public static int countParanthesis(String string) {
		int count = 0;
		for (final char c : string.toCharArray()) {
			if (c == '(') {
				++count;
			} else if (c == ')') {
				--count;
			}
		}
		return count;
	}

	public static SingleSentenceCollection read(File f) {
		return read(f, new StubStringFilter(), null);
	}

	public static SingleSentenceCollection read(File f,
			IStringFilter textFilter, ITokenizer tokenizer) {
		int readLineCounter = 0;
		try {
			// Open the file
			final List<SingleSentence> data = new LinkedList<SingleSentence>();
			try (final BufferedReader in = new BufferedReader(
					new FileReader(f))) {
				String line;
				String currentSentence = null;
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
							&& Properties.isPropertiesLine(line)) {
						currentProperties = Properties.readProperties(line);
					} else {
						// Case we don't have a logical expression, so we are
						// supposed to get it and create the data item
						final LogicalExpression exp;

						// Get the logical expression string. Consume lines
						// until we have balanced parentheses. In case we have a
						// indented expression.
						final StringBuilder expString = new StringBuilder(line);
						int paranthesisCount = countParanthesis(line);
						while (paranthesisCount > 0) {
							line = in.readLine();
							++readLineCounter;
							paranthesisCount += countParanthesis(line);
							expString.append("\n").append(line);
						}

						try {
							exp = Simplify.of(LogicalExpression
									.read(expString.toString()));
						} catch (final LogicalExpressionRuntimeException e) {
							// Wrap with a dataset exception and throw.
							throw new FileReadingException(e, readLineCounter,
									f.getName());
						}
						final Pair<Boolean, String> typeChecking = IsTypeConsistent
								.ofVerbose(exp);
						if (!typeChecking.first()) {
							// Throw exception
							throw new FileReadingException(
									"Expression not well-typed ["
											+ typeChecking.second() + "]:"
											+ exp,
									readLineCounter, f.getName());
						}
						final SingleSentence dataItem;
						final Sentence sentence = tokenizer == null
								? new Sentence(currentSentence)
								: new Sentence(currentSentence, tokenizer);
						if (currentProperties != null) {
							dataItem = new SingleSentence(sentence, exp,
									currentProperties);
						} else {
							dataItem = new SingleSentence(sentence, exp);
						}
						data.add(dataItem);
						currentSentence = null;
						currentProperties = null;
					}
				}
			}
			return new SingleSentenceCollection(data);
		} catch (final Exception e) {
			// Wrap with dataset exception and throw
			throw new FileReadingException(e, readLineCounter, f.getName());
		}
	}

	public static SingleSentenceCollection read(File f, ITokenizer tokenizer) {
		return read(f, new StubStringFilter(), tokenizer);
	}

	@Override
	public Iterator<SingleSentence> iterator() {
		return data.iterator();
	}

	@Override
	public int size() {
		return data.size();
	}

	public static class Creator
			implements IResourceObjectCreator<SingleSentenceCollection> {

		@Override
		public SingleSentenceCollection create(Parameters params,
				IResourceRepository repo) {
			return SingleSentenceCollection.read(params.getAsFile("file"),
					(IStringFilter) (params.contains("filter")
							? repo.get(params.get("filter"))
							: new StubStringFilter()),
					(ITokenizer) (params.contains("tokenizer")
							? repo.get(params.get("tokenizer")) : null));
		}

		@Override
		public String type() {
			return "data.single";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					SingleSentenceCollection.class)
							.setDescription(
									"Collection for pairs of sentences and logical forms")
							.addParam("tokenizer", ITokenizer.class,
									"Tokenizer to process the sentence string (default: default tokenizer)")
							.addParam("filter", IStringFilter.class,
									"Filter to process input strings (default: identify filter)")
							.addParam("file", "file",
									"File with pairs of sentences and logical forms. The file will include a line with sentence, a line with a LF, empty line, a line with a sentence, and so on")
							.build();
		}

	}
}
