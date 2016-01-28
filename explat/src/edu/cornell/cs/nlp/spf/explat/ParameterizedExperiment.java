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
package edu.cornell.cs.nlp.spf.explat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.ResourceCreatorRepository;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.counter.Counter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import jregex.Matcher;
import jregex.Pattern;
import jregex.Replacer;

public abstract class ParameterizedExperiment implements IResourceRepository {

	public static final String				CATEGORY_SERVICES_RESOURCE	= "categoryServices";
	public static final String				DOMAIN_ONTOLOGY_RESOURCE	= "domainOntology";
	public static final ILogger				LOG							= LoggerFactory
			.create(ParameterizedExperiment.class);

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
			ResourceCreatorRepository creatorRepo, File rootDir) {
		this.creatorRepo = creatorRepo;
		this.rootDir = rootDir;

		final Counter lineCounter = new Counter();

		try (final BufferedReader bufferedReader = new BufferedReader(
				new FileReader(file))) {
			String line;

			// Read parameters
			final Map<String, String> mutableParameters = new HashMap<String, String>();
			while ((line = readLineSkipComments(bufferedReader,
					lineCounter)) != null && !line.trim().equals("")) {
				final String[] split = line.trim().split("=", 2);
				if (split[0].equals(INCLUDE_DIRECTIVE)) {
					mutableParameters.putAll(readIncludedParamsFile(
							makeAbsolute(new File(split[1]))));
				} else {
					mutableParameters.put(split[0], split[1]);
				}
			}
			this.globalParams = new Parameters(mutableParameters, true);

			// Overwrite global params with provided environment params
			for (final Map.Entry<String, String> entry : envParams.entrySet()) {
				globalParams.parametersMap.put(entry.getKey(),
						entry.getValue());
			}

			// Read resources
			this.resourceParams = readSectionLines(bufferedReader, lineCounter);

			// Read jobs
			this.jobParams = readSectionLines(bufferedReader, lineCounter);

		} catch (final Exception e) {
			throw new FileReadingException(e, lineCounter.value());
		}
	}

	private static boolean isIncludeLine(String line) {
		return line.startsWith(INCLUDE_DIRECTIVE);
	}

	private static List<String> readIncludedLines(File file) {
		final Counter lineCounter = new Counter();
		try (final BufferedReader reader = new BufferedReader(
				new FileReader(file))) {
			String line;
			final List<String> lines = new LinkedList<String>();
			while ((line = readLineSkipComments(reader, lineCounter)) != null
					&& !line.trim().equals("")) {
				lines.add(line);
			}
			return lines;
		} catch (final Exception e) {
			throw new FileReadingException(e, lineCounter.value());
		}
	}

	private static Map<String, String> readIncludedParamsFile(File file) {
		final Counter lineCounter = new Counter();
		try (final BufferedReader reader = new BufferedReader(
				new FileReader(file))) {
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

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String id) {
		if (resources.containsKey(id)) {
			return (T) resources.get(id);
		} else {
			throw new IllegalStateException("Invalid resource: " + id);
		}
	}

	@Override
	public <T> T get(String id, T defaultObject) {
		if (resources.containsKey(id)) {
			return get(id);
		} else {
			return defaultObject;
		}
	}

	public IResourceObjectCreator<?> getCreator(String type) {
		return creatorRepo.getCreator(type);
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

	public void readResrouces() {
		for (final Parameters params : resourceParams) {
			final String type = params.get("type");
			final String id = params.get("id");
			if (getCreator(type) == null) {
				throw new IllegalArgumentException(
						"Invalid resource type: " + type);
			} else {
				LOG.info("Creating resource %s of type %s ...", id, type);
				storeResource(id, getCreator(type).create(params, this));
			}
		}
	}

	private List<Parameters> parseAttributesLine(String line) {
		final Matcher matcher = LINE_REPEAT_PATTERN.matcher(line);
		if (matcher.matches()) {
			final String var = matcher.group("var");
			final int end = Integer.valueOf(matcher.group("end"));
			final List<Parameters> paramsList = new LinkedList<Parameters>();
			for (int start = Integer
					.valueOf(matcher.group("start")); start <= end; ++start) {
				paramsList.addAll(parseAttributesLine(matcher.group("rest")
						.replace(var, String.valueOf(start))));
			}
			return paramsList;
		} else {
			return ListUtils.createSingletonList(parseAttributesLine(
					PARAM_SPLIT_PATTERN.tokenizer(line).split()));
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

	private List<Parameters> readLine(String line) throws IOException {
		if (isIncludeLine(line)) {
			final List<Parameters> ret = new LinkedList<Parameters>();
			for (final String includedLine : readIncludedLines(
					makeAbsolute(new File(line.trim().split("=", 2)[1])))) {
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
				VAR_REF, (match, dest) -> dest
						.append(globalParams.get(match.group("var"))));

		public Parameters(Map<String, String> parametersMap) {
			this(parametersMap, false);
		}

		public Parameters(Map<String, String> parametersMap, boolean global) {
			this.parametersMap = parametersMap;
			this.global = global;
		}

		public boolean contains(String name) {
			return parametersMap.containsKey(name)
					|| !global && globalParams.contains(name);
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
			final String value;
			if (!parametersMap.containsKey(name)) {
				if (global) {
					value = null;
				} else {
					value = substituteVars(globalParams.get(name));
				}
			} else {
				value = substituteVars(parametersMap.get(name));
			}
			if (value == null) {
				LOG.warn("Null parameter value: %s", name);
			}
			return value;
		}

		public String get(String name, String defaultValue) {
			if (contains(name)) {
				return get(name);
			} else {
				return defaultValue;
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
			final String pathname = get(name);
			if (pathname == null) {
				throw new IllegalArgumentException(
						"Unknown parameter: " + name);
			}
			return makeAbsolute(new File(pathname));
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

		public long getAsLong(String name) {
			return Long.valueOf(get(name));
		}

		public long getAsLong(String name, long defaultValue) {
			if (contains(name)) {
				return Long.valueOf(get(name));
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
			return parametersMap.entrySet().stream()
					.map(e -> Pair.of(e.getKey(), get(e.getKey()))).iterator();
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
