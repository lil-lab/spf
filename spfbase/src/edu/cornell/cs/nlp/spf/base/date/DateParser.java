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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jregex.Matcher;
import jregex.Pattern;

public class DateParser {
	private static final Map<String, Day>		DAYS		= new ConcurrentHashMap<String, Day>();
	private static final Map<String, Month>		MONTHS		= new ConcurrentHashMap<String, Month>();
	private static final Map<String, Integer>	TEENS		= new ConcurrentHashMap<String, Integer>();
	private static final Map<String, Integer>	TENS_DIGIT	= new ConcurrentHashMap<String, Integer>();
	private static final Map<String, Integer>	UNIT_DIGIT	= new ConcurrentHashMap<String, Integer>();
	private static final Map<String, Integer>	YEARS		= new ConcurrentHashMap<String, Integer>();
	private final Pattern						pattern;
	
	public DateParser() {
		final StringBuilder patternBuilder = new StringBuilder();
		
		// Day -- optional
		{
			patternBuilder.append("(()|({day}(");
			final Iterator<String> iterator = DAYS.keySet().iterator();
			while (iterator.hasNext()) {
				patternBuilder.append(iterator.next());
				if (iterator.hasNext()) {
					patternBuilder.append(")|(");
				}
			}
			patternBuilder.append(")) )");
		}
		
		// Month
		{
			patternBuilder.append("({month}(");
			final Iterator<String> iterator = MONTHS.keySet().iterator();
			while (iterator.hasNext()) {
				patternBuilder.append(iterator.next());
				if (iterator.hasNext()) {
					patternBuilder.append(")|(");
				}
			}
			patternBuilder.append(")) ");
		}
		
		// Possible 'the' between month and number of day
		patternBuilder.append("(()|the )");
		
		// Day in the month
		patternBuilder.append("(()|");
		{
			{
				// 13, 14 ...
				patternBuilder.append("({teens}(");
				final Iterator<String> iterator = TEENS.keySet().iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
				patternBuilder.append("))|");
			}
			
			{
				// 20, 30 ...
				patternBuilder.append("({round_tens}(");
				final Iterator<String> iterator = TENS_DIGIT.keySet()
						.iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
				patternBuilder.append("))|");
			}
			
			{
				// 1, 2, 3 ...
				patternBuilder.append("({unit_only}(");
				final Iterator<String> iterator = UNIT_DIGIT.keySet()
						.iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
				patternBuilder.append("))|");
			}
			
			{
				// 21, 23 ...
				patternBuilder.append("({tens}(");
				final Iterator<String> iterator = TENS_DIGIT.keySet()
						.iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
				patternBuilder.append(")) ");
				
				patternBuilder.append("({units}(");
				final Iterator<String> iteratorUnits = UNIT_DIGIT.keySet()
						.iterator();
				while (iteratorUnits.hasNext()) {
					patternBuilder.append(iteratorUnits.next());
					if (iteratorUnits.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
				patternBuilder.append("))");
			}
			
		}
		patternBuilder.append(")");
		
		// Year -- optional
		{
			patternBuilder.append("(()| ({year}(");
			final Iterator<String> iterator = YEARS.keySet().iterator();
			while (iterator.hasNext()) {
				patternBuilder.append(iterator.next());
				if (iterator.hasNext()) {
					patternBuilder.append(")|(");
				}
			}
			patternBuilder.append(")))");
		}
		
		// Day -- optional
		{
			patternBuilder.append("(()| ({day2}(");
			final Iterator<String> iterator = DAYS.keySet().iterator();
			while (iterator.hasNext()) {
				patternBuilder.append(iterator.next());
				if (iterator.hasNext()) {
					patternBuilder.append(")|(");
				}
			}
			patternBuilder.append(")))");
		}
		
		pattern = new Pattern(patternBuilder.toString());
	}
	
	static {
		// Init months
		MONTHS.put("january", Month.JAN);
		MONTHS.put("february", Month.FEB);
		MONTHS.put("march", Month.MAR);
		MONTHS.put("april", Month.APR);
		MONTHS.put("may", Month.MAY);
		MONTHS.put("june", Month.JUN);
		MONTHS.put("july", Month.JUL);
		MONTHS.put("august", Month.AUG);
		MONTHS.put("september", Month.SEP);
		MONTHS.put("october", Month.OCT);
		MONTHS.put("november", Month.NOV);
		MONTHS.put("december", Month.DEC);
		
		// Init days
		DAYS.put("sunday", Day.SUN);
		DAYS.put("monday", Day.MON);
		DAYS.put("tuesday", Day.TUE);
		DAYS.put("wednesday", Day.WED);
		DAYS.put("thursday", Day.THU);
		DAYS.put("friday", Day.FRI);
		DAYS.put("saturday", Day.SAT);
		
		// Tens digit
		TENS_DIGIT.put("twenty", 20);
		TENS_DIGIT.put("twentieth", 20);
		TENS_DIGIT.put("thirty", 30);
		TENS_DIGIT.put("thirtieth", 30);
		TENS_DIGIT.put("thirteeth", 30);
		
		// Teens
		TEENS.put("ten", 10);
		TEENS.put("tenth", 10);
		TEENS.put("eleven", 11);
		TEENS.put("eleventh", 11);
		TEENS.put("twelve", 12);
		TEENS.put("twelfth", 12);
		TEENS.put("thirteen", 13);
		TEENS.put("thirteenth", 13);
		TEENS.put("fourteen", 14);
		TEENS.put("fourteenth", 14);
		TEENS.put("fifteen", 15);
		TEENS.put("fifteenth", 15);
		TEENS.put("sixteen", 16);
		TEENS.put("sixteenth", 16);
		TEENS.put("seventeen", 17);
		TEENS.put("seventeenth", 17);
		TEENS.put("eighteen", 18);
		TEENS.put("eighteenth", 18);
		TEENS.put("nineteen", 19);
		TEENS.put("nineteenth", 19);
		
		// Unit digit
		UNIT_DIGIT.put("one", 1);
		UNIT_DIGIT.put("first", 1);
		UNIT_DIGIT.put("two", 2);
		UNIT_DIGIT.put("second", 2);
		UNIT_DIGIT.put("three", 3);
		UNIT_DIGIT.put("third", 3);
		UNIT_DIGIT.put("four", 4);
		UNIT_DIGIT.put("fourth", 4);
		UNIT_DIGIT.put("five", 5);
		UNIT_DIGIT.put("fifth", 5);
		UNIT_DIGIT.put("six", 6);
		UNIT_DIGIT.put("sixth", 6);
		UNIT_DIGIT.put("seven", 7);
		UNIT_DIGIT.put("seventh", 7);
		UNIT_DIGIT.put("eight", 8);
		UNIT_DIGIT.put("eighth", 8);
		UNIT_DIGIT.put("nine", 9);
		UNIT_DIGIT.put("ninth", 9);
		
		// Years -- very poor support
		YEARS.put("two thousand two", 2002);
	}
	
	public Date parse(String text) {
		// Strip commas and periods
		final String stripped = text.replaceAll("[.,]", "");
		final Matcher matcher = pattern.matcher(stripped);
		if (matcher.matches()) {
			Integer year = null;
			Day weekDay = null;
			Integer monthDay = null;
			Month month = null;
			
			if (matcher.isCaptured("day") && matcher.isCaptured("day2")) {
				// If we captured both days (at the beginning and end) this is
				// not a valid date string
				return null;
			}
			
			if (matcher.isCaptured("day")) {
				weekDay = DAYS.get(matcher.group("day"));
			} else if (matcher.isCaptured("day2")) {
				weekDay = DAYS.get(matcher.group("day2"));
			}
			
			if (matcher.isCaptured("month")) {
				month = MONTHS.get(matcher.group("month"));
			}
			
			if (matcher.isCaptured("year")) {
				year = YEARS.get(matcher.group("year"));
			}
			
			if (matcher.isCaptured("teens")) {
				monthDay = TEENS.get(matcher.group("teens"));
			} else if (matcher.isCaptured("round_tens")) {
				monthDay = TENS_DIGIT.get(matcher.group("round_tens"));
			} else if (matcher.isCaptured("unit_only")) {
				monthDay = UNIT_DIGIT.get(matcher.group("unit_only"));
			} else if (matcher.isCaptured("tens")
					|| matcher.isCaptured("units")) {
				monthDay = 0;
				monthDay += matcher.isCaptured("tens") ? TENS_DIGIT.get(matcher
						.group("tens")) : 0;
				monthDay += matcher.isCaptured("units") ? UNIT_DIGIT
						.get(matcher.group("units")) : 0;
			}
			
			return new Date(month, monthDay, weekDay, year);
		} else {
			return null;
		}
	}
	
}
