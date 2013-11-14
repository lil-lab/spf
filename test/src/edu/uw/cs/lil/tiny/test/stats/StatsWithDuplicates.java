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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.utils.counter.Counter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class StatsWithDuplicates<DI extends IDataItem<?>> implements
		IStatistics<DI> {
	
	public static final ILogger		LOG					= LoggerFactory
																.create(StatsWithDuplicates.class);
	
	private final Map<DI, Counter>	corrects			= new HashMap<DI, Counter>();
	private final Map<DI, Counter>	failures			= new HashMap<DI, Counter>();
	private final Map<DI, Counter>	incorrects			= new HashMap<DI, Counter>();
	private final String			label;
	private final Set<DI>			seendDataItems		= new HashSet<DI>();
	private final Map<DI, Counter>	sloppyCorrects		= new HashMap<DI, Counter>();
	private final Map<DI, Counter>	sloppyFailures		= new HashMap<DI, Counter>();
	private final Map<DI, Counter>	sloppyIncorrects	= new HashMap<DI, Counter>();
	
	private final Map<DI, Counter>	totals				= new HashMap<DI, Counter>();
	
	public StatsWithDuplicates(String label) {
		this.label = label;
	}
	
	@Override
	public double f1() {
		return (precision() + recall()) == 0.0 ? 0.0
				: (2 * precision() * recall()) / (precision() + recall());
	}
	
	@Override
	public double getCorrects() {
		double ret = 0.0;
		for (final Entry<DI, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), corrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	public int getCount(DI dataItem, Map<DI, Counter> map) {
		if (map.containsKey(dataItem)) {
			return map.get(dataItem).value();
		} else {
			return 0;
		}
	}
	
	@Override
	public double getFailures() {
		double ret = 0.0;
		for (final Entry<DI, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), failures)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	@Override
	public double getIncorrects() {
		double ret = 0.0;
		for (final Entry<DI, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), incorrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public double getSloppyCorrects() {
		double ret = 0.0;
		for (final Entry<DI, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), sloppyCorrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	@Override
	public double getSloppyFailures() {
		double ret = 0.0;
		for (final Entry<DI, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), sloppyFailures)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	@Override
	public double getSloppyIncorrects() {
		double ret = 0.0;
		for (final Entry<DI, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), sloppyIncorrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}
	
	@Override
	public double getTotal() {
		return totals.size();
	}
	
	public void inc(DI dataItem, Map<DI, Counter> map) {
		if (map.containsKey(dataItem)) {
			map.get(dataItem).inc();
		} else {
			map.put(dataItem, new Counter(1));
		}
	}
	
	@Override
	public double precision() {
		return (getTotal() - getFailures()) == 0.0 ? 0.0
				: (getCorrects() / (getTotal() - getFailures()));
	}
	
	@Override
	public double recall() {
		return getTotal() == 0.0 ? 0.0 : getCorrects() / getTotal();
	}
	
	@Override
	public void recordCorrect(DI dataItem) {
		LOG.info("[%s stats]  Record correct.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, totals);
		inc(dataItem, corrects);
	}
	
	@Override
	public void recordFailure(DI dataItem) {
		LOG.info("[%s stats]  Record failure.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, totals);
		inc(dataItem, failures);
	}
	
	@Override
	public void recordIncorrect(DI dataItem) {
		LOG.info("[%s stats]  Record incorrect.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, totals);
		inc(dataItem, incorrects);
	}
	
	@Override
	public void recordSloppyCorrect(DI dataItem) {
		LOG.info("[%s stats]  Record sloppy correct.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, sloppyCorrects);
	}
	
	@Override
	public void recordSloppyFailure(DI dataItem) {
		LOG.info("[%s stats]  Record sloppy failure.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, sloppyFailures);
	}
	
	@Override
	public void recordSloppyIncorrect(DI dataItem) {
		LOG.info("[%s stats]  Record sloppy incorrect.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, sloppyIncorrects);
	}
	
	@Override
	public double sloppyF1() {
		return (sloppyPrecision() + sloppyRecall()) == 0.0 ? 0.0
				: (2 * sloppyPrecision() * sloppyRecall())
						/ (sloppyPrecision() + sloppyRecall());
	}
	
	@Override
	public double sloppyPrecision() {
		return (getTotal() - getSloppyFailures()) == 0.0 ? 0.0
				: (getSloppyCorrects() + getCorrects())
						/ (getTotal() - getSloppyFailures());
	}
	
	@Override
	public double sloppyRecall() {
		return getTotal() == 0.0 ? 0.0 : (getSloppyCorrects() + getCorrects())
				/ getTotal();
	}
	
}
