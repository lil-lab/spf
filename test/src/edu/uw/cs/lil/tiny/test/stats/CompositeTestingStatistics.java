/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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
package edu.uw.cs.lil.tiny.test.stats;

import java.util.Iterator;
import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;

/**
 * Composition of few testing statistics.
 * 
 * @author Yoav Artzi
 * @param <X>
 * @param <Y>
 */
public class CompositeTestingStatistics<X, Y> implements
		ITestingStatistics<X, Y> {
	
	private final List<ITestingStatistics<X, Y>>	stats;
	
	public CompositeTestingStatistics(List<ITestingStatistics<X, Y>> stats) {
		this.stats = stats;
	}
	
	@Override
	public void recordNoParse(IDataItem<X> dataItem, Y gold) {
		for (final ITestingStatistics<X, Y> stat : stats) {
			stat.recordNoParse(dataItem, gold);
		}
	}
	
	@Override
	public void recordNoParseWithSkipping(IDataItem<X> dataItem, Y gold) {
		for (final ITestingStatistics<X, Y> stat : stats) {
			stat.recordNoParseWithSkipping(dataItem, gold);
		}
	}
	
	@Override
	public void recordParse(IDataItem<X> dataItem, Y gold, Y label) {
		for (final ITestingStatistics<X, Y> stat : stats) {
			stat.recordParse(dataItem, gold, label);
		}
	}
	
	@Override
	public void recordParses(IDataItem<X> dataItem, Y gold, List<Y> labels) {
		for (final ITestingStatistics<X, Y> stat : stats) {
			stat.recordParses(dataItem, gold, labels);
		}
	}
	
	@Override
	public void recordParsesWithSkipping(IDataItem<X> dataItem, Y gold,
			List<Y> labels) {
		for (final ITestingStatistics<X, Y> stat : stats) {
			stat.recordParsesWithSkipping(dataItem, gold, labels);
		}
	}
	
	@Override
	public void recordParseWithSkipping(IDataItem<X> dataItem, Y gold, Y label) {
		for (final ITestingStatistics<X, Y> stat : stats) {
			stat.recordParseWithSkipping(dataItem, gold, label);
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		final Iterator<ITestingStatistics<X, Y>> iterator = stats.iterator();
		while (iterator.hasNext()) {
			ret.append(iterator.next().toString());
			if (iterator.hasNext()) {
				ret.append("\n\n");
			}
		}
		return ret.toString();
	}
	
	@Override
	public String toTabDelimitedString() {
		final StringBuilder ret = new StringBuilder();
		final Iterator<ITestingStatistics<X, Y>> iterator = stats.iterator();
		while (iterator.hasNext()) {
			ret.append(iterator.next().toTabDelimitedString());
			if (iterator.hasNext()) {
				ret.append('\n');
			}
		}
		return ret.toString();
	}
	
}
