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
package edu.uw.cs.lil.tiny.explat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jregex.MatchResult;
import jregex.Matcher;
import jregex.Pattern;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;
import edu.uw.cs.lil.tiny.base.exceptions.FileReadingException;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.counter.Counter;

public abstract class ParameterizedExperiment implements IResourceRepository {
	
	public static final String				CATEGORY_SERVICES_RESOURCE	= "categoryServices";
	public static final String				DOMAIN_ONTOLOGY_RESOURCE	= "domainOntology";
	public static final String				EXECUTOR_RESOURCE			= "executor";
	public static final String				ONTOLOGY_RESOURCE			= "ontology";
	
	public static final String				PARSER_RESOURCE				= "parser";
	private static final String				INCLUDE_DIRECTIVE			= "include";
	private static final Pattern			LINE_REPEAT_PATTERN			= new Pattern(
																				"\\[({var}\\w+)=({start}\\d+)-({end}\\d+)\\]\\s+({rest}.+)$");
	private static final Pattern			PARAM_SPLIT_PATTERN			= new Pattern(
																				"(?<!\\\\)\\s");
	private static final Pattern			VAR_REF						= new Pattern(
																				"%\\{({var}[\\w@]+)\\}");
	private final ResourceCreatorRepository	creatorRepo;
	private final Map<String, Object>		resources					= new HashMap<String, Object>();
	private final File						rootDir;
	
	protected final Parameters				globalParams;
	protected final List<Parameters>		jobParams;
	
	protected final List<Parameters>		resourceParams;
	
	public ParameterizedExperiment(File file, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo) throws IOException {
		this(new FileReader(file), envParams, creatorRepo,
				file.getParentFile() == null ? new File(".") : file
						.getParentFile());
	}
	
	public ParameterizedExperiment(File file,
			ResourceCreatorRepository creatorRepo) throws IOException {
		this(file, Collections.<String, String> emptyMap(), creatorRepo);
	}
	
	public ParameterizedExperiment(Reader reader,
			Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo, File rootDir)
			throws IOException {
		this.creatorRepo = creatorRepo;
		this.rootDir = rootDir;
		
		final BufferedReader bufferedReader = new BufferedReader(reader);
		final Counter lineCounter = new Counter();
		
		try {
			String line;
			
			// Read parameters
			final Map<String, String> mutableParameters = new HashMap<String, String>();
			while ((line = readLineSkipComments(bufferedReader, lineCounter)) != null
					&& !line.trim().equals("")) {
				final String[] split = line.trim().split("=", 2);
				if (split[0].equals(INCLUDE_DIRECTIVE)) {
					mutableParameters
							.putAll(readIncludedParamsFile(makeAbsolute(new File(
									split[1]))));
				} else {
					mutableParameters.put(split[0], split[1]);
				}
			}
			this.globalParams = new Parameters(mutableParameters, true);
			
			// Overwrite global params with provided environment params
			for (final Map.Entry<String, String> entry : envParams.entrySet()) {
				globalParams.parametersMap
						.put(entry.getKey(), entry.getValue());
			}
			
			// Read resources
			this.resourceParams = readSectionLines(bufferedReader, lineCounter);
			
			// Read jobs
			this.jobParams = readSectionLines(bufferedReader, lineCounter);
			
		} catch (final Exception e) {
			throw new FileReadingException(e, lineCounter.value());
		} finally {
			bufferedReader.close();
		}
	}
	
	private static Map<String, String> readIncludedParamsFile(File file)
			throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		final Counter lineCounter = new Counter();
		try {
			final Map<String, String> parameters = new HashMap<String, String>();
			String line;
			while ((line = readLineSkipComments(reader, lineCounter)) != null
					&& !line.trim().equals("")) {
				final String[] split = line.trim().split("=", 2);
				parameters.put(split[0], split[1]);
			}
			return Collections.unmodifiableMap(parameters);
		} catch (final Exception e) {
			throw new FileReadingException(e, lineCounter.value());
		} finally {
			reader.close();
		}
	}
	
	private static String readLineSkipComments(BufferedReader reader,
			Counter lineCounter) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			lineCounter.inc();
			if (!line.startsWith("#")) {
				return line = line.split("\\s*//")[0];
			}
		}
		return line;
	}
	
	public IResourceObjectCreator<?> getCreator(String type) {
		return creatorRepo.getCreator(type);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getResource(String id) {
		if (resources.containsKey(id)) {
			return (T) resources.get(id);
		} else {
			throw new IllegalStateException("Invalid resource: " + id);
		}
	}
	
	public boolean hasResource(String id) {
		return resources.containsKey(id);
	}
	
	public File makeAbsolute(File file) {
		if (file.isAbsolute()) {
			return file;
		} else {
			return new File(rootDir, file.toString());
		}
	}
	
	private boolean isIncludeLine(String line) {
		return line.startsWith(INCLUDE_DIRECTIVE);
	}
	
	private List<Parameters> parseAttributesLine(String line) {
		final Matcher matcher = LINE_REPEAT_PATTERN.matcher(line);
		if (matcher.matches()) {
			final String var = matcher.group("var");
			final int end = Integer.valueOf(matcher.group("end"));
			final List<Parameters> paramsList = new LinkedList<Parameters>();
			for (int start = Integer.valueOf(matcher.group("start")); start <= end; ++start) {
				paramsList.addAll(parseAttributesLine(matcher.group("rest")
						.replace(var, String.valueOf(start))));
			}
			return paramsList;
		} else {
			return ListUtils
					.createSingletonList(parseAttributesLine(PARAM_SPLIT_PATTERN
							.tokenizer(line).split()));
		}
	}
	
	private Parameters parseAttributesLine(String[] line) {
		final Map<String, String> ret = new HashMap<String, String>();
		for (final String pair : line) {
			final String[] splitPair = pair.split("=", 2);
			ret.put(splitPair[0], splitPair[1].replace("\\ ", " "));
		}
		return new Parameters(ret);
	}
	
	private List<String> readIncludedLines(File file) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		final Counter lineCounter = new Counter();
		try {
			String line;
			final List<String> lines = new LinkedList<String>();
			while ((line = readLineSkipComments(reader, lineCounter)) != null
					&& !line.trim().equals("")) {
				lines.add(line);
			}
			return lines;
		} catch (final Exception e) {
			throw new FileReadingException(e, lineCounter.value());
		} finally {
			reader.close();
		}
	}
	
	private List<Parameters> readLine(String line) throws IOException {
		if (isIncludeLine(line)) {
			final List<Parameters> ret = new LinkedList<Parameters>();
			for (final String includedLine : readIncludedLines(makeAbsolute(new File(
					line.trim().split("=", 2)[1])))) {
				ret.addAll(readLine(includedLine));
			}
			return ret;
		} else {
			return parseAttributesLine(line);
		}
	}
	
	private List<Parameters> readSectionLines(BufferedReader reader,
			Counter lineCounter) throws IOException {
		final List<Parameters> ret = new LinkedList<Parameters>();
		String line;
		while ((line = readLineSkipComments(reader, lineCounter)) != null
				&& !line.trim().equals("")) {
			ret.addAll(readLine(line));
		}
		return Collections.unmodifiableList(ret);
	}
	
	protected <T> void storeResource(String id, T resource) {
		if (resources.containsKey(id)) {
			throw new IllegalStateException("Resource already exists: " + id);
		} else {
			resources.put(id, resource);
		}
	}
	
	public class Parameters implements Iterable<Pair<String, String>> {
		private final boolean				global;
		private final Map<String, String>	parametersMap;
		
		final private Replacer				substitutionReplacer	= new Replacer(
																			VAR_REF,
																			new Substitution() {
																				
																				@Override
																				public void appendSubstitution(
																						MatchResult match,
																						TextBuffer dest) {
																					dest.append(get(match
																							.group("var")));
																				}
																			});
		
		public Parameters(Map<String, String> parametersMap) {
			this(parametersMap, false);
		}
		
		public Parameters(Map<String, String> parametersMap, boolean global) {
			this.parametersMap = parametersMap;
			this.global = global;
		}
		
		public boolean contains(String name) {
			return get(name) != null;
		}
		
		/**
		 * Get parameter from a local map. Resolve a link to global parameters
		 * if present.
		 * 
		 * @param parametersMap
		 * @param name
		 * @return
		 */
		public String get(String name) {
			if (!parametersMap.containsKey(name)) {
				if (global) {
					return null;
				} else {
					return substituteVars(globalParams.get(name));
				}
			} else {
				return substituteVars(parametersMap.get(name));
			}
		}
		
		public String get(String name, String defaultValue) {
			if (!parametersMap.containsKey(name)) {
				if (global) {
					return defaultValue;
				} else {
					return substituteVars(globalParams.get(name));
				}
			} else {
				return substituteVars(parametersMap.get(name));
			}
		}
		
		public boolean getAsBoolean(String name) {
			return "true".equals(get(name));
		}
		
		public boolean getAsBoolean(String name, boolean defaultValue) {
			if (contains(name)) {
				return "true".equals(get(name));
			} else {
				return defaultValue;
			}
		}
		
		public double getAsDouble(String name) {
			return Double.valueOf(get(name));
		}
		
		public double getAsDouble(String name, double defaultValue) {
			if (contains(name)) {
				return Double.valueOf(get(name));
			} else {
				return defaultValue;
			}
		}
		
		public File getAsFile(String name) {
			return makeAbsolute(new File(get(name)));
		}
		
		public List<File> getAsFiles(String name) {
			final String value = get(name);
			final List<File> ret = new LinkedList<File>();
			if (value != null) {
				for (final String filename : value.split(":")) {
					ret.add(makeAbsolute(new File(filename)));
				}
			}
			return ret;
		}
		
		public double getAsFloat(String name) {
			return Float.valueOf(get(name));
		}
		
		public int getAsInteger(String name) {
			return Integer.valueOf(get(name));
		}
		
		public int getAsInteger(String name, int defaultValue) {
			if (contains(name)) {
				return Integer.valueOf(get(name));
			} else {
				return defaultValue;
			}
		}
		
		public List<String> getSplit(String name) {
			final String value = get(name);
			if (value == null) {
				return Collections.emptyList();
			} else {
				return Arrays.asList(value.split(","));
			}
		}
		
		@Override
		public Iterator<Pair<String, String>> iterator() {
			return Collections
					.unmodifiableList(
							ListUtils.map(
									parametersMap.entrySet(),
									new ListUtils.Mapper<Map.Entry<String, String>, Pair<String, String>>() {
										
										@Override
										public Pair<String, String> process(
												Entry<String, String> obj) {
											return Pair.of(obj.getKey(),
													obj.getValue());
										}
									})).iterator();
		}
		
		@Override
		public String toString() {
			return "Parameters [global=" + global + ", parametersMap="
					+ parametersMap + "]";
		}
		
		private String substituteVars(String string) {
			if (string == null) {
				return string;
			} else {
				return substitutionReplacer.replace(string);
			}
		}
	}
}
