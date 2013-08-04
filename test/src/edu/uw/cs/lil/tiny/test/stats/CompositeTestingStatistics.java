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
package edu.uw.cs.lil.tiny.test.stats;

import java.util.Iterator;
import java.util.List;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;

/**
 * Composition of few testing statistics.
 * 
 * @author Yoav Artzi
 * @param <SAMPLE>
 * @param <LABEL>
 */
public class CompositeTestingStatistics<SAMPLE, LABEL> implements
		ITestingStatistics<SAMPLE, LABEL> {
	
	private final List<ITestingStatistics<SAMPLE, LABEL>>	stats;
	
	public CompositeTestingStatistics(
			List<ITestingStatistics<SAMPLE, LABEL>> stats) {
		this.stats = stats;
	}
	
	@Override
	public void recordNoParse(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold) {
		for (final ITestingStatistics<SAMPLE, LABEL> stat : stats) {
			stat.recordNoParse(dataItem, gold);
		}
	}
	
	@Override
	public void recordNoParseWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold) {
		for (final ITestingStatistics<SAMPLE, LABEL> stat : stats) {
			stat.recordNoParseWithSkipping(dataItem, gold);
		}
	}
	
	@Override
	public void recordParse(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, LABEL label) {
		for (final ITestingStatistics<SAMPLE, LABEL> stat : stats) {
			stat.recordParse(dataItem, gold, label);
		}
	}
	
	@Override
	public void recordParses(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, List<LABEL> labels) {
		for (final ITestingStatistics<SAMPLE, LABEL> stat : stats) {
			stat.recordParses(dataItem, gold, labels);
		}
	}
	
	@Override
	public void recordParsesWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold,
			List<LABEL> labels) {
		for (final ITestingStatistics<SAMPLE, LABEL> stat : stats) {
			stat.recordParsesWithSkipping(dataItem, gold, labels);
		}
	}
	
	@Override
	public void recordParseWithSkipping(
			ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold, LABEL label) {
		for (final ITestingStatistics<SAMPLE, LABEL> stat : stats) {
			stat.recordParseWithSkipping(dataItem, gold, label);
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		final Iterator<ITestingStatistics<SAMPLE, LABEL>> iterator = stats
				.iterator();
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
		final Iterator<ITestingStatistics<SAMPLE, LABEL>> iterator = stats
				.iterator();
		while (iterator.hasNext()) {
			ret.append(iterator.next().toTabDelimitedString());
			if (iterator.hasNext()) {
				ret.append('\n');
			}
		}
		return ret.toString();
	}
	
}
