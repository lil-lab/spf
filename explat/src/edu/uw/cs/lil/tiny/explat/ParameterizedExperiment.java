/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jregex.Matcher;
import jregex.Pattern;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;

public abstract class ParameterizedExperiment implements IResourceRepository {
	
	public static final String			CATEGORY_SERVICES_RESOURCE	= "categoryServices";
	public static final String			DECODER_HELPER_RESOURCE		= "decoderHelper";
	public static final String			DOMAIN_ONTOLOGY_RESOURCE	= "domainOntology";
	public static final String			ONTOLOGY_RESOURCE			= "ontology";
	public static final String			PARSER_RESOURCE				= "parser";
	
	private static final String			INCLUDE_DIRECTIVE			= "include";
	private static final Pattern		LINE_REPEAT_PATTERN			= new Pattern(
																			"\\[({var}\\w+)=({start}\\d+)-({end}\\d+)\\]\\s+({rest}.+)$");
	private final Map<String, Object>	resources					= new HashMap<String, Object>();
	
	private final File					rootDir;
	protected final Parameters			globalParams;
	protected final List<Parameters>	jobParams;
	protected final List<Parameters>	resourceParams;
	
	public ParameterizedExperiment(File file) throws IOException {
		this.rootDir = file.getParentFile() == null ? new File(".") : file
				.getParentFile();
		
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line;
		
		// Read parameters
		final Map<String, String> mutableParameters = new HashMap<String, String>();
		while ((line = readLineSkipComments(reader)) != null
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
		
		// Read resources
		this.resourceParams = readSectionLines(reader);
		
		// Read jobs
		this.jobParams = readSectionLines(reader);
		
	}
	
	private static Map<String, String> readIncludedParamsFile(File file)
			throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		final Map<String, String> parameters = new HashMap<String, String>();
		String line;
		while ((line = readLineSkipComments(reader)) != null
				&& !line.trim().equals("")) {
			final String[] split = line.trim().split("=", 2);
			parameters.put(split[0], split[1]);
		}
		return Collections.unmodifiableMap(parameters);
	}
	
	private static String readLineSkipComments(BufferedReader reader)
			throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("#")) {
				return line = line.split("\\s*//")[0];
			}
		}
		return line;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getResource(String id) {
		if (resources.containsKey(id)) {
			return (T) resources.get(id);
		} else {
			throw new IllegalStateException("Invalid resource: " + id);
		}
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
			final String[] split = line.split("\\s+");
			return ListUtils.createSingletonList(parseAttributesLine(split));
		}
	}
	
	private Parameters parseAttributesLine(String[] line) {
		final Map<String, String> ret = new HashMap<String, String>();
		for (final String pair : line) {
			final String[] splitPair = pair.split("=", 2);
			ret.put(splitPair[0], splitPair[1]);
		}
		
		return new Parameters(ret);
	}
	
	private List<String> readIncludedLines(File file) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		final List<String> lines = new LinkedList<String>();
		while ((line = readLineSkipComments(reader)) != null
				&& !line.trim().equals("")) {
			lines.add(line);
		}
		return lines;
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
	
	private List<Parameters> readSectionLines(BufferedReader reader)
			throws IOException {
		final List<Parameters> ret = new LinkedList<Parameters>();
		String line;
		while ((line = readLineSkipComments(reader)) != null
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
					return globalParams.get(name);
				}
			} else if (parametersMap.get(name).startsWith("@")) {
				return globalParams.get(parametersMap.get(name).substring(1));
			} else {
				return parametersMap.get(name);
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
		
	}
}
