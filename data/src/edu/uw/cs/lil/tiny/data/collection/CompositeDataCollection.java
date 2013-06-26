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
package edu.uw.cs.lil.tiny.data.collection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.utils.collections.CompositeIterator;

/**
 * Concatenation of {@link IDataCollection}
 * 
 * @author Yoav Artzi
 * @param <T>
 */
public class CompositeDataCollection<T> implements IDataCollection<T> {
	
	private final List<IDataCollection<? extends T>>	datasets;
	
	public CompositeDataCollection(IDataCollection<? extends T>... datasets) {
		this(Arrays.asList(datasets));
	}
	
	public CompositeDataCollection(List<IDataCollection<? extends T>> datasets) {
		this.datasets = datasets;
	}
	
	@Override
	public Iterator<T> iterator() {
		return createIterator();
	}
	
	@Override
	public int size() {
		return calculateSize();
	}
	
	private int calculateSize() {
		int sum = 0;
		for (final IDataCollection<? extends T> dataset : datasets) {
			sum += dataset.size();
		}
		return sum;
	}
	
	private Iterator<T> createIterator() {
		final List<Iterator<? extends T>> iterators = new LinkedList<Iterator<? extends T>>();
		for (final IDataCollection<? extends T> dataset : datasets) {
			iterators.add(dataset.iterator());
		}
		return new CompositeIterator<T>(iterators);
	}
}
