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
package edu.cornell.cs.nlp.spf.test.stats;

import java.util.Iterator;
import java.util.List;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;

/**
 * Composition of few testing statistics.
 *
 * @author Yoav Artzi
 * @param <SAMPLE>
 * @param <LABEL>
 */
public class CompositeTestingStatistics<SAMPLE, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>>
		implements ITestingStatistics<SAMPLE, LABEL, DI> {

	private final List<ITestingStatistics<SAMPLE, LABEL, DI>>	stats;

	public CompositeTestingStatistics(
			List<ITestingStatistics<SAMPLE, LABEL, DI>> stats) {
		this.stats = stats;
	}

	@Override
	public void recordNoParse(DI dataItem) {
		for (final ITestingStatistics<SAMPLE, LABEL, DI> stat : stats) {
			stat.recordNoParse(dataItem);
		}
	}

	@Override
	public void recordNoParseWithSkipping(DI dataItem) {
		for (final ITestingStatistics<SAMPLE, LABEL, DI> stat : stats) {
			stat.recordNoParseWithSkipping(dataItem);
		}
	}

	@Override
	public void recordParse(DI dataItem, LABEL candidate) {
		for (final ITestingStatistics<SAMPLE, LABEL, DI> stat : stats) {
			stat.recordParse(dataItem, candidate);
		}
	}

	@Override
	public void recordParses(DI dataItem, List<LABEL> candidates) {
		for (final ITestingStatistics<SAMPLE, LABEL, DI> stat : stats) {
			stat.recordParses(dataItem, candidates);
		}
	}

	@Override
	public void recordParsesWithSkipping(DI dataItem, List<LABEL> candidates) {
		for (final ITestingStatistics<SAMPLE, LABEL, DI> stat : stats) {
			stat.recordParsesWithSkipping(dataItem, candidates);
		}
	}

	@Override
	public void recordParseWithSkipping(DI dataItem, LABEL candidate) {
		for (final ITestingStatistics<SAMPLE, LABEL, DI> stat : stats) {
			stat.recordParseWithSkipping(dataItem, candidate);
		}
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		final Iterator<ITestingStatistics<SAMPLE, LABEL, DI>> iterator = stats
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
		final Iterator<ITestingStatistics<SAMPLE, LABEL, DI>> iterator = stats
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
