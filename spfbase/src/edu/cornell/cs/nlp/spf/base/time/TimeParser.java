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
package edu.cornell.cs.nlp.spf.base.time;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jregex.Matcher;
import jregex.Pattern;

public class TimeParser {
	private static final Map<String, AmPm>		AM_PM_STRINGS	= new ConcurrentHashMap<String, AmPm>();
	private static final Map<Integer, AmPm>		AMPM_DEFAULTS	= new ConcurrentHashMap<Integer, AmPm>();
	private static final Map<String, Integer>	HOUR_TO_INT		= new ConcurrentHashMap<String, Integer>();
	private static final Map<String, Integer>	TEENS			= new ConcurrentHashMap<String, Integer>();
	private static final Map<String, Integer>	TENS_DIGIT		= new ConcurrentHashMap<String, Integer>();
	private static final Map<String, Integer>	UNIT_DIGIT		= new ConcurrentHashMap<String, Integer>();
	private final Pattern						pattern;
	
	public TimeParser() {
		final StringBuilder patternBuilder = new StringBuilder();
		
		// Hour
		{
			patternBuilder.append("({hour}(");
			final Iterator<String> iterator = HOUR_TO_INT.keySet().iterator();
			while (iterator.hasNext()) {
				patternBuilder.append(iterator.next());
				if (iterator.hasNext()) {
					patternBuilder.append(")|(");
				}
			}
			patternBuilder.append("))");
		}
		
		// Minutes, optional
		{
			// 12, 15, 16....
			patternBuilder.append("(()|( ({teens}(");
			{
				// Add teens
				final Iterator<String> iterator = TEENS.keySet().iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
			}
			patternBuilder.append("))");
			
			// 20, 30, 21, 31, 41 ...
			patternBuilder.append("|( ({tens}(");
			{
				final Iterator<String> iterator = TENS_DIGIT.keySet()
						.iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
				
			}
			patternBuilder.append("))");
			
			// 1, 2, 3 ...
			patternBuilder.append("(()|( ({units}(");
			{
				final Iterator<String> iterator = UNIT_DIGIT.keySet()
						.iterator();
				while (iterator.hasNext()) {
					patternBuilder.append(iterator.next());
					if (iterator.hasNext()) {
						patternBuilder.append(")|(");
					}
				}
			}
			patternBuilder.append(")))))");
			
			patternBuilder.append("))");
		}
		
		// Possible "o'clock" before the AM/PM indicator
		patternBuilder.append("(()| o'clock)");
		
		// AM/PM
		{
			patternBuilder.append("(()| ({ampm}(");
			final Iterator<String> iterator = AM_PM_STRINGS.keySet().iterator();
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
		// AM/PM
		AM_PM_STRINGS.put("a m", AmPm.AM);
		AM_PM_STRINGS.put("eh m", AmPm.AM);
		AM_PM_STRINGS.put("a.m.", AmPm.AM);
		AM_PM_STRINGS.put("a. m.", AmPm.AM);
		AM_PM_STRINGS.put("a . m .", AmPm.AM);
		AM_PM_STRINGS.put("am", AmPm.AM);
		AM_PM_STRINGS.put("p m", AmPm.PM);
		AM_PM_STRINGS.put("pm", AmPm.PM);
		AM_PM_STRINGS.put("p.m.", AmPm.PM);
		AM_PM_STRINGS.put("p. m.", AmPm.PM);
		AM_PM_STRINGS.put("p . m .", AmPm.PM);
		AM_PM_STRINGS.put("in the morning", AmPm.AM);
		AM_PM_STRINGS.put("in the evening", AmPm.PM);
		AM_PM_STRINGS.put("in the afternoon", AmPm.PM);
		
		// Strings describing the hour
		HOUR_TO_INT.put("noon", 12);
		HOUR_TO_INT.put("one", 1);
		HOUR_TO_INT.put("two", 2);
		HOUR_TO_INT.put("three", 3);
		HOUR_TO_INT.put("four", 4);
		HOUR_TO_INT.put("five", 5);
		HOUR_TO_INT.put("six", 6);
		HOUR_TO_INT.put("seven", 7);
		HOUR_TO_INT.put("eight", 8);
		HOUR_TO_INT.put("nine", 9);
		HOUR_TO_INT.put("ten", 10);
		HOUR_TO_INT.put("eleven", 11);
		HOUR_TO_INT.put("twelve", 12);
		HOUR_TO_INT.put("midnight", 0);
		
		// Unit digit
		UNIT_DIGIT.put("one", 1);
		UNIT_DIGIT.put("two", 2);
		UNIT_DIGIT.put("three", 3);
		UNIT_DIGIT.put("four", 4);
		UNIT_DIGIT.put("five", 5);
		UNIT_DIGIT.put("six", 6);
		UNIT_DIGIT.put("seven", 7);
		UNIT_DIGIT.put("eight", 8);
		UNIT_DIGIT.put("nine", 9);
		
		// Tens digit
		TENS_DIGIT.put("oh", 0);
		TENS_DIGIT.put("o '", 0);
		TENS_DIGIT.put("twenty", 20);
		TENS_DIGIT.put("thirty", 30);
		TENS_DIGIT.put("forty", 40);
		TENS_DIGIT.put("fifty", 50);
		
		// Teens
		TEENS.put("ten", 10);
		TEENS.put("eleven", 11);
		TEENS.put("twelve", 12);
		TEENS.put("thirteen", 13);
		TEENS.put("fourteen", 14);
		TEENS.put("fifteen", 15);
		TEENS.put("sixteen", 16);
		TEENS.put("seventeen", 17);
		TEENS.put("eighteen", 18);
		TEENS.put("nineteen", 19);
		
		// AM/PM defaults
		AMPM_DEFAULTS.put(0, AmPm.AM);
		AMPM_DEFAULTS.put(1, AmPm.PM);
		AMPM_DEFAULTS.put(2, AmPm.PM);
		AMPM_DEFAULTS.put(3, AmPm.PM);
		AMPM_DEFAULTS.put(4, AmPm.PM);
		AMPM_DEFAULTS.put(5, AmPm.PM);
		AMPM_DEFAULTS.put(6, AmPm.PM);
		AMPM_DEFAULTS.put(7, AmPm.AM);
		AMPM_DEFAULTS.put(8, AmPm.AM);
		AMPM_DEFAULTS.put(9, AmPm.AM);
		AMPM_DEFAULTS.put(10, AmPm.AM);
		AMPM_DEFAULTS.put(11, AmPm.AM);
		AMPM_DEFAULTS.put(12, AmPm.PM);
		
	}
	
	public Time parse(String text) {
		// Strip any periods
		final String stripped = text.replaceAll("\\.", "");
		
		final Matcher matcher = pattern.matcher(stripped);
		if (matcher.matches()) {
			int hour = -1;
			int minutes = 0;
			
			hour = HOUR_TO_INT.get(matcher.group("hour"));
			if (matcher.isCaptured("teens")) {
				minutes = TEENS.get(matcher.group("teens"));
			} else if (matcher.isCaptured("tens")) {
				minutes = TENS_DIGIT.get(matcher.group("tens"));
				if (matcher.isCaptured("units")) {
					minutes += UNIT_DIGIT.get(matcher.group("units"));
				}
			}
			
			if (matcher.isCaptured("ampm")) {
				// Case AM/PM tag captured
				if (AM_PM_STRINGS.get(matcher.group("ampm")) == AmPm.PM) {
					hour += 12;
				}
			} else {
				// No AM/PM tag specified
				if (AMPM_DEFAULTS.containsKey(hour)
						&& AMPM_DEFAULTS.get(hour) == AmPm.PM) {
					hour += 12;
				}
			}
			
			return new Time(hour, minutes);
		}
		
		return null;
	}
}
