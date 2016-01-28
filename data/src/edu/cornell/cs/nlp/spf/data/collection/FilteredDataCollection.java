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
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Filtered view of a {@link IDataCollection}.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item.
 */
public class FilteredDataCollection<DI extends IDataItem<?>>
		implements IDataCollection<DI> {
	public static final ILogger	LOG					= LoggerFactory
			.create(FilteredDataCollection.class);
	private static final long	serialVersionUID	= -3645133277010099976L;
	private final List<DI>		data;

	public FilteredDataCollection(IDataCollection<DI> data,
			IFilter<DI> filter) {
		final List<DI> dataItems = new LinkedList<DI>();
		for (final DI dataItem : data) {
			if (filter.test(dataItem)) {
				dataItems.add(dataItem);
			}
		}
		this.data = Collections.unmodifiableList(dataItems);
		LOG.info("Created filtered data collection: %d -> %d", data.size(),
				this.data.size());
	}

	@Override
	public Iterator<DI> iterator() {
		return data.iterator();
	}

	@Override
	public int size() {
		return data.size();
	}

	public static class Creator<DI extends IDataItem<?>>
			implements IResourceObjectCreator<FilteredDataCollection<DI>> {

		private String type;

		public Creator() {
			this("data.filter");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public FilteredDataCollection<DI> create(Parameters params,
				IResourceRepository repo) {
			return new FilteredDataCollection<DI>(
					(IDataCollection<DI>) repo.get(params.get("data")),
					(IFilter<DI>) repo.get(params.get("filter")));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, FilteredDataCollection.class)
					.setDescription("Filtered collection of data items")
					.addParam("data", IDataCollection.class,
							"Collection of data items to filter")
					.addParam("filter", IFilter.class,
							"Filter for testing data items")
					.build();
		}

	}

}
