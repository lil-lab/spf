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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Services for decoders.
 * 
 * @author Yoav Artzi
 */
public class DecoderServices {
	private static final ILogger	LOG								= LoggerFactory
																			.create(DecoderServices.class);
	private static final String		STATIC_GET_DECODER_METHOD_NAME	= "getDecoder";
	
	private DecoderServices() {
		// Services class, don't instantiate
	}
	
	/**
	 * Given a file, get the class name from the header, locate the class and
	 * instantiate the object.
	 * 
	 * @param <C>
	 * @param <Y>
	 * @param file
	 * @param decoderHelper
	 * @return
	 * @throws IOException
	 * @throws
	 */
	public static <C, Y> C decode(File file, DecoderHelper<Y> decoderHelper)
			throws IOException {
		final BufferedReader reader;
		if (file.isDirectory()) {
			reader = new BufferedReader(new FileReader(new File(file,
					AbstractDecoderIntoDir.ATTRIBUTE_FILE_NAME)));
		} else if (file.isFile()) {
			reader = new BufferedReader(new FileReader(file));
		} else {
			throw new IOException("File not found");
		}
		try {
			return decode(
					AbstractDecoder.readValueAttributeHeader(reader).get(
							AbstractDecoder.CLASS_ATTRIBUTE_NAME), file,
					decoderHelper);
		} finally {
			reader.close();
		}
	}
	
	/**
	 * Given a class name and a file, instantiate the class using the file.
	 * 
	 * @param name
	 *            Class name
	 * @param file
	 *            File written by StorableFile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <C, Y> C decode(String name, File file,
			DecoderHelper<Y> decoderHelper) throws IOException {
		try {
			// Get the class of the object we want to decode
			final Class<C> objClass = (Class<C>) Class.forName(name);
			
			final IDecoder<C> decoder = getDecoderForClass(objClass,
					decoderHelper);
			
			return decoder.decode(file);
		} catch (final ClassNotFoundException e) {
			LOG.error(
					"Exception (%s) while finding class through reflection: %s",
					e.getClass().getName(), name);
			throw new RuntimeException("Failed to get class", e);
		}
	}
	
	public static <C, Y> void encode(C object, File file,
			DecoderHelper<Y> decoderHelper) throws IOException {
		// Get the decoder
		@SuppressWarnings("unchecked")
		final IDecoder<C> decoder = (IDecoder<C>) getDecoderForClass(
				object.getClass(), decoderHelper);
		
		// Encode the object
		decoder.encode(object, file);
	}
	
	@SuppressWarnings("unchecked")
	private static <C, Y> IDecoder<C> getDecoderForClass(Class<C> c,
			DecoderHelper<Y> decoderHelper) {
		try {
			
			// First try to get a decoder that doesn't need a helper
			try {
				final Method method = c
						.getMethod(STATIC_GET_DECODER_METHOD_NAME);
				if (Modifier.isStatic(method.getModifiers())) {
					return (IDecoder<C>) method.invoke(null);
				}
			} catch (final NoSuchMethodException e) {
				// Didn't find the getDecoder method without the helper, so
				// continue to try with a helper, if we have one
			}
			
			// Case we failed to get a decoder with no helper, try to get a
			// decoder with a helper
			if (decoderHelper != null) {
				final Method method = c.getMethod(
						STATIC_GET_DECODER_METHOD_NAME, DecoderHelper.class);
				if (Modifier.isStatic(method.getModifiers())) {
					return (IDecoder<C>) method.invoke(null, decoderHelper);
				}
			}
			
			throw new RuntimeException(
					String.format(
							"Failed to get decoder for %s, check if the static method getDecoder() exists for this class",
							c.getName()));
			
		} catch (final Exception e) {
			throw new RuntimeException(
					String.format(
							"Failed to get decoder for %s, check if the static method getDecoder() exists for this class",
							c.getName()), e);
		}
	}
}
