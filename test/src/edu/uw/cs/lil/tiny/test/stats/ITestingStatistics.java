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

import java.util.List;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;

/**
 * Accumulates testing statistics.
 * 
 * @author Yoav Artzi
 */
public interface ITestingStatistics<SAMPLE, LABEL> {
	
	void recordNoParse(ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold);
	
	void recordNoParseWithSkipping(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold);
	
	/**
	 * Record a parse.
	 */
	void recordParse(ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold,
			LABEL label);
	
	void recordParses(ILabeledDataItem<SAMPLE, LABEL> dataItem, LABEL gold,
			List<LABEL> labels);
	
	void recordParsesWithSkipping(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, List<LABEL> labels);
	
	/**
	 * Record a parse with word skipping enabled. Assumes a record parse for
	 * this data item has been called earlier.
	 */
	void recordParseWithSkipping(ILabeledDataItem<SAMPLE, LABEL> dataItem,
			LABEL gold, LABEL label);
	
	@Override
	String toString();
	
	/**
	 * Generate machine readable tab-delimited string. Formatting:
	 * <key>=<value>\t<key>=<value>...
	 * 
	 * @return
	 */
	String toTabDelimitedString();
}
