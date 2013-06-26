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
package edu.uw.cs.tiny.utils.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Decoder for storing object into a file.
 * 
 * @author Yoav Artzi
 */
public abstract class AbstractDecoderIntoFile<C> extends AbstractDecoder<C> {
	
	private static final String	DEPENDENT_FILE_KEY_PREFIX	= "DEPENDENT_FILE:";
	
	protected AbstractDecoderIntoFile(Class<?> targetClass) {
		super(targetClass);
	}
	
	@Override
	public final C decode(File file) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		
		// Read the attributes map
		final Map<String, String> attributes = readValueAttributeHeader(reader);
		
		// Read saved dependent files into a key->file mapping
		final Map<String, File> dependentFiles = new HashMap<String, File>();
		final Iterator<Entry<String, String>> iterator = attributes.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			final Entry<String, String> entry = iterator.next();
			if (entry.getKey().startsWith(DEPENDENT_FILE_KEY_PREFIX)) {
				dependentFiles.put(
						entry.getKey().substring(
								DEPENDENT_FILE_KEY_PREFIX.length()), new File(
								file.getParentFile(), entry.getValue()));
				iterator.remove();
			}
		}
		
		// Read the rest of the data, create the object and return it
		return doDecode(attributes, dependentFiles, reader);
	}
	
	@Override
	public final void encode(C object, File file) throws IOException {
		// Create writer to output file
		final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		// Get attributes map for simple members and such
		final Map<String, String> attributes = createAttributesMap(object);
		
		// Store dependent files and get their file objects
		final Map<String, File> dependentFiles = encodeDependentFiles(object,
				file.getParentFile(), file);
		
		// Load dependent files into the attributes map to save in the header
		for (final Map.Entry<String, File> entry : dependentFiles.entrySet()) {
			attributes.put(DEPENDENT_FILE_KEY_PREFIX + entry.getKey(), entry
					.getValue().getName());
		}
		
		// Write header
		writeHeader(attributes, writer);
		
		// Write free form data
		doEncode(object, writer);
		
		// Close the writer
		writer.flush();
		writer.close();
	}
	
	protected abstract C doDecode(Map<String, String> attributes,
			Map<String, File> dependentFiles, BufferedReader reader)
			throws IOException;
	
	protected abstract void doEncode(C object, BufferedWriter writer)
			throws IOException;
	
	/**
	 * Write any dependent files (i.e. dumping of members that are complex
	 * objects.
	 * 
	 * @param object
	 * @param directory
	 *            The directory to store these files
	 * @param parentFile
	 *            The parent file is given, as it's often wise to use its name
	 *            to name dependent files.
	 * @return Mapping of keys that represent the members to the file written.
	 * @throws IOException
	 */
	protected abstract Map<String, File> encodeDependentFiles(C object,
			File directory, File parentFile) throws IOException;
}
