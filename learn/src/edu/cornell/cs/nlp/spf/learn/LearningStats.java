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
package edu.cornell.cs.nlp.spf.learn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cornell.cs.nlp.utils.counter.Counter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.util.Set;

public class LearningStats {
	public static final ILogger					LOG					= LoggerFactory
			.create(LearningStats.class);
	private static final String					DIGIT_STAT			= "###";
	private final Map<String, List<Counter>>	counters			= new HashMap<>();

	private final Map<String, Mean>				means				= new HashMap<String, Mean>();

	private final int							numSamples;
	final private String[][]					sampleStat;
	final private List<Integer>					sampleStatMaxLength	= new ArrayList<>();

	private final Map<String, String>			statDescription		= new HashMap<>();
	private final Set<String>					validStats;

	private LearningStats(int numSamples, Map<String, String> statLegend) {
		this.numSamples = numSamples;
		this.validStats = new HashSet<>(statLegend.keySet());
		this.sampleStat = new String[numSamples][];
		// Initialize a map to aggregate sample statistics.
		for (final Entry<String, String> statEntry : statLegend.entrySet()) {
			statDescription.put(statEntry.getKey(),
					"[" + statEntry.getKey() + "] " + statEntry.getValue());
		}

	}

	public void appendSampleStat(int itemNumber, int iterationNumber,
			int value) {
		extendSampleList(itemNumber, iterationNumber);
		verifyStat(DIGIT_STAT);
		if (sampleStat[itemNumber][iterationNumber] == null) {
			sampleStat[itemNumber][iterationNumber] = "";
		}
		sampleStat[itemNumber][iterationNumber] += String.valueOf(value);
		updateSampleStatMaxLength(itemNumber, iterationNumber);

		// Aggregate counts. Only aggregate when the description is
		// pre-recorded.
		if (statDescription.containsKey(DIGIT_STAT)) {
			count(statDescription.get(DIGIT_STAT), value, iterationNumber);
		}
	}

	public void appendSampleStat(int itemNumber, int iterationNumber,
			String stat) {
		extendSampleList(itemNumber, iterationNumber);
		verifyStat(stat);
		if (sampleStat[itemNumber][iterationNumber] == null) {
			sampleStat[itemNumber][iterationNumber] = "";
		}
		sampleStat[itemNumber][iterationNumber] += stat;
		updateSampleStatMaxLength(itemNumber, iterationNumber);

		// Aggregate counts. Only aggregate when the description is
		// pre-recorded.
		if (statDescription.containsKey(stat)) {
			count(statDescription.get(stat), iterationNumber);
		}
	}

	public void count(String label, int iterationNumber) {
		verifyCounterExist(label, iterationNumber);
		counters.get(label).get(iterationNumber).inc();
	}

	public void count(String label, int value, int iterationNumber) {
		verifyCounterExist(label, iterationNumber);
		counters.get(label).get(iterationNumber).inc(value);
	}

	public double getMean(String label) {
		return means.containsKey(label) ? means.get(label).mean : 0.0;
	}

	public void mean(String label, double value, String unit) {
		final Mean aggregate = means.get(label);
		if (aggregate == null) {
			means.put(label, new Mean(unit, value));
		} else {
			assert unit.equals(aggregate.unit);
			aggregate.add(value);
		}
	}

	public void setSampleStat(int itemNumber, int iterationNumber,
			String stat) {
		extendSampleList(itemNumber, iterationNumber);
		verifyStat(stat);
		sampleStat[itemNumber][iterationNumber] = stat;
		updateSampleStatMaxLength(itemNumber, iterationNumber);

		// Aggregate counts. Only aggregate when the description is
		// pre-recorded.
		if (statDescription.containsKey(stat)) {
			count(statDescription.get(stat), iterationNumber);
		}
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();

		final Iterator<Entry<String, Mean>> aggregateIterator = means.entrySet()
				.iterator();
		while (aggregateIterator.hasNext()) {
			final Entry<String, Mean> aggregateEntry = aggregateIterator.next();
			ret.append(String.format("Perfromend %d %s, mean: %.4f%s",
					aggregateEntry.getValue().count, aggregateEntry.getKey(),
					aggregateEntry.getValue().mean,
					aggregateEntry.getValue().unit));

			if (aggregateIterator.hasNext()) {
				ret.append("\n");
			}
		}

		for (final Entry<String, List<Counter>> counterArrayEntry : counters
				.entrySet()) {
			ret.append("\n");
			ret.append(counterArrayEntry.getKey()).append(": ");
			final int len = counterArrayEntry.getValue().size();
			for (int i = 0; i < len; ++i) {
				ret.append(counterArrayEntry.getValue().get(i).value());
				if (i + 1 < len) {
					ret.append(", ");
				}
			}
		}

		ret.append("\n");
		ret.append(
				String.format("Sample statistics (total: %d)\n", numSamples));
		final String sampleNumberFormat = "%1$-"
				+ String.valueOf(numSamples).length() + "s";
		for (int itemCounter = 0; itemCounter < numSamples; ++itemCounter) {
			ret.append(String.format(sampleNumberFormat,
					String.valueOf(itemCounter))).append(" :: ");
			if (sampleStat[itemCounter] != null) {
				for (int i = 0; i < sampleStat[itemCounter].length; ++i) {
					ret.append(String.format(
							"%1$-" + sampleStatMaxLength.get(i) + "s ",
							sampleStat[itemCounter][i] == null ? "-"
									: sampleStat[itemCounter][i]));
				}
			}
			ret.append('\n');
		}

		return ret.toString();
	}

	private void extendSampleList(int itemNumber, int iterationNumber) {
		if (sampleStat[itemNumber] == null) {
			sampleStat[itemNumber] = new String[iterationNumber + 1];
		} else if (sampleStat[itemNumber].length <= iterationNumber) {
			sampleStat[itemNumber] = Arrays.copyOf(sampleStat[itemNumber],
					iterationNumber + 1);
		}
	}

	private void updateSampleStatMaxLength(int itemNumber,
			int iterationNumber) {
		while (sampleStatMaxLength.size() <= iterationNumber) {
			sampleStatMaxLength.add(0);
		}
		if (sampleStat[itemNumber][iterationNumber]
				.length() > sampleStatMaxLength.get(iterationNumber)) {
			sampleStatMaxLength.set(iterationNumber,
					sampleStat[itemNumber][iterationNumber].length());
		}
	}

	private void verifyCounterExist(String label, int iterationNumber) {
		if (!counters.containsKey(label)) {
			counters.put(label, new ArrayList<>());
		}
		final List<Counter> list = counters.get(label);
		while (list.size() <= iterationNumber) {
			list.add(new Counter(0));
		}
	}

	private void verifyStat(String stat) {
		if (!validStats.contains(stat)) {
			LOG.error("Unknown stat: %s", stat);
		}
	}

	public static class Builder {

		private final int					numSamples;
		private final Map<String, String>	statLegend	= new HashMap<String, String>();

		public Builder(int numSamples) {
			this.numSamples = numSamples;
		}

		public Builder addStat(String stat, String description) {
			statLegend.put(stat, description);
			return this;
		}

		public LearningStats build() {
			return new LearningStats(numSamples, statLegend);
		}

		public Builder setNumberStat(String description) {
			statLegend.put(DIGIT_STAT, description);
			return this;
		}
	}

	private class Mean {
		int				count;
		double			mean;
		final String	unit;

		public Mean(String unit, double value) {
			this.unit = unit;
			this.mean = value;
			this.count = 1;
		}

		public void add(double value) {
			mean = (mean * count + value) / ++count;
		}
	}

}
