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
package edu.cornell.cs.nlp.spf.base.properties;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jregex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Service class for key value properties line.
 *
 * @author Yoav Artzi
 */
public class Properties {
	public static final ILogger		LOG								= LoggerFactory
																			.create(Properties.class);
	private static final String		JSON_PREFIX						= "JSON";
	private static final String		OLD_KEY_VALUE_SEPARATOR			= "=";
	private static final Pattern	OLD_PROPERTIES_PATTERN;

	private static final char		OLD_PROPERTY_SEPARATOR_CHAR		= '\t';

	private static final String		OLD_PROPERTY_SEPARATOR_REGEX	= "\\t+";

	private Properties() {
		// Service class.
	}

	static {
		OLD_PROPERTIES_PATTERN = new Pattern("[^" + OLD_KEY_VALUE_SEPARATOR
				+ "\\s]+" + OLD_KEY_VALUE_SEPARATOR + ".+("
				+ OLD_PROPERTY_SEPARATOR_REGEX + "[^" + OLD_KEY_VALUE_SEPARATOR
				+ "\\s]+" + OLD_KEY_VALUE_SEPARATOR + ".+)*");
	}

	public static boolean isPropertiesLine(String line) {
		return line.startsWith(JSON_PREFIX)
				|| OLD_PROPERTIES_PATTERN.matches(line);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> readProperties(String line) {
		if (line.startsWith(JSON_PREFIX)) {
			final JSONParser parser = new JSONParser();
			try {
				final Object jsonObject = parser.parse(line
						.substring(JSON_PREFIX.length()));
				if (jsonObject instanceof Map) {
					return (Map<String, String>) jsonObject;
				} else if (jsonObject instanceof JSONArray) {
					final Map<String, String> map = createMap((JSONArray) jsonObject);
					if (map == null) {
						throw new IllegalArgumentException(
								"Failed to read JSON properties: " + line);
					}
					return map;
				} else {
					throw new IllegalArgumentException(
							"Failed to read JSON properties: " + line);
				}
			} catch (final ParseException e) {
				LOG.error("Failed to read JSON properties: %s", line);
				throw new IllegalArgumentException(
						"Failed to read JSON properties: " + line);
			}
		} else {
			// Backward compatibility.
			final String[] split = line.split(OLD_PROPERTY_SEPARATOR_REGEX);
			final Map<String, String> properties = new HashMap<String, String>();
			for (final String entry : split) {
				final String[] entrySplit = entry.split(
						OLD_KEY_VALUE_SEPARATOR, 2);
				properties.put(entrySplit[0], entrySplit[1]);
			}
			return properties;
		}
	}

	public static String toString(Map<String, String> properties) {
		return toString(properties, false);
	}

	public static String toString(Map<String, String> properties,
			boolean useOldVersion) {
		if (useOldVersion) {
			final StringBuilder sb = new StringBuilder();
			final Iterator<Entry<String, String>> iterator = properties
					.entrySet().iterator();
			while (iterator.hasNext()) {
				final Entry<String, String> property = iterator.next();
				sb.append(property.getKey());
				sb.append(OLD_KEY_VALUE_SEPARATOR);
				sb.append(property.getValue());
				if (iterator.hasNext()) {
					sb.append(OLD_PROPERTY_SEPARATOR_CHAR);
				}
			}
			return sb.toString();
		} else {
			return JSON_PREFIX + JSONValue.toJSONString(properties);
		}
	}

	private static Map<String, String> createMap(JSONArray array) {
		final Map<String, String> map = new HashMap<String, String>();
		for (final Object entry : array) {
			if (entry instanceof JSONArray) {
				final JSONArray entryArray = (JSONArray) entry;
				if (entryArray.size() == 2
						&& entryArray.get(0) instanceof String
						&& entryArray.get(1) instanceof String) {
					map.put((String) entryArray.get(0),
							(String) entryArray.get(1));
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		return map;
	}

}
