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

import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;

/**
 * Accumulates testing statistics.
 * 
 * @author Yoav Artzi
 */
public interface ITestingStatistics<X, Y> {
	
	void recordNoParse(IDataItem<X> dataItem, Y gold);
	
	void recordNoParseWithSkipping(IDataItem<X> dataItem, Y gold);
	
	/**
	 * Record a parse.
	 */
	void recordParse(IDataItem<X> dataItem, Y gold, Y label);
	
	void recordParses(IDataItem<X> dataItem, Y gold, List<Y> labels);
	
	void recordParsesWithSkipping(IDataItem<X> dataItem, Y gold, List<Y> labels);
	
	/**
	 * Record a parse with word skipping enabled. Assumes a record parse for
	 * this data item has been called earlier.
	 */
	void recordParseWithSkipping(IDataItem<X> dataItem, Y gold, Y label);
	
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
