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
package edu.cornell.cs.nlp.spf.base.exceptions;

/**
 * Exception to throw when reading from a file.
 * 
 * @author Yoav Artzi
 */
public class FileReadingException extends RuntimeException {
	private static final long	serialVersionUID	= 8340881530382355110L;
	private final String		filename;
	private final int			lineNumber;
	
	public FileReadingException(Exception e) {
		super(e);
		this.lineNumber = -1;
		this.filename = null;
	}
	
	public FileReadingException(Exception e, int lineNumber) {
		super(e);
		this.lineNumber = lineNumber;
		this.filename = null;
	}
	
	public FileReadingException(Exception e, int lineNumber, String filename) {
		super(e);
		this.lineNumber = lineNumber;
		this.filename = filename;
	}
	
	public FileReadingException(Exception e, String msg) {
		super(msg, e);
		this.lineNumber = -1;
		this.filename = null;
	}
	
	public FileReadingException(String msg, int lineNumber) {
		super(msg);
		this.lineNumber = lineNumber;
		this.filename = null;
	}
	
	public FileReadingException(String msg, int lineNumber, String filename) {
		super(msg);
		this.lineNumber = lineNumber;
		this.filename = filename;
	}
	
	@Override
	public String toString() {
		return filename + ":" + lineNumber + " :: " + super.toString();
	}
}
