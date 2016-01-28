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

import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Testing statistics for the exact match metric.
 *
 * @author Yoav Artzi
 * @param <SAMPLE>
 */
public class SimpleStats<SAMPLE> implements IStatistics<SAMPLE> {

	public static final ILogger	LOG					= LoggerFactory
															.create(SimpleStats.class);

	/**
	 * The number of correct.
	 */
	private int					corrects			= 0;

	/**
	 * Number of inference failures.
	 */
	private int					failures			= 0;

	/**
	 * Number of incorrects.
	 */
	private int					incorrects			= 0;

	private final String		label;

	/**
	 * Number of correct. With sloppy inference.
	 */
	private int					sloppyCorrects		= 0;

	/**
	 * Number of inference failures. With sloppy inference.
	 */
	private int					sloppyFailures		= 0;

	/**
	 * Number of incorrects. With sloppy inference.
	 */
	private int					sloppyIncorrects	= 0;

	/**
	 * Total number of inference attempts.
	 */
	private int					total				= 0;

	public SimpleStats(String label) {
		this.label = label;
	}

	@Override
	public double f1() {
		return precision() + recall() == 0.0 ? 0.0 : 2 * precision() * recall()
				/ (precision() + recall());
	}

	@Override
	public double getCorrects() {
		return corrects;
	}

	@Override
	public double getFailures() {
		return failures;
	}

	@Override
	public double getIncorrects() {
		return incorrects;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public double getSloppyCorrects() {
		return sloppyCorrects;
	}

	@Override
	public double getSloppyFailures() {
		return sloppyFailures;
	}

	@Override
	public double getSloppyIncorrects() {
		return sloppyIncorrects;
	}

	@Override
	public double getTotal() {
		return total;
	}

	@Override
	public double precision() {
		return total - failures == 0.0 ? 0.0 : (double) corrects
				/ (total - failures);
	}

	@Override
	public double recall() {
		return total == 0.0 ? 0.0 : (double) corrects / total;
	}

	@Override
	public void recordCorrect(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record correct.", getLabel());
		total++;
		corrects++;
	}

	@Override
	public void recordFailure(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record failure.", getLabel());
		total++;
		failures++;
	}

	@Override
	public void recordIncorrect(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record incorrect.", getLabel());
		total++;
		incorrects++;
	}

	@Override
	public void recordSloppyCorrect(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record sloppy correct.", getLabel());
		sloppyCorrects++;
	}

	@Override
	public void recordSloppyFailure(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record sloppy failure.", getLabel());
		sloppyFailures++;
	}

	@Override
	public void recordSloppyIncorrect(SAMPLE dataItem) {
		LOG.info("[%s stats]  Record sloppy incorrect.", getLabel());
		sloppyIncorrects++;
	}

	@Override
	public double sloppyF1() {
		return sloppyPrecision() + sloppyRecall() == 0.0 ? 0.0 : 2
				* sloppyPrecision() * sloppyRecall()
				/ (sloppyPrecision() + sloppyRecall());
	}

	@Override
	public double sloppyPrecision() {
		return total - sloppyFailures == 0.0 ? 0.0
				: (double) (sloppyCorrects + corrects)
						/ (total - sloppyFailures);
	}

	@Override
	public double sloppyRecall() {
		return total == 0.0 ? 0.0 : (double) (sloppyCorrects + corrects)
				/ total;
	}

}
