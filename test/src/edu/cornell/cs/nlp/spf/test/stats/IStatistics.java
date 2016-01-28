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

/**
 * Accumulates testing statistics.
 *
 * @author Yoav Artzi
 */
public interface IStatistics<SAMPLE> {

	double f1();

	double getCorrects();

	double getFailures();

	double getIncorrects();

	String getLabel();

	double getSloppyCorrects();

	double getSloppyFailures();

	double getSloppyIncorrects();

	double getTotal();

	double precision();

	double recall();

	void recordCorrect(SAMPLE sample);

	void recordFailure(SAMPLE sample);

	void recordIncorrect(SAMPLE sample);

	void recordSloppyCorrect(SAMPLE sample);

	void recordSloppyFailure(SAMPLE sample);

	void recordSloppyIncorrect(SAMPLE sample);

	double sloppyF1();

	double sloppyPrecision();

	double sloppyRecall();
}
