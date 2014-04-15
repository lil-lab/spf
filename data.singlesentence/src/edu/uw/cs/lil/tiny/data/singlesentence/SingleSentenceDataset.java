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
package edu.uw.cs.lil.tiny.data.singlesentence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jregex.Pattern;
import edu.uw.cs.lil.tiny.base.exceptions.FileReadingException;
import edu.uw.cs.lil.tiny.base.string.IStringFilter;
import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpressionRuntimeException;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsTypeConsistent;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;

/**
 * Dataset of {@link SingleSentence}.
 * 
 * @author Yoav Artzi
 */
public class SingleSentenceDataset implements IDataCollection<SingleSentence> {
	private static final Pattern		PROPERTIES_PATTERN	= new Pattern(
																	"[^=\\s]+=.+(\\t+[^=\\s]+=.+)*");
	
	private final List<SingleSentence>	data;
	
	public SingleSentenceDataset(List<SingleSentence> data) {
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
	
	public static boolean isPropertiesLine(String line) {
		return PROPERTIES_PATTERN.matches(line);
	}
	
	public static SingleSentenceDataset read(File f, IStringFilter textFilter) {
		int readLineCounter = 0;
		try {
			// Open the file
			final BufferedReader in = new BufferedReader(new FileReader(f));
			final List<SingleSentence> data = new LinkedList<SingleSentence>();
			try {
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
							&& isPropertiesLine(line)) {
						currentProperties = readProperties(line);
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
							exp = Simplify.of(LogicalExpression.read(expString
									.toString()));
						} catch (final LogicalExpressionRuntimeException e) {
							// wrap with a dataset exception and throw
							in.close();
							throw new FileReadingException(e, readLineCounter,
									f.getName());
						}
						if (!IsTypeConsistent.of(exp)) {
							// Throw exception
							throw new FileReadingException(
									"Expression not well-typed: " + exp,
									readLineCounter, f.getName());
						}
						final SingleSentence dataItem;
						if (currentProperties != null) {
							dataItem = new SingleSentence(new Sentence(
									currentSentence), exp, currentProperties);
						} else {
							dataItem = new SingleSentence(new Sentence(
									currentSentence), exp);
						}
						data.add(dataItem);
						currentSentence = null;
						currentProperties = null;
					}
				}
			} finally {
				in.close();
			}
			return new SingleSentenceDataset(data);
		} catch (final Exception e) {
			// Wrap with dataset exception and throw
			throw new FileReadingException(e, readLineCounter, f.getName());
		}
	}
	
	public static Map<String, String> readProperties(String line) {
		final String[] split = line.split("\\t+");
		final Map<String, String> properties = new HashMap<String, String>();
		for (final String entry : split) {
			final String[] entrySplit = entry.split("=", 2);
			properties.put(entrySplit[0], entrySplit[1]);
		}
		return properties;
	}
	
	@Override
	public Iterator<SingleSentence> iterator() {
		return data.iterator();
	}
	
	@Override
	public int size() {
		return data.size();
	}
	
	public static class Creator implements
			IResourceObjectCreator<SingleSentenceDataset> {
		
		@Override
		public SingleSentenceDataset create(Parameters parameters,
				IResourceRepository resourceRepo) {
			return SingleSentenceDataset.read(parameters.getAsFile("file"),
					new StubStringFilter());
		}
		
		@Override
		public String type() {
			return "data.single";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					SingleSentenceDataset.class)
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
