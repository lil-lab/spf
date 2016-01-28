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
/**
 * [lsz] This is one big hackish mess because I hate to write file processing
 * code!
 */
package edu.cornell.cs.nlp.spf.base;

import java.io.IOException;
import java.io.Reader;

public class LispReader {
	private final Reader	in;
	private char			lastc;
	private int				lasti;
	
	public LispReader(Reader i) {
		in = i;
		lastc = ' ';
		lasti = 0;
		skipPast('(');
		skipWS();
	}
	
	public boolean hasNext() {
		return lasti != -1;
	}
	
	public String next() {
		if (lastc == '(') {
			return readList();
		}
		return readWord();
	}
	
	private String readList() {
		final StringBuilder result = new StringBuilder("(");
		int depth = 1;
		try {
			while (depth != 0 && lasti != -1) {
				lasti = in.read();
				lastc = (char) lasti;
				if (lastc == '(') {
					depth++;
				}
				if (lastc == ')') {
					depth--;
				}
				result.append(lastc);
			}
			lasti = in.read();
			lastc = (char) lasti;
		} catch (final IOException e) {
			System.out.println(e);
		}
		skipWS();
		return result.toString();
	}
	
	private String readWord() {
		final StringBuilder result = new StringBuilder();
		try {
			while (!Character.isWhitespace(lastc) && lastc != ')'
					&& lasti != -1) {
				result.append(lastc);
				lasti = in.read();
				lastc = (char) lasti;
			}
			lasti = in.read();
			lastc = (char) lasti;
		} catch (final IOException e) {
			System.out.println(e);
		}
		skipWS();
		return result.toString();
	}
	
	private void skipPast(char seek) {
		try {
			while (lastc != seek && lasti != -1) {
				lasti = in.read();
				lastc = (char) lasti;
			}
			lasti = in.read();
			lastc = (char) lasti;
		} catch (final IOException e) {
			System.out.println(e);
		}
	}
	
	private void skipWS() {
		try {
			while (Character.isWhitespace(lastc) || lastc == ')') {
				lasti = in.read();
				lastc = (char) lasti;
			}
		} catch (final IOException e) {
			System.out.println(e);
		}
	}
	
}
