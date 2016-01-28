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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.collections.iterators.CompositeIterator;

/**
 * Concatenation of {@link IDataCollection}
 *
 * @author Yoav Artzi
 * @param <DI>
 */
public class CompositeDataCollection<DI extends IDataItem<?>>
		implements IDataCollection<DI> {

	private static final long							serialVersionUID	= -6882854312373835889L;
	private final List<IDataCollection<? extends DI>>	datasets;

	public CompositeDataCollection(
			List<IDataCollection<? extends DI>> datasets) {
		this.datasets = datasets;
	}

	@Override
	public Iterator<DI> iterator() {
		return createIterator();
	}

	@Override
	public int size() {
		return calculateSize();
	}

	private int calculateSize() {
		int sum = 0;
		for (final IDataCollection<? extends DI> dataset : datasets) {
			sum += dataset.size();
		}
		return sum;
	}

	private Iterator<DI> createIterator() {
		final List<Iterator<? extends DI>> iterators = new LinkedList<Iterator<? extends DI>>();
		for (final IDataCollection<? extends DI> dataset : datasets) {
			iterators.add(dataset.iterator());
		}
		return new CompositeIterator<DI>(iterators);
	}

	public static class Creator<DI extends IDataItem<?>>
			implements IResourceObjectCreator<CompositeDataCollection<DI>> {
		private final String resourceName;

		public Creator() {
			this("data.composite");
		}

		public Creator(String resourceName) {
			this.resourceName = resourceName;
		}

		@Override
		public CompositeDataCollection<DI> create(Parameters parameters,
				final IResourceRepository resourceRepo) {
			return new CompositeDataCollection<DI>(ListUtils.map(
					parameters.getSplit("sets"), obj -> resourceRepo.get(obj)));
		}

		@Override
		public String type() {
			return resourceName;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					CompositeDataCollection.class)
							.setDescription(
									"Composite dataset. Concatenates separate datasets of the same type into a single one")
							.addParam("sets", "list of datasets",
									"List of datasets of the same type (e.g., 'data1,data2,data3')")
							.build();
		}

	}
}
