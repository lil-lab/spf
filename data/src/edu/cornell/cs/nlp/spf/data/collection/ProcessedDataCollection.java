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
package edu.cornell.cs.nlp.spf.data.collection;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;

/**
 * {@link IDataCollection} created by processing another {@link IDataCollection}
 * . .
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item.
 * @param <SRC>
 *            Source data item.
 */
public class ProcessedDataCollection<DI extends IDataItem<?>, SRC extends IDataItem<?>>
		implements IDataCollection<DI> {

	private static final long	serialVersionUID	= 4299908203383675784L;
	private final List<DI>		data;

	public ProcessedDataCollection(IDataCollection<SRC> sourceData,
			Function<SRC, DI> function) {
		this.data = Collections.unmodifiableList(
				StreamSupport.stream(sourceData.spliterator(), true)
						.map(function).collect(Collectors.toList()));
	}

	@Override
	public Iterator<DI> iterator() {
		return data.iterator();
	}

	@Override
	public int size() {
		return data.size();
	}

	public static class Creator<DI extends IDataItem<?>, SRC extends IDataItem<?>>
			implements
			IResourceObjectCreator<ProcessedDataCollection<DI, SRC>> {

		private String type;

		public Creator() {
			this("data.process");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ProcessedDataCollection<DI, SRC> create(Parameters params,
				IResourceRepository repo) {
			return new ProcessedDataCollection<>(repo.get(params.get("src")),
					repo.get(params.get("func")));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, ProcessedDataCollection.class)
					.addParam("src", IDataCollection.class, "Source data")
					.addParam("func", Function.class, "Processing function")
					.setDescription(
							"IDataCollection created by processing another IDataCollection")
					.build();
		}

	}

}
