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
 * Represents a weekday, in a 7 days week.
 * 
 * @author Yoav Artzi
 */
public enum Day {
	FRI("fri", 6), MON("mon", 2), SAT("sat", 7), SUN("sun", 1), THU("thu", 5), TUE(
			"tue", 3), WED("wed", 4);
	
	public static Comparator<Day>			COMPARATOR	= new DayComparator();
	
	private static final Map<Object, Day>	DAY_MAPPING	= new ConcurrentHashMap<Object, Day>();
	
	private final String					label;
	
	private final int						order;
	
	private Day(String label, int order) {
		this.label = label;
		this.order = order;
	}
	
	/**
	 * Get the day mapped to a given object.
	 * 
	 * @param key
	 * @return
	 */
	public static Day getDay(Object key) {
		return DAY_MAPPING.get(key);
	}
	
	/**
	 * Set mapping of a user defined object to a Day objet.
	 * 
	 * @param key
	 * @param day
	 */
	public static void setDayMapping(Object key, Day day) {
		DAY_MAPPING.put(key, day);
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	private static class DayComparator implements Comparator<Day> {
		
		@Override
		public int compare(Day o1, Day o2) {
			return o1.order - o2.order;
		}
		
	}
}
