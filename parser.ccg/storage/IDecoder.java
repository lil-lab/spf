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

import java.io.File;
import java.io.IOException;

/**
 * Decoder to store and load objects from the file system.
 * 
 * @author Yoav Artzi
 * @param <C>
 */
public interface IDecoder<C> {
	/**
	 * Decode an object from the file system.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	C decode(File file) throws IOException;
	
	/**
	 * Encode an object to the file system.
	 * 
	 * @param object
	 * @param file
	 * @throws IOException
	 */
	void encode(C object, File file) throws IOException;
}
