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

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.base.time.Time;
import edu.cornell.cs.nlp.spf.base.time.TimeParser;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class TimeParserTest {
	
	@Test
	public void test() {
		
		final List<Pair<String, String>> samples = new LinkedList<Pair<String, String>>();
		
		samples.add(Pair.of("seven twenty seven", "07:27"));
		samples.add(Pair.of("eleven o'clock in the morning", "11:00"));
		samples.add(Pair.of("four thirty five o'clock", "16:35"));
		samples.add(Pair.of("eight twenty one a m", "08:21"));
		samples.add(Pair.of("eleven eleven a m", "11:11"));
		samples.add(Pair.of("four forty five p. m.", "16:45"));
		samples.add(Pair.of("six fifteen a. m.", "06:15"));
		samples.add(Pair.of("six twenty one a. m.", "06:21"));
		samples.add(Pair.of("twelve twenty p. m.", "24:20"));
		samples.add(Pair.of("ten thirty four a m", "10:34"));
		samples.add(Pair.of("ten fifty five a m", "10:55"));
		samples.add(Pair.of("seven forty a. m.", "07:40"));
		samples.add(Pair.of("eight fifty four a m", "08:54"));
		samples.add(Pair.of("four thirty five p. m.", "16:35"));
		samples.add(Pair.of("five thirteen p. m.", "17:13"));
		samples.add(Pair.of("twelve fifteen p. m.", "24:15"));
		samples.add(Pair.of("eight forty eight a. m.", "08:48"));
		samples.add(Pair.of("seven fifty eight a. m.", "07:58"));
		samples.add(Pair.of("seven fifteen a. m.", "07:15"));
		samples.add(Pair.of("one a. m.", "01:00"));
		samples.add(Pair.of("eleven forty a m", "11:40"));
		samples.add(Pair.of("eleven fifty five a. m.", "11:55"));
		samples.add(Pair.of("one eighteen p. m.", "13:18"));
		samples.add(Pair.of("seven ten a. m.", "07:10"));
		samples.add(Pair.of("seven thirteen a. m.", "07:13"));
		samples.add(Pair.of("eight thirty a. m.", "08:30"));
		samples.add(Pair.of("seven nineteen p. m.", "19:19"));
		samples.add(Pair.of("four p. m.", "16:00"));
		samples.add(Pair.of("ten eleven a. m.", "10:11"));
		samples.add(Pair.of("three fifteen p. m.", "15:15"));
		samples.add(Pair.of("seven p. m.", "19:00"));
		samples.add(Pair.of("two twenty one p. m.", "14:21"));
		samples.add(Pair.of("six twenty five a. m.", "06:25"));
		samples.add(Pair.of("six fifty a. m.", "06:50"));
		samples.add(Pair.of("five fifty a. m.", "05:50"));
		samples.add(Pair.of("seven thirty five a. m.", "07:35"));
		samples.add(Pair.of("five sixteen p. m.", "17:16"));
		samples.add(Pair.of("seven oh nine p. m.", "19:09"));
		samples.add(Pair.of("eight forty seven p. m.", "20:47"));
		samples.add(Pair.of("seven forty six a m", "07:46"));
		samples.add(Pair.of("four ten p. m.", "16:10"));
		samples.add(Pair.of("four twenty p. m.", "16:20"));
		samples.add(Pair.of("seven thirty a m", "07:30"));
		samples.add(Pair.of("five fifty two p. m.", "17:52"));
		samples.add(Pair.of("ten fifty seven p. m.", "22:57"));
		samples.add(Pair.of("nine forty seven p. m.", "21:47"));
		samples.add(Pair.of("five oh one p. m.", "17:01"));
		samples.add(Pair.of("three p. m.", "15:00"));
		samples.add(Pair.of("nine thirty eight p. m.", "21:38"));
		samples.add(Pair.of("one a m", "01:00"));
		samples.add(Pair.of("twelve thirty three p. m.", "24:33"));
		samples.add(Pair.of("eleven fifty a. m.", "11:50"));
		samples.add(Pair.of("five thirty seven p. m.", "17:37"));
		samples.add(Pair.of("twelve fourteen p. m.", "24:14"));
		samples.add(Pair.of("six a. m.", "06:00"));
		samples.add(Pair.of("eight twenty five a. m.", "08:25"));
		samples.add(Pair.of("nine oh five a. m.", "09:05"));
		samples.add(Pair.of("seven forty five a m", "07:45"));
		samples.add(Pair.of("three twenty five p. m.", "15:25"));
		samples.add(Pair.of("seven fifteen p. m.", "19:15"));
		samples.add(Pair.of("eight twenty eight p. m.", "20:28"));
		samples.add(Pair.of("twelve oh six p. m.", "24:06"));
		samples.add(Pair.of("six oh eight p. m.", "18:08"));
		samples.add(Pair.of("two ten p. m.", "14:10"));
		samples.add(Pair.of("eleven oh five a. m.", "11:05"));
		samples.add(Pair.of("six twenty p. m.", "18:20"));
		samples.add(Pair.of("three fifty p. m.", "15:50"));
		samples.add(Pair.of("seven forty five a. m.", "07:45"));
		samples.add(Pair.of("ten fifty five a. m.", "10:55"));
		samples.add(Pair.of("two thirty five p. m.", "14:35"));
		samples.add(Pair.of("three twenty eight p. m.", "15:28"));
		samples.add(Pair.of("eleven p. m.", "23:00"));
		samples.add(Pair.of("nine twenty five a. m.", "09:25"));
		samples.add(Pair.of("six thirty four p. m.", "18:34"));
		samples.add(Pair.of("eight fifty a. m.", "08:50"));
		samples.add(Pair.of("five fifty one p. m.", "17:51"));
		samples.add(Pair.of("eight fifteen a. m.", "08:15"));
		samples.add(Pair.of("six forty six p. m.", "18:46"));
		samples.add(Pair.of("eleven eleven a. m.", "11:11"));
		samples.add(Pair.of("one fifteen p. m.", "13:15"));
		samples.add(Pair.of("one forty five p. m.", "13:45"));
		samples.add(Pair.of("one oh two p. m.", "13:02"));
		samples.add(Pair.of("twelve thirty four p. m.", "24:34"));
		samples.add(Pair.of("three twenty four p. m.", "15:24"));
		samples.add(Pair.of("twelve thirty two p. m.", "24:32"));
		samples.add(Pair.of("eleven seventeen a m", "11:17"));
		samples.add(Pair.of("ten fifteen a. m.", "10:15"));
		samples.add(Pair.of("eight twenty p. m.", "20:20"));
		samples.add(Pair.of("five forty nine p. m.", "17:49"));
		samples.add(Pair.of("three oh eight p. m.", "15:08"));
		samples.add(Pair.of("four thirty nine p. m.", "16:39"));
		samples.add(Pair.of("one twenty three p. m.", "13:23"));
		samples.add(Pair.of("eleven thirty p. m.", "23:30"));
		samples.add(Pair.of("eleven forty eight a. m.", "11:48"));
		samples.add(Pair.of("twelve thirty four a. m.", "12:34"));
		samples.add(Pair.of("eight oh two p. m.", "20:02"));
		samples.add(Pair.of("eight twenty three a. m.", "08:23"));
		samples.add(Pair.of("two forty nine p. m.", "14:49"));
		samples.add(Pair.of("three thirty five p. m.", "15:35"));
		samples.add(Pair.of("nine twenty a. m.", "09:20"));
		samples.add(Pair.of("ten oh two", "10:02"));
		
		final TimeParser parser = new TimeParser();
		
		for (final Pair<String, String> pair : samples) {
			final Time time = parser.parse(pair.first());
			Assert.assertEquals(pair.second(), String.format("%02d:%02d",
					time.getHour(), time.getMinute()));
		}
		
	}
	
}
