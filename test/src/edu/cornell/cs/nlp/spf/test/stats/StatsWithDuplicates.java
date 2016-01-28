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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import edu.cornell.cs.nlp.utils.counter.Counter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.util.Set;

public class StatsWithDuplicates<SAMPLE> implements IStatistics<SAMPLE> {

	public static final ILogger			LOG					= LoggerFactory
																	.create(StatsWithDuplicates.class);

	private final Map<SAMPLE, Counter>	corrects			= new HashMap<SAMPLE, Counter>();
	private final Map<SAMPLE, Counter>	failures			= new HashMap<SAMPLE, Counter>();
	private final Map<SAMPLE, Counter>	incorrects			= new HashMap<SAMPLE, Counter>();
	private final String				label;
	private final Set<SAMPLE>			seendDataItems		= new HashSet<SAMPLE>();
	private final Map<SAMPLE, Counter>	sloppyCorrects		= new HashMap<SAMPLE, Counter>();
	private final Map<SAMPLE, Counter>	sloppyFailures		= new HashMap<SAMPLE, Counter>();
	private final Map<SAMPLE, Counter>	sloppyIncorrects	= new HashMap<SAMPLE, Counter>();

	private final Map<SAMPLE, Counter>	totals				= new HashMap<SAMPLE, Counter>();

	public StatsWithDuplicates(String label) {
		this.label = label;
	}

	@Override
	public double f1() {
		return precision() + recall() == 0.0 ? 0.0 : 2 * precision() * recall()
				/ (precision() + recall());
	}

	@Override
	public double getCorrects() {
		double ret = 0.0;
		for (final Entry<SAMPLE, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), corrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}

	public int getCount(SAMPLE sample, Map<SAMPLE, Counter> map) {
		if (map.containsKey(sample)) {
			return map.get(sample).value();
		} else {
			return 0;
		}
	}

	@Override
	public double getFailures() {
		double ret = 0.0;
		for (final Entry<SAMPLE, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), failures)
					/ (double) entry.getValue().value();
		}
		return ret;
	}

	@Override
	public double getIncorrects() {
		double ret = 0.0;
		for (final Entry<SAMPLE, Counter> entry : totals.entrySet()) {
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
		for (final Entry<SAMPLE, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), sloppyCorrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}

	@Override
	public double getSloppyFailures() {
		double ret = 0.0;
		for (final Entry<SAMPLE, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), sloppyFailures)
					/ (double) entry.getValue().value();
		}
		return ret;
	}

	@Override
	public double getSloppyIncorrects() {
		double ret = 0.0;
		for (final Entry<SAMPLE, Counter> entry : totals.entrySet()) {
			ret += (double) getCount(entry.getKey(), sloppyIncorrects)
					/ (double) entry.getValue().value();
		}
		return ret;
	}

	@Override
	public double getTotal() {
		return totals.size();
	}

	public void inc(SAMPLE sample, Map<SAMPLE, Counter> map) {
		if (map.containsKey(sample)) {
			map.get(sample).inc();
		} else {
			map.put(sample, new Counter(1));
		}
	}

	@Override
	public double precision() {
		return getTotal() - getFailures() == 0.0 ? 0.0 : getCorrects()
				/ (getTotal() - getFailures());
	}

	@Override
	public double recall() {
		return getTotal() == 0.0 ? 0.0 : getCorrects() / getTotal();
	}

	@Override
	public void recordCorrect(SAMPLE sample) {
		LOG.info("[%s stats]  Record correct.", getLabel());
		seendDataItems.add(sample);
		inc(sample, totals);
		inc(sample, corrects);
	}

	@Override
	public void recordFailure(SAMPLE sample) {
		LOG.info("[%s stats]  Record failure.", getLabel());
		seendDataItems.add(sample);
		inc(sample, totals);
		inc(sample, failures);
	}

	@Override
	public void recordIncorrect(SAMPLE sample) {
		LOG.info("[%s stats]  Record incorrect.", getLabel());
		seendDataItems.add(sample);
		inc(sample, totals);
		inc(sample, incorrects);
	}

	@Override
	public void recordSloppyCorrect(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record sloppy correct.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, sloppyCorrects);
	}

	@Override
	public void recordSloppyFailure(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record sloppy failure.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, sloppyFailures);
	}

	@Override
	public void recordSloppyIncorrect(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record sloppy incorrect.", getLabel());
		seendDataItems.add(dataItem);
		inc(dataItem, sloppyIncorrects);
	}

	@Override
	public double sloppyF1() {
		return sloppyPrecision() + sloppyRecall() == 0.0 ? 0.0 : 2
				* sloppyPrecision() * sloppyRecall()
				/ (sloppyPrecision() + sloppyRecall());
	}

	@Override
	public double sloppyPrecision() {
		return getTotal() - getSloppyFailures() == 0.0 ? 0.0
				: (getSloppyCorrects() + getCorrects())
						/ (getTotal() - getSloppyFailures());
	}

	@Override
	public double sloppyRecall() {
		return getTotal() == 0.0 ? 0.0 : (getSloppyCorrects() + getCorrects())
				/ getTotal();
	}

}
