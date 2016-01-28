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

/**
 * Represents a date.
 * 
 * @author Yoav Artzi
 */
public class Date {
	private final Month		month;
	private final Integer	monthDay;
	private final Day		weekDay;
	private final Integer	year;
	
	public Date(Month month, Integer monthDay, Day weekDay, Integer year) {
		this.month = month;
		this.monthDay = monthDay;
		this.weekDay = weekDay;
		this.year = year;
	}
	
	public Month getMonth() {
		return month;
	}
	
	public Integer getMonthDay() {
		return monthDay;
	}
	
	public Day getWeekDay() {
		return weekDay;
	}
	
	public Integer getYear() {
		return year;
	}
	
	@Override
	public String toString() {
		return "Date [month=" + month + ", monthDay=" + monthDay + ", weekDay="
				+ weekDay + ", year=" + year + "]";
	}
}
