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
package edu.cornell.cs.nlp.spf.genlex.ccg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;

public abstract class AbstractLexiconGenerator<DI extends IDataItem<?>, MR, MODEL extends IModelImmutable<?, ?>>
		implements ILexiconGenerator<DI, MR, MODEL> {

	private static final long			serialVersionUID	= 54170715419544750L;
	protected final Map<String, String>	entryProperties;
	protected final String				origin;

	public AbstractLexiconGenerator(String origin, boolean mark) {
		this.origin = origin;
		assert origin != null;
		final Map<String, String> properties = new HashMap<String, String>();
		properties.put(LexicalEntry.ORIGIN_PROPERTY, origin);
		if (mark) {
			properties.put(GENLEX_MARKING_PROPERTY, null);
		}
		this.entryProperties = Collections.unmodifiableMap(properties);
	}

}
