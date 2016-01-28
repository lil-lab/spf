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
package edu.cornell.cs.nlp.spf.base.date;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a gregorian month.
 * 
 * @author Yoav Artzi
 */
public enum Month {
	
	APR("apr", 4), AUG("aug", 8), DEC("dec", 12), FEB("feb", 2), JAN("jan", 1), JUL(
			"jul", 7), JUN("jun", 6), MAR("mar", 3), MAY("may", 5), NOV("nov",
			11), OCT("oct", 10), SEP("sep", 9);
	
	public static final Comparator<Month>	COMPARATOR		= new MonthComparator();
	
	private static final Map<Object, Month>	MONTH_MAPPING	= new ConcurrentHashMap<Object, Month>();
	
	private final String					label;
	
	private final int						order;
	
	private Month(String label, int order) {
		this.label = label;
		this.order = order;
	}
	
	/**
	 * Get the month mapped to a given object.
	 * 
	 * @param key
	 * @return
	 */
	public static Month getMonth(Object key) {
		return MONTH_MAPPING.get(key);
	}
	
	/**
	 * Inits a mapping of a month to an user defined object.
	 * 
	 * @param key
	 * @param month
	 */
	public static void setMonthMapping(Object key, Month month) {
		MONTH_MAPPING.put(key, month);
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	private static class MonthComparator implements Comparator<Month> {
		@Override
		public int compare(Month o1, Month o2) {
			return o1.order - o2.order;
		}
	}
}
