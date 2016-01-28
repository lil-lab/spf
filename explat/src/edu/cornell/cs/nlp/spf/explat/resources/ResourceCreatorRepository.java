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
package edu.cornell.cs.nlp.spf.explat.resources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ResourceCreatorRepository implements
		Iterable<Entry<String, IResourceObjectCreator<?>>> {

	private final Map<String, IResourceObjectCreator<?>>	creators	= new HashMap<String, IResourceObjectCreator<?>>();

	public ResourceCreatorRepository() {
	}

	public final IResourceObjectCreator<?> getCreator(String type) {
		return creators.get(type);
	}

	@Override
	public Iterator<Entry<String, IResourceObjectCreator<?>>> iterator() {
		return creators.entrySet().iterator();
	}

	public final void registerResourceCreator(IResourceObjectCreator<?> creator) {
		if (creators.containsKey(creator.type())) {
			throw new IllegalStateException(
					"Trying to register an existing resource ID: " + creator);
		}
		creators.put(creator.type(), creator);
	}

}
