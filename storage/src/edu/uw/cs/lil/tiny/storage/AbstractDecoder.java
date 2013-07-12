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
package edu.uw.cs.lil.tiny.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An object that can be stored in a file. Each stored file contains a header of
 * value=attribute lines. The header ends at the first empty line and the rest
 * is object specific data.
 * 
 * @author Yoav Artzi
 */
public abstract class AbstractDecoder<C> implements IDecoder<C> {
	private static final String		VALUE_ATTRIBUTE_SEP		= "=";
	
	protected static final String	CLASS_ATTRIBUTE_NAME	= "class";
	
	protected static final String	VERSION_ATTRIBUTE_NAME	= "version";
	
	private final Class<?>			targetClass;
	
	protected AbstractDecoder(Class<?> targetClass) {
		this.targetClass = targetClass;
	}
	
	/**
	 * Reads a single line from the given buffer and strips its comment, if
	 * exist.
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	protected static String readTextLine(BufferedReader reader)
			throws IOException {
		final String line = reader.readLine();
		if (line == null) {
			return null;
		} else {
			final String[] split = line.split("//", 2);
			final String content = split[0].trim();
			if (content.equals("") && split.length > 1) {
				// Case the line included only a comment
				return readTextLine(reader);
			} else {
				return content;
			}
		}
	}
	
	/**
	 * Read the attribute=value header. The must end with an empty line.
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	protected static final Map<String, String> readValueAttributeHeader(
			BufferedReader reader) throws IOException {
		String line;
		final Map<String, String> valueAttributes = new HashMap<String, String>();
		while ((line = readTextLine(reader)) != null && !line.trim().equals("")) {
			final String[] split = line.trim().split("=", 2);
			valueAttributes.put(split[0], split[1]);
		}
		if (line.trim().equals("")) {
			return valueAttributes;
		} else {
			throw new IOException("Header not followed by empty line");
		}
	}
	
	public abstract C decode(File file) throws IOException;
	
	/**
	 * Encode object to given file.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public abstract void encode(C object, File file) throws IOException;
	
	public abstract int getVersion();
	
	/**
	 * Create attribute-value map. This map shouldn't include the class name and
	 * version. They will be added by {@link AbstractDecoder} when actually
	 * writing the attributes file.
	 * 
	 * @return
	 */
	protected abstract Map<String, String> createAttributesMap(C object);
	
	/**
	 * Write the header of the file. The header ends in an empty line.
	 * 
	 * @param valueAttributes
	 * @throws IOException
	 */
	protected final void writeHeader(Map<String, String> valueAttributes,
			BufferedWriter writer) throws IOException {
		// Write class=XXXXX attribute
		writer.write(CLASS_ATTRIBUTE_NAME);
		writer.write(VALUE_ATTRIBUTE_SEP);
		writer.write(targetClass.getName());
		writer.write('\n');
		
		// Write version=XX attribute
		writer.write(VERSION_ATTRIBUTE_NAME);
		writer.write(VALUE_ATTRIBUTE_SEP);
		writer.write(String.valueOf(getVersion()));
		writer.write('\n');
		
		// Write the attributes from the map
		for (final Map.Entry<String, String> entry : valueAttributes.entrySet()) {
			writer.write(entry.getKey());
			writer.write(VALUE_ATTRIBUTE_SEP);
			writer.write(entry.getValue());
			writer.write('\n');
		}
		
		// Write an empty line. Header must end with an empty line.
		writer.write('\n');
	}
}
